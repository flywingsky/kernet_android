/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kercer.kernet.http;

import android.os.SystemClock;

import com.kercer.kercore.debug.KCLog;
import com.kercer.kernet.http.KCCache.KCEntry;
import com.kercer.kercore.util.KCUtilDate;
import com.kercer.kernet.http.base.KCHeader;
import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCHttpContent;
import com.kercer.kernet.http.base.KCHttpStatus;
import com.kercer.kernet.http.base.KCProtocolVersion;
import com.kercer.kernet.http.base.KCStatusLine;
import com.kercer.kernet.http.error.KCAuthFailureError;
import com.kercer.kernet.http.error.KCNetError;
import com.kercer.kernet.http.error.KCNetworkError;
import com.kercer.kernet.http.error.KCNoConnectionError;
import com.kercer.kernet.http.error.KCServerError;
import com.kercer.kernet.http.error.KCTimeoutError;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.Date;


/**
 * A network performing requests over an {@link KCHttpStack}.
 */
public class KCNetworkBasic implements KCNetwork
{
	protected static final boolean DEBUG = KCLog.DEBUG;

	private static int SLOW_REQUEST_THRESHOLD_MS = 3000;

	protected final KCHttpStack mHttpStack;


	/**
	 * @param httpStack
	 *            HTTP stack to be used
	 */
	public KCNetworkBasic(KCHttpStack httpStack)
	{
		mHttpStack = httpStack;
	}

	@Override
	public KCHttpResponse performRequest(KCHttpRequest<?> request, KCDeliveryResponse aDelivery) throws KCNetError
	{
		long requestStart = SystemClock.elapsedRealtime();
		while (true)
		{
			KCHttpResponse httpResponse = null;
			byte[] responseContents = null;
			KCHeaderGroup responseHeaders = KCHeaderGroup.emptyHeaderGroup();
			try
			{
				// Gather headers.
				KCHeaderGroup additionalHeaders = new KCHeaderGroup();
				addCacheHeaders(additionalHeaders, request.getCacheEntry());
				httpResponse = mHttpStack.performRequest(request, additionalHeaders, aDelivery);
				int statusCode = httpResponse.getStatusCode();

				responseHeaders = httpResponse.getHeaderGroup();
				// Handle cache validation.
				if (statusCode == KCHttpStatus.HTTP_NOT_MODIFIED)
				{

					KCEntry entry = request.getCacheEntry();
					if (entry == null)
					{
						httpResponse.setNotModified(true);
						httpResponse.setNetworkTimeMs(SystemClock.elapsedRealtime() - requestStart);
						return httpResponse;
					}

					// A HTTP 304 response does not have all header fields. We
					// have to use the header fields from the cache entry plus
					// the new ones from the response.
					// http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5
					entry.responseHeaders.addHeaders(responseHeaders.getAllHeaders());

					httpResponse.setNotModified(true);
					httpResponse.setNetworkTimeMs(SystemClock.elapsedRealtime() - requestStart);
					KCHttpContent httpEntity = httpResponse.getHttpContent();
					httpEntity.setContent(entry.data);
					httpResponse.setHeaders(entry.responseHeaders.getAllHeaders());
					return httpResponse;
				}

				responseContents = httpResponse.getContent();

				// Some responses such as 204s do not have content. We must check.
				if (responseContents == null)
				{
					// Add 0 byte response as a way of honestly representing a
					// no-content request.
					responseContents = new byte[0];
				}

				// if the request is slow, log it.
				long requestLifetime = SystemClock.elapsedRealtime() - requestStart;
				logSlowRequests(requestLifetime, request, responseContents, statusCode);

				if (statusCode < 200 || statusCode > 299)
				{
					throw new IOException();
				}

				httpResponse.setNotModified(false);
				httpResponse.setNetworkTimeMs(SystemClock.elapsedRealtime() - requestStart);
				KCHttpContent httpEntity = httpResponse.getHttpContent();
				httpEntity.setContent(responseContents);
				return httpResponse;
			}
			catch (SocketTimeoutException e)
			{
				attemptRetryOnException("socket", request, new KCTimeoutError());
			}
			catch (ConnectTimeoutException e)
			{
				attemptRetryOnException("connection", request, new KCTimeoutError());
			}
			catch (MalformedURLException e)
			{
				throw new RuntimeException("Bad URL " + request.getUrl(), e);
			}
			catch (IOException e)
			{
				int statusCode = 0;
				if (httpResponse != null)
				{
					statusCode = httpResponse.getStatusLine().getStatusCode();
				}
				else
				{
					throw new KCNoConnectionError(e);
				}
				KCLog.e("Unexpected response code %d for %s", statusCode, request.getUrl());

				KCHttpResponse networkResponse = null;

				if (responseContents != null)
				{
					networkResponse = ( (httpResponse != null) ? httpResponse : new KCHttpResponse(new KCStatusLine(new KCProtocolVersion("HTTP", 1, 1), statusCode, "Unexpected response")) );

					networkResponse.setStatusCode(statusCode);
					KCHttpContent httpEntity = networkResponse.getHttpContent();
					if (httpEntity == null)
					{
						httpEntity = new KCHttpContent();
					}
					httpEntity.setContent(responseContents);
					networkResponse.setContent(httpEntity);
					networkResponse.setHeaders(responseHeaders.getAllHeaders());
					networkResponse.setNotModified(false);
					networkResponse.setNetworkTimeMs(SystemClock.elapsedRealtime() - requestStart);


					if (statusCode == KCHttpStatus.HTTP_UNAUTHORIZED || statusCode == KCHttpStatus.HTTP_FORBIDDEN)
					{
						attemptRetryOnException("auth", request, new KCAuthFailureError(networkResponse));
					}
					else
					{
						// TODO: Only throw ServerError for 5xx status codes.
						throw new KCServerError(networkResponse);
					}
				}
				else
				{
					throw new KCNetworkError(networkResponse);
				}
			}
		}
	}

	/**
	 * Logs requests that took over SLOW_REQUEST_THRESHOLD_MS to complete.
	 */
	private void logSlowRequests(long requestLifetime, KCHttpRequest<?> request, byte[] responseContents, int statusCode)
	{
		if (DEBUG || requestLifetime > SLOW_REQUEST_THRESHOLD_MS)
		{
			KCLog.d("HTTP response for request=<%s> [lifetime=%d], [size=%s], " + "[rc=%d], [retryCount=%s]", request, requestLifetime,
					responseContents != null ? responseContents.length : "null", statusCode, request.getRetryPolicy().getCurrentRetryCount());
		}
	}

	/**
	 * Attempts to prepare the request for a retry. If there are no more attempts remaining in the request's retry policy, a timeout exception is
	 * thrown.
	 *
	 * @param request
	 *            The request to use.
	 */
	private static void attemptRetryOnException(String logPrefix, KCHttpRequest<?> request, KCNetError exception) throws KCNetError
	{
		KCRetryPolicy retryPolicy = request.getRetryPolicy();
		int oldTimeout = request.getTimeoutMs();

		try
		{
			retryPolicy.retry(exception);
		}
		catch (KCNetError e)
		{
			request.addMarker(String.format("%s-timeout-giveup [timeout=%s]", logPrefix, oldTimeout));
			throw e;
		}
		request.addMarker(String.format("%s-retry [timeout=%s]", logPrefix, oldTimeout));
	}

	private void addCacheHeaders(KCHeaderGroup headers, KCEntry entry)
	{
		// If there's no cache entry, we're done.
		if (entry == null)
		{
			return;
		}

		if (entry.etag != null)
		{
			headers.addHeader(new KCHeader("If-None-Match", entry.etag));
		}

		if (entry.lastModified > 0)
		{
			Date refTime = new Date(entry.lastModified);
			headers.addHeader(new KCHeader("If-Modified-Since", KCUtilDate.formatDate(refTime)));
		}
	}

	protected void logError(String what, String url, long start)
	{
		long now = SystemClock.elapsedRealtime();
		KCLog.v("HTTP ERROR(%s) %d ms to fetch %s", what, (now - start), url);
	}

}

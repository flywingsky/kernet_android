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

import android.net.TrafficStats;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.kercer.kernet.http.listener.KCHttpBaseListener;
import com.kercer.kernet.http.listener.KCHttpErrorListener;
import com.kercer.kernet.http.listener.KCHttpListener;
import com.kercer.kernet.http.listener.KCHttpListener.KCProgressListener;
import com.kercer.kernet.http.base.KCHeader;
import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCLog;
import com.kercer.kernet.http.base.KCStatusLine;
import com.kercer.kernet.http.error.KCAuthFailureError;
import com.kercer.kernet.http.error.KCNetError;
import com.kercer.kernet.http.error.KCTimeoutError;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Base class for all network requests.
 *
 * @param <T>
 *            The type of parsed response this request expects.
 */
public abstract class KCHttpRequest<T> implements Comparable<KCHttpRequest<T>>
{

	/**
	 * Default encoding for POST or PUT parameters. See {@link #getParamsEncoding()}.
	 */
	private static final String DEFAULT_PARAMS_ENCODING = "UTF-8";

	/**
	 * Supported request methods.
	 */
	public interface Method
	{
		int DEPRECATED_GET_OR_POST = -1;
		int GET = 0;
		int POST = 1;
		int PUT = 2;
		int DELETE = 3;
		int HEAD = 4;
		int OPTIONS = 5;
		int TRACE = 6;
		int PATCH = 7;
	}

	/** An event log tracing the lifetime of this request; for debugging. */
	private final KCLog.KCMarkerLog mEventLog = KCLog.KCMarkerLog.ENABLED ? new KCLog.KCMarkerLog() : null;

	/**
	 * Request method of this request. Currently supports GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, and PATCH.
	 */
	private final int mMethod;

	/** URL of this request. */
	private final String mUrl;

	/** Default tag for {@link TrafficStats}. */
	private final int mDefaultTrafficStatsTag;

	/** Listener interface */
	protected KCHttpBaseListener mHttpListener;
	protected KCHttpListener.KCProgressListener mProgressListener;

	/** Sequence number of this request, used to enforce FIFO ordering. */
	private Integer mSequence;

	/** The request queue this request is associated with. */
	private KCRequestQueue mRequestQueue;

	/** Whether or not responses to this request should be cached. */
	private boolean mShouldCache = true;

	/** Whether or not this request has been canceled. */
	private boolean mCanceled = false;

	/** Whether or not a response has been delivered for this request yet. */
	private boolean mResponseDelivered = false;

	/** The retry policy for this request. */
	private KCRetryPolicy mRetryPolicy;

	/**
	 * When a request can be retrieved from cache but must be refreshed from the network, the cache entry will be stored here so that in the event of
	 * a "Not Modified" response, we can be sure it hasn't been evicted from cache.
	 */
	private KCCache.KCEntry mCacheEntry = null;

	/** An opaque token tagging this request; used for bulk cancellation. */
	private Object mTag;

	private KCHeaderGroup mHeaders = KCHeaderGroup.emptyHeaderGroup();

	protected KCHttpResponseParser mResponseParser;


	/**
	 * Creates a new request with the given method (one of the values from {@link Method}), URL, and error listener. Note that the normal response
	 * listener is not provided here as delivery of responses is provided by subclasses, who have a better idea of how to deliver an already-parsed
	 * response.
	 */
	public KCHttpRequest(int method, String url, KCHttpBaseListener listener, KCHttpListener.KCProgressListener progressListener, KCRetryPolicy retryPolicy)
	{
		mMethod = method;
		mUrl = url;
		mHttpListener = listener;
		mProgressListener = progressListener;
		setRetryPolicy((retryPolicy == null) ? new KCRetryPolicyDefault() : retryPolicy);

		mDefaultTrafficStatsTag = findDefaultTrafficStatsTag(url);

	}

	public KCHttpRequest(int method, String url, KCHttpBaseListener listener)
	{
		this(method, url, listener, null, null);
	}

	public KCHttpRequest(int method, String url)
	{
		this(method, url, null, null, null);
	}

	/**
	 * Return the method for this request. Can be one of the values in {@link Method}.
	 * @return get method
	 */
	public int getMethod()
	{
		return mMethod;
	}

	/**
	 * Set a tag on this request. Can be used to cancel all requests with this tag by {@link KCRequestQueue#cancelAll(Object)}.
	 * @param tag set request tag
	 * @return This Request object to allow for chaining.
	 */
	public KCHttpRequest<?> setTag(Object tag)
	{
		mTag = tag;
		return this;
	}

	/**
	 * Returns this request's tag.
	 *
	 * @see KCHttpRequest#setTag(Object)
	 * @return request tag
	 */
	public Object getTag()
	{
		return mTag;
	}


	/**
	 * @return A tag for use with {@link TrafficStats#setThreadStatsTag(int)}
	 */
	public int getTrafficStatsTag()
	{
		return mDefaultTrafficStatsTag;
	}

	/**
	 * @return The hashcode of the URL's host component, or 0 if there is none.
	 */
	private static int findDefaultTrafficStatsTag(String url)
	{
		if (!TextUtils.isEmpty(url))
		{
			Uri uri = Uri.parse(url);
			if (uri != null)
			{
				String host = uri.getHost();
				if (host != null)
				{
					return host.hashCode();
				}
			}
		}
		return 0;
	}

	/**
	 * Sets the retry policy for this request.
	 * @param retryPolicy  retry policy
	 * @return This Request object to allow for chaining.
	 */
	public KCHttpRequest<?> setRetryPolicy(KCRetryPolicy retryPolicy)
	{
		mRetryPolicy = retryPolicy;
		return this;
	}

	/**
	 * Adds an event to this request's event log; for debugging.
	 *  @param tag tag
	 */
	public void addMarker(String tag)
	{
		if (KCLog.KCMarkerLog.ENABLED)
		{
			mEventLog.add(tag, Thread.currentThread().getId());
		}
	}

	/**
	 * Notifies the request queue that this request has finished (successfully or with error).
	 *
	 * <p>
	 * Also dumps all events from this request's event log; for debugging.
	 * </p>
	 */
	void finish(final String tag)
	{
		if (mRequestQueue != null)
		{
			mRequestQueue.finish(this);
		}
		if (KCLog.KCMarkerLog.ENABLED)
		{
			final long threadId = Thread.currentThread().getId();
			if (Looper.myLooper() != Looper.getMainLooper())
			{
				// If we finish marking off of the main thread, we need to
				// actually do it on the main thread to ensure correct ordering.
				Handler mainThread = new Handler(Looper.getMainLooper());
				mainThread.post(new Runnable()
				{
					@Override
					public void run()
					{
						mEventLog.add(tag, threadId);
						mEventLog.finish(this.toString());
					}
				});
				return;
			}

			mEventLog.add(tag, threadId);
			mEventLog.finish(this.toString());
		}
	}

	/**
	 * Associates this request with the given queue. The request queue will be notified when this request has finished.
	 * @param requestQueue  request queue
	 * @return This Request object to allow for chaining.
	 */
	public KCHttpRequest<?> setRequestQueue(KCRequestQueue requestQueue)
	{
		mRequestQueue = requestQueue;
		return this;
	}

	/**
	 * Sets the sequence number of this request. Used by {@link KCRequestQueue}.
	 *
	 * @param sequence sequence
	 * @return This Request object to allow for chaining.
	 */
	public final KCHttpRequest<?> setSequence(int sequence)
	{
		mSequence = sequence;
		return this;
	}

	/**
	 * Returns the sequence number of this request.
	 *
	 * @return sequence
	 */
	public final int getSequence()
	{
		if (mSequence == null)
		{
			throw new IllegalStateException("getSequence called before setSequence");
		}
		return mSequence;
	}

	/**
	 * Returns the URL of this request.
	 * @return url
	 */
	public String getUrl()
	{
		return mUrl;
	}

	/**
	 * Returns the cache key for this request. By default, this is the URL.
	 * @return cache key
	 */
	public String getCacheKey()
	{
		return getUrl();
	}

	/**
	 * Annotates this request with an entry retrieved for it from cache. Used for cache coherency support.
	 *
	 * @param entry cache entry
	 * @return This Request object to allow for chaining.
	 */
	public KCHttpRequest<?> setCacheEntry(KCCache.KCEntry entry)
	{
		mCacheEntry = entry;
		return this;
	}

	/**
	 * Returns the annotated cache entry, or null if there isn't one.
	 *
	 * @return cache entry
	 */
	public KCCache.KCEntry getCacheEntry()
	{
		return mCacheEntry;
	}

	/**
	 * Mark this request as canceled. No callback will be delivered.
	 */
	public void cancel()
	{
		mCanceled = true;
	}

	/**
	 * Returns true if this request has been canceled.
	 *
	 * @return is canceled
	 */
	public boolean isCanceled()
	{
		return mCanceled;
	}

	/**
	 * Returns a list of extra HTTP headers to go along with this request. Can throw {@link KCAuthFailureError} as authentication may be required to
	 * provide these values.
	 *
	 * @return Header Group
	 * @throws KCAuthFailureError
	 *             In the event of auth failure
	 */
	public KCHeaderGroup getHeaders() throws KCAuthFailureError
	{
		return mHeaders;
	}


	public void addHeader(KCHeader aHeader)
	{
		if (aHeader != null)
			mHeaders.addHeader(aHeader);
	}


	/**
	 * Returns a Map of parameters to be used for a POST or PUT request. Can throw {@link KCAuthFailureError} as authentication may be required to
	 * provide these values.
	 *
	 * <p>
	 * Note that you can directly override {@link #getBody()} for custom data.
	 * </p>
	 *
	 * @return params map
	 * @throws KCAuthFailureError
	 *             in the event of auth failure
	 */
	protected Map<String, String> getParams() throws KCAuthFailureError
	{
		return null;
	}

	/**
	 * Returns which encoding should be used when converting POST or PUT parameters returned by {@link #getParams()} into a raw POST or PUT body.
	 *
	 * <p>
	 * This controls both encodings:
	 * <ol>
	 * <li>The string encoding used when converting parameter names and values into bytes prior to URL encoding them.</li>
	 * <li>The string encoding used when converting the URL encoded parameters into a raw byte array.</li>
	 * </ol>
	 *
	 * @return parame encoding
	 */
	protected String getParamsEncoding()
	{
		return DEFAULT_PARAMS_ENCODING;
	}

	/**
	 * Returns the content type of the POST or PUT body.
	 * @return body content type
	 */
	public String getBodyContentType()
	{
		return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
	}

	/**
	 * Returns the raw POST or PUT body to be sent.
	 *
	 * <p>
	 * By default, the body consists of the request parameters in application/x-www-form-urlencoded format. When overriding this method, consider
	 * overriding {@link #getBodyContentType()} as well to match the new body format.
	 *
	 * @return body bytes
	 * @throws KCAuthFailureError
	 *             in the event of auth failure
	 */
	public byte[] getBody() throws KCAuthFailureError
	{
		Map<String, String> params = getParams();
		if (params != null && params.size() > 0)
		{
			return encodeParameters(params, getParamsEncoding());
		}
		return null;
	}

	/**
	 * Converts <code>params</code> into an application/x-www-form-urlencoded encoded string.
	 */
	private byte[] encodeParameters(Map<String, String> params, String paramsEncoding)
	{
		StringBuilder encodedParams = new StringBuilder();
		try
		{
			for (Map.Entry<String, String> entry : params.entrySet())
			{
				encodedParams.append(URLEncoder.encode(entry.getKey(), paramsEncoding));
				encodedParams.append('=');
				encodedParams.append(URLEncoder.encode(entry.getValue(), paramsEncoding));
				encodedParams.append('&');
			}
			return encodedParams.toString().getBytes(paramsEncoding);
		}
		catch (UnsupportedEncodingException uee)
		{
			throw new RuntimeException("Encoding not supported: " + paramsEncoding, uee);
		}
	}

	/**
	 * Set whether or not responses to this request should be cached.
	 * @param shouldCache should cache
	 * @return This Request object to allow for chaining.
	 */
	public final KCHttpRequest<?> setShouldCache(boolean shouldCache)
	{
		mShouldCache = shouldCache;
		return this;
	}

	/**
	 * Returns true if responses to this request should be cached.
	 *
	 * @return should cache
	 */
	public final boolean shouldCache()
	{
		return mShouldCache;
	}

	/**
	 * Priority values. Requests will be processed from higher priorities to lower priorities, in FIFO order.
	 */
	public enum Priority
	{
		LOW, NORMAL, HIGH, IMMEDIATE
	}

	/**
	 * Returns the {@link Priority} of this request; {@link Priority#NORMAL} by default.
	 *
	 * @return priority
	 */
	public Priority getPriority()
	{
		return Priority.NORMAL;
	}

	/**
	 * @return  Returns the socket timeout in milliseconds per retry attempt. (This value can be changed per retry attempt if a backoff is specified via
	 * backoffTimeout()). If there are no retry attempts remaining, this will cause delivery of a {@link KCTimeoutError} error.
	 */
	public final int getTimeoutMs()
	{
		return mRetryPolicy.getCurrentTimeout();
	}

	/**
	 * @return Returns the retry policy that should be used for this request.
	 */
	public KCRetryPolicy getRetryPolicy()
	{
		return mRetryPolicy;
	}

	/**
	 * Mark this request as having a response delivered on it. This can be used later in the request's lifetime for suppressing identical responses.
	 */
	public void markDelivered()
	{
		mResponseDelivered = true;
	}

	/**
	 * @return Returns true if this request has had a response delivered for it.
	 */
	public boolean hasHadResponseDelivered()
	{
		return mResponseDelivered;
	}


	public void setResponseParser(KCHttpResponseParser aResponseParser)
	{
		if(aResponseParser != null)
			mResponseParser = aResponseParser;
	}

	public KCHttpResponseParser getResponseParser()
	{
		return mResponseParser;
	}


	/**
	 * Receive response headers, callback header group
	 * @param aHeaderGroup headers
	 */
	protected void notifyHeaders(KCStatusLine aStatusLine, KCHeaderGroup aHeaderGroup)
	{
		if (mHttpListener != null)
		{
			if (mHttpListener instanceof KCHttpListener)
			{
				KCHttpListener listener = (KCHttpListener)mHttpListener;
				listener.onResponseHeaders(aStatusLine, aHeaderGroup);
			}
		}
	}

	/**
	 * Subclasses must implement this to perform delivery of the parsed response to their listeners. The given response is guaranteed to be non-null;
	 * responses that fail to parse are not delivered.
	 *
	 * @param aResponse
	 *            The parsed response returned
	 * @param aResult
	 *            result
	 */
	protected void notifyResponse(KCHttpResponse aResponse, T aResult)
	{
		if (mHttpListener != null)
		{
			if (mHttpListener instanceof KCHttpListener)
			{
				KCHttpListener listener = (KCHttpListener) mHttpListener;
				listener.onHttpComplete(this, aResponse);
			}
		}
	}

	/**
	 * notify error message to the ErrorListener that the Request was initialized with.
	 *
	 * @param error
	 *            Error details
	 */
	protected void notifyError(KCNetError error)
	{
		if (mHttpListener != null)
		{
			if (mHttpListener instanceof KCHttpErrorListener)
			{
				KCHttpErrorListener listener = (KCHttpErrorListener) mHttpListener;
				listener.onHttpError(error);
			}
		}
	}


	public void setListener(KCHttpListener aListener)
	{
		mHttpListener = aListener;
	}

	/**
	 * @return this request's {@link KCHttpListener}.
	 */
	public KCHttpBaseListener getListener()
	{
		return mHttpListener;
	}

	/**
	 * Set listener for tracking progress
	 *
	 * @param listener listener
	 */
	public void setProgressListener(KCProgressListener listener)
	{
		mProgressListener = listener;
	}

	/**
	 * Our comparator sorts from high to low priority, and secondarily by sequence number to provide FIFO ordering.
	 */
	@Override
	public int compareTo(KCHttpRequest<T> other)
	{
		Priority left = this.getPriority();
		Priority right = other.getPriority();

		// High-priority requests are "lesser" so they are sorted to the front.
		// Equal priorities are sorted by sequence number to provide FIFO ordering.
		return left == right ? this.mSequence - other.mSequence : right.ordinal() - left.ordinal();
	}

	@Override
	public String toString()
	{
		String trafficStatsTag = "0x" + Integer.toHexString(getTrafficStatsTag());
		return (mCanceled ? "[X] " : "[ ] ") + getUrl() + " " + trafficStatsTag + " " + getPriority() + " " + mSequence;
	}
}

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

package com.kercer.kernet.http.request;

import com.kercer.kernet.http.KCHttpHeaderParser;
import com.kercer.kernet.http.KCHttpListener;
import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.KCHttpResponse;
import com.kercer.kernet.http.KCHttpResponseParser;
import com.kercer.kernet.http.KCHttpResult;
import com.kercer.kernet.http.KCHttpResult.KCHttpResultListener;
import com.kercer.kernet.http.error.KCNetError;

import java.io.UnsupportedEncodingException;

/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class KCStringRequest extends KCHttpRequest<String>
{
	private final KCHttpResultListener<String> mListener;

	/**
	 * Creates a new request with the given method.
	 *
	 * @param method
	 *            the request {@link Method} to use
	 * @param aUrl
	 *            URL to fetch the string at
	 * @param aResultListener
	 *            Listener to receive the String response
	 * @param aListener
	 *            listener, or null to ignore errors
	 */
	public KCStringRequest(int method, String aUrl, KCHttpResultListener<String> aResultListener, KCHttpListener aListener)
	{
		super(method, aUrl, aListener);
		mListener = aResultListener;
		parserResponse();
	}

	/**
	 * Creates a new GET request.
	 *
	 * @param aUrl
	 *            URL to fetch the string at
	 * @param aResultListener
	 *            Listener to receive the String response
	 * @param aListener
	 *            listener, or null to ignore errors
	 */
	public KCStringRequest(String aUrl, KCHttpResultListener<String> aResultListener, KCHttpListener aListener)
	{
		this(Method.GET, aUrl, aResultListener, aListener);
	}

	@Override
	protected void notifyResponse(KCHttpResponse aResponse, String aResult)
	{
		super.notifyResponse(aResponse, aResult);
		mListener.onHttpResult(aResult);
	}

    private void parserResponse()
    {
    	this.setResponseParser(new KCHttpResponseParser()
		{

			@Override
			public KCHttpResult<String> parseHttpResponse(KCHttpResponse response)
			{
				String parsed;
				try
				{
					parsed = new String(response.getContent(), KCHttpHeaderParser.parseCharset(response.getHeaderGroup()));
				}
				catch (UnsupportedEncodingException e)
				{
					parsed = new String(response.getContent());
				}
				return KCHttpResult.success(parsed, KCHttpHeaderParser.parseCacheHeaders(response));
			}

			@Override
			public KCNetError parseHttpError(KCNetError aError)
			{
				return aError;
			}
		});
    }
}

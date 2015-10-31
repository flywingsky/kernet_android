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
import com.kercer.kernet.http.KCHttpResponse;
import com.kercer.kernet.http.KCHttpResponseParser;
import com.kercer.kernet.http.KCHttpResult;
import com.kercer.kernet.http.KCHttpResult.KCHttpResultListener;
import com.kercer.kernet.http.error.KCNetError;
import com.kercer.kernet.http.error.KCParseError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * A request for retrieving a {@link JSONObject} response body at a given URL, allowing for an optional {@link JSONObject} to be passed in as part of
 * the request body.
 */
public class KCJsonObjectRequest extends KCJsonRequest<JSONObject>
{

	/**
	 * Creates a new request.
	 *
	 * @param method
	 *            the HTTP method to use
	 * @param url
	 *            URL to fetch the JSON from
	 * @param jsonRequest
	 *            A {@link JSONObject} to post with the request. Null is allowed and indicates no parameters will be posted along with request.
	 * @param listener
	 *            Listener to receive the JSON response
	 * @param errorListener
	 *            Error listener, or null to ignore errors.
	 */
	public KCJsonObjectRequest(int method, String url, JSONObject jsonRequest, KCHttpResultListener<JSONObject> listener, KCHttpListener errorListener)
	{
		super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(), listener, errorListener);
		parserResponse();
	}

	/**
	 * Constructor which defaults to <code>GET</code> if <code>jsonRequest</code> is <code>null</code>, <code>POST</code> otherwise.
	 *
	 * @param url url
	 * @param jsonRequest json request
	 * @param listener listener
	 * @param errorListener listener
	 */
	public KCJsonObjectRequest(String url, JSONObject jsonRequest, KCHttpResultListener<JSONObject> listener, KCHttpListener errorListener)
	{
		this(jsonRequest == null ? Method.GET : Method.POST, url, jsonRequest, listener, errorListener);
	}

    private void parserResponse()
    {
    	this.setResponseParser(new KCHttpResponseParser()
		{

			@Override
			public  KCHttpResult<JSONObject> parseHttpResponse(KCHttpResponse response)
			{
				try
				{
					String jsonString = new String(response.getContent(), KCHttpHeaderParser.parseCharset(response.getHeaderGroup(), PROTOCOL_CHARSET));
					return KCHttpResult.success(new JSONObject(jsonString), KCHttpHeaderParser.parseCacheHeaders(response));
				}
				catch (UnsupportedEncodingException e)
				{
					return KCHttpResult.error(new KCParseError(e));
				}
				catch (JSONException je)
				{
					return KCHttpResult.error(new KCParseError(je));
				}
			}

			@Override
			public KCNetError parseHttpError(KCNetError aError)
			{
				return aError;
			}
		});
    }
}

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

import android.text.TextUtils;

import com.kercer.kernet.http.listener.KCHttpBaseListener;
import com.kercer.kernet.http.KCHttpHeaderParser;
import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.KCHttpResponse;
import com.kercer.kernet.http.KCHttpResponseParser;
import com.kercer.kernet.http.KCHttpResult;
import com.kercer.kernet.http.error.KCNetError;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class KCSimpleDownloadRequest extends KCHttpRequest<String>
{
	private final String mDownloadPath;

	/**
	 * Creates a new request with the given method.
	 *
	 * @param url
	 *            URL to fetch the string at
	 * @param download_path
	 *            path to save the file to
	 * @param aListener
	 *            Listener to receive the String response
	 */
	public KCSimpleDownloadRequest(String url, String download_path, KCHttpBaseListener aListener)
	{
		super(Method.GET, url, aListener);
		mDownloadPath = download_path;
		setResponseParser(new KCHttpResponseParser()
		{

			@Override
			public KCHttpResult<?> parseHttpResponse(KCHttpResponse response)
			{
				String parsed = null;
				try
				{
					byte[] data = response.getContent();
					// convert array of bytes into file
					FileOutputStream fileOuputStream = new FileOutputStream(mDownloadPath);
					fileOuputStream.write(data);
					fileOuputStream.close();
					parsed = mDownloadPath;
				}
				catch (UnsupportedEncodingException e)
				{
					parsed = new String(response.getContent());
				}
				catch (FileNotFoundException e)
				{
					e.printStackTrace();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				finally
				{
					if (TextUtils.isEmpty(parsed))
					{
						parsed = "";
					}
				}

				return KCHttpResult.success(parsed, KCHttpHeaderParser.parseCacheHeaders(response));
			}

			@Override
			public KCNetError parseHttpError(KCNetError aError)
			{
				// TODO Auto-generated method stub
				return null;
			}
		});
	}

}

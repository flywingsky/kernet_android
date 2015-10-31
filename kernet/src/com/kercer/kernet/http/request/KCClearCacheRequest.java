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

import com.kercer.kernet.http.KCCache;
import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.KCHttpResponse;
import com.kercer.kernet.http.KCHttpResponseParser;
import com.kercer.kernet.http.KCHttpResult;
import com.kercer.kernet.http.error.KCNetError;

import android.os.Handler;
import android.os.Looper;

/**
 * A synthetic request used for clearing the cache.
 */
public class KCClearCacheRequest extends KCHttpRequest<Object>
{
	private final KCCache mCache;
	private final Runnable mCallback;

	/**
	 * Creates a synthetic request for clearing the cache.
	 * 
	 * @param cache
	 *            Cache to clear
	 * @param callback
	 *            Callback to make on the main thread once the cache is clear, or null for none
	 */
	public KCClearCacheRequest(KCCache cache, Runnable callback)
	{
		super(Method.GET, null, null);
		mCache = cache;
		mCallback = callback;
		parserResponse();
	}

	@Override
	public boolean isCanceled()
	{
		// This is a little bit of a hack, but hey, why not.
		mCache.clear();
		if (mCallback != null)
		{
			Handler handler = new Handler(Looper.getMainLooper());
			handler.postAtFrontOfQueue(mCallback);
		}
		return true;
	}

	@Override
	public Priority getPriority()
	{
		return Priority.IMMEDIATE;
	}

	private void parserResponse()
	{
		this.setResponseParser(new KCHttpResponseParser()
		{

			@Override
			public KCHttpResult<?> parseHttpResponse(KCHttpResponse response)
			{
				return null;
			}

			@Override
			public KCNetError parseHttpError(KCNetError aError)
			{
				return aError;
			}
		});
	}

	@Override
	protected void notifyResponse(KCHttpResponse aResponse, Object aResult)
	{
	}
}

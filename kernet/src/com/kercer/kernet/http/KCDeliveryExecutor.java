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

import android.os.Handler;

import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCStatusLine;
import com.kercer.kernet.http.error.KCNetError;

import java.util.concurrent.Executor;

/**
 * Delivers responses and errors.
 */
public class KCDeliveryExecutor implements KCDelivery
{
	/** Used for posting responses, typically to the main thread. */
	private final Executor mExecutor;

	/**
	 * Creates a new response delivery interface.
	 * 
	 * @param handler
	 *            {@link Handler} to post responses on
	 */
	public KCDeliveryExecutor(final Handler handler)
	{
		// Make an Executor that just wraps the handler.
		mExecutor = new Executor()
		{
			@Override
			public void execute(Runnable command)
			{
				handler.post(command);
			}
		};
	}

	/**
	 * Creates a new response delivery interface, mockable version for testing.
	 * 
	 * @param executor
	 *            For running delivery tasks
	 */
	public KCDeliveryExecutor(Executor executor)
	{
		mExecutor = executor;
	}

	@Override
	public void postHeaders(final KCHttpRequest<?> aRequest,final KCStatusLine aStatusLine,final KCHeaderGroup aHeaderGroup)
	{
		mExecutor.execute(new Runnable() {
			@Override
			public void run() {
				aRequest.notifyHeaders(aStatusLine, aHeaderGroup);
			}
		});

	}

	@Override
	public void postResponse(KCHttpRequest<?> aRequest, KCHttpResponse aResponse, KCHttpResult<?> aResult)
	{
		postResponse(aRequest,aResponse, aResult, null);
	}

	@Override
	public void postResponse(KCHttpRequest<?> aRequest, KCHttpResponse aResponse, KCHttpResult<?> aResult, Runnable aRunnable)
	{
		aRequest.markDelivered();
		aRequest.addMarker("post-response");
		mExecutor.execute(new ResponseDeliveryRunnable(aRequest, aResponse, aResult, aRunnable));
	}

	@Override
	public void postError(KCHttpRequest<?> aRequest, KCNetError aError)
	{
		aRequest.addMarker("post-error");
		KCHttpResult<?> result = KCHttpResult.error(aError);
		mExecutor.execute(new ResponseDeliveryRunnable(aRequest, null, result, null));
	}

	/**
	 * A Runnable used for delivering network responses to a listener on the main thread.
	 */
	@SuppressWarnings("rawtypes")
	private class ResponseDeliveryRunnable implements Runnable
	{
		private final KCHttpRequest mRequest;
		private final KCHttpResponse mResponse;
		private final KCHttpResult mResult;
		private final Runnable mRunnable;
		

		public ResponseDeliveryRunnable(KCHttpRequest aRequest, KCHttpResponse aResponse, KCHttpResult aResult, Runnable aRunnable)
		{
			mRequest = aRequest;
			mResponse = aResponse;
			mResult = aResult;
			mRunnable = aRunnable;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run()
		{
			// If this request has canceled, finish it and don't deliver.
			if (mRequest.isCanceled())
			{
				mRequest.finish("canceled-at-delivery");
				return;
			}

			// Deliver a normal response or error, depending.
			if (mResult.isSuccess())
			{
				mRequest.notifyResponse(mResponse, mResult.result);
			}
			else
			{
				mRequest.notifyError(mResult.error);
			}

			// If this is an intermediate response, add a marker, otherwise we're done
			// and the request can be finished.
			if (mResult.intermediate)
			{
				mRequest.addMarker("intermediate-response");
			}
			else
			{
				mRequest.finish("done");
			}

			// If we have been provided a post-delivery runnable, run it.
			if (mRunnable != null)
			{
				mRunnable.run();
			}
		}
	}
}

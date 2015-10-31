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

import android.os.Process;

import com.kercer.kernet.http.base.KCLog;

import java.util.concurrent.BlockingQueue;

/**
 * Provides a thread for performing cache triage on a queue of requests.
 *
 * Requests added to the specified cache queue are resolved from cache. Any deliverable response is posted back to the caller via a
 * {@link KCDeliveryResponse}. Cache misses and responses that require refresh are enqueued on the specified network queue for processing by a
 * {@link KCNetworkThread}.
 */
public class KCCacheThread extends Thread
{

	private static final boolean DEBUG = KCLog.DEBUG;

	/** The queue of requests coming in for triage. */
	private final BlockingQueue<KCHttpRequest<?>> mCacheQueue;

	private KCCacheRunner mCacheRunner;

	/** Used for telling us to die. */
	private volatile boolean mQuit = false;

	/**
	 * Creates a new cache triage dispatcher thread. You must call {@link #start()} in order to begin processing.
	 *
	 * @param cacheQueue
	 *            Queue of incoming requests for triage
	 * @param aNetworkQueue
	 *            Queue to post requests that require network to
	 * @param aCache
	 *            Cache interface to use for resolution
	 * @param aDelivery
	 *            Delivery interface to use for posting responses
	 */
	public KCCacheThread(BlockingQueue<KCHttpRequest<?>> cacheQueue, BlockingQueue<KCHttpRequest<?>> aNetworkQueue, KCCache aCache, KCDeliveryResponse aDelivery)
	{
		mCacheQueue = cacheQueue;
		mCacheRunner = new KCCacheRunner(aNetworkQueue, aCache, aDelivery);
	}

	/**
	 * Forces this dispatcher to quit immediately. If any requests are still in the queue, they are not guaranteed to be processed.
	 */
	public void quit()
	{
		mQuit = true;
		interrupt();
	}

	@Override
	public void run()
	{
		if (DEBUG)
			KCLog.v("start new dispatcher");
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

		mCacheRunner.initializeCache();

		while (true)
		{
			try
			{
				// Get a request from the cache triage queue, blocking until
				// at least one is available.
				final KCHttpRequest<?> request = mCacheQueue.take();
				request.addMarker("cache-queue-take");

				if (!mCacheRunner.start(request))
					continue;
			}
			catch (InterruptedException e)
			{
				// We may have been interrupted because it was time to quit.
				if (mQuit)
				{
					return;
				}
				continue;
			}
		}
	}
}

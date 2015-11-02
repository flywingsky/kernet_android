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

import java.util.concurrent.BlockingQueue;

/**
 * Provides a thread for performing network dispatch from a queue of requests.
 *
 * Requests added to the specified queue are processed from the network via a specified {@link KCNetwork} interface. Responses are committed to cache,
 * if eligible, using a specified {@link KCCache} interface. Valid responses and errors are posted back to the caller via a {@link KCDelivery}.
 */
public class KCNetworkThread extends Thread
{
	/** The queue of requests to service. */
	private final BlockingQueue<KCHttpRequest<?>> mQueue;

	/** Used for telling us to die. */
	private volatile boolean mQuit = false;

	private KCRequestRunner mRequestRunner;

	/**
	 * Creates a new network dispatcher thread. You must call {@link #start()} in order to begin processing.
	 *
	 * @param queue
	 *            Queue of incoming requests for triage
	 * @param network
	 *            Network interface to use for performing requests
	 * @param cache
	 *            Cache interface to use for writing responses to cache
	 * @param delivery
	 *            Delivery interface to use for posting responses
	 */
	public KCNetworkThread(BlockingQueue<KCHttpRequest<?>> queue, KCNetwork network, KCCache cache, KCDelivery delivery)
	{
		mQueue = queue;

		mRequestRunner = new KCRequestRunner(cache, network, delivery);
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
		Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
		while (true)
		{
			KCHttpRequest<?> request;
			try
			{
				// Take a request from the queue.
				request = mQueue.take();
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

			KCHttpResponse httpResponse = mRequestRunner.start(request);
			if (httpResponse == null)
			{
				continue;
			}
		}
	}

}

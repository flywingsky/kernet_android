/*
 * Copyright (C) 2012 The Android Open Source Project
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

import android.content.Context;

import java.io.File;

public class KerNet
{

	/** Default on-disk cache directory. */
	private static final String DEFAULT_CACHE_DIR = "kernet";


	private static KCNetwork newNetwork(KCHttpStack aStack)
	{

		if (aStack == null)
		{
			aStack = new KCHttpStackDefault();
		}

		KCNetwork network = new KCNetworkBasic(aStack);
		return  network;
	}

	private static KCCache newCache(Context aContext)
	{
		File cacheDir = new File(aContext.getCacheDir(), DEFAULT_CACHE_DIR);
		KCCache cache = new KCCacheDisk(cacheDir);
		return cache;
	}

	/**
	 * Creates a default instance of the worker pool and calls {@link KCRequestQueue#start()} on it.
	 *
	 * @param aContext
	 *            A {@link Context} to use for creating the cache dir.
	 * @param aStack
	 *            An {@link KCHttpStack} to use for the network, or null for default.
	 * @return A started {@link KCRequestQueue} instance.
	 */
	public static KCRequestQueue newRequestQueue(Context aContext, KCHttpStack aStack)
	{
		KCRequestQueue queue = new KCRequestQueue(newCache(aContext), newNetwork(aStack));
		queue.start();

		return queue;
	}

	/**
	 * Creates a default instance of the worker pool and calls {@link KCRequestQueue#start()} on it.
	 *
	 * @param aContext
	 *            A {@link Context} to use for creating the cache dir.
	 * @return A started {@link KCRequestQueue} instance.
	 */
	public static KCRequestQueue newRequestQueue(Context aContext)
	{
		return newRequestQueue(aContext, null);
	}



	public static KCRequestRunner newRequestRunner(Context aContext, KCHttpStack aStack)
	{
		KCRequestRunner requestRunner = new KCRequestRunner(newCache(aContext), newNetwork(aStack));
		return requestRunner;
	}

	public static KCRequestRunner newRequestRunner(Context aContext)
	{
		return newRequestRunner(aContext, null);
	}

}

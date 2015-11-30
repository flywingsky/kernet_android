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

package com.kercer.kernet;

import android.content.Context;

import com.kercer.kernet.download.KCDownloadEngine;
import com.kercer.kernet.http.KCCache;
import com.kercer.kernet.http.KCCacheDisk;
import com.kercer.kernet.http.KCHttpStack;
import com.kercer.kernet.http.KCHttpStackDefault;
import com.kercer.kernet.http.KCNetwork;
import com.kercer.kernet.http.KCNetworkBasic;
import com.kercer.kernet.http.KCRequestQueue;
import com.kercer.kernet.http.KCRequestRunner;

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
		KCCache cache = null;
		if (aContext != null)
		{
			File cacheDir = new File(aContext.getCacheDir(), DEFAULT_CACHE_DIR);
			cache = new KCCacheDisk(cacheDir);
		}
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

	/**
	 * Creates a default instance of KCRequestRunner and calls startAsyn on it
	 * @param aContext
	 * 				A {@link Context} to use for creating the cache dir.
	 * 				if null, not use cache, else use cache of Default on-disk cache directory
	 * @param aStack
	 * 				An {@link KCHttpStack} to use for the network, or null for default.
	 * @return	A Request Runner
	 */
	public static KCRequestRunner newRequestRunner(Context aContext, KCHttpStack aStack)
	{
		KCRequestRunner requestRunner = new KCRequestRunner(newCache(aContext), newNetwork(aStack));
		return requestRunner;
	}

	/**
	 * Creates a default instance of KCRequestRunner and calls startAsyn on it
	 * @param aContext
	 * 				A {@link Context} to use for creating the cache dir.
	 * 				if null, not use cache, else use cache of Default on-disk cache directory
	 * @return A Request Runner
	 */
	public static KCRequestRunner newRequestRunner(Context aContext)
	{
		return newRequestRunner(aContext, null);
	}

	/**
	 * Creates a default instace of KCDownloadEngine
	 * @param aUserAgent user agent
	 * @param aMaxConn max conn
	 * @return A KCDownloadEngine
	 */
	public static KCDownloadEngine newDownloadEngine(final String aUserAgent, final int aMaxConn)
	{
		return  new KCDownloadEngine(aUserAgent, aMaxConn);
	}

}

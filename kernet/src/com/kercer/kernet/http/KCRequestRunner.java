package com.kercer.kernet.http;

import android.annotation.TargetApi;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.kercer.kernet.http.base.KCProtocolVersion;
import com.kercer.kernet.http.base.KCStatusLine;
import com.kercer.kernet.http.error.KCNetError;
import com.kercer.kernet.task.KCTaskExecutor;

/**
 * A request tickle for single requests.
 *
 * Calling {@link KCHttpRequest} will add the request to request runner and {@link #start(KCHttpRequest)},
 * will give a {@link KCHttpResponse}. The listeners in the request will also be notified.
 */
public class KCRequestRunner
{

	private KCHttpRequest<?> mRequest;

	/** Cache interface for retrieving and storing response. */
	private final KCCache mCache;

	/** Network interface for performing requests. */
	private final KCNetwork mNetwork;

	/** Response delivery mechanism. */
	private final KCDeliveryResponse mDelivery;

	// private KCHttpResult<?> response;
//	private NetError error;

	/**
	 * Creates the worker pool. Processing will not begin until {@link #start(KCHttpRequest)} is called.
	 *
	 * @param cache
	 *            A Cache to use for persisting responses to disk
	 * @param network
	 *            A Network interface for performing HTTP requests
	 * @param delivery
	 *            A ResponseDelivery interface for posting responses and errors
	 */
	public KCRequestRunner(KCCache cache, KCNetwork network, KCDeliveryResponse delivery)
	{
		mCache = cache;
		mNetwork = network;
		mDelivery = delivery;
	}

	/**
	 * Creates the worker pool. Processing will not begin until {@link #start(KCHttpRequest)} is called.
	 *
	 * @param cache
	 *            A Cache to use for persisting responses to disk
	 * @param network
	 *            A Network interface for performing HTTP requests
	 */
	public KCRequestRunner(KCCache cache, KCNetwork network)
	{
		this(cache, network, new KCDeliveryExecutor(new Handler(Looper.getMainLooper())));
	}

	/**
	 * Cancel the request.
	 */
	public void cancel()
	{
		if (null == mRequest)
		{
			return;
		}
		mRequest.cancel();
	}

	/**
	 * Gets the {@link KCCache} instance being used.
	 */
	public KCCache getCache()
	{
		return mCache;
	}

	public <T> void startAsyn(final KCHttpRequest<T> request)
	{
		this.mRequest = request;
		KCTaskExecutor.executeTask(new Runnable()
		{
			@Override
			public void run()
			{
				start(request);
			}
		});
	}

	/**
	 * Starts the request and return {@link KCHttpResponse} or null.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public <T> KCHttpResponse start(KCHttpRequest<T> request)
	{
		this.mRequest = request;

		if (null == mRequest)
		{
			return null;
		}
		KCHttpResponse networkResponse = null;
		long startTimeMs = SystemClock.elapsedRealtime();
		try
		{
			mRequest.addMarker("network-queue-take");

			// If the request was cancelled already, do not perform the
			// network request.
			if (mRequest.isCanceled())
			{
				mRequest.finish("network-discard-cancelled");
				return null;
			}

			addTrafficStatsTag(request);

			// Perform the network request.
			networkResponse = mNetwork.performRequest(mRequest);
			mRequest.addMarker("network-http-complete");

			// If the server returned 304 AND we delivered a response already,
			// we're done -- don't deliver a second identical response.
			if (networkResponse.getNotModified() && mRequest.hasHadResponseDelivered())
			{
				mRequest.finish("not-modified");
//				return networkResponse;
				return null;
			}

			// Parse the response here on the worker thread.
			KCHttpResponseParser httpResponseParser = mRequest.getResponseParser();
			KCHttpResult<?> result = KCHttpResult.empty();
			if (httpResponseParser != null)
			{
				result = httpResponseParser.parseHttpResponse(networkResponse);
			}

			mRequest.addMarker("network-parse-complete");

			// Write to cache if applicable.
			// TODO: Only update cache metadata instead of entire record for 304s.
			if (mCache != null && mRequest.shouldCache() && result.cacheEntry != null)
			{
				mCache.put(mRequest.getCacheKey(), result.cacheEntry);
				mRequest.addMarker("network-cache-written");
			}

			// Post the response back.
			mRequest.markDelivered();
			mDelivery.postResponse(mRequest, networkResponse, result);
		}
		catch (KCNetError netError)
		{
			networkResponse = netError.networkResponse;
			netError.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
			parseAndDeliverNetworkError(mRequest, netError);
		}
		catch (Exception e)
		{
			// KCLog.e("Unhandled exception %s", e.toString());
			KCNetError error = new KCNetError(e);
			error.setNetworkTimeMs(SystemClock.elapsedRealtime() - startTimeMs);
			mDelivery.postError(mRequest, error);
		}

		if (null == networkResponse)
		{
			KCProtocolVersion protocolVersion = new KCProtocolVersion("HTTP", 1, 1);
			KCStatusLine responseStatus = new KCStatusLine(protocolVersion, 0, "");
			networkResponse = new KCHttpResponse(responseStatus);
		}
		return networkResponse;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void addTrafficStatsTag(KCHttpRequest<?> request)
	{
		// Tag the request (if API >= 14)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
		{
			TrafficStats.setThreadStatsTag(request.getTrafficStatsTag());
		}
	}

//	 public KCHttpResponse getResponse()
//	 {
//	 return response;
//	 }

//	public NetError getError()
//	{
//		return error;
//	}

	private void parseAndDeliverNetworkError(KCHttpRequest<?> request, KCNetError aError)
	{
		KCHttpResponseParser parser = request.getResponseParser();
		if (parser != null)
		{
			aError = parser.parseHttpError(aError);
		}

		mDelivery.postError(request, aError);
	}
}

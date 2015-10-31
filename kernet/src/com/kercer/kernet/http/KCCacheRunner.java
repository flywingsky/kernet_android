package com.kercer.kernet.http;

import com.kercer.kernet.http.base.KCHttpContent;
import com.kercer.kernet.http.base.KCHttpStatus;
import com.kercer.kernet.http.base.KCProtocolVersion;
import com.kercer.kernet.http.base.KCStatusLine;

import java.util.concurrent.BlockingQueue;

/**
 * Created by zihong on 15/10/29.
 */
public class KCCacheRunner
{

    /** The queue of requests going out to the network. */
    private final BlockingQueue<KCHttpRequest<?>> mNetworkQueue;

    /** The cache to read from. */
    private final KCCache mCache;

    /** For posting responses. */
    private final KCDeliveryResponse mDelivery;

    public KCCacheRunner(BlockingQueue<KCHttpRequest<?>> aNetworkQueue, KCCache aCache, KCDeliveryResponse aDelivery)
    {
        mNetworkQueue = aNetworkQueue;
        mCache = aCache;
        mDelivery = aDelivery;
    }


    public void initializeCache()
    {
        // Make a blocking call to initialize the cache.
        mCache.initialize();
    }

    public boolean start(final KCHttpRequest<?> request)
    {
        try
        {
            // If the request has been canceled, don't bother dispatching it.
            if (request.isCanceled())
            {
                request.finish("cache-discard-canceled");
                return false;
            }

            // Attempt to retrieve this item from cache.
            KCCache.KCEntry entry = mCache.get(request.getCacheKey());
            if (entry == null)
            {
                request.addMarker("cache-miss");
                // Cache miss; send off to the network dispatcher.
                mNetworkQueue.put(request);
                return false;
            }

            // If it is completely expired, just send it to the network.
            if (entry.isExpired())
            {
                request.addMarker("cache-hit-expired");
                request.setCacheEntry(entry);
                mNetworkQueue.put(request);
                return false;
            }

            // We have a cache hit; parse its data for delivery back to the request.
            request.addMarker("cache-hit");

            KCStatusLine responseStatus = new KCStatusLine(new KCProtocolVersion("HTTP", 1, 1), KCHttpStatus.HTTP_OK, "cache response");
            KCHttpResponse networkResponse = new KCHttpResponse(responseStatus);
            KCHttpContent httpEntity = new KCHttpContent();
            httpEntity.setContent(entry.data);
            networkResponse.setHeaders(entry.responseHeaders.getAllHeaders());
            networkResponse.setContent(httpEntity);

            KCHttpResult<?> result = KCHttpResult.empty();
            KCHttpResponseParser httpResponseParser = request.getResponseParser();
            if (httpResponseParser != null)
            {
                result = httpResponseParser.parseHttpResponse(networkResponse);
            }
            request.addMarker("cache-hit-parsed");

            if (!entry.refreshNeeded())
            {
                // Completely unexpired cache hit. Just deliver the response.
                mDelivery.postResponse(request, networkResponse,  result);
            }
            else
            {
                // Soft-expired cache hit. We can deliver the cached response,
                // but we need to also send the request to the network for
                // refreshing.
                request.addMarker("cache-hit-refresh-needed");
                request.setCacheEntry(entry);

                // Mark the response as intermediate.
                result.intermediate = true;

                // Post the intermediate response back to the user and have
                // the delivery then forward the request along to the network.
                mDelivery.postResponse(request, networkResponse, result, new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            mNetworkQueue.put(request);
                        }
                        catch (InterruptedException e)
                        {
                            // Not much we can do about this.
                        }
                    }
                });
            }
        }
        catch (InterruptedException e)
        {
            return false;
        }

        return true;
    }
}

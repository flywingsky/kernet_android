package com.kercer.kernet.http;

import com.kercer.kernet.http.error.KCNetError;

/**
 * Created by zihong on 15/11/2.
 */
public interface KCDeliveryResult extends KCDeliveryError
{
    /**
     * Parses a response from the network or cache and delivers it.
     */
    public void postResponse(KCHttpRequest<?> aRequest, KCHttpResponse aResponse, KCHttpResult<?> aResult);

    /**
     * Parses a response from the network or cache and delivers it. The provided Runnable will be executed after delivery.
     */
    public void postResponse(KCHttpRequest<?> aRequest, KCHttpResponse aResponse, KCHttpResult<?> aResult, Runnable aRunnable);

    /**
     * Posts an error for the given request.
     */
    public void postError(KCHttpRequest<?> aRequest, KCNetError aError);
}

package com.kercer.kernet.http.listener;

/**
 * Created by zihong on 15/11/25.
 */

/** Callback interface for delivering the progress of the responses. */
public interface KCHttpProgressListener extends KCHttpBaseListener
{
    /**
     * Callback method thats called on each byte transfer.
     */
    void onProgress(long aCurrent, long aTotal);
}

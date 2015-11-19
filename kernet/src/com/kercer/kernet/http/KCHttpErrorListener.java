package com.kercer.kernet.http;

import com.kercer.kernet.http.error.KCNetError;

/**
 * Created by zihong on 15/11/19.
 */
public interface KCHttpErrorListener
{
    /**
     * Callback method that an error has been occurred with the provided error code and optional user-readable message.
     */
    public void onHttpError(KCNetError error);
}

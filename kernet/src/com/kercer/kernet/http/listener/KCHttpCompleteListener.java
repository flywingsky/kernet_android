package com.kercer.kernet.http.listener;

import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.KCHttpResponse;

/**
 * Created by zihong on 15/11/25.
 */
public interface KCHttpCompleteListener extends KCHttpBaseListener
{
    public void onHttpComplete(KCHttpRequest<?> request, KCHttpResponse response);
}

package com.kercer.kernet.http;

import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCStatusLine;

/**
 * Created by zihong on 15/11/2.
 */
public interface KCDeliveryHttp
{
    public void postHeaders(final KCHttpRequest<?> aRequest,final KCStatusLine aStatusLine,final KCHeaderGroup aHeaderGroup);
}

package com.kercer.kernet.http.listener;

import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCStatusLine;

/**
 * Created by zihong on 15/11/25.
 */
public interface KCHttpHeaderListener extends KCHttpBaseListener
{
    /**
     * Receive response headers, callback header group
     * @param aStatusLine status line
     * @param aHeaderGroup headers
     */
    public void onResponseHeaders(KCStatusLine aStatusLine, KCHeaderGroup aHeaderGroup);
}

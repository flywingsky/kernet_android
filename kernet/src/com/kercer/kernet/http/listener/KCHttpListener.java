package com.kercer.kernet.http.listener;

import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.KCHttpResponse;
import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCStatusLine;

public interface KCHttpListener extends KCHttpErrorListener
{

	/**
	 * Receive response headers, callback header group
	 * @param aStatusLine status line
	 * @param aHeaderGroup headers
	 */
	public void onResponseHeaders(KCStatusLine aStatusLine, KCHeaderGroup aHeaderGroup);

	public void onHttpComplete(KCHttpRequest<?> request, KCHttpResponse response);


	/** Callback interface for delivering the progress of the responses. */
	public interface KCProgressListener
	{
		/**
		 * Callback method thats called on each byte transfer.
		 */
		void onProgress(long aCurrent, long aTotal);
	}
}

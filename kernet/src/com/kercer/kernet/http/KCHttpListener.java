package com.kercer.kernet.http;

import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCStatusLine;
import com.kercer.kernet.http.error.KCNetError;

public interface KCHttpListener
{

	/**
	 * Receive response headers, callback header group
	 * @param aStatusLine status line
	 * @param aHeaderGroup headers
	 */
	public void onResponseHeaders(KCStatusLine aStatusLine, KCHeaderGroup aHeaderGroup);

	/**
	 * Callback method that an error has been occurred with the provided error code and optional user-readable message.
	 */
	public void onHttpError(KCNetError error);

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

package com.kercer.kernet.http;

import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.error.KCNetError;

public interface KCHttpListener
{

	/**
	 * Callback method that an error has been occurred with the provided error code and optional user-readable message.
	 */
	public void onHttpError(KCNetError error);

	public void onHttpComplete(KCHttpRequest<?> request, KCHttpResponse response);


	/** Callback interface for delivering the progress of the responses. */
	public interface KCProgressListener
	{
		/**
		 * Receive response headers, callback header group
		 * @param aHeaderGroup headers
		 */
		void onResponseHeaders(KCHeaderGroup aHeaderGroup);

		/**
		 * Callback method thats called on each byte transfer.
		 */
		void onProgress(long aCurrent, long aTotal);
	}
}

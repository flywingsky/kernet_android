package com.kercer.kernet.http;

import com.kercer.kernet.http.error.KCNetError;

public interface KCHttpResponseParser
{
	/**
	 * Subclasses must implement this to parse the raw network response and return an appropriate response type. This method will be called from a
	 * worker thread. The response will not be delivered if you return null.
	 *
	 * @param response
	 *            Response from the network
	 * @return The parsed response, or null in the case of an error
	 */
	public KCHttpResult<?> parseHttpResponse(KCHttpResponse response);

	/**
	 * Subclasses can override this method to parse 'networkError' and return a more specific error.
	 *
	 * <p>
	 * The default implementation just returns the passed 'networkError'.
	 * </p>
	 *
	 * @param aError
	 *            the error retrieved from the network
	 * @return an NetworkError augmented with additional information
	 */
	public KCNetError parseHttpError(KCNetError aError);
}

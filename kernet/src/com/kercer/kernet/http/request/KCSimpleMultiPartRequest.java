package com.kercer.kernet.http.request;

import com.kercer.kernet.http.KCHttpHeaderParser;
import com.kercer.kernet.http.KCHttpListener;
import com.kercer.kernet.http.KCHttpResponse;
import com.kercer.kernet.http.KCHttpResponseParser;
import com.kercer.kernet.http.KCHttpResult;
import com.kercer.kernet.http.error.KCNetError;

import java.io.UnsupportedEncodingException;

/**
 * A Simple request for making a Multi Part request whose response is retrieve as String
 */
public class KCSimpleMultiPartRequest extends KCMultiPartRequest<String>
{
	/**
	 * Creates a new request with the given method.
	 *
	 * @param method
	 *            the request {@link Method} to use
	 * @param url
	 *            URL to fetch the string at
	 * @param listener
	 *            Listener to receive the String response
	 */
	public KCSimpleMultiPartRequest(int method, String url, KCHttpListener listener)
	{
		super(method, url, listener);

		this.setResponseParser(new KCHttpResponseParser()
		{
			@Override
			public KCHttpResult<?> parseHttpResponse(KCHttpResponse response)
			{
				String parsed;
				try
				{
					parsed = new String(response.getContent(), KCHttpHeaderParser.parseCharset(response.getHeaderGroup()));
				}
				catch (UnsupportedEncodingException e)
				{
					parsed = new String(response.getContent());
				}
				return KCHttpResult.success(parsed, KCHttpHeaderParser.parseCacheHeaders(response));
			}

			@Override
			public KCNetError parseHttpError(KCNetError aError)
			{
				// TODO Auto-generated method stub
				return null;
			}
		});
	}

	/**
	 * Creates a new GET request.
	 *
	 * @param url
	 *            URL to fetch the string at
	 * @param listener
	 *            Listener to receive the String response
	 */
	public KCSimpleMultiPartRequest(String url, KCHttpListener listener)
	{
		this(Method.POST, url, listener);
	}

}

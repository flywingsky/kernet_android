
package com.kercer.kernet.http.request;

import android.text.TextUtils;

import com.kercer.kercore.debug.KCLog;
import com.kercer.kernet.http.listener.KCHttpBaseListener;
import com.kercer.kernet.http.KCHttpHeaderParser;
import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.KCHttpResponse;
import com.kercer.kernet.http.KCHttpResponseParser;
import com.kercer.kernet.http.KCHttpResult;
import com.kercer.kernet.http.error.KCNetError;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class KCSimpleDownloadRequest extends KCHttpRequest<String>
{
	private final String mDownloadPath;

	/**
	 * Creates a new request with the given method.
	 *
	 * @param url
	 *            URL to fetch the string at
	 * @param download_path
	 *            path to save the file to
	 * @param aListener
	 *            Listener to receive the String response
	 */
	public KCSimpleDownloadRequest(String url, String download_path, KCHttpBaseListener aListener)
	{
		super(Method.GET, url, aListener);
		mDownloadPath = download_path;
		setResponseParser(new KCHttpResponseParser()
		{

			@Override
			public KCHttpResult<?> parseHttpResponse(KCHttpResponse response)
			{
				String parsed = null;
				try
				{
					byte[] data = response.getContent();
					// convert array of bytes into file
					FileOutputStream fileOuputStream = new FileOutputStream(mDownloadPath);
					fileOuputStream.write(data);
					fileOuputStream.close();
					parsed = mDownloadPath;
				}
				catch (UnsupportedEncodingException e)
				{
					parsed = new String(response.getContent());
				}
				catch (FileNotFoundException e)
				{
					KCLog.e(e);
				}
				catch (IOException e)
				{
					KCLog.e(e);
				}
				finally
				{
					if (TextUtils.isEmpty(parsed))
					{
						parsed = "";
					}
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

}

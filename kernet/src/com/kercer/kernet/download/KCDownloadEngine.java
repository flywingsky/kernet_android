package com.kercer.kernet.download;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author zihong
 *
 */
public class KCDownloadEngine
{
	private ExecutorService mThreadService;

	// threads used to concurrently initiate requests
	public KCDownloadEngine(String aUserAgent, final int aMaxConn)
	{
		mThreadService = Executors.newCachedThreadPool();
		initHttpConnectionProperties(aUserAgent, aMaxConn);
	}

	private void initHttpConnectionProperties(String aUserAgent, final int aMaxConn)
	{
		System.setProperty("http.keepAlive", "true"); // enabling connection pooling
		System.setProperty("http.maxConnections", String.valueOf(aMaxConn));
		System.setProperty("http.agent", aUserAgent);
		HttpURLConnection.setFollowRedirects(false);
	}

	public KCDownloadTask startDownload(String aUrl, String aDestFilePath, KCDownloadListener aListener, boolean aUseResumable, boolean aNeedProgress) throws FileNotFoundException,
			URISyntaxException
	{
		return createDownloadTask(aUrl, aDestFilePath, aListener, aUseResumable, aNeedProgress);
	}

	private KCDownloadTask createDownloadTask(String aUrl, String aDestFilePath, final KCDownloadListener aListener, final boolean aUseResumable, final boolean aNeedProgress)
			throws FileNotFoundException, URISyntaxException
	{
		URL urlObj = null;
		try
		{
			urlObj = new URL(aUrl);
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}

		if (urlObj == null)
			return null;

		final KCDownloadTask dt = new KCDownloadTask(this, urlObj, aDestFilePath, aListener);

		getExecutorService().execute(new Runnable()
		{
			@Override
			public void run()
			{
				dt.runTask(aUseResumable, aNeedProgress);
			}
		});

		// so dt is useless in that case, the reason I design the interface like this is that sometimes I don't need
		// a KCDownloadTask to use its public methods(stop, resume, cancel)
		return dt;
	}

	public void startDownload(final KCDownloadTask aDT, final boolean aUseResumable, final boolean aNeedProgress)
	{
		getExecutorService().execute(new Runnable()
		{
			@Override
			public void run()
			{
				aDT.runTask(aUseResumable, aNeedProgress);
			}
		});
	}

	protected ExecutorService getExecutorService()
	{
		if (mThreadService == null)
			mThreadService = Executors.newCachedThreadPool();
		return mThreadService;
	}

	public void shutdown()
	{
		if (mThreadService != null)
		{
			mThreadService.shutdown();
			mThreadService = null;
		}
	}
}

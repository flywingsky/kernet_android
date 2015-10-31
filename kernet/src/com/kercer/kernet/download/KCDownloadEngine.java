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
	public KCDownloadEngine(String userAgent, final int maxConn)
	{
		mThreadService = Executors.newCachedThreadPool();
		initHttpConnectionProperties(userAgent, maxConn);
	}

	private void initHttpConnectionProperties(String userAgent, final int maxConn)
	{
		System.setProperty("http.keepAlive", "true"); // enabling connection pooling
		System.setProperty("http.maxConnections", String.valueOf(maxConn));
		System.setProperty("http.agent", userAgent);
		HttpURLConnection.setFollowRedirects(false);
	}

	public KCDownloadTask startDownload(String url, String destFilePath, KCDownloadListener notifier, boolean useResumable, boolean needProgress) throws FileNotFoundException,
			URISyntaxException
	{
		return createDownloadTask(url, destFilePath, notifier, useResumable, needProgress);
	}

	private KCDownloadTask createDownloadTask(String url, String destFilePath, final KCDownloadListener notifier, final boolean useResumable, final boolean needProgress)
			throws FileNotFoundException, URISyntaxException
	{
		URL urlObj = null;
		try
		{
			urlObj = new URL(url);
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
		}

		if (urlObj == null)
			return null;

		final KCDownloadTask dt = new KCDownloadTask(this, urlObj, destFilePath, notifier);

		getExecutorService().execute(new Runnable()
		{
			@Override
			public void run()
			{
				dt.runTask(useResumable, needProgress);
			}
		});

		// so dt is useless in that case, the reason I design the interface like this is that sometimes I don't need
		// a KCDownloadTask to use its public methods(stop, resume, cancel)
		return dt;
	}

	public void startDownload(final KCDownloadTask dt, final boolean useResumable, final boolean needProgress)
	{
		getExecutorService().execute(new Runnable()
		{
			@Override
			public void run()
			{
				dt.runTask(useResumable, needProgress);
			}
		});
	}

	public ExecutorService getExecutorService()
	{
		if (mThreadService == null)
			mThreadService = Executors.newCachedThreadPool();
		return mThreadService;
	}

	public void quit()
	{
		if (mThreadService != null)
		{
			mThreadService.shutdown();
			mThreadService = null;
		}
	}
}

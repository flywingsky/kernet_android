package com.kercer.kernet.download;

/**
 * 
 * @author zihong
 *
 */
public interface KCDownloadListener
{
	void onPrepare();

	void onReceiveFileLength(long downloadedBytes, long fileLength);

	void onProgressUpdate(long downloadedBytes, long fileLength, int speed);

	void onComplete(long downloadedBytes, long fileLength, int totalTimeInSeconds);

	void onError(long downloadedBytes, Throwable e);
}

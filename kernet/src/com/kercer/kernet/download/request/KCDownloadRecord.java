package com.kercer.kernet.download.request;

public class KCDownloadRecord
{

	private String mUrl;
	private String mDestRecordPath;
	private String RecordName;
	// private Bitmap RecordIcon;
	private long mRecordsize;
	private long mDownloadsize;

	public KCDownloadRecord(String url)
	{
		this.mUrl = url;
	}

	public void setRecordName(String RecordName)
	{
		this.RecordName = RecordName;
	}

	public String getRecordName()
	{
		return this.RecordName;
	}

	/*
	 * public void setRecordIcon(Bitmap bitmap){ this.RecordIcon = bitmap; }
	 */
	public void setRecordSize(long size)
	{
		this.mRecordsize = size;
	}

	public long getRecordSize()
	{
		return this.mRecordsize;
	}

	public String getRecordUrl()
	{
		return this.mUrl;
	}

	public String getRecordPath()
	{
		return this.mDestRecordPath;
	}

	public void setRecordPath(String path)
	{
		this.mDestRecordPath = path;
	}

	public void setDownloadSize(long recordsize)
	{
		this.mDownloadsize = recordsize;
	}

	public long getDownloadSize()
	{
		return this.mDownloadsize;
	}
}

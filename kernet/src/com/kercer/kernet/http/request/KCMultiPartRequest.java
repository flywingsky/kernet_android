package com.kercer.kernet.http.request;

import com.kercer.kernet.http.KCRetryPolicyDefault;
import com.kercer.kernet.http.KCHttpListener;
import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.KCHttpListener.KCProgressListener;

import java.util.HashMap;
import java.util.Map;

/**
 * A request for making a Multi Part request
 *
 * @param <T>
 *            Response expected
 */
public abstract class KCMultiPartRequest<T> extends KCHttpRequest<T>
{

	private static final String PROTOCOL_CHARSET = "utf-8";
	private Map<String, KCMultiPartParam> mMultipartParams = null;
	private Map<String, String> mFileUploads = null;
	public static final int TIMEOUT_MS = 30000;
	private boolean isFixedStreamingMode;

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
	public KCMultiPartRequest(int method, String url, KCHttpListener listener)
	{

		super(method, url, listener, null, null);
		new KCRetryPolicyDefault(TIMEOUT_MS, KCRetryPolicyDefault.DEFAULT_MAX_RETRIES, KCRetryPolicyDefault.DEFAULT_BACKOFF_MULT);
		mMultipartParams = new HashMap<String, KCMultiPartRequest.KCMultiPartParam>();
		mFileUploads = new HashMap<String, String>();

	}

	/**
	 * Add a parameter to be sent in the multipart request
	 *
	 * @param name
	 *            The name of the paramter
	 * @param contentType
	 *            The content type of the paramter
	 * @param value
	 *            the value of the paramter
	 * @return The Multipart request for chaining calls
	 */
	public KCMultiPartRequest<T> addMultipartParam(String name, String contentType, String value)
	{
		mMultipartParams.put(name, new KCMultiPartParam(contentType, value));
		return this;
	}

	/**
	 * Add a file to be uploaded in the multipart request
	 *
	 * @param name
	 *            The name of the file key
	 * @param filePath
	 *            The path to the file. This file MUST exist.
	 * @return The Multipart request for chaining method calls
	 */
	public KCMultiPartRequest<T> addFile(String name, String filePath)
	{

		mFileUploads.put(name, filePath);
		return this;
	}

	/**
	 * Set listener for tracking download progress
	 *
	 * @param listener listener
	 */
	public void setOnProgressListener(KCProgressListener listener)
	{
		super.mProgressListener = listener;
	}

	/**
	 * A representation of a MultiPart parameter
	 */
	public static final class KCMultiPartParam
	{

		public String contentType;
		public String value;

		/**
		 * Initialize a multipart request param with the value and content type
		 *
		 * @param contentType
		 *            The content type of the param
		 * @param value
		 *            The value of the param
		 */
		public KCMultiPartParam(String contentType, String value)
		{
			this.contentType = contentType;
			this.value = value;
		}
	}

	/**
	 * Get all the multipart params for this request
	 *
	 * @return A map of all the multipart params NOT including the file uploads
	 */
	public Map<String, KCMultiPartParam> getMultipartParams()
	{
		return mMultipartParams;
	}

	/**
	 * Get all the files to be uploaded for this request
	 *
	 * @return A map of all the files to be uploaded for this request
	 */
	public Map<String, String> getFilesToUpload()
	{
		return mFileUploads;
	}

	/**
	 * Get the protocol charset
	 * @return protocol charset
	 */
	public String getProtocolCharset()
	{
		return PROTOCOL_CHARSET;
	}

	public boolean isFixedStreamingMode()
	{
		return isFixedStreamingMode;
	}

	public void setFixedStreamingMode(boolean isFixedStreamingMode)
	{
		this.isFixedStreamingMode = isFixedStreamingMode;
	}
}

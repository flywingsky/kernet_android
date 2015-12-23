package com.kercer.kernet.http.error;

/**
 * Created by zihong on 15/12/16.
 */
public class KCCookieError extends Exception
{

	private static final long serialVersionUID = -6695462944287282185L;

	/**
	 * Creates a new MalformedCookieException with a {@code null} detail message.
	 */
	public KCCookieError()
	{
		super();
	}

	/**
	 * Creates a new MalformedCookieException with a specified message string.
	 *
	 * @param message
	 *            The exception detail message
	 */
	public KCCookieError(final String message)
	{
		super(message);
	}
}

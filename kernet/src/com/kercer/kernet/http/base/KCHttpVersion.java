package com.kercer.kernet.http.base;

import java.io.Serializable;

public class KCHttpVersion extends KCProtocolVersion implements Serializable
{

	private static final long serialVersionUID = -5856653513894415344L;

	/** The protocol name. */
	public static final String HTTP = "HTTP";

	/** HTTP protocol version 0.9 */
	public static final KCHttpVersion HTTP_0_9 = new KCHttpVersion(0, 9);

	/** HTTP protocol version 1.0 */
	public static final KCHttpVersion HTTP_1_0 = new KCHttpVersion(1, 0);

	/** HTTP protocol version 1.1 */
	public static final KCHttpVersion HTTP_1_1 = new KCHttpVersion(1, 1);

	/**
	 * Create an HTTP protocol version designator.
	 *
	 * @param major
	 *            the major version number of the HTTP protocol
	 * @param minor
	 *            the minor version number of the HTTP protocol
	 *
	 * @throws IllegalArgumentException
	 *             if either major or minor version number is negative
	 */
	public KCHttpVersion(int major, int minor)
	{
		super(HTTP, major, minor);
	}

	/**
	 * Obtains a specific HTTP version.
	 *
	 * @param major
	 *            the major version
	 * @param minor
	 *            the minor version
	 *
	 * @return an instance of {@link KCHttpVersion} with the argument version
	 */
	public KCProtocolVersion forVersion(int major, int minor)
	{

		if ((major == this.major) && (minor == this.minor))
		{
			return this;
		}

		if (major == 1)
		{
			if (minor == 0)
			{
				return HTTP_1_0;
			}
			if (minor == 1)
			{
				return HTTP_1_1;
			}
		}
		if ((major == 0) && (minor == 9))
		{
			return HTTP_0_9;
		}

		// argument checking is done in the constructor
		return new KCHttpVersion(major, minor);
	}

}

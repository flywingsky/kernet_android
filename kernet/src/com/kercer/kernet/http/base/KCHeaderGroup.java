package com.kercer.kernet.http.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class KCHeaderGroup implements Cloneable
{

	/** The list of headers for this group, in the order in which they were added */
	private List<KCHeader> headers;


	public KCHeaderGroup()
	{
		this(16);
	}

	public KCHeaderGroup(int aSize)
	{
		this.headers = new ArrayList<KCHeader>(aSize);
	}

	public int size()
	{
		return headers.size();
	}

	/**
	 * Removes any contained headers.
	 */
	public void clear()
	{
		headers.clear();
	}

	/**
	 * Adds the given header to the group. The order in which this header was added is preserved.
	 *
	 * @param header
	 *            the header to add
	 */
	public void addHeader(KCHeader header)
	{
		if (header == null)
		{
			return;
		}
		headers.add(header);
	}

	public void addHeaders(KCHeader[] headers)
	{
		if (headers == null)
		{
			return;
		}
		for (int i = 0; i < headers.length; i++)
		{
			this.headers.add(headers[i]);
		}
	}


	/**
	 * Removes the given header.
	 *
	 * @param header
	 *            the header to remove
	 */
	public void removeHeader(KCHeader header)
	{
		if (header == null)
		{
			return;
		}
		headers.remove(header);
	}

	/**
	 * Replaces the first occurence of the header with the same name. If no header with the same name is found the given header is added to the end of
	 * the list.
	 *
	 * @param header
	 *            the new header that should replace the first header with the same name if present in the list.
	 */
	public void updateHeader(KCHeader header)
	{
		if (header == null)
		{
			return;
		}
		for (int i = 0; i < this.headers.size(); i++)
		{
			KCHeader current = (KCHeader) this.headers.get(i);
			if (current.getName().equalsIgnoreCase(header.getName()))
			{
				this.headers.set(i, header);
				return;
			}
		}
		this.headers.add(header);
	}

	/**
	 * Sets all of the headers contained within this group overriding any existing headers. The headers are added in the order in which they appear in
	 * the array.
	 *
	 * @param headers
	 *            the headers to set
	 */
	public void setHeaders(KCHeader[] headers)
	{
		clear();
		addHeaders(headers);
	}

	/**
	 * Gets a header representing all of the header values with the given name. If more that one header with the given name exists the values will be
	 * combined with a "," as per RFC 2616.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name
	 *            the name of the header(s) to get
	 * @return a header with a condensed value or <code>null</code> if no headers by the given name are present
	 */
	public KCHeader getCondensedHeader(String name)
	{
		KCHeader[] headers = getHeaders(name);

		if (headers.length == 0)
		{
			return null;
		}
		else if (headers.length == 1)
		{
			return headers[0];
		}
		else
		{
			KCCharArrayBuffer valueBuffer = new KCCharArrayBuffer(128);
			valueBuffer.append(headers[0].getValue());
			for (int i = 1; i < headers.length; i++)
			{
				valueBuffer.append(", ");
				valueBuffer.append(headers[i].getValue());
			}

			return new KCHeader(name.toLowerCase(Locale.ENGLISH), valueBuffer.toString());
		}
	}

	/**
	 * Gets all of the headers with the given name. The returned array maintains the relative order in which the headers were added.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name
	 *            the name of the header(s) to get
	 *
	 * @return an array of length
	 */
	public KCHeader[] getHeaders(String name)
	{
		ArrayList<KCHeader> headersFound = new ArrayList<KCHeader>();

		for (int i = 0; i < headers.size(); i++)
		{
			KCHeader header = (KCHeader) headers.get(i);
			if (header.getName().equalsIgnoreCase(name))
			{
				headersFound.add(header);
			}
		}

		return (KCHeader[]) headersFound.toArray(new KCHeader[headersFound.size()]);
	}


	/**
	 * Gets the first header with the given name.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param aName
	 *            the name of the header to get
	 * @return the first header value or <code>null</code>
	 */
	public String get(String aName)
	{
		String value = null;
		KCHeader header = getFirstHeader(aName);
		if (header != null)
			value = header.getValue();
		return value;
	}

	/**
	 * Gets the first header with the given name.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name
	 *            the name of the header to get
	 * @return the first header or <code>null</code>
	 */
	public KCHeader getFirstHeader(String name)
	{
		for (int i = 0; i < headers.size(); i++)
		{
			KCHeader header = (KCHeader) headers.get(i);
			if (header.getName().equalsIgnoreCase(name))
			{
				return header;
			}
		}
		return null;
	}

	/**
	 * Gets the last header with the given name.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name
	 *            the name of the header to get
	 * @return the last header or <code>null</code>
	 */
	public KCHeader getLastHeader(String name)
	{
		// start at the end of the list and work backwards
		for (int i = headers.size() - 1; i >= 0; i--)
		{
			KCHeader header = (KCHeader) headers.get(i);
			if (header.getName().equalsIgnoreCase(name))
			{
				return header;
			}
		}

		return null;
	}

	/**
	 * Gets all of the headers contained within this group.
	 *
	 * @return an array of length
	 */
	public KCHeader[] getAllHeaders()
	{
		return (KCHeader[]) headers.toArray(new KCHeader[headers.size()]);
	}

	/**
	 * Tests if headers with the given name are contained within this group.
	 *
	 * <p>
	 * Header name comparison is case insensitive.
	 *
	 * @param name
	 *            the header name to test for
	 * @return <code>true</code> if at least one header with the name is contained, <code>false</code> otherwise
	 */
	public boolean containsHeader(String name)
	{
		for (int i = 0; i < headers.size(); i++)
		{
			KCHeader header = (KCHeader) headers.get(i);
			if (header.getName().equalsIgnoreCase(name))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * Returns a copy of this object
	 *
	 * @return copy of this object
	 */
	public KCHeaderGroup copy()
	{
		KCHeaderGroup clone = new KCHeaderGroup();
		clone.headers.addAll(this.headers);
		return clone;
	}

	public Object clone() throws CloneNotSupportedException
	{
		KCHeaderGroup clone = (KCHeaderGroup) super.clone();
		clone.headers = new ArrayList<KCHeader>(this.headers);
		return clone;
	}


	public static KCHeaderGroup emptyHeaderGroup()
	{
		return new KCHeaderGroup(0);
	}

	public String toString()
	{
		return headers.toString();
	}
}

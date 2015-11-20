package com.kercer.kernet.http;

import com.kercer.kernet.http.base.KCHeader;
import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.base.KCHttpContent;
import com.kercer.kernet.http.base.KCProtocolVersion;
import com.kercer.kernet.http.base.KCReasonPhraseCatalog;
import com.kercer.kernet.http.base.KCStatusLine;

import java.util.Locale;

public class KCHttpResponse
{
	private KCStatusLine statusline;
	private KCHttpContent mHttpContent;
	private KCReasonPhraseCatalog reasonCatalog;
	private Locale locale;
	protected KCHeaderGroup headergroup;

    /** True if the server returned a 304 (Not Modified). */
    protected boolean notModified = false;

    /** Network roundtrip time in milliseconds. */
    protected long networkTimeMs = 0;


	/**
	 * Creates a new response. This is the constructor to which all others map.
	 *
	 * @param statusline
	 *            the status line
	 * @param catalog
	 *            the reason phrase catalog, or <code>null</code> to disable automatic reason phrase lookup
	 * @param locale
	 *            the locale for looking up reason phrases, or <code>null</code> for the system locale
	 */
	public KCHttpResponse(final KCStatusLine statusline, final KCReasonPhraseCatalog catalog, final Locale locale)
	{
		super();
		if (statusline == null)
		{
			throw new IllegalArgumentException("Status line may not be null.");
		}
		this.statusline = statusline;
		this.reasonCatalog = catalog;
		this.locale = (locale != null) ? locale : Locale.getDefault();
		this.headergroup = new KCHeaderGroup();
		this.mHttpContent = new KCHttpContent();
	}

	/**
	 * Creates a response from a status line. The response will not have a reason phrase catalog and use the system default locale.
	 *
	 * @param statusline
	 *            the status line
	 */
	public KCHttpResponse(final KCStatusLine statusline)
	{
		this(statusline, null, null);
	}





	/**
	 * Creates a response from elements of a status line. The response will not have a reason phrase catalog and use the system default locale.
	 *
	 * @param ver
	 *            the protocol version of the response
	 * @param code
	 *            the status code of the response
	 * @param reason
	 *            the reason phrase to the status code, or <code>null</code>
	 */
	public KCHttpResponse(final KCProtocolVersion ver, final int code, final String reason)
	{
		this(new KCStatusLine(ver, code, reason), null, null);
	}

	public KCProtocolVersion getProtocolVersion()
	{
		return this.statusline.getProtocolVersion();
	}

	public KCStatusLine getStatusLine()
	{
		return this.statusline;
	}

	public int getStatusCode()
	{
		return this.statusline != null ? this.statusline.getStatusCode() : -1;
	}

	public byte[] getContent()
	{
		return mHttpContent != null ? mHttpContent.getContent() : null;
	}

	public KCHttpContent getHttpContent()
	{
		return this.mHttpContent;
	}

	public Locale getLocale()
	{
		return this.locale;
	}

	public void setStatusLine(final KCStatusLine statusline)
	{
		if (statusline == null)
		{
			throw new IllegalArgumentException("Status line may not be null");
		}
		this.statusline = statusline;
	}

	public void setStatusLine(final KCProtocolVersion ver, final int code)
	{
		// arguments checked in BasicStatusLine constructor
		this.statusline = new KCStatusLine(ver, code, getReason(code));
	}

	public void setStatusLine(final KCProtocolVersion ver, final int code, final String reason)
	{
		// arguments checked in BasicStatusLine constructor
		this.statusline = new KCStatusLine(ver, code, reason);
	}

	public void setStatusCode(int code)
	{
		// argument checked in BasicStatusLine constructor
		KCProtocolVersion ver = this.statusline.getProtocolVersion();
		this.statusline = new KCStatusLine(ver, code, getReason(code));
	}

	public void setReasonPhrase(String reason)
	{

		if ((reason != null) && ((reason.indexOf('\n') >= 0) || (reason.indexOf('\r') >= 0)))
		{
			throw new IllegalArgumentException("Line break in reason phrase.");
		}
		this.statusline = new KCStatusLine(this.statusline.getProtocolVersion(), this.statusline.getStatusCode(), reason);
	}

	public void setContent(final KCHttpContent aContent)
	{
		this.mHttpContent = aContent;
	}

	public void setLocale(Locale loc)
	{
		if (loc == null)
		{
			throw new IllegalArgumentException("Locale may not be null.");
		}
		this.locale = loc;
		final int code = this.statusline.getStatusCode();
		this.statusline = new KCStatusLine(this.statusline.getProtocolVersion(), code, getReason(code));
	}

	/**
	 * Looks up a reason phrase. This method evaluates the currently set catalog and locale. It also handles a missing catalog.
	 *
	 * @param code
	 *            the status code for which to look up the reason
	 *
	 * @return the reason phrase, or <code>null</code> if there is none
	 */
	protected String getReason(int code)
	{
		return (this.reasonCatalog == null) ? null : this.reasonCatalog.getReason(code, this.locale);
	}

	/*******************
	 * header opt
	 *******************/

	public boolean containsHeader(String name)
	{
		return this.headergroup.containsHeader(name);
	}

	public KCHeaderGroup getHeaderGroup()
	{
		return this.headergroup;
	}

	public KCHeader[] getHeaders(final String name)
	{
		return this.headergroup.getHeaders(name);
	}

	public KCHeader getFirstHeader(final String name)
	{
		return this.headergroup.getFirstHeader(name);
	}

	public KCHeader getLastHeader(final String name)
	{
		return this.headergroup.getLastHeader(name);
	}

	public KCHeader[] getAllHeaders()
	{
		return this.headergroup.getAllHeaders();
	}

	public void addHeader(final KCHeader header)
	{
		this.headergroup.addHeader(header);
	}

	public void addHeader(final String name, final String value)
	{
		if (name == null)
		{
			throw new IllegalArgumentException("Header name may not be null");
		}
		this.headergroup.addHeader(new KCHeader(name, value));
	}

	public void setHeader(final KCHeader header)
	{
		this.headergroup.updateHeader(header);
	}

	public void setHeader(final String name, final String value)
	{
		if (name == null)
		{
			throw new IllegalArgumentException("Header name may not be null");
		}
		this.headergroup.updateHeader(new KCHeader(name, value));
	}

	public void setHeaders(final KCHeader[] headers)
	{
		this.headergroup.setHeaders(headers);
	}

	public void removeHeader(final KCHeader header)
	{
		this.headergroup.removeHeader(header);
	}


	public void setNotModified(boolean aNotModified)
	{
		notModified = aNotModified;
	}
	public boolean getNotModified()
	{
		return notModified;
	}

	public void setNetworkTimeMs(long aNetworkTimeMs)
	{
		networkTimeMs = aNetworkTimeMs;
	}
	public long getNetworkTimeMs()
	{
		return networkTimeMs;
	}


}

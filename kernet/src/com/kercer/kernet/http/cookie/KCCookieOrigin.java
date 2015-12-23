package com.kercer.kernet.http.cookie;

import com.kercer.kercore.annotation.KCImmutable;
import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kercore.util.KCUtilText;
import com.kercer.kernet.uri.KCURI;

import java.util.Locale;

/**
 * Created by zihong on 15/12/16.
 */

/**
 * CookieOrigin class encapsulates details of an origin server that are relevant when parsing, validating or matching HTTP cookies.
 *
 */
@KCImmutable
public final class KCCookieOrigin
{

	private String host;
	private int port;
	private String path;
	private boolean secure;

	public KCCookieOrigin(final KCURI aUri)
	{
		super();
		final String path = aUri != null ? aUri.getPath() : null;
		final String hostName = aUri.getHost();
		int port = aUri.getPort();
		if (port < 0)
		{
			port = aUri.inferredPort();
		}
		boolean secure = "https".equalsIgnoreCase(aUri.getScheme());
		init(hostName, port >= 0 ? port : 0, !KCUtilText.isEmpty(path) ? path : "/", secure);

	}
	public KCCookieOrigin(final String host, final int port, final String path, final boolean secure)
	{
		super();
		init(host, port, path, secure);
	}
	public void init(final String host, final int port, final String path, final boolean secure)
	{
		KCUtilArgs.notBlank(host, "Host");
		KCUtilArgs.notNegative(port, "Port");
		KCUtilArgs.notNull(path, "Path");
		this.host = host.toLowerCase(Locale.getDefault());
		this.port = port;
		if (!KCUtilText.isBlank(path))
		{
			this.path = path;
		}
		else
		{
			this.path = "/";
		}
		this.secure = secure;
	}

	public String getHost()
	{
		return this.host;
	}

	public String getPath()
	{
		return this.path;
	}

	public int getPort()
	{
		return this.port;
	}

	public boolean isSecure()
	{
		return this.secure;
	}

	@Override
	public String toString()
	{
		final StringBuilder buffer = new StringBuilder();
		buffer.append('[');
		if (this.secure)
		{
			buffer.append("(secure)");
		}
		buffer.append(this.host);
		buffer.append(':');
		buffer.append(Integer.toString(this.port));
		buffer.append(this.path);
		buffer.append(']');
		return buffer.toString();
	}

}

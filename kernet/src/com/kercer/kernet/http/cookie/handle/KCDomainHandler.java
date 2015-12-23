package com.kercer.kernet.http.cookie.handle;

import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kernet.http.cookie.KCClientCookie;
import com.kercer.kernet.http.cookie.KCCookie;
import com.kercer.kernet.http.cookie.KCCookieOrigin;
import com.kercer.kernet.http.error.KCCookieError;
import com.kercer.kercore.util.KCUtilText;
import com.kercer.kercore.annotation.KCImmutable;
import com.kercer.kernet.uri.KCUtilNetAddress;

/**
 * Created by zihong on 15/12/16.
 */

@KCImmutable
public class KCDomainHandler implements KCCookieHandler
{

	public KCDomainHandler()
	{
		super();
	}

	@Override
	public void parse(final KCClientCookie cookie, final String value) throws KCCookieError
	{
		KCUtilArgs.notNull(cookie, "Cookie");
		if (KCUtilText.isBlank(value))
		{
			throw new KCCookieError("Blank or null value for domain attribute");
		}
		// Ignore domain attributes ending with '.' per RFC 6265, 4.1.2.3
		if (value.endsWith("."))
		{
			return;
		}
		String domain = value;
		if (domain.startsWith("."))
		{
			domain = domain.substring(1);
		}
		domain = domain.toLowerCase();
		cookie.setDomain(domain);
	}

	@Override
	public void validate(final KCCookie cookie, final KCCookieOrigin origin) throws KCCookieError
	{
		KCUtilArgs.notNull(cookie, "Cookie");
		KCUtilArgs.notNull(origin, "Cookie origin");
		// Validate the cookies domain attribute. NOTE: Domains without
		// any dots are allowed to support hosts on private LANs that don't
		// have DNS names. Since they have no dots, to domain-match the
		// request-host and domain must be identical for the cookie to sent
		// back to the origin-server.
		final String host = origin.getHost();
		final String domain = cookie.getDomain();
		if (domain == null)
		{
			throw new KCCookieError("Cookie 'domain' may not be null");
		}
		if (!host.equals(domain) && !domainMatch(domain, host))
		{
			throw new KCCookieError("Illegal 'domain' attribute \"" + domain + "\". Domain of origin: \"" + host + "\"");
		}
	}

	static boolean domainMatch(final String domain, final String host)
	{
		if (KCUtilNetAddress.isIPv4Address(host) || KCUtilNetAddress.isIPv6Address(host))
		{
			return false;
		}
		final String normalizedDomain = domain.startsWith(".") ? domain.substring(1) : domain;
		if (host.endsWith(normalizedDomain))
		{
			final int prefix = host.length() - normalizedDomain.length();
			// Either a full match or a prefix endidng with a '.'
			if (prefix == 0)
			{
				return true;
			}
			if (prefix > 1 && host.charAt(prefix - 1) == '.')
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean match(final KCCookie cookie, final KCCookieOrigin origin)
	{
		KCUtilArgs.notNull(cookie, "Cookie");
		KCUtilArgs.notNull(origin, "Cookie origin");
		final String host = origin.getHost();
		String domain = cookie.getDomain();
		if (domain == null)
		{
			return false;
		}
		if (domain.startsWith("."))
		{
			domain = domain.substring(1);
		}
		domain = domain.toLowerCase();
		if (host.equals(domain))
		{
			return true;
		}
		if ((cookie.containsAttribute(KCCookie.DOMAIN_ATTR)))
		{
			return domainMatch(domain, host);
		}
		return false;
	}

	@Override
	public String getAttributeName()
	{
		return KCCookie.DOMAIN_ATTR;
	}

}

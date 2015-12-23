package com.kercer.kernet.http.cookie.handle.suffix;

/**
 * Created by zihong on 15/12/16.
 */

import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kernet.http.cookie.KCClientCookie;
import com.kercer.kernet.http.cookie.KCCookie;
import com.kercer.kernet.http.cookie.KCCookieOrigin;
import com.kercer.kernet.http.cookie.handle.KCCookieHandler;
import com.kercer.kernet.http.error.KCCookieError;
import com.kercer.kercore.annotation.KCImmutable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps and leverages its match method to never match a suffix from a black list. May be used to provide additional security for cross-site attack
 * types by preventing cookies from apparent domains that are not publicly available.
 *
 */
@KCImmutable
// dependencies are expected to be immutable or thread-safe
public class KCPublicSuffixDomainFilter implements KCCookieHandler
{

	private final KCCookieHandler handler;
	private final KCPublicSuffixMatcher publicSuffixMatcher;
	private final Map<String, Boolean> localDomainMap;

	private static Map<String, Boolean> createLocalDomainMap()
	{
		final ConcurrentHashMap<String, Boolean> map = new ConcurrentHashMap<String, Boolean>();
		map.put(".localhost.", Boolean.TRUE); // RFC 6761
		map.put(".test.", Boolean.TRUE); // RFC 6761
		map.put(".local.", Boolean.TRUE); // RFC 6762
		map.put(".local", Boolean.TRUE);
		map.put(".localdomain", Boolean.TRUE);
		return map;
	}

	public KCPublicSuffixDomainFilter(final KCCookieHandler handler, final KCPublicSuffixMatcher publicSuffixMatcher)
	{
		this.handler = KCUtilArgs.notNull(handler, "Cookie handler");
		this.publicSuffixMatcher = KCUtilArgs.notNull(publicSuffixMatcher, "Public suffix matcher");
		this.localDomainMap = createLocalDomainMap();
	}

	public KCPublicSuffixDomainFilter(final KCCookieHandler handler, final KCPublicSuffixList suffixList)
	{
		KCUtilArgs.notNull(handler, "Cookie handler");
		KCUtilArgs.notNull(suffixList, "Public suffix list");
		this.handler = handler;
		this.publicSuffixMatcher = new KCPublicSuffixMatcher(suffixList.getRules(), suffixList.getExceptions());
		this.localDomainMap = createLocalDomainMap();
	}

	/**
	 * Never matches if the cookie's domain is from the blacklist.
	 */
	@Override
	public boolean match(final KCCookie cookie, final KCCookieOrigin origin)
	{
		final String host = cookie.getDomain();
		final int i = host.indexOf('.');
		if (i >= 0)
		{
			final String domain = host.substring(i);
			if (!this.localDomainMap.containsKey(domain))
			{
				if (this.publicSuffixMatcher.matches(host))
				{
					return false;
				}
			}
		}
		else
		{
			if (!host.equalsIgnoreCase(origin.getHost()))
			{
				if (this.publicSuffixMatcher.matches(host))
				{
					return false;
				}
			}
		}
		return handler.match(cookie, origin);
	}

	@Override
	public void parse(final KCClientCookie cookie, final String value) throws KCCookieError
	{
		handler.parse(cookie, value);
	}

	@Override
	public void validate(final KCCookie cookie, final KCCookieOrigin origin) throws KCCookieError
	{
		handler.validate(cookie, origin);
	}

	@Override
	public String getAttributeName()
	{
		return handler.getAttributeName();
	}

	public static KCCookieHandler decorate(final KCCookieHandler handler, final KCPublicSuffixMatcher publicSuffixMatcher)
	{
		KCUtilArgs.notNull(handler, "Cookie attribute handler");
		return publicSuffixMatcher != null ? new KCPublicSuffixDomainFilter(handler, publicSuffixMatcher) : handler;
	}

}

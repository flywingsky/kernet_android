package com.kercer.kernet.http.cookie.handle;

import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kernet.http.cookie.KCClientCookie;
import com.kercer.kernet.http.cookie.KCCookie;
import com.kercer.kernet.http.cookie.KCCookieOrigin;
import com.kercer.kernet.http.error.KCCookieError;
import com.kercer.kercore.util.KCUtilText;
import com.kercer.kercore.annotation.KCImmutable;

/**
 * Created by zihong on 15/12/16.
 */
@KCImmutable
public class KCPathHandler implements KCCookieHandler
{

	public KCPathHandler()
	{
		super();
	}

	@Override
	public void parse(final KCClientCookie cookie, final String value) throws KCCookieError
	{
		KCUtilArgs.notNull(cookie, "Cookie");
		cookie.setPath(!KCUtilText.isBlank(value) ? value : "/");
	}

	@Override
	public void validate(final KCCookie cookie, final KCCookieOrigin origin) throws KCCookieError
	{
		if (!match(cookie, origin))
		{
			throw new KCCookieError("Illegal 'path' attribute \"" + cookie.getPath()+ "\". Path of origin: \"" + origin.getPath() + "\"");
		}
	}

	static boolean pathMatch(final String uriPath, final String cookiePath)
	{
		String normalizedCookiePath = cookiePath;
		if (normalizedCookiePath == null)
		{
			normalizedCookiePath = "/";
		}
		if (normalizedCookiePath.length() > 1 && normalizedCookiePath.endsWith("/"))
		{
			normalizedCookiePath = normalizedCookiePath.substring(0, normalizedCookiePath.length() - 1);
		}
		if (uriPath.startsWith(normalizedCookiePath))
		{
			if (normalizedCookiePath.equals("/"))
			{
				return true;
			}
			if (uriPath.length() == normalizedCookiePath.length())
			{
				return true;
			}
			if (uriPath.charAt(normalizedCookiePath.length()) == '/')
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
		return pathMatch(origin.getPath(), cookie.getPath());
	}

	@Override
	public String getAttributeName()
	{
		return KCCookie.PATH_ATTR;
	}

}

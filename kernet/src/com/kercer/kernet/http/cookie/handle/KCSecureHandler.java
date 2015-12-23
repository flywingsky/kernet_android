package com.kercer.kernet.http.cookie.handle;

import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kernet.http.cookie.KCClientCookie;
import com.kercer.kernet.http.cookie.KCCookie;
import com.kercer.kernet.http.cookie.KCCookieOrigin;
import com.kercer.kernet.http.error.KCCookieError;
import com.kercer.kercore.annotation.KCImmutable;

/**
 * Created by zihong on 15/12/16.
 */
@KCImmutable
public class KCSecureHandler implements KCCookieHandler
{

	public KCSecureHandler()
	{
		super();
	}

	@Override
	public void validate(final KCCookie cookie, final KCCookieOrigin origin) throws KCCookieError
	{
		// Do nothing
	}

	@Override
	public void parse(final KCClientCookie cookie, final String value) throws KCCookieError
	{
		KCUtilArgs.notNull(cookie, "Cookie");
		cookie.setSecure(true);
	}

	@Override
	public boolean match(final KCCookie cookie, final KCCookieOrigin origin)
	{
		KCUtilArgs.notNull(cookie, "Cookie");
		KCUtilArgs.notNull(origin, "Cookie origin");
		//return !cookie.getSecure() || "https".equalsIgnoreCase(uri.getScheme());
		return !cookie.isSecure() || origin.isSecure();
	}

	@Override
	public String getAttributeName()
	{
		return KCCookie.SECURE_ATTR;
	}

}

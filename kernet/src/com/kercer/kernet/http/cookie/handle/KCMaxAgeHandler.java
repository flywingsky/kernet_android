package com.kercer.kernet.http.cookie.handle;

import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kernet.http.cookie.KCClientCookie;
import com.kercer.kernet.http.cookie.KCCookie;
import com.kercer.kernet.http.cookie.KCCookieOrigin;
import com.kercer.kernet.http.error.KCCookieError;
import com.kercer.kercore.annotation.KCImmutable;

import java.util.Date;

/**
 * Created by zihong on 15/12/16.
 */
@KCImmutable
public class KCMaxAgeHandler implements KCCookieHandler
{

	public KCMaxAgeHandler()
	{
		super();
	}

	@Override
	public void validate(final KCCookie cookie, final KCCookieOrigin origin) throws KCCookieError
	{
		// Do nothing
	}

	@Override
	public boolean match(final KCCookie cookie, final KCCookieOrigin origin)
	{
		// Always match
		return true;
	}

	@Override
	public void parse(final KCClientCookie cookie, final String value) throws KCCookieError
	{
		KCUtilArgs.notNull(cookie, "Cookie");
		if (value == null)
		{
			throw new KCCookieError("Missing value for 'max-age' attribute");
		}
		final int age;
		try
		{
			age = Integer.parseInt(value);
		}
		catch (final NumberFormatException e)
		{
			throw new KCCookieError("Invalid 'max-age' attribute: " + value);
		}
		if (age < 0)
		{
			throw new KCCookieError("Negative 'max-age' attribute: " + value);
		}
		cookie.setExpiryDate(new Date(System.currentTimeMillis() + age * 1000L));
	}

	@Override
	public String getAttributeName()
	{
		return KCCookie.MAX_AGE_ATTR;
	}

}

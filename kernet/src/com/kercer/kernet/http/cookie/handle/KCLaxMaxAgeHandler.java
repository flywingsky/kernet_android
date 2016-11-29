package com.kercer.kernet.http.cookie.handle;

import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kernet.http.cookie.KCClientCookie;
import com.kercer.kernet.http.cookie.KCCookie;
import com.kercer.kernet.http.cookie.KCCookieOrigin;
import com.kercer.kernet.http.error.KCCookieError;
import com.kercer.kercore.util.KCUtilText;
import com.kercer.kercore.annotation.KCImmutable;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zihong on 15/12/16.
 */
@KCImmutable
public class KCLaxMaxAgeHandler implements KCCookieHandler
{

	private final static Pattern MAX_AGE_PATTERN = Pattern.compile("^\\-?[0-9]+$");

	public KCLaxMaxAgeHandler()
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
		if (KCUtilText.isBlank(value))
		{
			return;
		}
		final Matcher matcher = MAX_AGE_PATTERN.matcher(value);
		if (matcher.matches())
		{
			final int age;
			try
			{
				age = Integer.parseInt(value);
			}
			catch (final NumberFormatException e)
			{
				return;
			}
			final Date expiryDate = age > 0 ? new Date(System.currentTimeMillis() + age * 1000L) : new Date(Long.MIN_VALUE);
			cookie.setExpiryDate(expiryDate);
		}
	}

	@Override
	public String getAttributeName()
	{
		return KCCookie.MAX_AGE_ATTR;
	}

}

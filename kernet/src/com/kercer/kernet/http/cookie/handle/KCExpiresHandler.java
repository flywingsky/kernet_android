package com.kercer.kernet.http.cookie.handle;

import com.kercer.kercore.util.KCUtilDate;
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
public class KCExpiresHandler implements KCCookieHandler
{

	/** Valid date patterns */
	private final String[] datepatterns;

	public KCExpiresHandler(final String[] datepatterns)
	{
		KCUtilArgs.notNull(datepatterns, "Array of date patterns");
		this.datepatterns = datepatterns;
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
			throw new KCCookieError("Missing value for 'expires' attribute");
		}
		final Date expiry = KCUtilDate.parseDate(value, this.datepatterns);
		if (expiry == null)
		{
			throw new KCCookieError("Invalid 'expires' attribute: " + value);
		}
		cookie.setExpiryDate(expiry);
	}

	@Override
	public String getAttributeName()
	{
		return KCCookie.EXPIRES_ATTR;
	}

}

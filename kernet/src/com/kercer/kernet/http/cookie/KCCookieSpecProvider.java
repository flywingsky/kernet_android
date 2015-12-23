package com.kercer.kernet.http.cookie;

/**
 * Created by zihong on 15/12/16.
 */

import com.kercer.kercore.annotation.KCImmutable;
import com.kercer.kernet.http.cookie.handle.KCDomainHandler;
import com.kercer.kernet.http.cookie.handle.KCExpiresHandler;
import com.kercer.kernet.http.cookie.handle.KCMaxAgeHandler;
import com.kercer.kernet.http.cookie.handle.KCPathHandler;
import com.kercer.kernet.http.cookie.handle.KCSecureHandler;
import com.kercer.kernet.http.cookie.handle.KCLaxExpiresHandler;
import com.kercer.kernet.http.cookie.handle.KCLaxMaxAgeHandler;
import com.kercer.kernet.http.cookie.handle.suffix.KCPublicSuffixDomainFilter;
import com.kercer.kernet.http.cookie.handle.suffix.KCPublicSuffixMatcher;
import com.kercer.kernet.http.error.KCCookieError;

/**
 * CookieSpecProvider implementation that provides an instance of RFC 6265 conformant cookie policy. The instance returned by this factory can be
 * shared by multiple threads.
 *
 */
@KCImmutable
public class KCCookieSpecProvider
{

	public enum KCCompatibilityLevel
	{
		STRICT,
		RELAXED, // default and STANDARD
		IE_MEDIUM_SECURITY
	}

	private final KCCompatibilityLevel compatibilityLevel;
	private final KCPublicSuffixMatcher publicSuffixMatcher;

	private volatile KCCookieSpec cookieSpec;

	public KCCookieSpecProvider(final KCCompatibilityLevel compatibilityLevel, final KCPublicSuffixMatcher publicSuffixMatcher)
	{
		super();
		this.compatibilityLevel = compatibilityLevel != null ? compatibilityLevel : KCCompatibilityLevel.RELAXED;
		this.publicSuffixMatcher = publicSuffixMatcher;
	}

	public KCCookieSpecProvider(final KCPublicSuffixMatcher publicSuffixMatcher)
	{
		this(KCCompatibilityLevel.RELAXED, publicSuffixMatcher);
	}

	public KCCookieSpecProvider()
	{
		this(KCCompatibilityLevel.RELAXED, null);
	}

	public KCCookieSpec create()
	{
		if (cookieSpec == null)
		{
			synchronized (this)
			{
				if (cookieSpec == null)
				{
					switch (this.compatibilityLevel)
					{
					case STRICT:
						this.cookieSpec = new KCCookieSpec(new KCPathHandler(), KCPublicSuffixDomainFilter.decorate(new KCDomainHandler(), this.publicSuffixMatcher),
								new KCMaxAgeHandler(), new KCSecureHandler(), new KCExpiresHandler(KCCookieSpec.DATE_PATTERNS));
						break;
					case IE_MEDIUM_SECURITY:
						this.cookieSpec = new KCCookieSpec(new KCPathHandler()
						{
							@Override
							public void validate(final KCCookie cookie, final KCCookieOrigin origin) throws KCCookieError
							{
								// No validation
							}
						}, KCPublicSuffixDomainFilter.decorate(new KCDomainHandler(), this.publicSuffixMatcher), new KCMaxAgeHandler(), new KCSecureHandler(),
								new KCExpiresHandler(KCCookieSpec.DATE_PATTERNS));
						break;
					default:
						this.cookieSpec = new KCCookieSpec(new KCPathHandler(), KCPublicSuffixDomainFilter.decorate(new KCDomainHandler(), this.publicSuffixMatcher),
								new KCLaxMaxAgeHandler(), new KCSecureHandler(), new KCLaxExpiresHandler());
					}
				}
			}
		}
		return this.cookieSpec;
	}

}

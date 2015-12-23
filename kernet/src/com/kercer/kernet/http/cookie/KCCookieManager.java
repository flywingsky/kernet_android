package com.kercer.kernet.http.cookie;

import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kernet.http.KCHttpRequest;
import com.kercer.kernet.http.base.KCHeader;
import com.kercer.kernet.http.base.KCHeaderGroup;
import com.kercer.kernet.http.error.KCCookieError;
import com.kercer.kernet.uri.KCURI;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by zihong on 15/12/16.
 */
public class KCCookieManager
{
	private KCCookieStore mCookieStore;
	private KCCookieSpecProvider mCookieSpecProvider;


	public KCCookieManager()
	{
		this(null, null);
	}

	public KCCookieManager(KCCookieStore aCookieStore, KCCookieSpecProvider aCookieSpecProvider)
	{
		mCookieStore =  aCookieStore!=null ? aCookieStore : new KCCookieStore();
		mCookieSpecProvider = aCookieSpecProvider!=null ? aCookieSpecProvider : new KCCookieSpecProvider();
	}

	public synchronized void processResponse(KCHeaderGroup aHeaderGroup, KCURI aUri)
	{
		if (aHeaderGroup != null)
		{
			KCHeader[] headers = aHeaderGroup.getHeaders("Set-Cookie");
			if(headers.length > 0)
			{
				KCCookieOrigin cookieOrigin = new KCCookieOrigin(aUri);
				storeCookies(headers, getCookieSpec(), cookieOrigin, mCookieStore);
			}
		}
	}

	private void storeCookies(final KCHeader[] aHeaders, final KCCookieSpec aCookieSpec,
							  final KCCookieOrigin aCookieOrigin, final KCCookieStore aCookieStore)
	{
		int count = aHeaders.length;
		for (int i = 0; i < count; ++i)
		{
			final KCHeader header = aHeaders[i];
			try
			{
				final List<KCCookie> cookies = aCookieSpec.parse(header, aCookieOrigin);
				for (final KCCookie cookie : cookies)
				{
					try
					{
						aCookieSpec.validate(cookie, aCookieOrigin);
						aCookieStore.addCookie(cookie);
					}
					catch (final KCCookieError ex)
					{
						ex.printStackTrace();
					}
				}
			}
			catch (final KCCookieError ex)
			{
				ex.printStackTrace();
			}
		}
	}


	public static String formatCooke(final KCCookie cookie)
	{
		final StringBuilder buf = new StringBuilder();
		buf.append(cookie.getName());
		buf.append("=\"");
		String v = cookie.getValue();
		if (v != null)
		{
			if (v.length() > 100)
			{
				v = v.substring(0, 100) + "...";
			}
			buf.append(v);
		}
		buf.append("\"");
		buf.append(", domain:");
		buf.append(cookie.getDomain());
		buf.append(", path:");
		buf.append(cookie.getPath());
		buf.append(", expiry:");
		buf.append(cookie.getExpiryDate());
		return buf.toString();
	}

	//Get all cookies in cookie store which are not expired.
	private List<KCCookie> getCookies(final KCURI aUri, final KCCookieSpec aCookieSpec, final KCCookieStore aCookieStore)
	{
		// Find cookies matching the given origin
		final List<KCCookie> matchedCookies = new ArrayList<KCCookie>();
		final KCCookieOrigin cookieOrigin = new KCCookieOrigin(aUri);

		if (aCookieStore == null)
		{
			return matchedCookies;
		}

		// Get all cookies available in the HTTP state
		final List<KCCookie> cookies = aCookieStore.getCookies();
		final Date now = new Date();
		boolean expired = false;
		for (final KCCookie cookie : cookies)
		{
			if (!cookie.isExpired(now))
			{
				if (aCookieSpec.match(cookie, cookieOrigin))
				{
					matchedCookies.add(cookie);
				}
			}
			else
			{
				expired = true;
			}
		}
		// Per RFC 6265, 5.3
		// The user agent must evict all expired cookies if, at any time, an expired cookie
		// exists in the cookie store
		if (expired)
		{
			aCookieStore.clearExpired(now);
		}

		return matchedCookies;
	}

	public synchronized void processRequest(final KCHttpRequest request) throws IOException
	{
		KCUtilArgs.notNull(request, "HTTP request");
		final int method = request.getMethod();
		if (method == KCHttpRequest.Method.TRACE)
			return;

		try
		{
			KCURI uri = KCURI.parse(request.getUrl());

			// Get an instance of the selected cookie policy
			if (mCookieSpecProvider == null)
			{
				return;
			}
			final KCCookieSpec cookieSpec = getCookieSpec();
			final List<KCCookie> matchedCookies = getCookies(uri, cookieSpec, mCookieStore);

			try
			{
				KCHeaderGroup headerGroup = request.getHeaders();
				KCHeader[] headers = headerGroup.getHeaders("Cookie");
				KCCookieOrigin cookieOrigin = new KCCookieOrigin(uri);
				int count = headers.length;
				for (int i = 0; i<count; i++)
				{
					KCHeader header = headers[i];
					try
					{
						final List<KCCookie> cookies = cookieSpec.parse(header, cookieOrigin);
						final Date now = new Date();
						for (final KCCookie cookie : cookies)
						{
							try
							{
								cookieSpec.validate(cookie, cookieOrigin);
								if (!cookie.isExpired(now))
								{
									if (cookieSpec.match(cookie, cookieOrigin))
									{
										matchedCookies.add(cookie);
									}
								}
							}
							catch (final KCCookieError ex)
							{
								ex.printStackTrace();
							}
						}

						//remove frome header
						headerGroup.removeHeader(header);
					}
					catch (final KCCookieError ex)
					{
						ex.printStackTrace();
					}
				}

			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			// Generate Cookie request headers
			if (matchedCookies!=null && !matchedCookies.isEmpty())
			{
				final List<KCHeader> headers = cookieSpec.cookiesToHeaders(matchedCookies);
				for (final KCHeader header : headers)
				{
					request.addHeader(header);
				}
			}
		}
		catch (final URISyntaxException e)
		{
			e.printStackTrace();
		}

	}

	public KCCookieSpec getCookieSpec()
	{
		return mCookieSpecProvider.create();
	}
}

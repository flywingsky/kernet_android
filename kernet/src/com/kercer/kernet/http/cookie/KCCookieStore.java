package com.kercer.kernet.http.cookie;

import com.kercer.kercore.annotation.KCGuardedBy;
import com.kercer.kercore.annotation.KCThreadSafe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by zihong on 15/12/16.
 */
@KCThreadSafe
public class KCCookieStore implements Serializable
{

	private static final long serialVersionUID = -7581093305228232025L;

	@KCGuardedBy("this")
	private final TreeSet<KCCookie> cookies;

	public KCCookieStore()
	{
		super();
		this.cookies = new TreeSet<KCCookie>(new KCCookieComparatorIdentity());
	}

	/**
	 * Adds an {@link KCCookie HTTP cookie}, replacing any existing equivalent cookies. If the given cookie has already expired it will not be added,
	 * but existing values will still be removed.
	 *
	 * @param cookie
	 *            the {@link KCCookie cookie} to be added
	 *
	 * @see #addCookies(KCCookie[])
	 *
	 */
	public synchronized void addCookie(final KCCookie cookie)
	{
		if (cookie != null)
		{
			// first remove any old cookie that is equivalent
			cookies.remove(cookie);
			if (!cookie.isExpired(new Date()))
			{
				cookies.add(cookie);
			}
		}
	}

	/**
	 * Adds an array of {@link KCCookie HTTP cookies}. Cookies are added individually and in the given array order. If any of the given cookies has
	 * already expired it will not be added, but existing values will still be removed.
	 *
	 * @param cookies
	 *            the {@link KCCookie cookies} to be added
	 *
	 * @see #addCookie(KCCookie)
	 *
	 */
	public synchronized void addCookies(final KCCookie[] cookies)
	{
		if (cookies != null)
		{
			for (final KCCookie cooky : cookies)
			{
				this.addCookie(cooky);
			}
		}
	}

	/**
	 * Returns an immutable array of {@link KCCookie cookies} that this HTTP state currently contains.
	 *
	 * @return an array of {@link KCCookie cookies}.
	 */
	public synchronized List<KCCookie> getCookies()
	{
		// create defensive copy so it won't be concurrently modified
		return new ArrayList<KCCookie>(cookies);
	}

	/**
	 * Removes all of {@link KCCookie cookies} in this HTTP state that have expired by the specified {@link java.util.Date date}.
	 *
	 * @return true if any cookies were purged.
	 *
	 * @see KCCookie#isExpired(Date)
	 */
	public synchronized boolean clearExpired(final Date date)
	{
		if (date == null)
		{
			return false;
		}
		boolean removed = false;
		for (final Iterator<KCCookie> it = cookies.iterator(); it.hasNext();)
		{
			if (it.next().isExpired(date))
			{
				it.remove();
				removed = true;
			}
		}
		return removed;
	}

	/**
	 * Clears all cookies.
	 */
	public synchronized void clear()
	{
		cookies.clear();
	}

	@Override
	public synchronized String toString()
	{
		return cookies.toString();
	}

}

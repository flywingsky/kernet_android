package com.kercer.kernet.http.cookie;

import com.kercer.kercore.annotation.KCGuardedBy;
import com.kercer.kercore.annotation.KCThreadSafe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by zihong on 15/12/16.
 */
@KCThreadSafe
public class KCCookieStore implements Serializable
{

	private static final long serialVersionUID = -7581093305228232025L;

	@KCGuardedBy("this")
	private final TreeSet<KCCookie> cookies;

	// use ReentrantLock instead of syncronized for scalability
	private ReentrantLock lock = null;

	public KCCookieStore()
	{
		super();
		this.cookies = new TreeSet<KCCookie>(new KCCookieComparatorIdentity());
		lock = new ReentrantLock(false);
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
	public void addCookie(final KCCookie cookie)
	{
		lock.lock();
		try
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
		finally
		{
			lock.unlock();
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
	public void addCookies(final KCCookie[] cookies)
	{
		lock.lock();
		try
		{
			if (cookies != null)
			{
				for (final KCCookie cooky : cookies)
				{
					this.addCookie(cooky);
				}
			}
		}
		finally
		{
			lock.unlock();
		}

	}

	/**
	 * Returns an immutable array of {@link KCCookie cookies} that this HTTP state currently contains.
	 *
	 * @return an array of {@link KCCookie cookies}.
	 */
	public List<KCCookie> getCookies()
	{
		List<KCCookie> list;
		lock.lock();
		try
		{
			// create defensive copy so it won't be concurrently modified
			list = new ArrayList<KCCookie>(cookies);
		}
		finally
		{
			lock.unlock();
		}
		return list;
	}


	/**
	 * Remove a cookie from store
	 */
	public boolean remove(KCCookie cookie)
	{
		// argument can't be null
		if (cookie == null)
		{
			throw new NullPointerException("cookie is null");
		}

		boolean modified = false;
		lock.lock();
		try
		{
			modified = cookies.remove(cookie);
		}
		finally
		{
			lock.unlock();
		}

		return modified;
	}


	/**
	 * Remove all cookies from store
	 */
	public void removeAll(String aCookieName)
	{
		// argument can't be null
		if (aCookieName == null)
		{
			throw new NullPointerException("cookie name is null");
		}

		lock.lock();
		try
		{
			for (KCCookie cookie : cookies)
			{
				if (cookie != null && cookie.getName().equals(aCookieName))
				{
					cookies.remove(cookie);
				}
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Removes all of {@link KCCookie cookies} in this HTTP state that have expired by the specified {@link java.util.Date date}.
	 *
	 * @return true if any cookies were purged.
	 *
	 * @see KCCookie#isExpired(Date)
	 */
	public boolean clearExpired(final Date date)
	{
		if (date == null)
		{
			return false;
		}
		boolean removed = false;

		lock.lock();
		try
		{
			for (final Iterator<KCCookie> it = cookies.iterator(); it.hasNext();)
			{
				if (it.next().isExpired(date))
				{
					it.remove();
					removed = true;
				}
			}
		}
		finally
		{
			lock.unlock();
		}

		return removed;
	}

	/**
	 * Clears all cookies.
	 */
	public boolean clear()
	{
		lock.lock();
		try
		{
			if (cookies.isEmpty())
			{
				return false;
			}
			cookies.clear();
		}
		finally
		{
			lock.unlock();
		}
		return true;
	}

	@Override
	public synchronized String toString()
	{
		return cookies.toString();
	}

}

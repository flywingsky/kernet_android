package com.kercer.kernet.http.cookie;

/**
 * Created by zihong on 15/12/16.
 */

import com.kercer.kercore.annotation.KCImmutable;

import java.io.Serializable;
import java.util.Comparator;

/**
 * This cookie comparator can be used to compare identity of cookies.
 * <p>
 * Cookies are considered identical if their names are equal and their domain attributes match ignoring case.
 *
 */
@KCImmutable
public class KCCookieComparatorIdentity implements Serializable, Comparator<KCCookie>
{

	private static final long serialVersionUID = 4466565437490631532L;

	@Override
	public int compare(final KCCookie c1, final KCCookie c2)
	{
		int res = c1.getName().compareTo(c2.getName());
		if (res == 0)
		{
			// do not differentiate empty and null domains
			String d1 = c1.getDomain();
			if (d1 == null)
			{
				d1 = "";
			}
			else if (d1.indexOf('.') == -1)
			{
				d1 = d1 + ".local";
			}
			String d2 = c2.getDomain();
			if (d2 == null)
			{
				d2 = "";
			}
			else if (d2.indexOf('.') == -1)
			{
				d2 = d2 + ".local";
			}
			res = d1.compareToIgnoreCase(d2);
		}
		if (res == 0)
		{
			String p1 = c1.getPath();
			if (p1 == null)
			{
				p1 = "/";
			}
			String p2 = c2.getPath();
			if (p2 == null)
			{
				p2 = "/";
			}
			res = p1.compareTo(p2);
		}
		return res;
	}

}

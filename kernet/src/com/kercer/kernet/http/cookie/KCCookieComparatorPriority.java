package com.kercer.kernet.http.cookie;

/**
 * Created by zihong on 15/12/16.
 */

import com.kercer.kercore.annotation.KCImmutable;

import java.util.Comparator;
import java.util.Date;

/**
 * This cookie comparator ensures that cookies with longer paths take precedence over cookies with shorter path. Among cookies with equal path length
 * cookies with ealier creation time take precedence over cookies with later creation time
 *
 */
@KCImmutable
public class KCCookieComparatorPriority implements Comparator<KCCookie>
{

	public static final KCCookieComparatorPriority INSTANCE = new KCCookieComparatorPriority();

	private int getPathLength(final KCCookie cookie)
	{
		final String path = cookie.getPath();
		return path != null ? path.length() : 1;
	}

	@Override
	public int compare(final KCCookie c1, final KCCookie c2)
	{
		final int l1 = getPathLength(c1);
		final int l2 = getPathLength(c2);
		// TODO: processChallenge this class once Cookie interface has been expended with #getCreationTime method
		final int result = l2 - l1;
		if (result == 0 && c1 instanceof KCClientCookie && c2 instanceof KCClientCookie)
		{
			final Date d1 = ((KCClientCookie) c1).getCreationDate();
			final Date d2 = ((KCClientCookie) c2).getCreationDate();
			if (d1 != null && d2 != null)
			{
				return (int) (d1.getTime() - d2.getTime());
			}
		}
		return result;
	}

}

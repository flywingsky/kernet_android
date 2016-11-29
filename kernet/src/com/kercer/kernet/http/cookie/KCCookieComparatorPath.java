package com.kercer.kernet.http.cookie;

import java.io.Serializable;
import java.util.Comparator;

/**
 * This cookie comparator ensures that multiple cookies satisfying
 * a common criteria are ordered in the {@code KCCookie} header such
 * that those with more specific Path attributes precede those with
 * less specific.
 *
 * <p>
 * This comparator assumes that Path attributes of two cookies
 * path-match a commmon request-URI. Otherwise, the result of the
 * comparison is undefined.
 * </p>
 *
 *
 * Created by zihong on 2016/11/29.
 */

public class KCCookieComparatorPath implements Serializable, Comparator<KCCookie>
{

    public static final KCCookieComparatorPath INSTANCE = new KCCookieComparatorPath();

    private static final long serialVersionUID = 7523645369616405818L;

    private String normalizePath(final KCCookie cookie)
    {
        String path = cookie.getPath();
        if (path == null)
        {
            path = "/";
        }
        if (!path.endsWith("/"))
        {
            path = path + '/';
        }
        return path;
    }

    @Override
    public int compare(final KCCookie c1, final KCCookie c2)
    {
        final String path1 = normalizePath(c1);
        final String path2 = normalizePath(c2);
        if (path1.equals(path2))
        {
            return 0;
        }
        else if (path1.startsWith(path2))
        {
            return -1;
        }
        else if (path2.startsWith(path1))
        {
            return 1;
        }
        else
        {
            // Does not really matter
            return 0;
        }
    }
}

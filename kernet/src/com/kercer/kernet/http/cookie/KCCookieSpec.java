package com.kercer.kernet.http.cookie;

/**
 * Created by zihong on 15/12/16.
 */

import android.annotation.SuppressLint;

import com.kercer.kercore.annotation.KCThreadSafe;
import com.kercer.kercore.base.KCKeyValuePair;
import com.kercer.kercore.buffer.KCCharArrayBuffer;
import com.kercer.kercore.parser.KCParserCursor;
import com.kercer.kercore.parser.KCTokenParser;
import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kercore.util.KCUtilDate;
import com.kercer.kernet.http.base.KCHeader;
import com.kercer.kernet.http.base.KCHeaderElement;
import com.kercer.kernet.http.cookie.handle.KCCookieHandler;
import com.kercer.kernet.http.error.KCCookieError;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cookie management functions shared by RFC C6265 compliant specification.
 */
@SuppressLint("DefaultLocale")
@KCThreadSafe
public class KCCookieSpec
{

    final static String[] DATE_PATTERNS = {KCUtilDate.PATTERN_RFC1123, KCUtilDate.PATTERN_RFC1036, KCUtilDate.PATTERN_ASCTIME};

    private final static char PARAM_DELIMITER = ';';
    private final static char COMMA_CHAR = ',';
    private final static char EQUAL_CHAR = '=';
    private final static char DQUOTE_CHAR = '"';
    private final static char ESCAPE_CHAR = '\\';

    // IMPORTANT!
    // These private static variables must be treated as immutable and never exposed outside this class
    private static final BitSet TOKEN_DELIMS = KCTokenParser.INIT_BITSET(EQUAL_CHAR, PARAM_DELIMITER);
    private static final BitSet VALUE_DELIMS = KCTokenParser.INIT_BITSET(PARAM_DELIMITER);
    private static final BitSet SPECIAL_CHARS = KCTokenParser.INIT_BITSET(' ', DQUOTE_CHAR, COMMA_CHAR, PARAM_DELIMITER, ESCAPE_CHAR);

    private final KCCookieHandler[] mAttribHandlers;
    private final Map<String, KCCookieHandler> mAttribHandlerMap;
    private final KCTokenParser mTokenParser;

    protected KCCookieSpec(final KCCookieHandler... aHandlers)
    {
        super();
        this.mAttribHandlers = aHandlers.clone();
        this.mAttribHandlerMap = new ConcurrentHashMap<String, KCCookieHandler>(aHandlers.length);
        for (KCCookieHandler handler : aHandlers)
        {
            this.mAttribHandlerMap.put(handler.getAttributeName().toLowerCase(), handler);
        }
        this.mTokenParser = KCTokenParser.INSTANCE;
    }

    static String getDefaultPath(final KCCookieOrigin aOrigin)
    {
        String defaultPath = aOrigin.getPath();
        int lastSlashIndex = defaultPath.lastIndexOf('/');
        if (lastSlashIndex >= 0)
        {
            if (lastSlashIndex == 0)
            {
                // Do not remove the very first slash
                lastSlashIndex = 1;
            }
            defaultPath = defaultPath.substring(0, lastSlashIndex);
        }
        return defaultPath;
    }

    static String getDefaultDomain(final KCCookieOrigin aOrigin)
    {
        return aOrigin.getHost();
    }

    public final List<KCCookie> parse(final KCHeader aHeader, final KCCookieOrigin aOrigin) throws KCCookieError
    {
        KCUtilArgs.notNull(aHeader, "Header");
        KCUtilArgs.notNull(aOrigin, "Cookie origin");
        //		if (!aHeader.getName().equalsIgnoreCase("Set-Cookie"))
        //		{
        //			throw new KCCookieError("Unrecognized cookie header: '" + aHeader.toString() + "'");
        //		}
        final KCCharArrayBuffer buffer;
        final KCParserCursor cursor;


        final String s = aHeader.getValue();
        if (s == null)
        {
            throw new KCCookieError("Header value is null");
        }
        buffer = new KCCharArrayBuffer(s.length());
        buffer.append(s);
        cursor = new KCParserCursor(0, buffer.length());


        final String name = mTokenParser.parseToken(buffer, cursor, TOKEN_DELIMS);
        if (name.length() == 0)
        {
            return Collections.emptyList();
        }
        if (cursor.atEnd())
        {
            return Collections.emptyList();
        }
        final int valueDelim = buffer.charAt(cursor.getPos());
        cursor.updatePos(cursor.getPos() + 1);
        if (valueDelim != '=')
        {
            throw new KCCookieError("Cookie value is invalid: '" + aHeader.toString() + "'");
        }
        final String value = mTokenParser.parseValue(buffer, cursor, VALUE_DELIMS);
        if (!cursor.atEnd())
        {
            cursor.updatePos(cursor.getPos() + 1);
        }
        final KCClientCookie cookie = new KCClientCookie(name, value);
        cookie.setPath(getDefaultPath(aOrigin));
        cookie.setDomain(getDefaultDomain(aOrigin));
        cookie.setCreationDate(new Date());

        final Map<String, String> attribMap = new LinkedHashMap<String, String>();
        while (!cursor.atEnd())
        {
            final String paramName = mTokenParser.parseToken(buffer, cursor, TOKEN_DELIMS).toLowerCase();
            String paramValue = null;
            if (!cursor.atEnd())
            {
                final int paramDelim = buffer.charAt(cursor.getPos());
                cursor.updatePos(cursor.getPos() + 1);
                if (paramDelim == EQUAL_CHAR)
                {
                    paramValue = mTokenParser.parseToken(buffer, cursor, VALUE_DELIMS);
                    if (!cursor.atEnd())
                    {
                        cursor.updatePos(cursor.getPos() + 1);
                    }
                }
            }
            cookie.setAttribute(paramName, paramValue);
            attribMap.put(paramName, paramValue);
        }
        // Ignore 'Expires' if 'Max-Age' is present
        if (attribMap.containsKey(KCCookie.MAX_AGE_ATTR))
        {
            attribMap.remove(KCCookie.EXPIRES_ATTR);
        }

        for (Map.Entry<String, String> entry : attribMap.entrySet())
        {
            final String paramName = entry.getKey();
            final String paramValue = entry.getValue();
            final KCCookieHandler handler = this.mAttribHandlerMap.get(paramName);
            if (handler != null)
            {
                handler.parse(cookie, paramValue);
            }
        }

        return Collections.<KCCookie>singletonList(cookie);
    }


    public List<KCCookie> parse(final KCHeaderElement[] aElems, final KCCookieOrigin aOrigin) throws KCCookieError
    {
        final List<KCCookie> cookies = new ArrayList<>(aElems.length);
        for (final KCHeaderElement headerelement : aElems)
        {
            final String name = headerelement.getName();
            final String value = headerelement.getValue();
            if (name == null || name.length() == 0)
            {
                throw new KCCookieError("Cookie name may not be empty");
            }

            final KCClientCookie cookie = new KCClientCookie(name, value);
            cookie.setPath(getDefaultPath(aOrigin));
            cookie.setDomain(getDefaultDomain(aOrigin));

            // cycle through the parameters
            final KCKeyValuePair[] attribs = headerelement.getParameters();
            for (int j = attribs.length - 1; j >= 0; j--)
            {
                final KCKeyValuePair attrib = attribs[j];
                final String s = attrib.getName().toLowerCase();

                cookie.setAttribute(s, attrib.getValue());

                final KCCookieHandler handler = this.mAttribHandlerMap.get(s);
                if (handler != null)
                {
                    handler.parse(cookie, attrib.getValue());
                }
            }
            cookies.add(cookie);
        }
        return cookies;
    }

    public final void validate(final KCCookie aCookie, final KCCookieOrigin aOrigin) throws KCCookieError
    {
        KCUtilArgs.notNull(aCookie, "Cookie");
        KCUtilArgs.notNull(aOrigin, "Cookie origin");
        for (final KCCookieHandler handler : this.mAttribHandlers)
        {
            handler.validate(aCookie, aOrigin);
        }
    }

    public final boolean match(final KCCookie aCookie, final KCCookieOrigin aOrigin)
    {
        KCUtilArgs.notNull(aCookie, "Cookie");
        KCUtilArgs.notNull(aOrigin, "Cookie origin");
        for (final KCCookieHandler handler : this.mAttribHandlers)
        {
            if (!handler.match(aCookie, aOrigin))
            {
                return false;
            }
        }
        return true;
    }

    public List<KCHeader> cookiesToHeaders(final List<KCCookie> aCookies)
    {
        KCUtilArgs.notEmpty(aCookies, "List of cookies");
        final List<? extends KCCookie> sortedCookies;
        if (aCookies.size() > 1)
        {
            // Create a mutable copy and sort the copy.
            sortedCookies = new ArrayList<KCCookie>(aCookies);
            Collections.sort(sortedCookies, KCCookieComparatorPriority.INSTANCE);
        }
        else
        {
            sortedCookies = aCookies;
        }
        final KCCharArrayBuffer buffer = new KCCharArrayBuffer(20 * sortedCookies.size());
        // buffer.append("Cookie");
        // buffer.append(": ");
        for (int n = 0; n < sortedCookies.size(); n++)
        {
            final KCCookie cookie = sortedCookies.get(n);
            if (n > 0)
            {
                buffer.append(PARAM_DELIMITER);
                buffer.append(' ');
            }
            buffer.append(cookie.getName());
            final String s = cookie.getValue();
            if (s != null)
            {
                buffer.append(EQUAL_CHAR);
                if (containsSpecialChar(s))
                {
                    buffer.append(DQUOTE_CHAR);
                    for (int i = 0; i < s.length(); i++)
                    {
                        final char ch = s.charAt(i);
                        if (ch == DQUOTE_CHAR || ch == ESCAPE_CHAR)
                        {
                            buffer.append(ESCAPE_CHAR);
                        }
                        buffer.append(ch);
                    }
                    buffer.append(DQUOTE_CHAR);
                }
                else
                {
                    buffer.append(s);
                }
            }
        }
        final List<KCHeader> headers = new ArrayList<KCHeader>(1);
        headers.add(new KCHeader("Cookie", buffer.toString()));
        return headers;
    }


    private boolean containsSpecialChar(final CharSequence aChar)
    {
        return containsChars(aChar, SPECIAL_CHARS);
    }

    private boolean containsChars(final CharSequence aChar, final BitSet aChars)
    {
        for (int i = 0; i < aChar.length(); i++)
        {
            final char ch = aChar.charAt(i);
            if (aChars.get(ch))
            {
                return true;
            }
        }
        return false;
    }

}

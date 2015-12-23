package com.kercer.kernet.http.cookie.handle;

import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kernet.http.cookie.KCClientCookie;
import com.kercer.kernet.http.cookie.KCCookie;
import com.kercer.kernet.http.cookie.KCCookieOrigin;
import com.kercer.kernet.http.error.KCCookieError;
import com.kercer.kercore.parser.KCParserCursor;
import com.kercer.kercore.annotation.KCImmutable;

import java.util.BitSet;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zihong on 15/12/16.
 */
@KCImmutable
public class KCLaxExpiresHandler implements KCCookieHandler
{

	static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	private static final BitSet DELIMS;
	static
	{
		final BitSet bitSet = new BitSet();
		bitSet.set(0x9);
		for (int b = 0x20; b <= 0x2f; b++)
		{
			bitSet.set(b);
		}
		for (int b = 0x3b; b <= 0x40; b++)
		{
			bitSet.set(b);
		}
		for (int b = 0x5b; b <= 0x60; b++)
		{
			bitSet.set(b);
		}
		for (int b = 0x7b; b <= 0x7e; b++)
		{
			bitSet.set(b);
		}
		DELIMS = bitSet;
	}
	private static final Map<String, Integer> MONTHS;
	static
	{
		final ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<String, Integer>(12);
		map.put("jan", Calendar.JANUARY);
		map.put("feb", Calendar.FEBRUARY);
		map.put("mar", Calendar.MARCH);
		map.put("apr", Calendar.APRIL);
		map.put("may", Calendar.MAY);
		map.put("jun", Calendar.JUNE);
		map.put("jul", Calendar.JULY);
		map.put("aug", Calendar.AUGUST);
		map.put("sep", Calendar.SEPTEMBER);
		map.put("oct", Calendar.OCTOBER);
		map.put("nov", Calendar.NOVEMBER);
		map.put("dec", Calendar.DECEMBER);
		MONTHS = map;
	}

	private final static Pattern TIME_PATTERN = Pattern.compile("^([0-9]{1,2}):([0-9]{1,2}):([0-9]{1,2})([^0-9].*)?$");
	private final static Pattern DAY_OF_MONTH_PATTERN = Pattern.compile("^([0-9]{1,2})([^0-9].*)?$");
	private final static Pattern MONTH_PATTERN = Pattern.compile("^(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)(.*)?$", Pattern.CASE_INSENSITIVE);
	private final static Pattern YEAR_PATTERN = Pattern.compile("^([0-9]{2,4})([^0-9].*)?$");

	public KCLaxExpiresHandler()
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
		final KCParserCursor cursor = new KCParserCursor(0, value.length());
		final StringBuilder content = new StringBuilder();

		int second = 0, minute = 0, hour = 0, day = 0, month = 0, year = 0;
		boolean foundTime = false, foundDayOfMonth = false, foundMonth = false, foundYear = false;
		try
		{
			while (!cursor.atEnd())
			{
				skipDelims(value, cursor);
				content.setLength(0);
				copyContent(value, cursor, content);

				if (content.length() == 0)
				{
					break;
				}
				if (!foundTime)
				{
					final Matcher matcher = TIME_PATTERN.matcher(content);
					if (matcher.matches())
					{
						foundTime = true;
						hour = Integer.parseInt(matcher.group(1));
						minute = Integer.parseInt(matcher.group(2));
						second = Integer.parseInt(matcher.group(3));
						continue;
					}
				}
				if (!foundDayOfMonth)
				{
					final Matcher matcher = DAY_OF_MONTH_PATTERN.matcher(content);
					if (matcher.matches())
					{
						foundDayOfMonth = true;
						day = Integer.parseInt(matcher.group(1));
						continue;
					}
				}
				if (!foundMonth)
				{
					final Matcher matcher = MONTH_PATTERN.matcher(content);
					if (matcher.matches())
					{
						foundMonth = true;
						month = MONTHS.get(matcher.group(1).toLowerCase(Locale.ROOT));
						continue;
					}
				}
				if (!foundYear)
				{
					final Matcher matcher = YEAR_PATTERN.matcher(content);
					if (matcher.matches())
					{
						foundYear = true;
						year = Integer.parseInt(matcher.group(1));
						continue;
					}
				}
			}
		}
		catch (NumberFormatException ignore)
		{
			throw new KCCookieError("Invalid 'expires' attribute: " + value);
		}
		if (!foundTime || !foundDayOfMonth || !foundMonth || !foundYear)
		{
			throw new KCCookieError("Invalid 'expires' attribute: " + value);
		}
		if (year >= 70 && year <= 99)
		{
			year = 1900 + year;
		}
		if (year >= 0 && year <= 69)
		{
			year = 2000 + year;
		}
		if (day < 1 || day > 31 || year < 1601 || hour > 23 || minute > 59 || second > 59)
		{
			throw new KCCookieError("Invalid 'expires' attribute: " + value);
		}

		final Calendar c = Calendar.getInstance();
		c.setTimeZone(UTC);
		c.setTimeInMillis(0L);
		c.set(Calendar.SECOND, second);
		c.set(Calendar.MINUTE, minute);
		c.set(Calendar.HOUR_OF_DAY, hour);
		c.set(Calendar.DAY_OF_MONTH, day);
		c.set(Calendar.MONTH, month);
		c.set(Calendar.YEAR, year);
		cookie.setExpiryDate(c.getTime());
	}

	private void skipDelims(final CharSequence buf, final KCParserCursor cursor)
	{
		int pos = cursor.getPos();
		final int indexFrom = cursor.getPos();
		final int indexTo = cursor.getUpperBound();
		for (int i = indexFrom; i < indexTo; i++)
		{
			final char current = buf.charAt(i);
			if (DELIMS.get(current))
			{
				pos++;
			}
			else
			{
				break;
			}
		}
		cursor.updatePos(pos);
	}

	private void copyContent(final CharSequence buf, final KCParserCursor cursor, final StringBuilder dst)
	{
		int pos = cursor.getPos();
		final int indexFrom = cursor.getPos();
		final int indexTo = cursor.getUpperBound();
		for (int i = indexFrom; i < indexTo; i++)
		{
			final char current = buf.charAt(i);
			if (DELIMS.get(current))
			{
				break;
			}
			else
			{
				pos++;
				dst.append(current);
			}
		}
		cursor.updatePos(pos);
	}

	@Override
	public String getAttributeName()
	{
		return KCCookie.EXPIRES_ATTR;
	}

}

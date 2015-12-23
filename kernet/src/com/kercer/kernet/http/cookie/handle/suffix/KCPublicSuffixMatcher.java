package com.kercer.kernet.http.cookie.handle.suffix;

/**
 * Created by zihong on 15/12/16.
 */

import android.annotation.TargetApi;
import android.os.Build;

import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kercore.annotation.KCThreadSafe;

import java.net.IDN;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class that can test if DNS names match the content of the Public Suffix List.
 * <p>
 * An up-to-date list of suffixes can be obtained from <a href="http://publicsuffix.org/">publicsuffix.org</a>
 *
 *
 */
@KCThreadSafe
public final class KCPublicSuffixMatcher
{

	private final Map<String, KCDomainType> rules;
	private final Map<String, KCDomainType> exceptions;

	public KCPublicSuffixMatcher(final Collection<String> rules, final Collection<String> exceptions)
	{
		this(KCDomainType.UNKNOWN, rules, exceptions);
	}

	public KCPublicSuffixMatcher(final KCDomainType domainType, final Collection<String> rules, final Collection<String> exceptions)
	{
		KCUtilArgs.notNull(domainType, "Domain type");
		KCUtilArgs.notNull(rules, "Domain suffix rules");
		this.rules = new ConcurrentHashMap<String, KCDomainType>(rules.size());
		for (String rule : rules)
		{
			this.rules.put(rule, domainType);
		}
		this.exceptions = new ConcurrentHashMap<String, KCDomainType>();
		if (exceptions != null)
		{
			for (String exception : exceptions)
			{
				this.exceptions.put(exception, domainType);
			}
		}
	}

	public KCPublicSuffixMatcher(final Collection<KCPublicSuffixList> lists)
	{
		KCUtilArgs.notNull(lists, "Domain suffix lists");
		this.rules = new ConcurrentHashMap<String, KCDomainType>();
		this.exceptions = new ConcurrentHashMap<String, KCDomainType>();
		for (KCPublicSuffixList list : lists)
		{
			final KCDomainType domainType = list.getType();
			final List<String> rules = list.getRules();
			for (String rule : rules)
			{
				this.rules.put(rule, domainType);
			}
			final List<String> exceptions = list.getExceptions();
			if (exceptions != null)
			{
				for (String exception : exceptions)
				{
					this.exceptions.put(exception, domainType);
				}
			}
		}
	}

	private static boolean hasEntry(final Map<String, KCDomainType> map, final String rule, final KCDomainType expectedType)
	{
		if (map == null)
		{
			return false;
		}
		final KCDomainType domainType = map.get(rule);
		if (domainType == null)
		{
			return false;
		}
		else
		{
			return expectedType == null || domainType.equals(expectedType);
		}
	}

	private boolean hasRule(final String rule, final KCDomainType expectedType)
	{
		return hasEntry(this.rules, rule, expectedType);
	}

	private boolean hasException(final String exception, final KCDomainType expectedType)
	{
		return hasEntry(this.exceptions, exception, expectedType);
	}

	/**
	 * Returns registrable part of the domain for the given domain name or {@code null} if given domain represents a public suffix.
	 *
	 * @param domain
	 * @return domain root
	 */
	public String getDomainRoot(final String domain)
	{
		return getDomainRoot(domain, null);
	}

	/**
	 * Returns registrable part of the domain for the given domain name or {@code null} if given domain represents a public suffix.
	 *
	 * @param domain
	 * @param expectedType
	 *            expected domain type or {@code null} if any.
	 * @return domain root
	 *
	 */
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public String getDomainRoot(final String domain, final KCDomainType expectedType)
	{
		if (domain == null)
		{
			return null;
		}
		if (domain.startsWith("."))
		{
			return null;
		}
		String domainName = null;
		String segment = domain.toLowerCase(Locale.ROOT);
		while (segment != null)
		{

			// An exception rule takes priority over any other matching rule.
			if (hasException(IDN.toUnicode(segment), expectedType))
			{
				return segment;
			}

			if (hasRule(IDN.toUnicode(segment), expectedType))
			{
				break;
			}

			final int nextdot = segment.indexOf('.');
			final String nextSegment = nextdot != -1 ? segment.substring(nextdot + 1) : null;

			if (nextSegment != null)
			{
				if (hasRule("*." + IDN.toUnicode(nextSegment), expectedType))
				{
					break;
				}
			}
			if (nextdot != -1)
			{
				domainName = segment;
			}
			segment = nextSegment;
		}
		return domainName;
	}

	/**
	 * Tests whether the given domain matches any of entry from the public suffix list.
	 */
	public boolean matches(final String domain)
	{
		return matches(domain, null);
	}

	/**
	 * Tests whether the given domain matches any of entry from the public suffix list.
	 *
	 * @param domain
	 * @param expectedType
	 *            expected domain type or {@code null} if any.
	 * @return {@code true} if the given domain matches any of the public suffixes.
	 *
	 */
	public boolean matches(final String domain, final KCDomainType expectedType)
	{
		if (domain == null)
		{
			return false;
		}
		final String domainRoot = getDomainRoot(domain.startsWith(".") ? domain.substring(1) : domain, expectedType);
		return domainRoot == null;
	}

}

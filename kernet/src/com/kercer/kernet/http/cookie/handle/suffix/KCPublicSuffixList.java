package com.kercer.kernet.http.cookie.handle.suffix;

/**
 * Created by zihong on 15/12/16.
 */

import com.kercer.kercore.util.KCUtilArgs;
import com.kercer.kercore.annotation.KCImmutable;

import java.util.Collections;
import java.util.List;

/**
 * Public suffix is a set of DNS names or wildcards concatenated with dots. It represents the part of a domain name which is not under the control of
 * the individual registrant
 * <p>
 * An up-to-date list of suffixes can be obtained from <a href="http://publicsuffix.org/">publicsuffix.org</a>
 *
 * @since 4.4
 */
@KCImmutable
public final class KCPublicSuffixList
{

	private final KCDomainType type;
	private final List<String> rules;
	private final List<String> exceptions;

	public KCPublicSuffixList(final KCDomainType type, final List<String> rules, final List<String> exceptions)
	{
		this.type = KCUtilArgs.notNull(type, "Domain type");
		this.rules = Collections.unmodifiableList(KCUtilArgs.notNull(rules, "Domain suffix rules"));
		this.exceptions = Collections.unmodifiableList(exceptions != null ? exceptions : Collections.<String> emptyList());
	}

	public KCPublicSuffixList(final List<String> rules, final List<String> exceptions)
	{
		this(KCDomainType.UNKNOWN, rules, exceptions);
	}

	public KCDomainType getType()
	{
		return type;
	}

	public List<String> getRules()
	{
		return rules;
	}

	public List<String> getExceptions()
	{
		return exceptions;
	}

}

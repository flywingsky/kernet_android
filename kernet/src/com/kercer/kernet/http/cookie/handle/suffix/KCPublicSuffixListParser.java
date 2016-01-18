package com.kercer.kernet.http.cookie.handle.suffix;

/**
 * Created by zihong on 15/12/16.
 */

import com.kercer.kercore.annotation.KCImmutable;
import com.kercer.kercore.util.KCUtilText;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the list from <a href="http://publicsuffix.org/">publicsuffix.org</a> and configures a PublicSuffixFilter.
 *
 */
@KCImmutable
public final class KCPublicSuffixListParser
{

	public KCPublicSuffixListParser()
	{
	}

	/**
	 * Parses the public suffix list format.
	 * <p>
	 * When creating the reader from the file, make sure to use the correct encoding (the original list is in UTF-8).
	 *
	 * @param reader
	 *            the data reader. The caller is responsible for closing the reader.
	 * @throws java.io.IOException
	 *             on error while reading from list
	 */
	public KCPublicSuffixList parse(final Reader reader) throws IOException
	{
		final List<String> rules = new ArrayList<String>();
		final List<String> exceptions = new ArrayList<String>();
		final BufferedReader r = new BufferedReader(reader);

		String line;
		while ((line = r.readLine()) != null)
		{
			if (KCUtilText.isEmpty(line))
			{
				continue;
			}
			if (line.startsWith("//"))
			{
				continue; // entire lines can also be commented using //
			}
			if (line.startsWith("."))
			{
				line = line.substring(1); // A leading dot is optional
			}
			// An exclamation mark (!) at the start of a rule marks an exception to a previous wildcard rule
			final boolean isException = line.startsWith("!");
			if (isException)
			{
				line = line.substring(1);
			}

			if (isException)
			{
				exceptions.add(line);
			}
			else
			{
				rules.add(line);
			}
		}
		return new KCPublicSuffixList(KCDomainType.UNKNOWN, rules, exceptions);
	}

	/**
	 * Parses the public suffix list format by domain type (currently supported ICANN and PRIVATE).
	 * <p>
	 * When creating the reader from the file, make sure to use the correct encoding (the original list is in UTF-8).
	 *
	 * @param reader
	 *            the data reader. The caller is responsible for closing the reader.
	 * @throws java.io.IOException
	 *             on error while reading from list
	 *
	 */
	public List<KCPublicSuffixList> parseByType(final Reader reader) throws IOException
	{
		final List<KCPublicSuffixList> result = new ArrayList<KCPublicSuffixList>(2);

		final BufferedReader r = new BufferedReader(reader);
		// final StringBuilder sb = new StringBuilder(256);

		KCDomainType domainType = null;
		List<String> rules = null;
		List<String> exceptions = null;
		String line;
		while ((line = r.readLine()) != null)
		{
			if (KCUtilText.isEmpty(line))
			{
				continue;
			}
			if (line.startsWith("//"))
			{

				if (domainType == null)
				{
					if (line.contains("===BEGIN ICANN DOMAINS==="))
					{
						domainType = KCDomainType.ICANN;
					}
					else if (line.contains("===BEGIN PRIVATE DOMAINS==="))
					{
						domainType = KCDomainType.PRIVATE;
					}
				}
				else
				{
					if (line.contains("===END ICANN DOMAINS===") || line.contains("===END PRIVATE DOMAINS==="))
					{
						if (rules != null)
						{
							result.add(new KCPublicSuffixList(domainType, rules, exceptions));
						}
						domainType = null;
						rules = null;
						exceptions = null;
					}
				}

				continue; // entire lines can also be commented using //
			}
			if (domainType == null)
			{
				continue;
			}

			if (line.startsWith("."))
			{
				line = line.substring(1); // A leading dot is optional
			}
			// An exclamation mark (!) at the start of a rule marks an exception to a previous wildcard rule
			final boolean isException = line.startsWith("!");
			if (isException)
			{
				line = line.substring(1);
			}

			if (isException)
			{
				if (exceptions == null)
				{
					exceptions = new ArrayList<String>();
				}
				exceptions.add(line);
			}
			else
			{
				if (rules == null)
				{
					rules = new ArrayList<String>();
				}
				rules.add(line);
			}
		}
		return result;
	}

}

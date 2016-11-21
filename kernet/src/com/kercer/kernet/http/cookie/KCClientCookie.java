package com.kercer.kernet.http.cookie;

import com.kercer.kercore.util.KCUtilArgs;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by zihong on 15/12/16.
 */
public class KCClientCookie implements Cloneable, Serializable, KCCookie
{

	private static final long serialVersionUID = -3869795591041535538L;


	private static final Set<String> RESERVED_NAMES = new HashSet<String>();
	static
	{
		RESERVED_NAMES.add("comment");    //           RFC 2109  RFC 2965  RFC 6265
		RESERVED_NAMES.add("commenturl"); //                     RFC 2965  RFC 6265
		RESERVED_NAMES.add("discard");    //                     RFC 2965  RFC 6265
		RESERVED_NAMES.add("domain");     // Netscape  RFC 2109  RFC 2965  RFC 6265
		RESERVED_NAMES.add("expires");    // Netscape
		RESERVED_NAMES.add("httponly");   //                               RFC 6265
		RESERVED_NAMES.add("max-age");    //           RFC 2109  RFC 2965  RFC 6265
		RESERVED_NAMES.add("path");       // Netscape  RFC 2109  RFC 2965  RFC 6265
		RESERVED_NAMES.add("port");       //                     RFC 2965  RFC 6265
		RESERVED_NAMES.add("secure");     // Netscape  RFC 2109  RFC 2965  RFC 6265
		RESERVED_NAMES.add("version");    //           RFC 2109  RFC 2965  RFC 6265
	}


	/**
	 * Default Constructor taking a name and a value. The value may be null.
	 *
	 * @param name
	 *            The name.
	 * @param value
	 *            The value.
	 */
	public KCClientCookie(final String name, final String value)
	{
		super();
		KCUtilArgs.notNull(name, "Name");
		this.name = name;
		this.attribs = new HashMap<String, String>();
		this.value = value;
	}

	/**
	 * Returns the name.
	 *
	 * @return String name The name
	 */
	@Override
	public String getName()
	{
		return this.name;
	}

	/**
	 * Returns the value.
	 *
	 * @return String value The current value.
	 */
	@Override
	public String getValue()
	{
		return this.value;
	}

	/**
	 * Sets the value
	 *
	 * @param value
	 */
	public void setValue(final String value)
	{
		this.value = value;
	}

	/**
	 * Returns the expiration {@link Date} of the cookie, or {@code null} if none exists.
	 * <p>
	 * <strong>Note:</strong> the object returned by this method is considered immutable. Changing it (e.g. using setTime()) could result in undefined
	 * behaviour. Do so at your peril.
	 * </p>
	 *
	 * @return Expiration {@link Date}, or {@code null}.
	 *
	 * @see #setExpiryDate(java.util.Date)
	 *
	 */
	@Override
	public Date getExpiryDate()
	{
		return cookieExpiryDate;
	}

	/**
	 * Sets expiration date.
	 * <p>
	 * <strong>Note:</strong> the object returned by this method is considered immutable. Changing it (e.g. using setTime()) could result in undefined
	 * behaviour. Do so at your peril.
	 * </p>
	 *
	 * @param expiryDate
	 *            the {@link Date} after which this cookie is no longer valid.
	 *
	 * @see #getExpiryDate
	 *
	 */
	public void setExpiryDate(final Date expiryDate)
	{
		cookieExpiryDate = expiryDate;
	}

	/**
	 * Returns {@code false} if the cookie should be discarded at the end of the "session"; {@code true} otherwise.
	 *
	 * @return {@code false} if the cookie should be discarded at the end of the "session"; {@code true} otherwise
	 */
	@Override
	public boolean isPersistent()
	{
		return (null != cookieExpiryDate);
	}

	/**
	 * Returns domain attribute of the cookie.
	 *
	 * @return the value of the domain attribute
	 *
	 * @see #setDomain(java.lang.String)
	 */
	@Override
	public String getDomain()
	{
		return cookieDomain;
	}

	/**
	 * Sets the domain attribute.
	 *
	 * @param domain
	 *            The value of the domain attribute
	 *
	 * @see #getDomain
	 */
	public void setDomain(final String domain)
	{
		if (domain != null)
		{
			cookieDomain = domain.toLowerCase();
		}
		else
		{
			cookieDomain = null;
		}
	}

	/**
	 * Returns the path attribute of the cookie
	 *
	 * @return The value of the path attribute.
	 *
	 * @see #setPath(java.lang.String)
	 */
	@Override
	public String getPath()
	{
		return cookiePath;
	}

	/**
	 * Sets the path attribute.
	 *
	 * @param path
	 *            The value of the path attribute
	 *
	 * @see #getPath
	 *
	 */
	public void setPath(final String path)
	{
		cookiePath = path;
	}

	/**
	 * @return {@code true} if this cookie should only be sent over secure connections.
	 * @see #setSecure(boolean)
	 */
	@Override
	public boolean isSecure()
	{
		return isSecure;
	}

	/**
	 * Sets the secure attribute of the cookie.
	 * <p>
	 * When {@code true} the cookie should only be sent using a secure protocol (https). This should only be set when the cookie's originating server
	 * used a secure protocol to set the cookie's value.
	 *
	 * @param secure
	 *            The value of the secure attribute
	 *
	 * @see #isSecure()
	 */
	public void setSecure(final boolean secure)
	{
		isSecure = secure;
	}

	/**
	 * Returns true if this cookie has expired.
	 *
	 * @param date
	 *            Current time
	 *
	 * @return {@code true} if the cookie has expired.
	 */
	@Override
	public boolean isExpired(final Date date)
	{
		KCUtilArgs.notNull(date, "Date");
		return (cookieExpiryDate != null && cookieExpiryDate.getTime() <= date.getTime());
	}

	public Date getCreationDate()
	{
		return creationDate;
	}

	public void setCreationDate(final Date creationDate)
	{
		this.creationDate = creationDate;
	}

	public void setAttribute(final String name, final String value)
	{
		this.attribs.put(name, value);
	}

	@Override
	public String getAttribute(final String name)
	{
		return this.attribs.get(name);
	}

	@Override
	public boolean containsAttribute(final String name)
	{
		return this.attribs.containsKey(name);
	}

	public boolean removeAttribute(final String name)
	{
		return this.attribs.remove(name) != null;
	}

	@Override
	public Object clone() throws CloneNotSupportedException
	{
		final KCClientCookie clone = (KCClientCookie) super.clone();
		clone.attribs = new HashMap<String, String>(this.attribs);
		return clone;
	}

	@Override
	public String toString()
	{
		final StringBuilder buffer = new StringBuilder();
		buffer.append("[name: ");
		buffer.append(this.name);
		buffer.append("; ");
		buffer.append("value: ");
		buffer.append(this.value);
		buffer.append("; ");
		buffer.append("domain: ");
		buffer.append(this.cookieDomain);
		buffer.append("; ");
		buffer.append("path: ");
		buffer.append(this.cookiePath);
		buffer.append("; ");
		buffer.append("expiry: ");
		buffer.append(this.cookieExpiryDate);
		buffer.append("]");
		return buffer.toString();
	}

	// ----------------------------------------------------- Instance Variables

	/** Cookie name */
	private final String name;

	/** Cookie attributes as specified by the origin server */
	private Map<String, String> attribs;

	/** Cookie value */
	private String value;

	/** Domain attribute. */
	private String cookieDomain;

	/** Expiration {@link Date}. */
	private Date cookieExpiryDate;

	/** Path attribute. */
	private String cookiePath;

	/** My secure flag. */
	private boolean isSecure;

	private Date creationDate;

}

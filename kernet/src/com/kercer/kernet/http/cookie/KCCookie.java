package com.kercer.kernet.http.cookie;

/**
 * Created by zihong on 15/12/16.
 */

import java.util.Date;

/**
 * Cookie interface represents a token or short packet of state information (also referred to as "magic-cookie") that
 * the HTTP agent and the target server can exchange to maintain a session.
 * In its simples form an HTTP cookie is merely a name / value pair. <p>
 * Please do not use attributes marked as @Obsolete. They have been rendered obsolete by RFC 6265.
 */
public interface KCCookie
{

	String PATH_ATTR = "path";
	String DOMAIN_ATTR = "domain";
	String MAX_AGE_ATTR = "max-age";
	String SECURE_ATTR = "secure";
	String EXPIRES_ATTR = "expires";

	String getAttribute(String name);

	boolean containsAttribute(String name);

	/**
	 * Returns the name.
	 *
	 * @return String name The name
	 */
	String getName();

	/**
	 * Returns the value.
	 *
	 * @return String value The current value.
	 */
	String getValue();

	/**
	 * Returns the expiration {@link Date} of the cookie, or {@code null} if none exists.
	 * <p>
	 * <strong>Note:</strong> the object returned by this method is considered immutable. Changing it (e.g. using setTime()) could result in undefined
	 * behaviour. Do so at your peril.
	 * </p>
	 *
	 * @return Expiration {@link Date}, or {@code null}.
	 */
	Date getExpiryDate();

	/**
	 * Returns {@code false} if the cookie should be discarded at the end of the "session"; {@code true} otherwise.
	 *
	 * @return {@code false} if the cookie should be discarded at the end of the "session"; {@code true} otherwise
	 */
	boolean isPersistent();

	/**
	 * Returns domain attribute of the cookie. The value of the Domain attribute specifies the domain for which the cookie is valid.
	 *
	 * @return the value of the domain attribute.
	 */
	String getDomain();

	/**
	 * Returns the path attribute of the cookie. The value of the Path attribute specifies the subset of URLs on the origin server to which this
	 * cookie applies.
	 *
	 * @return The value of the path attribute.
	 */
	String getPath();

	/**
	 * Indicates whether this cookie requires a secure connection.
	 *
	 * @return {@code true} if this cookie should only be sent over secure connections, {@code false} otherwise.
	 */
	boolean isSecure();

	/**
	 * Returns true if this cookie has expired.
	 *
	 * @param date
	 *            Current time
	 *
	 * @return {@code true} if the cookie has expired.
	 */
	boolean isExpired(final Date date);

	// TODO: RFC 6265 requires cookies to track their creation time; add #getCreationDate()

}

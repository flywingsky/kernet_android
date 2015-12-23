package com.kercer.kernet.http.cookie.handle;

/**
 * Created by zihong on 15/12/16.
 */

import com.kercer.kernet.http.cookie.KCClientCookie;
import com.kercer.kernet.http.cookie.KCCookie;
import com.kercer.kernet.http.cookie.KCCookieOrigin;
import com.kercer.kernet.http.error.KCCookieError;

/**
 * This interface represents a cookie attribute handler responsible for parsing,
 * validating, and matching a specific cookie attribute, such as path,
 * domain, port, etc.
 *
 * Different cookie specifications can provide a specific implementation for this class based on their cookie handling rules.
 *
 *
 */
public interface KCCookieHandler
{

	/**
	 * Parse the given cookie attribute value and processChallenge the corresponding Cookie property.
	 *
	 * @param cookie
	 *            to be updated
	 * @param value
	 *            cookie attribute value from the cookie response header
	 */
	void parse(KCClientCookie cookie, String value) throws KCCookieError;

	/**
	 * Peforms cookie validation for the given attribute value.
	 *
	 * @param cookie
	 *            to validate
	 * @param origin
	 *            the cookie source to validate against
	 * @throws KCCookieError
	 *             if cookie validation fails for this attribute
	 */
	void validate(KCCookie cookie, KCCookieOrigin origin) throws KCCookieError;

	/**
	 * Matches the given value (property of the destination host where request is being submitted) with the corresponding cookie attribute.
	 *
	 * @param cookie
	 *            to match
	 * @param origin
	 *            the cookie source to match against
	 * @return {@code true} if the match is successful; {@code false} otherwise
	 */
	boolean match(KCCookie cookie, KCCookieOrigin origin);

	String getAttributeName();

}

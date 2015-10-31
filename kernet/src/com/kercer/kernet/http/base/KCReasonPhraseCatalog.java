package com.kercer.kernet.http.base;

import java.util.Locale;

public class KCReasonPhraseCatalog
{
	/**
	 * The default instance of this catalog. This catalog is thread safe, so there typically is no need to create other instances.
	 */
	public final static KCReasonPhraseCatalog INSTANCE = new KCReasonPhraseCatalog();

	/**
	 * Restricted default constructor, for derived classes. If you need an instance of this class, use {@link #INSTANCE INSTANCE}.
	 */
	protected KCReasonPhraseCatalog()
	{
		// no body
	}

	/**
	 * Obtains the reason phrase for a status code.
	 *
	 * @param status
	 *            the status code, in the range 100-599
	 * @param loc
	 *            ignored
	 *
	 * @return the reason phrase, or <code>null</code>
	 */
	public String getReason(int status, Locale loc)
	{
		if ((status < 100) || (status >= 600))
		{
			throw new IllegalArgumentException("Unknown category for status code " + status + ".");
		}

		final int category = status / 100;
		final int subcode = status - 100 * category;

		String reason = null;
		if (REASON_PHRASES[category].length > subcode)
			reason = REASON_PHRASES[category][subcode];

		return reason;
	}

	/** Reason phrases lookup table. */
	private static final String[][] REASON_PHRASES = new String[][] { null, new String[3], // 1xx
			new String[8], // 2xx
			new String[8], // 3xx
			new String[25], // 4xx
			new String[8] // 5xx
	};

	/**
	 * Stores the given reason phrase, by status code. Helper method to initialize the static lookup table.
	 *
	 * @param status
	 *            the status code for which to define the phrase
	 * @param reason
	 *            the reason phrase for this status code
	 */
	private static void setReason(int status, String reason)
	{
		final int category = status / 100;
		final int subcode = status - 100 * category;
		REASON_PHRASES[category][subcode] = reason;
	}

	// ----------------------------------------------------- Static Initializer

	/** Set up status code to "reason phrase" map. */
	static
	{
		// HTTP 1.0 Server status codes -- see RFC 1945
		setReason(KCHttpStatus.HTTP_OK, "OK");
		setReason(KCHttpStatus.HTTP_CREATED, "Created");
		setReason(KCHttpStatus.HTTP_ACCEPTED, "Accepted");
		setReason(KCHttpStatus.HTTP_NO_CONTENT, "No Content");
		setReason(KCHttpStatus.HTTP_MOVED_PERMANENTLY, "Moved Permanently");
		setReason(KCHttpStatus.HTTP_MOVED_TEMPORARILY, "Moved Temporarily");
		setReason(KCHttpStatus.HTTP_NOT_MODIFIED, "Not Modified");
		setReason(KCHttpStatus.HTTP_BAD_REQUEST, "Bad Request");
		setReason(KCHttpStatus.HTTP_UNAUTHORIZED, "Unauthorized");
		setReason(KCHttpStatus.HTTP_FORBIDDEN, "Forbidden");
		setReason(KCHttpStatus.HTTP_NOT_FOUND, "Not Found");
		setReason(KCHttpStatus.HTTP_INTERNAL_SERVER_ERROR, "Internal Server Error");
		setReason(KCHttpStatus.HTTP_NOT_IMPLEMENTED, "Not Implemented");
		setReason(KCHttpStatus.HTTP_BAD_GATEWAY, "Bad Gateway");
		setReason(KCHttpStatus.HTTP_SERVICE_UNAVAILABLE, "Service Unavailable");

		// HTTP 1.1 Server status codes -- see RFC 2048
		setReason(KCHttpStatus.HTTP_CONTINUE, "Continue");
		setReason(KCHttpStatus.HTTP_TEMPORARY_REDIRECT, "Temporary Redirect");
		setReason(KCHttpStatus.HTTP_METHOD_NOT_ALLOWED, "Method Not Allowed");
		setReason(KCHttpStatus.HTTP_CONFLICT, "Conflict");
		setReason(KCHttpStatus.HTTP_PRECONDITION_FAILED, "Precondition Failed");
		setReason(KCHttpStatus.HTTP_REQUEST_TOO_LONG, "Request Too Long");
		setReason(KCHttpStatus.HTTP_REQUEST_URI_TOO_LONG, "Request-URI Too Long");
		setReason(KCHttpStatus.HTTP_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
		setReason(KCHttpStatus.HTTP_MULTIPLE_CHOICES, "Multiple Choices");
		setReason(KCHttpStatus.HTTP_SEE_OTHER, "See Other");
		setReason(KCHttpStatus.HTTP_USE_PROXY, "Use Proxy");
		setReason(KCHttpStatus.HTTP_PAYMENT_REQUIRED, "Payment Required");
		setReason(KCHttpStatus.HTTP_NOT_ACCEPTABLE, "Not Acceptable");
		setReason(KCHttpStatus.HTTP_PROXY_AUTHENTICATION_REQUIRED, "Proxy Authentication Required");
		setReason(KCHttpStatus.HTTP_REQUEST_TIMEOUT, "Request Timeout");

		setReason(KCHttpStatus.HTTP_SWITCHING_PROTOCOLS, "Switching Protocols");
		setReason(KCHttpStatus.HTTP_NON_AUTHORITATIVE_INFORMATION, "Non Authoritative Information");
		setReason(KCHttpStatus.HTTP_RESET_CONTENT, "Reset Content");
		setReason(KCHttpStatus.HTTP_PARTIAL_CONTENT, "Partial Content");
		setReason(KCHttpStatus.HTTP_GATEWAY_TIMEOUT, "Gateway Timeout");
		setReason(KCHttpStatus.HTTP_VERSION_NOT_SUPPORTED, "Http Version Not Supported");
		setReason(KCHttpStatus.HTTP_GONE, "Gone");
		setReason(KCHttpStatus.HTTP_LENGTH_REQUIRED, "Length Required");
		setReason(KCHttpStatus.HTTP_REQUESTED_RANGE_NOT_SATISFIABLE, "Requested Range Not Satisfiable");
		setReason(KCHttpStatus.HTTP_EXPECTATION_FAILED, "Expectation Failed");

		// WebDAV Server-specific status codes
		setReason(KCHttpStatus.HTTP_PROCESSING, "Processing");
		setReason(KCHttpStatus.HTTP_MULTI_STATUS, "Multi-Status");
		setReason(KCHttpStatus.HTTP_UNPROCESSABLE_ENTITY, "Unprocessable Entity");
		setReason(KCHttpStatus.HTTP_INSUFFICIENT_SPACE_ON_RESOURCE, "Insufficient Space On Resource");
		setReason(KCHttpStatus.HTTP_METHOD_FAILURE, "Method Failure");
		setReason(KCHttpStatus.HTTP_LOCKED, "Locked");
		setReason(KCHttpStatus.HTTP_INSUFFICIENT_STORAGE, "Insufficient Storage");
		setReason(KCHttpStatus.HTTP_FAILED_DEPENDENCY, "Failed Dependency");
	}

}

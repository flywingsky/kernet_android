package com.kercer.kernet.http.base;

public final class KCHttpDefine
{
    /** HTTP header definitions */
    public static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String HEADER_CONTENT_LEN  = "Content-Length";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String HEADER_EXPECT_DIRECTIVE = "Expect";
    public static final String HEADER_CONN_DIRECTIVE = "Connection";
    public static final String HEADER_TARGET_HOST = "Host";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String HEADER_DATE_HEADER = "Date";
    public static final String HEADER_SERVER_HEADER = "Server";

    /** HTTP expectations */
    public static final String EXPECT_CONTINUE = "100-continue";

    /** HTTP connection control */
    public static final String CONN_CLOSE = "Close";
    public static final String CONN_KEEP_ALIVE = "Keep-Alive";

    /** Transfer encoding definitions */
    public static final String CHUNK_CODING = "chunked";
    public static final String IDENTITY_CODING = "identity";

    /** Common charset definitions */
    public static final String UTF_8 = "UTF-8";
    public static final String UTF_16 = "UTF-16";
    public static final String US_ASCII = "US-ASCII";
    public static final String ASCII = "ASCII";
    public static final String ISO_8859_1 = "ISO-8859-1";

    /** Default charsets */
    public static final String DEFAULT_CONTENT_CHARSET = ISO_8859_1;
    public static final String DEFAULT_PROTOCOL_CHARSET = US_ASCII;

    /** Content type definitions */
    public final static String OCTET_STREAM_TYPE = "application/octet-stream";
    public final static String PLAIN_TEXT_TYPE = "text/plain";
    public final static String CHARSET_PARAM = "; charset=";

    /** Default content type */
    public final static String DEFAULT_CONTENT_TYPE = OCTET_STREAM_TYPE;

    private KCHttpDefine() {
    }
}

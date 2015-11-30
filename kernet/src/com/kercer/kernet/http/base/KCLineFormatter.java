package com.kercer.kernet.http.base;

import com.kercer.kercore.buffer.KCCharArrayBuffer;

public class KCLineFormatter
{
	  /**
     * A default instance of this class, for use as default or fallback.
     * Note that {@link KCLineFormatter} is not a singleton, there can
     * be many instances of the class itself and of derived classes.
     * The instance here provides non-customized, default behavior.
     */
    public final static KCLineFormatter DEFAULT = new KCLineFormatter();



    // public default constructor


    /**
     * Obtains a buffer for formatting.
     *
     * @param buffer    a buffer already available, or <code>null</code>
     *
     * @return  the cleared argument buffer if there is one, or
     *          a new empty buffer that can be used for formatting
     */
    protected KCCharArrayBuffer initBuffer(KCCharArrayBuffer buffer) {
        if (buffer != null) {
            buffer.clear();
        } else {
            buffer = new KCCharArrayBuffer(64);
        }
        return buffer;
    }


    /**
     * Formats a protocol version.
     *
     * @param version           the protocol version to format
     * @param formatter         the formatter to use, or
     *                          <code>null</code> for the
     *                          {@link #DEFAULT default}
     *
     * @return  the formatted protocol version
     */
    public final static
        String formatProtocolVersion(final KCProtocolVersion version,
        		KCLineFormatter formatter) {
        if (formatter == null)
            formatter = KCLineFormatter.DEFAULT;
        return formatter.appendProtocolVersion(null, version).toString();
    }


    // non-javadoc, see interface LineFormatter
    public KCCharArrayBuffer appendProtocolVersion(final KCCharArrayBuffer buffer,
                                                 final KCProtocolVersion version) {
        if (version == null) {
            throw new IllegalArgumentException
                ("Protocol version may not be null");
        }

        // can't use initBuffer, that would clear the argument!
        KCCharArrayBuffer result = buffer;
        final int len = estimateProtocolVersionLen(version);
        if (result == null) {
            result = new KCCharArrayBuffer(len);
        } else {
            result.ensureCapacity(len);
        }

        result.append(version.getProtocol());
        result.append('/');
        result.append(Integer.toString(version.getMajor()));
        result.append('.');
        result.append(Integer.toString(version.getMinor()));

        return result;
    }


    /**
     * Guesses the length of a formatted protocol version.
     * Needed to guess the length of a formatted request or status line.
     *
     * @param version   the protocol version to format, or <code>null</code>
     *
     * @return  the estimated length of the formatted protocol version,
     *          in characters
     */
    protected int estimateProtocolVersionLen(final KCProtocolVersion version) {
        return version.getProtocol().length() + 4; // room for "HTTP/1.1"
    }


//    /**
//     * Formats a request line.
//     *
//     * @param reqline           the request line to format
//     * @param formatter         the formatter to use, or
//     *                          <code>null</code> for the
//     *                          {@link #DEFAULT default}
//     *
//     * @return  the formatted request line
//     */
//    public final static String formatRequestLine(final RequestLine reqline,
//                                                 LineFormatter formatter) {
//        if (formatter == null)
//            formatter = BasicLineFormatter.DEFAULT;
//        return formatter.formatRequestLine(null, reqline).toString();
//    }
//
//
//    // non-javadoc, see interface LineFormatter
//    public CharArrayBuffer formatRequestLine(CharArrayBuffer buffer,
//                                             RequestLine reqline) {
//        if (reqline == null) {
//            throw new IllegalArgumentException
//                ("Request line may not be null");
//        }
//
//        CharArrayBuffer result = initBuffer(buffer);
//        doFormatRequestLine(result, reqline);
//
//        return result;
//    }
//
//
//    /**
//     * Actually formats a request line.
//     * Called from {@link #formatRequestLine}.
//     *
//     * @param buffer    the empty buffer into which to format,
//     *                  never <code>null</code>
//     * @param reqline   the request line to format, never <code>null</code>
//     */
//    protected void doFormatRequestLine(final CharArrayBuffer buffer,
//                                       final RequestLine reqline) {
//        final String method = reqline.getMethod();
//        final String uri    = reqline.getUri();
//
//        // room for "GET /index.html HTTP/1.1"
//        int len = method.length() + 1 + uri.length() + 1 +
//            estimateProtocolVersionLen(reqline.getProtocolVersion());
//        buffer.ensureCapacity(len);
//
//        buffer.append(method);
//        buffer.append(' ');
//        buffer.append(uri);
//        buffer.append(' ');
//        appendProtocolVersion(buffer, reqline.getProtocolVersion());
//    }
//


    /**
     * Formats a status line.
     *
     * @param statline          the status line to format
     * @param formatter         the formatter to use, or
     *                          <code>null</code> for the
     *                          {@link #DEFAULT default}
     *
     * @return  the formatted status line
     */
    public final static String formatStatusLine(final KCStatusLine statline,
                                                KCLineFormatter formatter) {
        if (formatter == null)
            formatter = KCLineFormatter.DEFAULT;
        return formatter.formatStatusLine(null, statline).toString();
    }


    // non-javadoc, see interface LineFormatter
    public KCCharArrayBuffer formatStatusLine(final KCCharArrayBuffer buffer,
                                            final KCStatusLine statline) {
        if (statline == null) {
            throw new IllegalArgumentException
                ("Status line may not be null");
        }

        KCCharArrayBuffer result = initBuffer(buffer);
        doFormatStatusLine(result, statline);

        return result;
    }


    /**
     * Actually formats a status line.
     * Called from {@link #formatStatusLine}.
     *
     * @param buffer    the empty buffer into which to format,
     *                  never <code>null</code>
     * @param statline  the status line to format, never <code>null</code>
     */
    protected void doFormatStatusLine(final KCCharArrayBuffer buffer,
                                      final KCStatusLine statline) {

        int len = estimateProtocolVersionLen(statline.getProtocolVersion())
            + 1 + 3 + 1; // room for "HTTP/1.1 200 "
        final String reason = statline.getReasonPhrase();
        if (reason != null) {
            len += reason.length();
        }
        buffer.ensureCapacity(len);

        appendProtocolVersion(buffer, statline.getProtocolVersion());
        buffer.append(' ');
        buffer.append(Integer.toString(statline.getStatusCode()));
        buffer.append(' '); // keep whitespace even if reason phrase is empty
        if (reason != null) {
            buffer.append(reason);
        }
    }


    /**
     * Formats a header.
     *
     * @param header            the header to format
     * @param formatter         the formatter to use, or
     *                          <code>null</code> for the
     *                          {@link #DEFAULT default}
     *
     * @return  the formatted header
     */
    public final static String formatHeader(final KCHeader header,
                                            KCLineFormatter formatter) {
        if (formatter == null)
            formatter = KCLineFormatter.DEFAULT;
        return formatter.formatHeader(null, header).toString();
    }


    // non-javadoc, see interface LineFormatter
    public KCCharArrayBuffer formatHeader(KCCharArrayBuffer buffer,
                                        KCHeader header) {
        if (header == null) {
            throw new IllegalArgumentException
                ("Header may not be null");
        }
        KCCharArrayBuffer result = null;

//        if (header instanceof FormattedHeader) {
//            // If the header is backed by a buffer, re-use the buffer
//            result = ((FormattedHeader)header).getBuffer();
//        } else {
            result = initBuffer(buffer);
            doFormatHeader(result, header);
//        }
        return result;

    } // formatHeader


    /**
     * Actually formats a header.
     * Called from {@link #formatHeader}.
     *
     * @param buffer    the empty buffer into which to format,
     *                  never <code>null</code>
     * @param header    the header to format, never <code>null</code>
     */
    protected void doFormatHeader(final KCCharArrayBuffer buffer,
                                  final KCHeader header) {
        final String name = header.getName();
        final String value = header.getValue();

        int len = name.length() + 2;
        if (value != null) {
            len += value.length();
        }
        buffer.ensureCapacity(len);

        buffer.append(name);
        buffer.append(": ");
        if (value != null) {
            buffer.append(value);
        }
    }
}

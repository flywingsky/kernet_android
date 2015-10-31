package com.kercer.kernet.http.base;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class KCHttpContent
{
	protected KCHeader contentType;
	protected KCHeader contentEncoding;
	protected boolean chunked;

//	private InputStream content;
    /** Raw data from this response. */
    public /*final*/ byte[] content;
	private boolean contentObtained;
	private long length;


	/**
	 * Creates a new basic entity. The content is initially missing, the content length is set to a negative number.
	 */
	public KCHttpContent()
	{
		super();
		this.length = -1;
	}

	// non-javadoc, see interface HttpEntity
	public long getContentLength()
	{
		return this.length;
	}

	/**
	 * Obtains the content, once only.
	 *
	 * @return the content, if this is the first call to this method since {@link #setContent setContent} has been called
	 *
	 * @throws IllegalStateException
	 *             if the content has been obtained before, or has not yet been provided
	 */
	public byte[] getContent() throws IllegalStateException
	{
		if (this.content == null)
		{
			throw new IllegalStateException("Content has not been provided");
		}
//		if (this.contentObtained)
//		{
//			throw new IllegalStateException("Content has been consumed");
//		}
		this.contentObtained = true;
		return this.content;

	} // getContent

	/**
	 * Tells that this entity is not repeatable.
	 *
	 * @return <code>false</code>
	 */
	public boolean isRepeatable()
	{
		return false;
	}

	/**
	 * Specifies the length of the content.
	 *
	 * @param len
	 *            the number of bytes in the content, or a negative number to indicate an unknown length
	 */
	public void setContentLength(long len)
	{
		this.length = len;
	}

	/**
	 * Specifies the content.
	 *
	 * @param data
	 *            the stream to return with the next call to {@link #getContent getContent}
	 */
	public void setContent(final byte[] data)
	{
		this.content = data;
		this.contentObtained = false;
	}

	// non-javadoc, see interface HttpEntity
	public void writeTo(final OutputStream outstream) throws IOException
	{
		if (outstream == null)
		{
			throw new IllegalArgumentException("Output stream may not be null");
		}
		InputStream instream = new ByteArrayInputStream(getContent());
		int l;
		byte[] tmp = new byte[2048];
		while ((l = instream.read(tmp)) != -1)
		{
			outstream.write(tmp, 0, l);
		}
	}

	// non-javadoc, see interface HttpEntity
	public boolean isStreaming()
	{
		return !this.contentObtained && this.content != null;
	}

//	public void consumeContent() throws IOException
//	{
//		if (content != null)
//		{
//			content.close(); // reads to the end of the entity
//		}
//	}

/********************************/


    /**
     * Obtains the Content-Type header.
     * The default implementation returns the value of the
     * {@link #contentType contentType} attribute.
     *
     * @return  the Content-Type header, or <code>null</code>
     */
    public KCHeader getContentType() {
        return this.contentType;
    }


    /**
     * Obtains the Content-Encoding header.
     * The default implementation returns the value of the
     * {@link #contentEncoding contentEncoding} attribute.
     *
     * @return  the Content-Encoding header, or <code>null</code>
     */
    public KCHeader getContentEncoding() {
        return this.contentEncoding;
    }

    /**
     * Obtains the 'chunked' flag.
     * The default implementation returns the value of the
     * {@link #chunked chunked} attribute.
     *
     * @return  the 'chunked' flag
     */
    public boolean isChunked() {
        return this.chunked;
    }


    /**
     * Specifies the Content-Type header.
     * The default implementation sets the value of the
     * {@link #contentType contentType} attribute.
     *
     * @param contentType       the new Content-Encoding header, or
     *                          <code>null</code> to unset
     */
    public void setContentType(final KCHeader contentType) {
        this.contentType = contentType;
    }

    /**
     * Specifies the Content-Type header, as a string.
     * The default implementation calls
     * {@link #setContentType(String) setContentType(Header)}.
     *
     * @param ctString     the new Content-Type header, or
     *                     <code>null</code> to unset
     */
    public void setContentType(final String ctString) {
    	KCHeader h = null;
        if (ctString != null) {
            h = new KCHeader(KCHttpDefine.HEADER_CONTENT_TYPE, ctString);
        }
        setContentType(h);
    }


    /**
     * Specifies the Content-Encoding header.
     * The default implementation sets the value of the
     * {@link #contentEncoding contentEncoding} attribute.
     *
     * @param contentEncoding   the new Content-Encoding header, or
     *                          <code>null</code> to unset
     */
    public void setContentEncoding(final KCHeader contentEncoding) {
        this.contentEncoding = contentEncoding;
    }

    /**
     * Specifies the Content-Encoding header, as a string.
     * The default implementation calls
     * {@link #setContentEncoding(String) setContentEncoding(Header)}.
     *
     * @param ceString     the new Content-Encoding header, or
     *                     <code>null</code> to unset
     */
    public void setContentEncoding(final String ceString) {
    	KCHeader h = null;
        if (ceString != null) {
            h = new KCHeader(KCHttpDefine.HEADER_CONTENT_ENCODING, ceString);
        }
        setContentEncoding(h);
    }


    /**
     * Specifies the 'chunked' flag.
     * The default implementation sets the value of the
     * {@link #chunked chunked} attribute.
     *
     * @param b         the new 'chunked' flag
     */
    public void setChunked(boolean b) {
        this.chunked = b;
    }

}

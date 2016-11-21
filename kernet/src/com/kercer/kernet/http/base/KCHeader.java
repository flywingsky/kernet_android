package com.kercer.kernet.http.base;

import com.kercer.kernet.http.error.KCParseError;

public class KCHeader
{

    private final String name;
    private final String value;

    public static KCHeader header(final String name, final String value)
    {
        KCHeader header = new KCHeader(name, value);
        return header;
    }

    /**
     * Constructor with name and value
     *
     * @param name  the header name
     * @param value the header value
     */
    public KCHeader(final String name, final String value)
    {
        super();
        if (name == null)
        {
            throw new IllegalArgumentException("Name may not be null");
        }
        this.name = name;
        this.value = value;
    }

    public String getName()
    {
        return this.name;
    }

    public String getValue()
    {
        return this.value;
    }

    public String toString()
    {
        // no need for non-default formatting in toString()
        return KCLineFormatter.DEFAULT.formatHeader(null, this).toString();
    }

    public KCHeaderElement[] getElements() throws KCParseError
    {
        if (this.value != null)
        {
            // result intentionally not cached, it's probably not used again
            return KCHeaderValueParser.parseElements(this.value, null);
        }
        else
        {
            return new KCHeaderElement[]{};
        }
    }

    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}

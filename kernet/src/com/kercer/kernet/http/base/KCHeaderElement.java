package com.kercer.kernet.http.base;

import com.kercer.kercore.base.KCKeyValuePair;
import com.kercer.kercore.buffer.KCCharArrayBuffer;
import com.kercer.kercore.util.KCUtilLang;

/**
 * One element of an HTTP {@link KCHeader header} value consisting of
 * a name / value pair and a number of optional name / value parameters.
 * <p>
 * Some HTTP headers (such as the set-cookie header) have values that
 * can be decomposed into multiple elements.  Such headers must be in the
 * following form:
 * </p>
 * <pre>
 * header  = [ element ] *( "," [ element ] )
 * element = name [ "=" [ value ] ] *( ";" [ param ] )
 * param   = name [ "=" [ value ] ]
 *
 * name    = token
 * value   = ( token | quoted-string )
 *
 * token         = 1*&lt;any char except "=", ",", ";", &lt;"&gt; and
 *                       white space&gt;
 * quoted-string = &lt;"&gt; *( text | quoted-char ) &lt;"&gt;
 * text          = any char except &lt;"&gt;
 * quoted-char   = "\" char
 * </pre>
 * <p>
 * Any amount of white space is allowed between any part of the
 * header, element or param and is ignored. A missing value in any
 * element or param will be stored as the empty {@link String};
 * if the "=" is also missing <var>null</var> will be stored instead.
 *
 *
 * Created by zihong on 2016/11/21.
 */

public class KCHeaderElement implements Cloneable
{

    private final String name;
    private final String value;
    private final KCKeyValuePair[] parameters;

    /**
     * Constructor with name, value and parameters.
     *
     * @param name       header element name
     * @param value      header element value. May be <tt>null</tt>
     * @param parameters header element parameters. May be <tt>null</tt>.
     *                   Parameters are copied by reference, not by value
     */
    public KCHeaderElement(final String name, final String value, final KCKeyValuePair[] parameters)
    {
        super();
        if (name == null)
        {
            throw new IllegalArgumentException("Name may not be null");
        }
        this.name = name;
        this.value = value;
        if (parameters != null)
        {
            this.parameters = parameters;
        }
        else
        {
            this.parameters = new KCKeyValuePair[]{};
        }
    }

    /**
     * Constructor with name and value.
     *
     * @param name  header element name
     * @param value header element value. May be <tt>null</tt>
     */
    public KCHeaderElement(final String name, final String value)
    {
        this(name, value, null);
    }

    public String getName()
    {
        return this.name;
    }

    public String getValue()
    {
        return this.value;
    }

    public KCKeyValuePair[] getParameters()
    {
        return (KCKeyValuePair[]) this.parameters.clone();
    }

    public int getParameterCount()
    {
        return this.parameters.length;
    }

    public KCKeyValuePair getParameter(int index)
    {
        // ArrayIndexOutOfBoundsException is appropriate
        return this.parameters[index];
    }

    public KCKeyValuePair getParameterByName(final String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("Name may not be null");
        }
        KCKeyValuePair found = null;
        for (int i = 0; i < this.parameters.length; i++)
        {
            KCKeyValuePair current = this.parameters[i];
            if (current.getName().equalsIgnoreCase(name))
            {
                found = current;
                break;
            }
        }
        return found;
    }

    public boolean equals(final Object object)
    {
        if (object == null) return false;
        if (this == object) return true;
        if (object instanceof KCHeaderElement)
        {
            KCHeaderElement that = (KCHeaderElement) object;
            return this.name.equals(that.name) && KCUtilLang.equals(this.value, that.value) && KCUtilLang.equals(this.parameters, that.parameters);
        }
        else
        {
            return false;
        }
    }

    public int hashCode()
    {
        int hash = KCUtilLang.HASH_SEED;
        hash = KCUtilLang.hashCode(hash, this.name);
        hash = KCUtilLang.hashCode(hash, this.value);
        for (int i = 0; i < this.parameters.length; i++)
        {
            hash = KCUtilLang.hashCode(hash, this.parameters[i]);
        }
        return hash;
    }

    public String toString()
    {
        KCCharArrayBuffer buffer = new KCCharArrayBuffer(64);
        buffer.append(this.name);
        if (this.value != null)
        {
            buffer.append("=");
            buffer.append(this.value);
        }
        for (int i = 0; i < this.parameters.length; i++)
        {
            buffer.append("; ");
            buffer.append(this.parameters[i]);
        }
        return buffer.toString();
    }

    public Object clone() throws CloneNotSupportedException
    {
        // parameters array is considered immutable
        // no need to make a copy of it
        return super.clone();
    }

}

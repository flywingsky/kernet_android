package com.kercer.kernet.http.base;

import com.kercer.kercore.base.KCKeyValuePair;
import com.kercer.kercore.buffer.KCCharArrayBuffer;
import com.kercer.kercore.parser.KCParserCursor;
import com.kercer.kercore.parser.KCTokenParser;
import com.kercer.kernet.http.error.KCParseError;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for parsing header values into elements.
 * Instances of this class are stateless and thread-safe.
 * Derived classes are expected to maintain these properties.
 *
 * Created by zihong on 2016/11/21.
 */

public class KCHeaderValueParser
{
    /**
     * A default instance of this class, for use as default or fallback.
     * Note that {@link KCHeaderValueParser} is not a singleton, there
     * can be many instances of the class itself and of derived classes.
     * The instance here provides non-customized, default behavior.
     */
    public final static KCHeaderValueParser DEFAULT = new KCHeaderValueParser();

    private final static char PARAM_DELIMITER = ';';
    private final static char ELEM_DELIMITER = ',';
    private final static char[] ALL_DELIMITERS = new char[]{
            PARAM_DELIMITER, ELEM_DELIMITER
    };

    // public default constructor


    /**
     * Parses elements with the given parser.
     *
     * @param value  the header value to parse
     * @param parser the parser to use, or <code>null</code> for default
     * @return array holding the header elements, never <code>null</code>
     */
    public final static KCHeaderElement[] parseElements(final String value, KCHeaderValueParser parser) throws KCParseError
    {

        if (value == null)
        {
            throw new IllegalArgumentException("Value to parse may not be null");
        }

        if (parser == null) parser = KCHeaderValueParser.DEFAULT;

        KCCharArrayBuffer buffer = new KCCharArrayBuffer(value.length());
        buffer.append(value);
        KCParserCursor cursor = new KCParserCursor(0, value.length());
        return parser.parseElements(buffer, cursor);
    }


    // non-javadoc, see interface HeaderValueParser
    public KCHeaderElement[] parseElements(final KCCharArrayBuffer buffer, final KCParserCursor cursor)
    {

        if (buffer == null)
        {
            throw new IllegalArgumentException("Char array buffer may not be null");
        }
        if (cursor == null)
        {
            throw new IllegalArgumentException("Parser cursor may not be null");
        }

        List elements = new ArrayList();
        while (!cursor.atEnd())
        {
            KCHeaderElement element = parseHeaderElement(buffer, cursor);
            if (!(element.getName().length() == 0 && element.getValue() == null))
            {
                elements.add(element);
            }
        }
        return (KCHeaderElement[]) elements.toArray(new KCHeaderElement[elements.size()]);
    }


    /**
     * Parses an element with the given parser.
     *
     * @param value  the header element to parse
     * @param parser the parser to use, or <code>null</code> for default
     * @return the parsed header element
     */
    public final static KCHeaderElement parseHeaderElement(final String value, KCHeaderValueParser parser) throws KCParseError
    {

        if (value == null)
        {
            throw new IllegalArgumentException("Value to parse may not be null");
        }

        if (parser == null) parser = KCHeaderValueParser.DEFAULT;

        KCCharArrayBuffer buffer = new KCCharArrayBuffer(value.length());
        buffer.append(value);
        KCParserCursor cursor = new KCParserCursor(0, value.length());
        return parser.parseHeaderElement(buffer, cursor);
    }


    // non-javadoc, see interface HeaderValueParser
    public KCHeaderElement parseHeaderElement(final KCCharArrayBuffer buffer, final KCParserCursor cursor)
    {

        if (buffer == null)
        {
            throw new IllegalArgumentException("Char array buffer may not be null");
        }
        if (cursor == null)
        {
            throw new IllegalArgumentException("Parser cursor may not be null");
        }

        KCKeyValuePair nvp = parseNameValuePair(buffer, cursor);
        KCKeyValuePair[] params = null;
        if (!cursor.atEnd())
        {
            char ch = buffer.charAt(cursor.getPos() - 1);
            if (ch != ELEM_DELIMITER)
            {
                params = parseParameters(buffer, cursor);
            }
        }
        return createHeaderElement(nvp.getName(), nvp.getValue(), params);
    }


    /**
     * Creates a header element.
     * Called from {@link #parseHeaderElement}.
     *
     * @return a header element representing the argument
     */
    protected KCHeaderElement createHeaderElement(final String name, final String value, final KCKeyValuePair[] params)
    {
        return new KCHeaderElement(name, value, params);
    }


    /**
     * Parses parameters with the given parser.
     *
     * @param value  the parameter list to parse
     * @param parser the parser to use, or <code>null</code> for default
     * @return array holding the parameters, never <code>null</code>
     */
    public final static KCKeyValuePair[] parseParameters(final String value, KCHeaderValueParser parser) throws KCParseError
    {

        if (value == null)
        {
            throw new IllegalArgumentException("Value to parse may not be null");
        }

        if (parser == null) parser = KCHeaderValueParser.DEFAULT;

        KCCharArrayBuffer buffer = new KCCharArrayBuffer(value.length());
        buffer.append(value);
        KCParserCursor cursor = new KCParserCursor(0, value.length());
        return parser.parseParameters(buffer, cursor);
    }


    // non-javadoc, see interface HeaderValueParser
    public KCKeyValuePair[] parseParameters(final KCCharArrayBuffer buffer, final KCParserCursor cursor)
    {

        if (buffer == null)
        {
            throw new IllegalArgumentException("Char array buffer may not be null");
        }
        if (cursor == null)
        {
            throw new IllegalArgumentException("Parser cursor may not be null");
        }

        int pos = cursor.getPos();
        int indexTo = cursor.getUpperBound();

        while (pos < indexTo)
        {
            char ch = buffer.charAt(pos);
            if (KCTokenParser.isWhitespace(ch))
            {
                pos++;
            }
            else
            {
                break;
            }
        }
        cursor.updatePos(pos);
        if (cursor.atEnd())
        {
            return new KCKeyValuePair[]{};
        }

        List params = new ArrayList();
        while (!cursor.atEnd())
        {
            KCKeyValuePair param = parseNameValuePair(buffer, cursor);
            params.add(param);
            char ch = buffer.charAt(cursor.getPos() - 1);
            if (ch == ELEM_DELIMITER)
            {
                break;
            }
        }

        return (KCKeyValuePair[]) params.toArray(new KCKeyValuePair[params.size()]);
    }

    /**
     * Parses a name-value-pair with the given parser.
     *
     * @param value  the NVP to parse
     * @param parser the parser to use, or <code>null</code> for default
     * @return the parsed name-value pair
     */
    public final static KCKeyValuePair parseNameValuePair(final String value, KCHeaderValueParser parser) throws KCParseError
    {

        if (value == null)
        {
            throw new IllegalArgumentException("Value to parse may not be null");
        }

        if (parser == null) parser = KCHeaderValueParser.DEFAULT;

        KCCharArrayBuffer buffer = new KCCharArrayBuffer(value.length());
        buffer.append(value);
        KCParserCursor cursor = new KCParserCursor(0, value.length());
        return parser.parseNameValuePair(buffer, cursor);
    }


    // non-javadoc, see interface HeaderValueParser
    public KCKeyValuePair parseNameValuePair(final KCCharArrayBuffer buffer, final KCParserCursor cursor)
    {
        return parseNameValuePair(buffer, cursor, ALL_DELIMITERS);
    }

    private static boolean isOneOf(final char ch, final char[] chs)
    {
        if (chs != null)
        {
            for (int i = 0; i < chs.length; i++)
            {
                if (ch == chs[i])
                {
                    return true;
                }
            }
        }
        return false;
    }

    public KCKeyValuePair parseNameValuePair(final KCCharArrayBuffer buffer, final KCParserCursor cursor, final char[] delimiters)
    {

        if (buffer == null)
        {
            throw new IllegalArgumentException("Char array buffer may not be null");
        }
        if (cursor == null)
        {
            throw new IllegalArgumentException("Parser cursor may not be null");
        }

        boolean terminated = false;

        int pos = cursor.getPos();
        int indexFrom = cursor.getPos();
        int indexTo = cursor.getUpperBound();

        // Find name
        String name = null;
        while (pos < indexTo)
        {
            char ch = buffer.charAt(pos);
            if (ch == '=')
            {
                break;
            }
            if (isOneOf(ch, delimiters))
            {
                terminated = true;
                break;
            }
            pos++;
        }

        if (pos == indexTo)
        {
            terminated = true;
            name = buffer.substringTrimmed(indexFrom, indexTo);
        }
        else
        {
            name = buffer.substringTrimmed(indexFrom, pos);
            pos++;
        }

        if (terminated)
        {
            cursor.updatePos(pos);
            return createNameValuePair(name, null);
        }

        // Find value
        String value = null;
        int i1 = pos;

        boolean qouted = false;
        boolean escaped = false;
        while (pos < indexTo)
        {
            char ch = buffer.charAt(pos);
            if (ch == '"' && !escaped)
            {
                qouted = !qouted;
            }
            if (!qouted && !escaped && isOneOf(ch, delimiters))
            {
                terminated = true;
                break;
            }
            if (escaped)
            {
                escaped = false;
            }
            else
            {
                escaped = qouted && ch == '\\';
            }
            pos++;
        }

        int i2 = pos;
        // Trim leading white spaces
        while (i1 < i2 && (KCTokenParser.isWhitespace(buffer.charAt(i1))))
        {
            i1++;
        }
        // Trim trailing white spaces
        while ((i2 > i1) && (KCTokenParser.isWhitespace(buffer.charAt(i2 - 1))))
        {
            i2--;
        }
        // Strip away quotes if necessary
        if (((i2 - i1) >= 2) && (buffer.charAt(i1) == '"') && (buffer.charAt(i2 - 1) == '"'))
        {
            i1++;
            i2--;
        }
        value = buffer.substring(i1, i2);
        if (terminated)
        {
            pos++;
        }
        cursor.updatePos(pos);
        return createNameValuePair(name, value);
    }

    /**
     * Creates a name-value pair.
     * Called from {@link #parseNameValuePair}.
     *
     * @param name  the name
     * @param value the value, or <code>null</code>
     * @return a name-value pair representing the arguments
     */
    protected KCKeyValuePair createNameValuePair(final String name, final String value)
    {
        return new KCKeyValuePair(name, value);
    }
}

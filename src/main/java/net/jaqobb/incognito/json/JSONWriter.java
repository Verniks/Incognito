/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017. Incognito (by Jakub Zagórski (aka Jaqobb))
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.jaqobb.incognito.json;

import java.io.IOException;

public class JSONWriter
{
    private static final int MAX_DEPTH = 200;

    private         boolean      comma;
    protected       char         mode;
    private final   JSONObject[] stack;
    private         int          top;
    protected final Appendable   writer;

    public JSONWriter(Appendable writer)
    {
        this.comma = false;
        this.mode = 'i';
        this.stack = new JSONObject[MAX_DEPTH];
        this.top = 0;
        this.writer = writer;
    }

    private JSONWriter append(String val) throws JSONException
    {
        if (val == null)
        {
            throw new JSONException("Null value.");
        }
        if ((this.mode == 'o') || (this.mode == 'a'))
        {
            try
            {
                if (this.comma && (this.mode == 'a'))
                {
                    this.writer.append(',');
                }
                this.writer.append(val);
            }
            catch (IOException ex)
            {
                throw new JSONException(ex);
            }
            if (this.mode == 'o')
            {
                this.mode = 'k';
            }
            this.comma = true;
            return this;
        }
        throw new JSONException("Value out of sequence.");
    }

    public JSONWriter array() throws JSONException
    {
        if ((this.mode == 'i') || (this.mode == 'o') || (this.mode == 'a'))
        {
            this.push(null);
            this.append("[");
            this.comma = false;
            return this;
        }
        throw new JSONException("Misplaced array.");
    }

    private JSONWriter end(char mode, char closingChar) throws JSONException
    {
        if (this.mode != mode)
        {
            throw new JSONException((mode == 'a') ? "Misplaced end array." : "Misplaced end object.");
        }
        this.pop(mode);
        try
        {
            this.writer.append(closingChar);
        }
        catch (IOException ex)
        {
            throw new JSONException(ex);
        }
        this.comma = true;
        return this;
    }

    public JSONWriter endArray() throws JSONException
    {
        return this.end('a', ']');
    }

    public JSONWriter endObject() throws JSONException
    {
        return this.end('k', '}');
    }

    public JSONWriter key(String key) throws JSONException
    {
        if (key == null)
        {
            throw new JSONException("Null key.");
        }
        if (this.mode == 'k')
        {
            try
            {
                this.stack[this.top - 1].putOnce(key, Boolean.TRUE);
                if (this.comma)
                {
                    this.writer.append(',');
                }
                this.writer.append(JSONObject.quote(key));
                this.writer.append(':');
                this.comma = false;
                this.mode = 'o';
                return this;
            }
            catch (IOException ex)
            {
                throw new JSONException(ex);
            }
        }
        throw new JSONException("Misplaced key.");
    }

    public JSONWriter object() throws JSONException
    {
        if (this.mode == 'i')
        {
            this.mode = 'o';
        }
        if ((this.mode == 'o') || (this.mode == 'a'))
        {
            this.append("{");
            this.push(new JSONObject());
            this.comma = false;
            return this;
        }
        throw new JSONException("Misplaced object.");
    }

    private void pop(char ch) throws JSONException
    {
        if (this.top <= 0)
        {
            throw new JSONException("Nesting error.");
        }
        char mode = (this.stack[this.top - 1] == null) ? 'a' : 'k';
        if (mode != ch)
        {
            throw new JSONException("Nesting error.");
        }
        this.top -= 1;
        this.mode = (this.top == 0) ? 'd' : ((this.stack[this.top - 1] == null) ? 'a' : 'k');
    }

    private void push(JSONObject obj) throws JSONException
    {
        if (this.top >= MAX_DEPTH)
        {
            throw new JSONException("Nesting too deep.");
        }
        this.stack[this.top] = obj;
        this.mode = (obj == null) ? 'a' : 'k';
        this.top += 1;
    }

    public JSONWriter value(boolean val) throws JSONException
    {
        return this.append(val ? "true" : "false");
    }

    public JSONWriter value(double val) throws JSONException
    {
        return this.value((Object) val);
    }

    public JSONWriter value(long val) throws JSONException
    {
        return this.append(Long.toString(val));
    }

    public JSONWriter value(Object val) throws JSONException
    {
        return this.append(JSONObject.valueToString(val));
    }
}
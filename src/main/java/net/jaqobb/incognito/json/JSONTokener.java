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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class JSONTokener
{
    private static final int READ_AHEAD_LIMIT = 1000000;

    private       long    character;
    private       boolean eof;
    private       long    index;
    private       long    line;
    private       char    previous;
    private final Reader  reader;
    private       boolean usePrevious;
    private       long    characterPreviousLine;

    public JSONTokener(Reader reader)
    {
        this.reader = reader.markSupported() ? reader : new BufferedReader(reader);
        this.character = 1;
        this.line = 1;
    }

    public JSONTokener(InputStream inputStream)
    {
        this(new InputStreamReader(inputStream));
    }

    public JSONTokener(String src)
    {
        this(new StringReader(src));
    }

    public void back() throws JSONException
    {
        if (this.usePrevious || (this.index <= 0))
        {
            throw new JSONException("Stepping back two steps is not supported.");
        }
        this.decrementIndexes();
        this.usePrevious = true;
        this.eof = false;
    }

    private void decrementIndexes()
    {
        this.index--;
        if ((this.previous == '\r') || (this.previous == '\n'))
        {
            this.line--;
            this.character = this.characterPreviousLine;
        }
        else if (this.character > 0)
        {
            this.character--;
        }
    }

    public static int dehexchar(char ch)
    {
        if ((ch >= '0') && (ch <= '9'))
        {
            return ch - '0';
        }
        if ((ch >= 'A') && (ch <= 'F'))
        {
            return ch - ('A' - 10);
        }
        if ((ch >= 'a') && (ch <= 'f'))
        {
            return ch - ('a' - 10);
        }
        return - 1;
    }

    public boolean end()
    {
        return this.eof && ! this.usePrevious;
    }

    public boolean more() throws JSONException
    {
        if (this.usePrevious)
        {
            return true;
        }
        try
        {
            this.reader.mark(1);
        }
        catch (IOException ex)
        {
            throw new JSONException("Unable to preserve stream position.", ex);
        }
        try
        {
            if (this.reader.read() <= 0)
            {
                this.eof = true;
                return false;
            }
            this.reader.reset();
        }
        catch (IOException ex)
        {
            throw new JSONException("Unable to read the next character from the stream.", ex);
        }
        return true;
    }

    public char next() throws JSONException
    {
        int ch;
        if (this.usePrevious)
        {
            this.usePrevious = false;
            ch = this.previous;
        }
        else
        {
            try
            {
                ch = this.reader.read();
            }
            catch (IOException exception)
            {
                throw new JSONException(exception);
            }
        }
        if (ch <= 0)
        {
            this.eof = true;
            return 0;
        }
        this.incrementIndexes(ch);
        this.previous = (char) ch;
        return this.previous;
    }

    private void incrementIndexes(int ch)
    {
        if (ch > 0)
        {
            this.index++;
            if (ch == '\r')
            {
                this.line++;
                this.characterPreviousLine = this.character;
                this.character = 0;
            }
            else if (ch == '\n')
            {
                if (this.previous != '\r')
                {
                    this.line++;
                    this.characterPreviousLine = this.character;
                }
                this.character = 0;
            }
            else
            {
                this.character++;
            }
        }
    }

    public char next(char ch) throws JSONException
    {
        char next = this.next();
        if (next != ch)
        {
            if (next > 0)
            {
                throw this.syntaxError("Expected '" + ch + "' and instead saw '" + ch + "'.");
            }
            throw this.syntaxError("Expected '" + ch + "' and instead saw '<insert nothing here>'.");
        }
        return next;
    }

    public String next(int next) throws JSONException
    {
        if (next == 0)
        {
            return "";
        }
        char[] chars = new char[next];
        int pos = 0;
        while (pos < next)
        {
            chars[pos] = this.next();
            if (this.end())
            {
                throw this.syntaxError("Substring bounds error.");
            }
            pos += 1;
        }
        return new String(chars);
    }

    public char nextClean() throws JSONException
    {
        while (true)
        {
            char ch = this.next();
            if ((ch == 0) || (ch > ' '))
            {
                return ch;
            }
        }
    }

    public String nextString(char quote) throws JSONException
    {
        char ch;
        StringBuilder builder = new StringBuilder();
        while (true)
        {
            ch = this.next();
            switch (ch)
            {
                case 0:
                case '\n':
                case '\r':
                    throw this.syntaxError("Unterminated string.");
                case '\\':
                    ch = this.next();
                    switch (ch)
                    {
                        case 'b':
                            builder.append('\b');
                            break;
                        case 't':
                            builder.append('\t');
                            break;
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'f':
                            builder.append('\f');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 'u':
                            try
                            {
                                //noinspection MagicNumber
                                builder.append((char) Integer.parseInt(this.next(4), 16));
                            }
                            catch (NumberFormatException ex)
                            {
                                throw this.syntaxError("Illegal escape.", ex);
                            }
                            break;
                        case '"':
                        case '\'':
                        case '\\':
                        case '/':
                            builder.append(ch);
                            break;
                        default:
                            throw this.syntaxError("Illegal escape.");
                    }
                    break;
                default:
                    if (ch == quote)
                    {
                        return builder.toString();
                    }
                    builder.append(ch);
            }
        }
    }

    public String nextTo(char delimiter) throws JSONException
    {
        StringBuilder builder = new StringBuilder();
        while (true)
        {
            char ch = this.next();
            if ((ch == delimiter) || (ch == 0) || (ch == '\n') || (ch == '\r'))
            {
                if (ch != 0)
                {
                    this.back();
                }
                return builder.toString().trim();
            }
            builder.append(ch);
        }
    }

    public String nextTo(String delimiters) throws JSONException
    {
        char ch;
        StringBuilder builder = new StringBuilder();
        while (true)
        {
            ch = this.next();
            if ((delimiters.indexOf(ch) >= 0) || (ch == 0) || (ch == '\n') || (ch == '\r'))
            {
                if (ch != 0)
                {
                    this.back();
                }
                return builder.toString().trim();
            }
            builder.append(ch);
        }
    }

    public Object nextValue() throws JSONException
    {
        char ch = this.nextClean();
        String str;
        switch (ch)
        {
            case '"':
            case '\'':
                return this.nextString(ch);
            case '{':
                this.back();
                return new JSONObject(this);
            case '[':
                this.back();
                return new JSONArray(this);
            default:
        }
        StringBuilder builder = new StringBuilder();
        while ((ch >= ' ') && (",:]}/\\\"[{;=#".indexOf(ch) < 0))
        {
            builder.append(ch);
            ch = this.next();
        }
        this.back();
        str = builder.toString().trim();
        if (str.isEmpty())
        {
            throw this.syntaxError("Missing value,");
        }
        return JSONObject.stringToValue(str);
    }

    public char skipTo(char to) throws JSONException
    {
        char ch;
        try
        {
            long startIndex = this.index;
            long startCharacter = this.character;
            long startLine = this.line;
            this.reader.mark(READ_AHEAD_LIMIT);
            do
            {
                ch = this.next();
                if (ch == 0)
                {
                    this.reader.reset();
                    this.index = startIndex;
                    this.character = startCharacter;
                    this.line = startLine;
                    return 0;
                }
            } while (ch != to);
            this.reader.mark(1);
        }
        catch (IOException ex)
        {
            throw new JSONException(ex);
        }
        this.back();
        return ch;
    }

    public JSONException syntaxError(String msg)
    {
        return new JSONException(msg + this.toString());
    }

    public JSONException syntaxError(String msg, Throwable cause)
    {
        return new JSONException(msg + this.toString(), cause);
    }

    @Override
    public String toString()
    {
        return " at " + this.index + " [character " + this.character + " line " + this.line + "]";
    }
}
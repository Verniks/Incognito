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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
public class JSONPointer
{
    private static final String ENCODING = "utf-8";

    public static class Builder
    {
        private final List<String> refTokens = new ArrayList<>(10);

        public JSONPointer build()
        {
            return new JSONPointer(this.refTokens);
        }

        public Builder append(String token)
        {
            if (token == null)
            {
                throw new NullPointerException("Null token.");
            }
            this.refTokens.add(token);
            return this;
        }

        public Builder append(int arrayIndex)
        {
            this.refTokens.add(String.valueOf(arrayIndex));
            return this;
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    private final List<String> refTokens;

    public JSONPointer(String pointer)
    {
        if (pointer == null)
        {
            throw new NullPointerException("Null pointer.");
        }
        if (pointer.isEmpty() || pointer.equals("#"))
        {
            this.refTokens = Collections.emptyList();
            return;
        }
        String refs;
        if (pointer.startsWith("#/"))
        {
            refs = pointer.substring(2);
            try
            {
                refs = URLDecoder.decode(refs, ENCODING);
            }
            catch (UnsupportedEncodingException e)
            {
                throw new RuntimeException(e);
            }
        }
        else if (pointer.startsWith("/"))
        {
            refs = pointer.substring(1);
        }
        else
        {
            throw new IllegalArgumentException("A JSON pointer should start with '/' or '#/'.");
        }
        this.refTokens = new ArrayList<>(10);
        for (String token : refs.split("/"))
        {
            this.refTokens.add(this.unescape(token));
        }
    }

    public JSONPointer(List<String> refTokens)
    {
        this.refTokens = new ArrayList<>(refTokens);
    }

    private String unescape(String token)
    {
        return token.replace("~1", "/").replace("~0", "~").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    public Object queryFrom(Object doc)
    {
        if (this.refTokens.isEmpty())
        {
            return doc;
        }
        Object curr = doc;
        for (String token : this.refTokens)
        {
            if (curr instanceof JSONObject)
            {
                curr = ((JSONObject) curr).opt(this.unescape(token));
            }
            else if (curr instanceof JSONArray)
            {
                curr = this.readByIndexToken(curr, token);
            }
            else
            {
                throw new JSONPointerException("Value[" + curr + "] is not an array or object therefore its key " + token + " cannot be resolved.");
            }
        }
        return curr;
    }

    private Object readByIndexToken(Object curr, String indexToken)
    {
        try
        {
            int index = Integer.parseInt(indexToken);
            JSONArray currArray = (JSONArray) curr;
            if (index >= currArray.length())
            {
                throw new JSONPointerException("Index " + index + " is out of bounds - the array has " + currArray.length() + " elements.");
            }
            return currArray.get(index);
        }
        catch (NumberFormatException ex)
        {
            throw new JSONPointerException(indexToken + " is not an array index.", ex);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("");
        for (String token : this.refTokens)
        {
            builder.append('/');
            builder.append(this.escape(token));
        }
        return builder.toString();
    }

    private String escape(String token)
    {
        return token.replace("~", "~0").replace("/", "~1").replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public String toURIFragment()
    {
        try
        {
            StringBuilder builder = new StringBuilder("#");
            for (String token : this.refTokens)
            {
                builder.append('/');
                builder.append(URLEncoder.encode(token, ENCODING));
            }
            return builder.toString();
        }
        catch (UnsupportedEncodingException ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
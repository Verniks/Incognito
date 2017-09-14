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
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JSONArray implements Iterable<Object>
{
    private final ArrayList<Object> list = new ArrayList<>(10);

    public JSONArray()
    {
    }

    public JSONArray(JSONTokener tokener) throws JSONException
    {
        if (tokener.nextClean() != '[')
        {
            throw tokener.syntaxError("A JSONArray text must start with '['.");
        }
        if (tokener.nextClean() != ']')
        {
            tokener.back();
            while (true)
            {
                if (tokener.nextClean() == ',')
                {
                    tokener.back();
                    this.list.add(JSONObject.NULL);
                }
                else
                {
                    tokener.back();
                    this.list.add(tokener.nextValue());
                }
                switch (tokener.nextClean())
                {
                    case ',':
                        if (tokener.nextClean() == ']')
                        {
                            return;
                        }
                        tokener.back();
                        break;
                    case ']':
                        return;
                    default:
                        throw tokener.syntaxError("Expected a ',' or ']'.");
                }
            }
        }
    }

    public JSONArray(String src) throws JSONException
    {
        this(new JSONTokener(src));
    }

    public JSONArray(Collection<?> coll)
    {
        for (Object obj : coll)
        {
            this.list.add(JSONObject.wrap(obj));
        }
    }

    public JSONArray(Object array) throws JSONException
    {
        if (array.getClass().isArray())
        {
            int length = Array.getLength(array);
            this.list.ensureCapacity(length);
            for (int i = 0; i < length; i += 1)
            {
                this.put(JSONObject.wrap(Array.get(array, i)));
            }
        }
        else
        {
            throw new JSONException("JSONArray initial value should be a string, collection or array.");
        }
    }

    @Override
    public Iterator<Object> iterator()
    {
        return this.list.iterator();
    }

    public Object get(int index) throws JSONException
    {
        Object obj = this.opt(index);
        if (obj == null)
        {
            throw new JSONException("JSONArray[" + index + "] not found.");
        }
        return obj;
    }

    public boolean getBoolean(int index) throws JSONException
    {
        Object obj = this.get(index);
        if (obj.equals(Boolean.TRUE) || ((obj instanceof String) && ((String) obj).equalsIgnoreCase("true")))
        {
            return true;
        }
        if (obj.equals(Boolean.FALSE) || ((obj instanceof String) && ((String) obj).equalsIgnoreCase("false")))
        {
            return false;
        }
        throw new JSONException("JSONArray[" + index + "] is not a boolean.");
    }

    public double getDouble(int index) throws JSONException
    {
        Object obj = this.get(index);
        try
        {
            return (obj instanceof Number) ? ((Number) obj).doubleValue() : Double.parseDouble((String) obj);
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONArray[" + index + "] is not a Number.", ex);
        }
    }

    public float getFloat(int index) throws JSONException
    {
        Object obj = this.get(index);
        try
        {
            return (obj instanceof Number) ? ((Number) obj).floatValue() : Float.parseFloat(obj.toString());
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONArray[" + index + "] is not a Number.", ex);
        }
    }

    public Number getNumber(int index) throws JSONException
    {
        Object obj = this.get(index);
        try
        {
            if (obj instanceof Number)
            {
                return (Number) obj;
            }
            return JSONObject.stringToNumber(obj.toString());
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONArray[" + index + "] is not a Number.", ex);
        }
    }

    public <E extends Enum<E>> E getEnum(Class<E> clazz, int index) throws JSONException
    {
        E val = this.optEnum(clazz, index);
        if (val == null)
        {
            throw new JSONException("JSONArray[" + index + "] is not an enum of type " + JSONObject.quote(clazz.getSimpleName()) + ".");
        }
        return val;
    }

    public BigDecimal getBigDecimal(int index) throws JSONException
    {
        Object obj = this.get(index);
        try
        {
            return new BigDecimal(obj.toString());
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONArray[" + index + "] could not convert to BigDecimal.", ex);
        }
    }

    public BigInteger getBigInteger(int index) throws JSONException
    {
        Object obj = this.get(index);
        try
        {
            return new BigInteger(obj.toString());
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONArray[" + index + "] could not convert to BigInteger.", ex);
        }
    }

    public int getInt(int index) throws JSONException
    {
        Object obj = this.get(index);
        try
        {
            return (obj instanceof Number) ? ((Number) obj).intValue() : Integer.parseInt((String) obj);
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONArray[" + index + "] is not a Number.", ex);
        }
    }

    public JSONArray getJSONArray(int index) throws JSONException
    {
        Object obj = this.get(index);
        if (obj instanceof JSONArray)
        {
            return (JSONArray) obj;
        }
        throw new JSONException("JSONArray[" + index + "] is not a JSONArray.");
    }

    public JSONObject getJSONObject(int index) throws JSONException
    {
        Object obj = this.get(index);
        if (obj instanceof JSONObject)
        {
            return (JSONObject) obj;
        }
        throw new JSONException("JSONArray[" + index + "] is not a JSONObject.");
    }

    public long getLong(int index) throws JSONException
    {
        Object obj = this.get(index);
        try
        {
            return (obj instanceof Number) ? ((Number) obj).longValue() : Long.parseLong((String) obj);
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONArray[" + index + "] is not a Number.", ex);
        }
    }

    public String getString(int index) throws JSONException
    {
        Object obj = this.get(index);
        if (obj instanceof String)
        {
            return (String) obj;
        }
        throw new JSONException("JSONArray[" + index + "] not a String.");
    }

    public boolean isNull(int index)
    {
        return JSONObject.NULL.equals(this.opt(index));
    }

    public String join(String separator) throws JSONException
    {
        int len = this.length();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < len; i += 1)
        {
            if (i > 0)
            {
                builder.append(separator);
            }
            builder.append(JSONObject.valueToString(this.list.get(i)));
        }
        return builder.toString();
    }

    public int length()
    {
        return this.list.size();
    }

    public Object opt(int index)
    {
        return ((index < 0) || (index >= this.length())) ? null : this.list.get(index);
    }

    public boolean optBoolean(int index)
    {
        return this.optBoolean(index, false);
    }

    public boolean optBoolean(int index, boolean defaultValue)
    {
        try
        {
            return this.getBoolean(index);
        }
        catch (Exception ex)
        {
            return defaultValue;
        }
    }

    public double optDouble(int index)
    {
        return this.optDouble(index, Double.NaN);
    }

    public double optDouble(int index, double defaultValue)
    {
        Object val = this.opt(index);
        if (JSONObject.NULL.equals(val))
        {
            return defaultValue;
        }
        if (val instanceof Number)
        {
            return ((Number) val).doubleValue();
        }
        if (val instanceof String)
        {
            try
            {
                return Double.parseDouble((String) val);
            }
            catch (Exception ex)
            {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public float optFloat(int index)
    {
        return this.optFloat(index, Float.NaN);
    }

    public float optFloat(int index, float defaultValue)
    {
        Object val = this.opt(index);
        if (JSONObject.NULL.equals(val))
        {
            return defaultValue;
        }
        if (val instanceof Number)
        {
            return ((Number) val).floatValue();
        }
        if (val instanceof String)
        {
            try
            {
                return Float.parseFloat((String) val);
            }
            catch (Exception ex)
            {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public int optInt(int index)
    {
        return this.optInt(index, 0);
    }

    public int optInt(int index, int defaultValue)
    {
        Object val = this.opt(index);
        if (JSONObject.NULL.equals(val))
        {
            return defaultValue;
        }
        if (val instanceof Number)
        {
            return ((Number) val).intValue();
        }

        if (val instanceof String)
        {
            try
            {
                return new BigDecimal(val.toString()).intValue();
            }
            catch (Exception ex)
            {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public <E extends Enum<E>> E optEnum(Class<E> clazz, int index)
    {
        return this.optEnum(clazz, index, null);
    }

    public <E extends Enum<E>> E optEnum(Class<E> clazz, int index, E defaultValue)
    {
        try
        {
            Object val = this.opt(index);
            if (JSONObject.NULL.equals(val))
            {
                return defaultValue;
            }
            if (clazz.isAssignableFrom(val.getClass()))
            {
                //noinspection unchecked
                return (E) val;
            }
            return Enum.valueOf(clazz, val.toString());
        }
        catch (IllegalArgumentException | NullPointerException ex)
        {
            return defaultValue;
        }
    }

    public BigInteger optBigInteger(int index, BigInteger defaultValue)
    {
        Object val = this.opt(index);
        if (JSONObject.NULL.equals(val))
        {
            return defaultValue;
        }
        if (val instanceof BigInteger)
        {
            return (BigInteger) val;
        }
        if (val instanceof BigDecimal)
        {
            return ((BigDecimal) val).toBigInteger();
        }
        if ((val instanceof Double) || (val instanceof Float))
        {
            return new BigDecimal(((Number) val).doubleValue()).toBigInteger();
        }
        if ((val instanceof Long) || (val instanceof Integer) || (val instanceof Short) || (val instanceof Byte))
        {
            return BigInteger.valueOf(((Number) val).longValue());
        }
        try
        {
            String valStr = val.toString();
            if (JSONObject.isDecimalNotation(valStr))
            {
                return new BigDecimal(valStr).toBigInteger();
            }
            return new BigInteger(valStr);
        }
        catch (Exception ex)
        {
            return defaultValue;
        }
    }

    public BigDecimal optBigDecimal(int index, BigDecimal defaultValue)
    {
        Object val = this.opt(index);
        if (JSONObject.NULL.equals(val))
        {
            return defaultValue;
        }
        if (val instanceof BigDecimal)
        {
            return (BigDecimal) val;
        }
        if (val instanceof BigInteger)
        {
            return new BigDecimal((BigInteger) val);
        }
        if ((val instanceof Double) || (val instanceof Float))
        {
            return new BigDecimal(((Number) val).doubleValue());
        }
        if ((val instanceof Long) || (val instanceof Integer) || (val instanceof Short) || (val instanceof Byte))
        {
            return new BigDecimal(((Number) val).longValue());
        }
        try
        {
            return new BigDecimal(val.toString());
        }
        catch (Exception ex)
        {
            return defaultValue;
        }
    }

    public JSONArray optJSONArray(int index)
    {
        Object obj = this.opt(index);
        return (obj instanceof JSONArray) ? (JSONArray) obj : null;
    }

    public JSONObject optJSONObject(int index)
    {
        Object obj = this.opt(index);
        return (obj instanceof JSONObject) ? (JSONObject) obj : null;
    }

    public long optLong(int index)
    {
        return this.optLong(index, 0);
    }

    public long optLong(int index, long defaultValue)
    {
        Object val = this.opt(index);
        if (JSONObject.NULL.equals(val))
        {
            return defaultValue;
        }
        if (val instanceof Number)
        {
            return ((Number) val).longValue();
        }

        if (val instanceof String)
        {
            try
            {
                return new BigDecimal(val.toString()).longValue();
            }
            catch (Exception ex)
            {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public Number optNumber(int index)
    {
        return this.optNumber(index, null);
    }

    public Number optNumber(int index, Number defaultValue)
    {
        Object val = this.opt(index);
        if (JSONObject.NULL.equals(val))
        {
            return defaultValue;
        }
        if (val instanceof Number)
        {
            return (Number) val;
        }

        if (val instanceof String)
        {
            try
            {
                return JSONObject.stringToNumber((String) val);
            }
            catch (Exception ex)
            {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public String optString(int index)
    {
        return this.optString(index, "");
    }

    public String optString(int index, String defaultValue)
    {
        Object obj = this.opt(index);
        return JSONObject.NULL.equals(obj) ? defaultValue : obj.toString();
    }

    public JSONArray put(boolean val)
    {
        this.put(val ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }

    public JSONArray put(Collection<?> val)
    {
        this.put(new JSONArray(val));
        return this;
    }

    public JSONArray put(double val) throws JSONException
    {
        JSONObject.testValidity(val);
        this.put((Object) val);
        return this;
    }

    public JSONArray put(int val)
    {
        this.put((Object) val);
        return this;
    }

    public JSONArray put(long val)
    {
        this.put((Object) val);
        return this;
    }

    public JSONArray put(Map<?, ?> val)
    {
        this.put(new JSONObject(val));
        return this;
    }

    public JSONArray put(Object val)
    {
        this.list.add(val);
        return this;
    }

    public JSONArray put(int index, boolean val) throws JSONException
    {
        this.put(index, val ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }

    public JSONArray put(int index, Collection<?> val) throws JSONException
    {
        this.put(index, new JSONArray(val));
        return this;
    }

    public JSONArray put(int index, double val) throws JSONException
    {
        this.put(index, (Object) val);
        return this;
    }

    public JSONArray put(int index, int val) throws JSONException
    {
        this.put(index, (Object) val);
        return this;
    }

    public JSONArray put(int index, long val) throws JSONException
    {
        this.put(index, (Object) val);
        return this;
    }

    public JSONArray put(int index, Map<?, ?> val) throws JSONException
    {
        this.put(index, new JSONObject(val));
        return this;
    }

    public JSONArray put(int index, Object val) throws JSONException
    {
        JSONObject.testValidity(val);
        if (index < 0)
        {
            throw new JSONException("JSONArray[" + index + "] not found.");
        }
        if (index < this.length())
        {
            this.list.set(index, val);
        }
        else if (index == this.length())
        {
            this.put(val);
        }
        else
        {
            this.list.ensureCapacity(index + 1);
            while (index != this.length())
            {
                this.put(JSONObject.NULL);
            }
            this.put(val);
        }
        return this;
    }

    public Object query(String pointer)
    {
        return this.query(new JSONPointer(pointer));
    }

    public Object query(JSONPointer pointer)
    {
        return pointer.queryFrom(this);
    }

    public Object optQuery(String pointer)
    {
        return this.optQuery(new JSONPointer(pointer));
    }

    public Object optQuery(JSONPointer pointer)
    {
        try
        {
            return pointer.queryFrom(this);
        }
        catch (JSONPointerException ex)
        {
            return null;
        }
    }

    public Object remove(int index)
    {
        return ((index >= 0) && (index < this.length())) ? this.list.remove(index) : null;
    }

    public boolean isSimilar(Object other)
    {
        if (! (other instanceof JSONArray))
        {
            return false;
        }
        int len = this.length();
        if (len != ((JSONArray) other).length())
        {
            return false;
        }
        for (int i = 0; i < len; i += 1)
        {
            Object valueThis = this.list.get(i);
            Object valueOther = ((JSONArray) other).list.get(i);
            if (valueThis == null)
            {
                return false;
            }
            if (valueThis.equals(valueOther))
            {
                return true;
            }
            if (valueThis instanceof JSONObject)
            {
                if (! ((JSONObject) valueThis).similar(valueOther))
                {
                    return false;
                }
            }
            else if (valueThis instanceof JSONArray)
            {
                if (! ((JSONArray) valueThis).isSimilar(valueOther))
                {
                    return false;
                }
            }
            else if (! valueThis.equals(valueOther))
            {
                return false;
            }
        }
        return true;
    }

    public JSONObject toJSONObject(JSONArray array) throws JSONException
    {
        if ((array == null) || (array.length() == 0) || (this.length() == 0))
        {
            return null;
        }
        JSONObject obj = new JSONObject(array.length());
        for (int i = 0; i < array.length(); i += 1)
        {
            obj.put(array.getString(i), this.opt(i));
        }
        return obj;
    }

    @Override
    public String toString()
    {
        try
        {
            return this.toString(0);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public String toString(int indentFactor) throws JSONException
    {
        try (StringWriter writer = new StringWriter())
        {
            synchronized (writer.getBuffer())
            {
                return this.write(writer, indentFactor, 0).toString();
            }
        }
        catch (IOException ex)
        {
            throw new JSONException(ex);
        }
    }

    public Writer write(Writer writer) throws JSONException
    {
        return this.write(writer, 0, 0);
    }

    public Writer write(Writer writer, int indentFactor, int indent) throws JSONException
    {
        try
        {
            boolean commanate = false;
            int length = this.length();
            writer.write('[');
            if (length == 1)
            {
                try
                {
                    JSONObject.writeValue(writer, this.list.get(0), indentFactor, indent);
                }
                catch (Exception ex)
                {
                    throw new JSONException("Unable to write JSONArray value at index: 0.", ex);
                }
            }
            else if (length != 0)
            {
                int newIndent = indent + indentFactor;
                for (int i = 0; i < length; i += 1)
                {
                    if (commanate)
                    {
                        writer.write(',');
                    }
                    if (indentFactor > 0)
                    {
                        writer.write('\n');
                    }
                    JSONObject.indent(writer, newIndent);
                    try
                    {
                        JSONObject.writeValue(writer, this.list.get(i), indentFactor, newIndent);
                    }
                    catch (Exception ex)
                    {
                        throw new JSONException("Unable to write JSONArray value at index: " + i + ".", ex);
                    }
                    commanate = true;
                }
                if (indentFactor > 0)
                {
                    writer.write('\n');
                }
                JSONObject.indent(writer, indent);
            }
            writer.write(']');
            return writer;
        }
        catch (IOException ex)
        {
            throw new JSONException(ex);
        }
    }

    public List<Object> toList()
    {
        List<Object> results = new ArrayList<>(this.list.size());
        for (Object elem : this.list)
        {
            if ((elem == null) || JSONObject.NULL.equals(elem))
            {
                results.add(null);
            }
            else if (elem instanceof JSONArray)
            {
                results.add(((JSONArray) elem).toList());
            }
            else if (elem instanceof JSONObject)
            {
                results.add(((JSONObject) elem).toMap());
            }
            else
            {
                results.add(elem);
            }
        }
        return results;
    }
}
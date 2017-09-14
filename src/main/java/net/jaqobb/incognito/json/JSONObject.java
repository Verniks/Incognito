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

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

@SuppressWarnings("MagicNumber")
public class JSONObject
{
    private static class Null
    {
        @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException", "MethodDoesntCallSuperMethod"})
        @Override
        protected Object clone()
        {
            return this;
        }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        @Override
        public boolean equals(Object object)
        {
            return (object == null) || (object == this);
        }

        @Override
        public int hashCode()
        {
            return 0;
        }

        @Override
        public String toString()
        {
            return "null";
        }
    }

    private final Map<String, Object> map;

    public static final Object NULL = new Null();

    public JSONObject()
    {
        this.map = new HashMap<>(16);
    }

    public JSONObject(JSONObject obj, String[] names)
    {
        this(names.length);
        for (String name : names)
        {
            try
            {
                this.putOnce(name, obj.opt(name));
            }
            catch (Exception ignored)
            {
            }
        }
    }

    public JSONObject(JSONTokener tokener) throws JSONException
    {
        this();
        char ch;
        String key;
        if (tokener.nextClean() != '{')
        {
            throw tokener.syntaxError("A JSONObject text must begin with '{'.");
        }
        while (true)
        {
            ch = tokener.nextClean();
            switch (ch)
            {
                case 0:
                    throw tokener.syntaxError("A JSONObject text must end with '}'.");
                case '}':
                    return;
                default:
                    tokener.back();
                    key = tokener.nextValue().toString();
            }
            ch = tokener.nextClean();
            if (ch != ':')
            {
                throw tokener.syntaxError("Expected a ':' after a key.");
            }
            if (key != null)
            {
                if (this.opt(key) != null)
                {
                    throw tokener.syntaxError("Duplicate key \"" + key + "\".");
                }
                Object val = tokener.nextValue();
                if (val != null)
                {
                    this.put(key, val);
                }
            }
            switch (tokener.nextClean())
            {
                case ';':
                case ',':
                    if (tokener.nextClean() == '}')
                    {
                        return;
                    }
                    tokener.back();
                    break;
                case '}':
                    return;
                default:
                    throw tokener.syntaxError("Expected a ',' or '}'.");
            }
        }
    }

    public JSONObject(Map<?, ?> map)
    {
        if (map == null)
        {
            this.map = new HashMap<>(16);
        }
        else
        {
            this.map = new HashMap<>(map.size());
            for (Entry<?, ?> entry : map.entrySet())
            {
                Object val = entry.getValue();
                if (val != null)
                {
                    this.map.put(String.valueOf(entry.getKey()), wrap(val));
                }
            }
        }
    }

    public JSONObject(Object bean)
    {
        this();
        this.populateMap(bean);
    }

    public JSONObject(Object object, String[] names)
    {
        this(names.length);
        Class<?> clazz = object.getClass();
        for (String name : names)
        {
            try
            {
                this.putOpt(name, clazz.getField(name).get(object));
            }
            catch (Exception ignored)
            {
            }
        }
    }

    public JSONObject(String src) throws JSONException
    {
        this(new JSONTokener(src));
    }

    public JSONObject(String baseName, Locale locale) throws JSONException
    {
        this();
        ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, Thread.currentThread().getContextClassLoader());
        Enumeration<String> keys = bundle.getKeys();
        while (keys.hasMoreElements())
        {
            Object key = keys.nextElement();
            if (key != null)
            {
                String[] path = ((String) key).split("\\.");
                int last = path.length - 1;
                JSONObject target = this;
                for (int i = 0; i < last; i += 1)
                {
                    String segment = path[i];
                    JSONObject nextTarget = target.optJSONObject(segment);
                    if (nextTarget == null)
                    {
                        nextTarget = new JSONObject();
                        target.put(segment, nextTarget);
                    }
                    target = nextTarget;
                }
                target.put(path[last], bundle.getString((String) key));
            }
        }
    }

    protected JSONObject(int initialCapacity)
    {
        this.map = new HashMap<>(initialCapacity);
    }

    public JSONObject accumulate(String key, Object val) throws JSONException
    {
        testValidity(val);
        Object obj = this.opt(key);
        if (obj == null)
        {
            this.put(key, (val instanceof JSONArray) ? new JSONArray().put(val) : val);
        }
        else if (obj instanceof JSONArray)
        {
            ((JSONArray) obj).put(val);
        }
        else
        {
            this.put(key, new JSONArray().put(obj).put(val));
        }
        return this;
    }

    public JSONObject append(String key, Object val) throws JSONException
    {
        testValidity(val);
        Object obj = this.opt(key);
        if (obj == null)
        {
            this.put(key, new JSONArray().put(val));
        }
        else if (obj instanceof JSONArray)
        {
            this.put(key, ((JSONArray) obj).put(val));
        }
        else
        {
            throw new JSONException("JSONObject[" + key + "] is not a JSONArray.");
        }
        return this;
    }

    public static String doubleToString(double val)
    {
        if (Double.isInfinite(val) || Double.isNaN(val))
        {
            return "null";
        }
        String str = Double.toString(val);
        if ((str.indexOf('.') > 0) && (str.indexOf('e') < 0) && (str.indexOf('E') < 0))
        {
            while (str.endsWith("0"))
            {
                str = str.substring(0, str.length() - 1);
            }
            if (str.endsWith("."))
            {
                str = str.substring(0, str.length() - 1);
            }
        }
        return str;
    }

    public Object get(String key) throws JSONException
    {
        if (key == null)
        {
            throw new JSONException("Null key.");
        }
        Object obj = this.opt(key);
        if (obj == null)
        {
            throw new JSONException("JSONObject[" + quote(key) + "] not found.");
        }
        return obj;
    }

    public <E extends Enum<E>> E getEnum(Class<E> clazz, String key) throws JSONException
    {
        E val = this.optEnum(clazz, key);
        if (val == null)
        {
            throw new JSONException("JSONObject[" + quote(key) + "] is not an enum of type " + quote(clazz.getSimpleName()) + ".");
        }
        return val;
    }

    public boolean getBoolean(String key) throws JSONException
    {
        Object obj = this.get(key);
        if (obj.equals(Boolean.TRUE) || ((obj instanceof String) && ((String) obj).equalsIgnoreCase("true")))
        {
            return true;
        }
        if (obj.equals(Boolean.FALSE) || ((obj instanceof String) && ((String) obj).equalsIgnoreCase("false")))
        {
            return false;
        }
        throw new JSONException("JSONObject[" + quote(key) + "] is not a Boolean.");
    }

    public BigInteger getBigInteger(String key) throws JSONException
    {
        Object obj = this.get(key);
        try
        {
            return new BigInteger(obj.toString());
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONObject[" + quote(key) + "] could not be converted to BigInteger.", ex);
        }
    }

    public BigDecimal getBigDecimal(String key) throws JSONException
    {
        Object obj = this.get(key);
        if (obj instanceof BigDecimal)
        {
            return (BigDecimal) obj;
        }
        try
        {
            return new BigDecimal(obj.toString());
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONObject[" + quote(key) + "] could not be converted to BigDecimal.", ex);
        }
    }

    public double getDouble(String key) throws JSONException
    {
        Object obj = this.get(key);
        try
        {
            return (obj instanceof Number) ? ((Number) obj).doubleValue() : Double.parseDouble(obj.toString());
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONObject[" + quote(key) + "] is not a Number.", ex);
        }
    }

    public float getFloat(String key) throws JSONException
    {
        Object obj = this.get(key);
        try
        {
            return (obj instanceof Number) ? ((Number) obj).floatValue() : Float.parseFloat(obj.toString());
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONObject[" + quote(key) + "] is not a Number.", ex);
        }
    }

    public Number getNumber(String key) throws JSONException
    {
        Object obj = this.get(key);
        try
        {
            if (obj instanceof Number)
            {
                return (Number) obj;
            }
            return stringToNumber(obj.toString());
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONObject[" + quote(key) + "] is not a Number.", ex);
        }
    }

    public int getInt(String key) throws JSONException
    {
        Object obj = this.get(key);
        try
        {
            return (obj instanceof Number) ? ((Number) obj).intValue() : Integer.parseInt((String) obj);
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONObject[" + quote(key) + "] is not a Number.", ex);
        }
    }

    public JSONArray getJSONArray(String key) throws JSONException
    {
        Object obj = this.get(key);
        if (obj instanceof JSONArray)
        {
            return (JSONArray) obj;
        }
        throw new JSONException("JSONObject[" + quote(key) + "] is not a JSONArray.");
    }

    public JSONObject getJSONObject(String key) throws JSONException
    {
        Object obj = this.get(key);
        if (obj instanceof JSONObject)
        {
            return (JSONObject) obj;
        }
        throw new JSONException("JSONObject[" + quote(key) + "] is not a JSONObject.");
    }

    public long getLong(String key) throws JSONException
    {
        Object obj = this.get(key);
        try
        {
            return (obj instanceof Number) ? ((Number) obj).longValue() : Long.parseLong((String) obj);
        }
        catch (Exception ex)
        {
            throw new JSONException("JSONObject[" + quote(key) + "] is not a Number.", ex);
        }
    }

    public static String[] getNames(JSONObject obj)
    {
        int length = obj.length();
        if (length == 0)
        {
            return null;
        }
        return obj.keySet().toArray(new String[length]);
    }

    public static String[] getNames(Object object)
    {
        if (object == null)
        {
            return null;
        }
        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getFields();
        int length = fields.length;
        if (length == 0)
        {
            return null;
        }
        String[] names = new String[length];
        for (int i = 0; i < length; i += 1)
        {
            names[i] = fields[i].getName();
        }
        return names;
    }

    public String getString(String key) throws JSONException
    {
        Object obj = this.get(key);
        if (obj instanceof String)
        {
            return (String) obj;
        }
        throw new JSONException("JSONObject[" + quote(key) + "] not a String.");
    }

    public boolean has(String key)
    {
        return this.map.containsKey(key);
    }

    public JSONObject increment(String key) throws JSONException
    {
        Object val = this.opt(key);
        if (val == null)
        {
            this.put(key, 1);
        }
        else if (val instanceof BigInteger)
        {
            this.put(key, ((BigInteger) val).add(BigInteger.ONE));
        }
        else if (val instanceof BigDecimal)
        {
            this.put(key, ((BigDecimal) val).add(BigDecimal.ONE));
        }
        else if (val instanceof Integer)
        {
            this.put(key, (Integer) val + 1);
        }
        else if (val instanceof Long)
        {
            this.put(key, (Long) val + 1L);
        }
        else if (val instanceof Double)
        {
            this.put(key, (Double) val + 1.0d);
        }
        else if (val instanceof Float)
        {
            this.put(key, (Float) val + 1.0f);
        }
        else
        {
            throw new JSONException("Unable to increment [" + quote(key) + "].");
        }
        return this;
    }

    public boolean isNull(String key)
    {
        return JSONObject.NULL.equals(this.opt(key));
    }

    public Iterator<String> keys()
    {
        return this.keySet().iterator();
    }

    public Set<String> keySet()
    {
        return this.map.keySet();
    }

    protected Set<Entry<String, Object>> entrySet()
    {
        return this.map.entrySet();
    }

    public int length()
    {
        return this.map.size();
    }

    public JSONArray names()
    {
        if (this.map.isEmpty())
        {
            return null;
        }
        return new JSONArray(this.map.keySet());
    }

    public static String numberToString(Number number) throws JSONException
    {
        if (number == null)
        {
            throw new JSONException("Null number.");
        }
        testValidity(number);
        String str = number.toString();
        if ((str.indexOf('.') > 0) && (str.indexOf('e') < 0) && (str.indexOf('E') < 0))
        {
            while (str.endsWith("0"))
            {
                str = str.substring(0, str.length() - 1);
            }
            if (str.endsWith("."))
            {
                str = str.substring(0, str.length() - 1);
            }
        }
        return str;
    }

    public Object opt(String key)
    {
        return (key == null) ? null : this.map.get(key);
    }

    public <E extends Enum<E>> E optEnum(Class<E> clazz, String key)
    {
        return this.optEnum(clazz, key, null);
    }

    public <E extends Enum<E>> E optEnum(Class<E> clazz, String key, E defaultValue)
    {
        try
        {
            Object val = this.opt(key);
            if (NULL.equals(val))
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

    public boolean optBoolean(String key)
    {
        return this.optBoolean(key, false);
    }

    public boolean optBoolean(String key, boolean defaultValue)
    {
        Object val = this.opt(key);
        if (NULL.equals(val))
        {
            return defaultValue;
        }
        if (val instanceof Boolean)
        {
            return (Boolean) val;
        }
        try
        {
            return this.getBoolean(key);
        }
        catch (Exception ex)
        {
            return defaultValue;
        }
    }

    public BigDecimal optBigDecimal(String key, BigDecimal defaultValue)
    {
        Object val = this.opt(key);
        if (NULL.equals(val))
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

    public BigInteger optBigInteger(String key, BigInteger defaultValue)
    {
        Object val = this.opt(key);
        if (NULL.equals(val))
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
            if (isDecimalNotation(valStr))
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

    public double optDouble(String key)
    {
        return this.optDouble(key, Double.NaN);
    }

    public double optDouble(String key, double defaultValue)
    {
        Object val = this.opt(key);
        if (NULL.equals(val))
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
            catch (NumberFormatException ex)
            {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public float optFloat(String key)
    {
        return this.optFloat(key, Float.NaN);
    }

    public float optFloat(String key, float defaultValue)
    {
        Object val = this.opt(key);
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
            catch (NumberFormatException ex)
            {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public int optInt(String key)
    {
        return this.optInt(key, 0);
    }

    public int optInt(String key, int defaultValue)
    {
        Object val = this.opt(key);
        if (NULL.equals(val))
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
                return new BigDecimal((String) val).intValue();
            }
            catch (NumberFormatException ex)
            {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public JSONArray optJSONArray(String key)
    {
        Object obj = this.opt(key);
        return (obj instanceof JSONArray) ? (JSONArray) obj : null;
    }

    public JSONObject optJSONObject(String key)
    {
        Object obj = this.opt(key);
        return (obj instanceof JSONObject) ? (JSONObject) obj : null;
    }

    public long optLong(String key)
    {
        return this.optLong(key, 0);
    }

    public long optLong(String key, long defaultValue)
    {
        Object val = this.opt(key);
        if (NULL.equals(val))
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
                return new BigDecimal((String) val).longValue();
            }
            catch (NumberFormatException ex)
            {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public Number optNumber(String key)
    {
        return this.optNumber(key, null);
    }

    public Number optNumber(String key, Number defaultValue)
    {
        Object val = this.opt(key);
        if (NULL.equals(val))
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
                return stringToNumber((String) val);
            }
            catch (NumberFormatException ex)
            {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public String optString(String key)
    {
        return this.optString(key, "");
    }

    public String optString(String key, String defaultValue)
    {
        Object obj = this.opt(key);
        return NULL.equals(obj) ? defaultValue : obj.toString();
    }

    private void populateMap(Object bean)
    {
        Class<?> clazz = bean.getClass();
        boolean includeSuperClass = clazz.getClassLoader() != null;
        Method[] methods = includeSuperClass ? clazz.getMethods() : clazz.getDeclaredMethods();
        for (Method method : methods)
        {
            int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && ! Modifier.isStatic(modifiers) && (method.getParameterTypes().length == 0) && ! method.isBridge() &&
                (! method.getReturnType().equals(Void.TYPE)))
            {
                String name = method.getName();
                String key;
                if (name.startsWith("get"))
                {
                    if ("getClass".equals(name) || "getDeclaringClass".equals(name))
                    {
                        continue;
                    }
                    key = name.substring(3);
                }
                else if (name.startsWith("is"))
                {
                    key = name.substring(2);
                }
                else
                {
                    continue;
                }
                if ((! key.isEmpty()) && Character.isUpperCase(key.charAt(0)))
                {
                    if (key.length() == 1)
                    {
                        key = key.toLowerCase(Locale.ROOT);
                    }
                    else if (! Character.isUpperCase(key.charAt(1)))
                    {
                        key = key.substring(0, 1).toLowerCase(Locale.ROOT) + key.substring(1);
                    }
                    try
                    {
                        Object res = method.invoke(bean);
                        if (res != null)
                        {
                            this.map.put(key, wrap(res));
                            if (res instanceof Closeable)
                            {
                                try
                                {
                                    ((Closeable) res).close();
                                }
                                catch (IOException ignored)
                                {
                                }
                            }
                        }
                    }
                    catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ignored)
                    {
                    }
                }
            }
        }
    }

    public JSONObject put(String key, boolean val) throws JSONException
    {
        this.put(key, val ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }

    public JSONObject put(String key, Collection<?> val) throws JSONException
    {
        this.put(key, new JSONArray(val));
        return this;
    }

    public JSONObject put(String key, double val) throws JSONException
    {
        this.put(key, (Object) val);
        return this;
    }

    public JSONObject put(String key, float val) throws JSONException
    {
        this.put(key, (Object) val);
        return this;
    }

    public JSONObject put(String key, int val) throws JSONException
    {
        this.put(key, (Object) val);
        return this;
    }

    public JSONObject put(String key, long val) throws JSONException
    {
        this.put(key, (Object) val);
        return this;
    }

    public JSONObject put(String key, Map<?, ?> val) throws JSONException
    {
        this.put(key, new JSONObject(val));
        return this;
    }

    public JSONObject put(String key, Object val) throws JSONException
    {
        if (key == null)
        {
            throw new NullPointerException("Null key.");
        }
        if (val != null)
        {
            testValidity(val);
            this.map.put(key, val);
        }
        else
        {
            this.remove(key);
        }
        return this;
    }

    public JSONObject putOnce(String key, Object val) throws JSONException
    {
        if ((key != null) && (val != null))
        {
            if (this.opt(key) != null)
            {
                throw new JSONException("Duplicate key \"" + key + "\".");
            }
            this.put(key, val);
        }
        return this;
    }

    public JSONObject putOpt(String key, Object val) throws JSONException
    {
        if ((key != null) && (val != null))
        {
            this.put(key, val);
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

    public static String quote(String str)
    {
        try (StringWriter writer = new StringWriter())
        {
            synchronized (writer.getBuffer())
            {
                return quote(str, writer).toString();
            }
        }
        catch (IOException ex)
        {
            return "";
        }
    }

    public static Writer quote(String str, Writer writer) throws IOException
    {
        if ((str == null) || (str.isEmpty()))
        {
            writer.write("\"\"");
            return writer;
        }
        char b;
        char ch = 0;
        String hhhh;
        int i;
        int len = str.length();
        writer.write('"');
        for (i = 0; i < len; i += 1)
        {
            b = ch;
            ch = str.charAt(i);
            switch (ch)
            {
                case '\\':
                case '"':
                    writer.write('\\');
                    writer.write(ch);
                    break;
                case '/':
                    if (b == '<')
                    {
                        writer.write('\\');
                    }
                    writer.write(ch);
                    break;
                case '\b':
                    writer.write("\\b");
                    break;
                case '\t':
                    writer.write("\\t");
                    break;
                case '\n':
                    writer.write("\\n");
                    break;
                case '\f':
                    writer.write("\\f");
                    break;
                case '\r':
                    writer.write("\\r");
                    break;
                default:
                    if ((ch < ' ') || ((ch >= '\u0080') && (ch < '\u00a0')) || ((ch >= '\u2000') && (ch < '\u2100')))
                    {
                        writer.write("\\u");
                        hhhh = Integer.toHexString(ch);
                        writer.write("0000", 0, 4 - hhhh.length());
                        writer.write(hhhh);
                    }
                    else
                    {
                        writer.write(ch);
                    }
            }
        }
        writer.write('"');
        return writer;
    }

    public Object remove(String key)
    {
        return this.map.remove(key);
    }

    public boolean similar(Object other)
    {
        try
        {
            if (! (other instanceof JSONObject))
            {
                return false;
            }
            if (! this.keySet().equals(((JSONObject) other).keySet()))
            {
                return false;
            }
            for (Entry<String, ?> entry : this.entrySet())
            {
                String name = entry.getKey();
                Object valueThis = entry.getValue();
                Object valueOther = ((JSONObject) other).get(name);
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
        catch (Throwable ex)
        {
            return false;
        }
    }

    protected static boolean isDecimalNotation(String val)
    {
        return (val.indexOf('.') > - 1) || (val.indexOf('e') > - 1) || (val.indexOf('E') > - 1) || "-0".equals(val);
    }

    protected static Number stringToNumber(String val) throws NumberFormatException
    {
        char initial = val.charAt(0);
        if (((initial >= '0') && (initial <= '9')) || (initial == '-'))
        {
            if (isDecimalNotation(val))
            {
                if (val.length() > 14)
                {
                    return new BigDecimal(val);
                }
                Double dVal = Double.valueOf(val);
                if (dVal.isInfinite() || dVal.isNaN())
                {
                    return new BigDecimal(val);
                }
                return dVal;
            }
            BigInteger iVal = new BigInteger(val);
            if (iVal.bitLength() <= 31)
            {
                return iVal.intValue();
            }
            if (iVal.bitLength() <= 63)
            {
                return iVal.longValue();
            }
            return iVal;
        }
        throw new NumberFormatException("Value[" + val + "] is not a valid Number.");
    }

    public static Object stringToValue(String str)
    {
        if (str.isEmpty())
        {
            return str;
        }
        if (str.equalsIgnoreCase("true"))
        {
            return Boolean.TRUE;
        }
        if (str.equalsIgnoreCase("false"))
        {
            return Boolean.FALSE;
        }
        if (str.equalsIgnoreCase("null"))
        {
            return JSONObject.NULL;
        }
        char initial = str.charAt(0);
        if (((initial >= '0') && (initial <= '9')) || (initial == '-'))
        {
            try
            {
                if (isDecimalNotation(str))
                {
                    Double val = Double.valueOf(str);
                    if (! val.isInfinite() && ! val.isNaN())
                    {
                        return val;
                    }
                }
                else
                {
                    Long val = Long.valueOf(str);
                    if (str.equals(val.toString()))
                    {
                        if (val == val.intValue())
                        {
                            return val.intValue();
                        }
                        return val;
                    }
                }
            }
            catch (Exception ignored)
            {
            }
        }
        return str;
    }

    public static void testValidity(Object obj) throws JSONException
    {
        if (obj == null)
        {
            return;
        }
        if (obj instanceof Double)
        {
            if (((Double) obj).isInfinite() || ((Double) obj).isNaN())
            {
                throw new JSONException("JSON does not allow infinite numbers.");
            }
        }
        else if (obj instanceof Float)
        {
            if (((Float) obj).isInfinite() || ((Float) obj).isNaN())
            {
                throw new JSONException("JSON does not allow infinite numbers.");
            }
        }
    }

    public JSONArray toJSONArray(JSONArray names) throws JSONException
    {
        if ((names == null) || (names.length() == 0))
        {
            return null;
        }
        JSONArray array = new JSONArray();
        for (int i = 0; i < names.length(); i += 1)
        {
            array.put(this.opt(names.getString(i)));
        }
        return array;
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

    public static String valueToString(Object val) throws JSONException
    {
        if (val == null)
        {
            return "null";
        }
        if (val instanceof JSONString)
        {
            Object obj;
            try
            {
                obj = ((JSONString) val).toJSONString();
            }
            catch (Exception ex)
            {
                throw new JSONException(ex);
            }
            if (obj != null)
            {
                return (String) obj;
            }
            throw new JSONException("Bad value from toJSONString: " + val + ".");
        }
        if (val instanceof Number)
        {
            String numberAsString = numberToString((Number) val);
            try
            {
                return numberAsString;
            }
            catch (NumberFormatException ex)
            {
                return quote(numberAsString);
            }
        }
        if ((val instanceof Boolean) || (val instanceof JSONObject) || (val instanceof JSONArray))
        {
            return val.toString();
        }
        if (val instanceof Map)
        {
            Map<?, ?> map = (Map<?, ?>) val;
            return new JSONObject(map).toString();
        }
        if (val instanceof Collection)
        {
            Collection<?> coll = (Collection<?>) val;
            return new JSONArray(coll).toString();
        }
        if (val.getClass().isArray())
        {
            return new JSONArray(val).toString();
        }
        if (val instanceof Enum<?>)
        {
            return quote(((Enum<?>) val).name());
        }
        return quote(val.toString());
    }

    public static Object wrap(Object obj)
    {
        try
        {
            if (obj == null)
            {
                return NULL;
            }
            if ((obj instanceof JSONObject) || (obj instanceof JSONArray) || NULL.equals(obj) || (obj instanceof JSONString) || (obj instanceof Byte) ||
                (obj instanceof Character) || (obj instanceof Short) || (obj instanceof Integer) || (obj instanceof Long) || (obj instanceof Boolean) ||
                (obj instanceof Float) || (obj instanceof Double) || (obj instanceof String) || (obj instanceof BigInteger) || (obj instanceof BigDecimal) ||
                (obj instanceof Enum))
            {
                return obj;
            }
            if (obj instanceof Collection)
            {
                Collection<?> coll = (Collection<?>) obj;
                return new JSONArray(coll);
            }
            if (obj.getClass().isArray())
            {
                return new JSONArray(obj);
            }
            if (obj instanceof Map)
            {
                Map<?, ?> map = (Map<?, ?>) obj;
                return new JSONObject(map);
            }
            Package objPackage = obj.getClass().getPackage();
            String objPackageName = (objPackage != null) ? objPackage.getName() : "";
            if (objPackageName.startsWith("java.") || objPackageName.startsWith("javax.") || (obj.getClass().getClassLoader() == null))
            {
                return obj.toString();
            }
            return new JSONObject(obj);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    public Writer write(Writer writer) throws JSONException
    {
        return this.write(writer, 0, 0);
    }

    static Writer writeValue(Writer writer, Object val, int indentFactor, int indent) throws JSONException, IOException
    {
        if (val == null)
        {
            writer.write("null");
        }
        else if (val instanceof JSONString)
        {
            Object obj;
            try
            {
                obj = ((JSONString) val).toJSONString();
            }
            catch (Exception e)
            {
                throw new JSONException(e);
            }
            writer.write((obj != null) ? obj.toString() : quote(val.toString()));
        }
        else if (val instanceof Number)
        {
            String numberAsString = numberToString((Number) val);
            try
            {
                writer.write(numberAsString);
            }
            catch (NumberFormatException ex)
            {
                quote(numberAsString, writer);
            }
        }
        else if (val instanceof Boolean)
        {
            writer.write(val.toString());
        }
        else if (val instanceof Enum<?>)
        {
            writer.write(quote(((Enum<?>) val).name()));
        }
        else if (val instanceof JSONObject)
        {
            ((JSONObject) val).write(writer, indentFactor, indent);
        }
        else if (val instanceof JSONArray)
        {
            ((JSONArray) val).write(writer, indentFactor, indent);
        }
        else if (val instanceof Map)
        {
            Map<?, ?> map = (Map<?, ?>) val;
            new JSONObject(map).write(writer, indentFactor, indent);
        }
        else if (val instanceof Collection)
        {
            Collection<?> coll = (Collection<?>) val;
            new JSONArray(coll).write(writer, indentFactor, indent);
        }
        else if (val.getClass().isArray())
        {
            new JSONArray(val).write(writer, indentFactor, indent);
        }
        else
        {
            quote(val.toString(), writer);
        }
        return writer;
    }

    static void indent(Writer writer, int indent) throws IOException
    {
        for (int i = 0; i < indent; i += 1)
        {
            writer.write(' ');
        }
    }

    public Writer write(Writer writer, int indentFactor, int indent) throws JSONException
    {
        try
        {
            boolean commanate = false;
            int length = this.length();
            writer.write('{');
            if (length == 1)
            {
                Entry<String, ?> entry = this.entrySet().iterator().next();
                String key = entry.getKey();
                writer.write(quote(key));
                writer.write(':');
                if (indentFactor > 0)
                {
                    writer.write(' ');
                }
                try
                {
                    writeValue(writer, entry.getValue(), indentFactor, indent);
                }
                catch (Exception ex)
                {
                    throw new JSONException("Unable to write JSONObject value for key: " + key + ".", ex);
                }
            }
            else if (length != 0)
            {
                int newindent = indent + indentFactor;
                for (Entry<String, ?> entry : this.entrySet())
                {
                    if (commanate)
                    {
                        writer.write(',');
                    }
                    if (indentFactor > 0)
                    {
                        writer.write('\n');
                    }
                    indent(writer, newindent);
                    String key = entry.getKey();
                    writer.write(quote(key));
                    writer.write(':');
                    if (indentFactor > 0)
                    {
                        writer.write(' ');
                    }
                    try
                    {
                        writeValue(writer, entry.getValue(), indentFactor, newindent);
                    }
                    catch (Exception ex)
                    {
                        throw new JSONException("Unable to write JSONObject value for key: " + key + ".", ex);
                    }
                    commanate = true;
                }
                if (indentFactor > 0)
                {
                    writer.write('\n');
                }
                indent(writer, indent);
            }
            writer.write('}');
            return writer;
        }
        catch (IOException ex)
        {
            throw new JSONException(ex);
        }
    }

    public Map<String, Object> toMap()
    {
        Map<String, Object> results = new HashMap<>(16);
        for (Entry<String, Object> entry : this.entrySet())
        {
            Object val;
            if ((entry.getValue() == null) || NULL.equals(entry.getValue()))
            {
                val = null;
            }
            else if (entry.getValue() instanceof JSONObject)
            {
                val = ((JSONObject) entry.getValue()).toMap();
            }
            else if (entry.getValue() instanceof JSONArray)
            {
                val = ((JSONArray) entry.getValue()).toList();
            }
            else
            {
                val = entry.getValue();
            }
            results.put(entry.getKey(), val);
        }
        return results;
    }
}
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

package net.jaqobb.incognito.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class IncognitoUtils
{
    private IncognitoUtils()
    {
    }

    public static File getWorkingDirectory()
    {
        return getWorkingDirectory("minecraft");
    }

    public static File getWorkingDirectory(String appName)
    {
        String home = System.getProperty("user.home", ".");
        File workingDir = null;
        switch (getPlatform())
        {
            case WINDOWS:
                String appData = System.getenv("APPDATA");
                if (appData != null)
                {
                    workingDir = new File(appData, "." + appName);
                    break;
                }
                workingDir = new File(home, "." + appName);
                break;
            case MACOS:
                workingDir = new File(home, "Library" + File.separator + "Application Support" + File.separator + appName);
                break;
            case LINUX:
            case SOLARIS:
                workingDir = new File(home, "." + appName);
                break;
            default:
                break;
        }
        return workingDir;
    }

    public static OS getPlatform()
    {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win"))
        {
            return OS.WINDOWS;
        }
        if (os.contains("mac"))
        {
            return OS.MACOS;
        }
        if (os.contains("linux") || os.contains("unix"))
        {
            return OS.LINUX;
        }
        if (os.contains("solaris") || os.contains("sunos"))
        {
            return OS.SOLARIS;
        }
        return OS.UNKNOWN;
    }

    public enum OS
    {
        WINDOWS,
        MACOS,
        LINUX,
        SOLARIS,
        UNKNOWN
    }

    public static String getStackTrace(Throwable throwable)
    {
        if (throwable == null)
        {
            return "Could not obtain stack trace.";
        }
        try (StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter))
        {
            throwable.printStackTrace(printWriter);
            return stringWriter.getBuffer().toString();
        }
        catch (IOException ignored)
        {
            return "Could not obtain stack trace.";
        }
    }
}
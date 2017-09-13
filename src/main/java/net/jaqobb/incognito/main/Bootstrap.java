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

package net.jaqobb.incognito.main;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import java.lang.instrument.Instrumentation;

import net.jaqobb.incognito.IncognitoLauncher;
import net.jaqobb.incognito.utils.IncognitoUtils;
import net.jaqobb.incognito.utils.IncognitoWindowUtils;

public final class Bootstrap
{
    private static final float JAVA_8_VERSION = 52.0F;

    static
    {
        try
        {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception ex)
        {
            IncognitoWindowUtils.showErrorDialog(null, IncognitoUtils.getStackTrace(ex));
            System.exit(- 1);
        }
    }

    private Bootstrap()
    {
    }

    public static void main(String[] args)
    {
        if (Float.parseFloat(System.getProperty("java.class.version")) < JAVA_8_VERSION)
        {
            JOptionPane.showMessageDialog(null, "Error", "Incognito requires Java 8 or above to function!", JOptionPane.ERROR_MESSAGE);
            System.exit(- 1);
            return;
        }
        IncognitoLauncher.launch(args);
    }

    public static void premain(String args, Instrumentation inst)
    {
        if (Float.parseFloat(System.getProperty("java.class.version")) < JAVA_8_VERSION)
        {
            JOptionPane.showMessageDialog(null, "Error", "Incognito requires Java 8 or above to function!", JOptionPane.ERROR_MESSAGE);
            System.exit(- 1);
            return;
        }
        IncognitoLauncher.launchAgent(args, inst);
    }
}
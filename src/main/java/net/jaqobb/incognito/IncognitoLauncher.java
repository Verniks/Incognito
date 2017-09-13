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

package net.jaqobb.incognito;

import javax.swing.JOptionPane;

import java.lang.instrument.Instrumentation;

public final class IncognitoLauncher
{
    private IncognitoLauncher()
    {
    }

    public static void launch(String[] args)
    {
        //TODO check for previous configuration
        int option = JOptionPane.showConfirmDialog(null, "Do you want to reconfigure Incognito?" + "\n" + "If no, options from previous run will be chosen.",
                                                   "Setup", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        switch (option)
        {
            case 0:
                //TODO reconfigure incognito
                break;
            case 1:
                //TODO start incognito
                break;
            default:
                System.exit(- 1);
                break;
        }
    }

    public static void launchAgent(String args, Instrumentation inst)
    {
    }
}
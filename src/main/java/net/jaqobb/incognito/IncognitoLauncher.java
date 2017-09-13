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

import java.io.File;
import java.lang.instrument.Instrumentation;

import net.jaqobb.incognito.utils.IncognitoUtils;

public final class IncognitoLauncher
{
    private IncognitoLauncher()
    {
    }

    public static void launch(String[] args)
    {
        File profiles = new File(IncognitoUtils.getWorkingDirectory(), "launcher_profiles.json");
        if (! profiles.exists() || ! profiles.isFile())
        {
            JOptionPane.showMessageDialog(null, "File launcher_profiles.json does not exist or it is not a folder.", "Setup", JOptionPane.ERROR_MESSAGE);
            System.exit(- 1);
            return;
        }
        //TODO check for previous setup
        int option = JOptionPane.showConfirmDialog(null, "Do you want to resetup Incognito?", "Setup", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        switch (option)
        {
            case JOptionPane.YES_OPTION:
                //TODO resetup incognito
                break;
            case JOptionPane.NO_OPTION:
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
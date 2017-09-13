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

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

@SuppressWarnings("DynamicRegexReplaceableByCompiledPattern")
public final class IncognitoWindowUtils
{
    private IncognitoWindowUtils()
    {
    }

    public static void showInfoDialog(Component parent, String title, String message, int messageType)
    {
        JOptionPane.showMessageDialog(parent, message, title, messageType);
    }

    public static void showErrorDialog(Component parent, String stackTrace)
    {
        stackTrace = stackTrace.replace("\t", "  ");
        JTextArea textArea = new JTextArea(stackTrace);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", textArea.getFont().getStyle(), textArea.getFont().getSize()));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(null, scrollPane, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
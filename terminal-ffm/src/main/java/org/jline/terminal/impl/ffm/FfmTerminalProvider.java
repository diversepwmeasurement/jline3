/*
 * Copyright (c) 2022-2023, the original author(s).
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package org.jline.terminal.impl.ffm;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.PosixPtyTerminal;
import org.jline.terminal.impl.PosixSysTerminal;
import org.jline.terminal.spi.Pty;
import org.jline.terminal.spi.SystemStream;
import org.jline.terminal.spi.TerminalProvider;
import org.jline.utils.OSUtils;

public class FfmTerminalProvider implements TerminalProvider {

    public FfmTerminalProvider() {
        if (!FfmTerminalProvider.class.getModule().isNativeAccessEnabled()) {
            throw new UnsupportedOperationException(
                    "Native access is not enabled for the current module: " + FfmTerminalProvider.class.getModule());
        }
    }

    @Override
    public String name() {
        return "ffm";
    }

    @Override
    public Terminal sysTerminal(
            String name,
            String type,
            boolean ansiPassThrough,
            Charset encoding,
            boolean nativeSignals,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            SystemStream systemStream)
            throws IOException {
        if (OSUtils.IS_WINDOWS) {
            return NativeWinSysTerminal.createTerminal(
                    this, systemStream, name, type, ansiPassThrough, encoding, nativeSignals, signalHandler, paused);
        } else {
            Pty pty = new FfmNativePty(
                    this,
                    systemStream,
                    -1,
                    null,
                    0,
                    FileDescriptor.in,
                    systemStream == SystemStream.Output ? 1 : 2,
                    systemStream == SystemStream.Output ? FileDescriptor.out : FileDescriptor.err,
                    CLibrary.ttyName(0));
            return new PosixSysTerminal(name, type, pty, encoding, nativeSignals, signalHandler);
        }
    }

    @Override
    public Terminal newTerminal(
            String name,
            String type,
            InputStream in,
            OutputStream out,
            Charset encoding,
            Terminal.SignalHandler signalHandler,
            boolean paused,
            Attributes attributes,
            Size size)
            throws IOException {
        Pty pty = CLibrary.openpty(this, attributes, size);
        return new PosixPtyTerminal(name, type, pty, in, out, encoding, signalHandler, paused);
    }

    @Override
    public boolean isSystemStream(SystemStream stream) {
        if (OSUtils.IS_WINDOWS) {
            return isWindowsSystemStream(stream);
        } else {
            return isPosixSystemStream(stream);
        }
    }

    public boolean isWindowsSystemStream(SystemStream stream) {
        return NativeWinSysTerminal.isWindowsSystemStream(stream);
    }

    public boolean isPosixSystemStream(SystemStream stream) {
        return FfmNativePty.isPosixSystemStream(stream);
    }

    @Override
    public String systemStreamName(SystemStream stream) {
        return FfmNativePty.posixSystemStreamName(stream);
    }

    @Override
    public int systemStreamWidth(SystemStream stream) {
        return FfmNativePty.systemStreamWidth(stream);
    }
}

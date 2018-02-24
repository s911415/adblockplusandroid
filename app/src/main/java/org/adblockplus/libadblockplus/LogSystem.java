/*
 * This file is part of Adblock Plus <https://adblockplus.org/>,
 * Copyright (C) 2006-present eyeo GmbH
 *
 * Adblock Plus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * Adblock Plus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Adblock Plus.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.adblockplus.libadblockplus;

public abstract class LogSystem implements Disposable {
    static {
        System.loadLibrary("adblockplus-jni");
        registerNatives();
    }

    protected final long ptr;
    private final Disposer disposer;

    public LogSystem() {
        this.ptr = ctor(this);
        this.disposer = new Disposer(this, new DisposeWrapper(this.ptr));
    }

    private final static native void registerNatives();

    private final static native long ctor(Object callbackObject);

    private final static native void dtor(long ptr);

    public abstract void logCallback(LogLevel level, String message, String source);

    @Override
    public void dispose() {
        this.disposer.dispose();
    }

    public static enum LogLevel {
        TRACE, LOG, INFO, WARN, ERROR;
    }

    private final static class DisposeWrapper implements Disposable {
        private final long ptr;

        public DisposeWrapper(final long ptr) {
            this.ptr = ptr;
        }

        @Override
        public void dispose() {
            dtor(this.ptr);
        }
    }
}

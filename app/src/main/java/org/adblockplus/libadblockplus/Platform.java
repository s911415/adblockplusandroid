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

public class Platform implements Disposable {
    static {
        System.loadLibrary("adblockplus-jni");
        registerNatives();
    }

    protected final long ptr;
    private final Disposer disposer;

    /**
     * If an interface parameter value is null then a default implementation is
     * chosen.
     * If basePath is null then paths are not resolved to a full path, thus
     * current working directory is used.
     */
    public Platform(final LogSystem logSystem, final WebRequest webRequest, final String basePath) {
        this(ctor(logSystem, webRequest, basePath));
    }

    protected Platform(final long ptr) {
        this.ptr = ptr;
        this.disposer = new Disposer(this, new DisposeWrapper(ptr));
    }

    private final static native void registerNatives();

    private final static native long ctor(LogSystem logSystem, WebRequest webRequest, String basePath);

    private final static native void setUpJsEngine(long ptr, AppInfo appInfo, long v8IsolateProviderPtr);

    private final static native long getJsEnginePtr(long ptr);

    private final static native void setUpFilterEngine(long ptr, IsAllowedConnectionCallback isSubscriptionDownloadAllowedCallback);

    private final static native void ensureFilterEngine(long ptr);

    private final static native void dtor(long ptr);

    public void setUpJsEngine(final AppInfo appInfo, final long v8IsolateProviderPtr) {
        setUpJsEngine(this.ptr, appInfo, v8IsolateProviderPtr);
    }

    public void setUpJsEngine(final AppInfo appInfo) {
        setUpJsEngine(appInfo, 0L);
    }

    public JsEngine getJsEngine() {
        return new JsEngine(getJsEnginePtr(this.ptr));
    }

    public void setUpFilterEngine(final IsAllowedConnectionCallback isSubscriptionDownloadAllowedCallback) {
        setUpFilterEngine(this.ptr, isSubscriptionDownloadAllowedCallback);
    }

    public FilterEngine getFilterEngine() {
        // Initially FilterEngine is not constructed when Platform is instantiated
        // and in addition FilterEngine is being created asynchronously, the call
        // of `ensureFilterEngine` causes a construction of FilterEngine if it's
        // not created yet and waits for it.
        ensureFilterEngine(this.ptr);
        return new FilterEngine(this.ptr);
    }

    @Override
    public void dispose() {
        this.disposer.dispose();
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

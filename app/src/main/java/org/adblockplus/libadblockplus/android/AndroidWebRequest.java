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

package org.adblockplus.libadblockplus.android;

import android.util.Log;
import org.adblockplus.libadblockplus.*;
import org.adblockplus.libadblockplus.ServerResponse.NsStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class AndroidWebRequest implements WebRequest {
    public final static String TAG = Utils.getTag(WebRequest.class);
    protected static final String ENCODING_GZIP = "gzip";
    protected static final String ENCODING_IDENTITY = "identity";
    private final HashSet<String> subscriptionURLs = new HashSet<String>();
    private final boolean elemhideEnabled;
    private final boolean compressedStream;

    /**
     * Ctor
     *
     * @param enableElemhide   Enable element hiding?
     *                         Element hiding requires significantly more memory
     *                         but allows better ad blocking
     * @param compressedStream Request for gzip compressed stream from the server
     */
    public AndroidWebRequest(boolean enableElemhide, boolean compressedStream) {
        this.elemhideEnabled = enableElemhide;
        this.compressedStream = compressedStream;
    }

    public AndroidWebRequest() {
        this(false, true);
    }

    private boolean isListedSubscriptionUrl(final URL url) {
        String toCheck = url.toString();

        final int idx = toCheck.indexOf('?');
        if (idx != -1) {
            toCheck = toCheck.substring(0, idx);
        }

        return this.subscriptionURLs.contains(toCheck);
    }

    protected void updateSubscriptionURLs(final FilterEngine engine) {
        for (final org.adblockplus.libadblockplus.Subscription s : engine.fetchAvailableSubscriptions()) {
            try {
                JsValue jsUrl = s.getProperty("url");
                try {
                    this.subscriptionURLs.add(jsUrl.toString());
                } finally {
                    jsUrl.dispose();
                }
            } finally {
                s.dispose();
            }
        }
        JsValue jsPref = engine.getPref("subscriptions_exceptionsurl");
        try {
            this.subscriptionURLs.add(jsPref.toString());
        } finally {
            jsPref.dispose();
        }
    }

    @Override
    public ServerResponse httpGET(final String urlStr, final List<HeaderEntry> headers) {
        try {
            final URL url = new URL(urlStr);
            Log.d(TAG, "Downloading from: " + url);

            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept-Encoding",
                    (compressedStream ? ENCODING_GZIP : ENCODING_IDENTITY));
            connection.connect();

            final ServerResponse response = new ServerResponse();
            try {
                response.setResponseStatus(connection.getResponseCode());

                if (response.getResponseStatus() == 200) {
                    final InputStream inputStream =
                            (compressedStream && ENCODING_GZIP.equals(connection.getContentEncoding())
                                    ? new GZIPInputStream(connection.getInputStream())
                                    : connection.getInputStream());
                    final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    final StringBuilder sb = new StringBuilder();

                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            // We're only appending non-element-hiding filters here.
                            //
                            // See:
                            //      https://issues.adblockplus.org/ticket/303
                            //
                            // Follow-up issue for removing this hack:
                            //      https://issues.adblockplus.org/ticket/1541
                            //
                            if (this.elemhideEnabled || !isListedSubscriptionUrl(url) || line.indexOf('#') == -1) {
                                sb.append(line);
                                sb.append('\n');
                            }
                        }
                    } finally {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            // ignored
                        }
                    }

                    response.setStatus(NsStatus.OK);
                    response.setResponse(sb.toString());

                    if (connection.getHeaderFields().size() > 0) {
                        List<HeaderEntry> responseHeaders = new LinkedList<HeaderEntry>();
                        for (Map.Entry<String, List<String>> eachEntry : connection.getHeaderFields().entrySet()) {
                            for (String eachValue : eachEntry.getValue()) {
                                if (eachEntry.getKey() != null && eachValue != null) {
                                    responseHeaders.add(new HeaderEntry(eachEntry.getKey().toLowerCase(), eachValue));
                                }
                            }
                        }
                        response.setReponseHeaders(responseHeaders);
                    }
                } else {
                    response.setStatus(NsStatus.ERROR_FAILURE);
                }
            } finally {
                connection.disconnect();
            }
            Log.d(TAG, "Downloading finished");
            return response;
        } catch (final Throwable t) {
            Log.e(TAG, "WebRequest failed", t);
            throw new AdblockPlusException("WebRequest failed", t);
        }
    }
}

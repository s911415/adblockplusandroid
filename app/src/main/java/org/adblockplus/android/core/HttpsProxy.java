package org.adblockplus.android.core;

import android.content.Context;
import android.util.Log;
import org.adblockplus.android.AdblockPlus;
import org.adblockplus.android.Utils;
import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import javax.net.ssl.KeyManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class HttpsProxy extends NanoHTTPD {
    private static final String TAG = Utils.getTag(HttpsProxy.class);
    private final Context c;
    private KeyStore _ks = null;


    /**
     * Constructs an HTTP server on given port.
     *
     * @param port
     */
    public HttpsProxy(final Context context, final String host, final int port) {
        super(host, port);
        this.c = context;

        FileInputStream fis = null;
        try {
            File ksF = new File(c.getExternalFilesDir("cert"), "keystore.bks");
            fis = new FileInputStream(ksF);
            _ks = KeyStore.getInstance("BKS");
            _ks.load(fis, null);
            KeyManagerFactory
                    keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(_ks, null);

            makeSecure(NanoHTTPD.makeSSLSocketFactory(_ks, keyManagerFactory), null);

            start();
        } catch (IOException | KeyStoreException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (session.getUri().equals("/analytics.js")) {
            Response r = Response.newFixedLengthResponse(Status.OK, "application/javascript; charset=UTF-8", AdblockPlus.getApplication().getInjectContent());
            r.addHeader("Strict-Transport-Security", "max-age=31536000");

            return r;
        }
        return super.serve(session);
    }
}

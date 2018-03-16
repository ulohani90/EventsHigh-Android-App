package com.eventshigh.nearme.app.utils;

import android.content.Context;

import com.android.volley.toolbox.Volley;
import com.bumptech.glide.integration.volley.VolleyRequestFactory;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.io.InputStream;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by umesh on 16/03/18.
 */

public class HttpsUrlTrustManager {

    public static void trust(Context context){
        try {
            // Load CAs from an InputStream
// (could be from a resource or ByteArrayInputStream or ...)
            CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
// From https://www.washington.edu/itconnect/security/ca/load-der.crt
            InputStream caInput = context.getAssets().open("ca-bundle.crt");
            Certificate ca;
            try {
                ca = cf.generateCertificate(caInput);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }

// Create a KeyStore containing our trusted CAs
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

// Create a TrustManager that trusts the CAs in our KeyStore
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

// Create an SSLContext that uses our TrustManager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

// Tell the URLConnection to use a SocketFactory from our SSLContext

            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());


        }catch (IOException| KeyStoreException|CertificateException|NoSuchAlgorithmException|KeyManagementException|NoSuchProviderException e){
            e.printStackTrace();
        }
    }
}

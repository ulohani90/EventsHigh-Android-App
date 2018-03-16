package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.Volley;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.squareup.okhttp.OkHttpClient;

import java.io.FileNotFoundException;
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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Volley Helper which provide the simple methods to manage VolleyRequestQueue
 * and submit requests in queue for parallel processing.
 */
public class VolleyHelper {

    private static VolleyHelper instance;

    public static synchronized VolleyHelper getInstance(Context context) {
        if (instance == null) {
            instance = new VolleyHelper(context);
        }
        return instance;
    }

    private final RequestQueue requestQueue;

    private VolleyHelper(Context context) {
        //requestQueue = Volley.newRequestQueue(context.getApplicationContext(),new HurlStack(null, getSocketFactory(context)));
        requestQueue = Volley.newRequestQueue(context.getApplicationContext(),new OkHttpStack(new OkHttpClient()));
    }

    public static RequestQueue getRequestQueue(Context context){
        return getInstance(context).requestQueue;
    }

    public static <T> void addToRequestQueue(Context context, Request<T> req){
        getRequestQueue(context).add(req);
    }

    public static void log(BaseActivity activity, VolleyError volleyError) {
        String logTag = activity.getClass().getSimpleName();
        Throwable cause = volleyError.getCause();
        if (cause != null) {
            Crashlytics.getInstance().core.logException(cause);
            Log.w(logTag, "Volley Error: " + volleyError.getMessage(), cause);
            activity.reportActionToAnalytics("failedRequest", cause.getClass().getSimpleName());
        } else {
            Log.w(logTag, "Volley Error: " + volleyError.getMessage());
            activity.reportActionToAnalytics("failedRequest");
        }
    }

    private SSLSocketFactory getSocketFactory(Context context) {

        CertificateFactory cf = null;
        try {

            cf = CertificateFactory.getInstance("X.509","BC");
            InputStream caInput = context.getAssets().open("ca-bundle.crt");
            Certificate ca;
            try {

                ca = cf.generateCertificate(caInput);
                Log.e("CERT", "ca=" + ((X509Certificate) ca).getSubjectDN());
            } finally {
                caInput.close();
            }


            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);


            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);


           /* HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {

                    Log.e("CipherUsed", session.getCipherSuite());
                    return hostname.compareTo(hostname)==0; //The Hostname of your server.

                }
            };*/


         //   HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
            SSLContext sslContext = null;
            sslContext = SSLContext.getInstance("TLS");

            sslContext.init(null, tmf.getTrustManagers(), null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            SSLSocketFactory sf = sslContext.getSocketFactory();


            return sf;

        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }catch(NoSuchProviderException e){
            e.printStackTrace();
        }

        return  null;
    }

}

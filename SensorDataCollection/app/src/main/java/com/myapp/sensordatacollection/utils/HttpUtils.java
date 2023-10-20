package com.myapp.sensordatacollection.utils;

import android.content.Context;

import com.myapp.sensordatacollection.BaseApplication;
import com.myapp.sensordatacollection.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Handshake;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpUtils {
    private static final String TAG = "HttpUtil";
    public static final int STATUS_OK = 200;
    private static final String secret_key = "dlna6e52cazld5q0z5dqr4dlpf6";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final MediaType CSV = MediaType.parse("text/csv; charset=utf-8");
    private static final MediaType FORM_DATA = MediaType.parse("multipart/form-data");
    private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");

    private OkHttpClient okHttpClient;

    public HttpUtils(Context context){
        customTrust(context);
    }

    public void customTrust(Context context) {
        try {
            InputStream inputStream = context.getResources().openRawResource(R.raw.cert);
//            int length = inputStream.available();
//            byte[] buff = new byte[length];
//            inputStream.read(buff);
//            String cert = new String(buff);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", x509Certificate);

            X509TrustManager x509TrustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null,new TrustManager[]{x509TrustManager}, null);


            okHttpClient = new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), x509TrustManager)
                    .hostnameVerifier(new TrustAnyHostnameVerifier())
                    .build();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private static class TrustAnyHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public Response downloadFile(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = null;

        try {
            response = okHttpClient.newCall(request).execute();
        } catch (SocketTimeoutException e) {
            ToastUtils.show(BaseApplication.getContext(), "连接服务器超时");
        } catch (IOException e) {
            ToastUtils.show(BaseApplication.getContext(), e.toString());
            e.printStackTrace();
        }

        return response;
    }


    public Response postJson(String url, String json) {
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Authorization", "Bearer " +  secret_key)
                .build();
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
        } catch (SocketTimeoutException e) {
           ToastUtils.show(BaseApplication.getContext(), "连接服务器超时");
        } catch (IOException e) {
            ToastUtils.show(BaseApplication.getContext(), e.toString());
        }

        return response;
    }

    public Response postCsvFile(String url, File csvFile, int scene_id){
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file",csvFile.getPath(), RequestBody.create(csvFile,OCTET_STREAM))
                .addFormDataPart("scene_id",String.valueOf(scene_id))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " +  secret_key)
                .addHeader("Content-Type", "multipart/form-data")
                .build();

        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
        } catch (SocketTimeoutException e) {
            ToastUtils.show(BaseApplication.getContext(), "连接服务器超时");
        } catch (IOException e) {
            e.printStackTrace();
            ToastUtils.show(BaseApplication.getContext(), e.toString());
        }

        return response;
    }
}

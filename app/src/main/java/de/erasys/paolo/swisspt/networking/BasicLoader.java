package de.erasys.paolo.swisspt.networking;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by paolo on 24.04.15.
 */
public abstract class BasicLoader implements Runnable {

    private static final String LOG_TAG = BasicLoader.class.getSimpleName();

    protected void preExecute() {

    };

    public void run() {
        InputStream is = null;
        preExecute();

        try {
            URL url = new URL(getUrl());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(LOG_TAG, "GETTING RESPONSE!! The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            handleResult(convertInputStreamToString(is));
        } catch (IOException e) {
            Log.d(LOG_TAG, "Unable to retrieve web page. URL may be invalid.", e);
        } finally {
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    protected abstract void handleResult(String result);

    protected abstract String getUrl() throws UnsupportedEncodingException;

    private static String convertInputStreamToString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        String result = "";
        while ((line = reader.readLine()) != null)  result += line;
        stream.close();
        return result;
    }
}

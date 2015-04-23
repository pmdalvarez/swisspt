package de.erasys.paolo.swisspt.helpers;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by paolo on 23.04.15.
 */
public class HttpRequestHelper {

    private static final String LOG_TAG = HttpRequestHelper.class.getSimpleName();

    public static String getStationboard(String queryString) throws IOException {
        String urlStr =  String.format(
                "http://transport.opendata.ch/v1/stationboard?station=%s",
                URLEncoder.encode(queryString, "UTF-8")
        );
        return getHttpRequestResult(urlStr);
    }

    public static String getLocations(String queryString) throws IOException {
        String urlStr =  String.format(
            "http://transport.opendata.ch/v1/locations?query=%s",
         URLEncoder.encode(queryString, "UTF-8")
        );
        return getHttpRequestResult(urlStr);
    }

    private static String getHttpRequestResult(String urlStr) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(urlStr);
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
            String contentAsString = convertInputStreamToString(is);
            Log.d(LOG_TAG, "GETTING RESPONSE!! The content is: " + contentAsString);

            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private static String convertInputStreamToString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        String result = "";
        while ((line = reader.readLine()) != null)  result += line;
        stream.close();
        return result;
    }
}

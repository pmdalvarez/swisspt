package de.erasys.paolo.swisspt.networking;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import de.erasys.paolo.swisspt.content.ModelFactory;
import de.erasys.paolo.swisspt.content.model.Connection;

/**
 * Created by paolo on 24.04.15.
 */
public class StationboardLoader extends BasicLoader {

    private static final String LOG_TAG = StationboardLoader.class.getSimpleName();

    private StationboardCallbacks mCallbacks;

    private String mQueryStr;

    public StationboardLoader(String queryStr, StationboardCallbacks callbacks) {
        mQueryStr = queryStr;
        mCallbacks = callbacks;
    }

    protected void preExecute() {
        mCallbacks.onConnectionsLoading();
    }

    protected void handleResult(String result) {
        Log.d(LOG_TAG, "API STATIONBOARD RESPONSE is " + result);
        ArrayList<Connection> connections = ModelFactory.getConnectionsFromJsonString(result);
        mCallbacks.onConnectionsLoaded(connections);
    }

    @Override
    protected String getUrl() throws UnsupportedEncodingException {
        return String.format(
            "http://transport.opendata.ch/v1/stationboard?station=%s",
            URLEncoder.encode(mQueryStr, "UTF-8")
        );
    }
}

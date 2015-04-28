package de.erasys.paolo.swisspt.networking;

import android.content.ContentValues;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import de.erasys.paolo.swisspt.content.provider.LocationsTable;

/**
 * Created by paolo on 24.04.15.
 */
public class LocationsLoader extends BasicLoader {

    private static final String LOG_TAG = LocationsLoader.class.getSimpleName();

    private LocationsCallbacks mCallbacks;

    private String mQueryStr;

    public LocationsLoader(String queryStr, LocationsCallbacks callbacks) {
        mQueryStr = queryStr;
        mCallbacks = callbacks;
    }

    protected void handleResult(String result) {
        Log.d(LOG_TAG, "API LOCATIONS RESPONSE is " + result);

        try {
            JSONObject jObject  = new JSONObject(result); // json
            JSONArray stations = jObject.getJSONArray("stations"); // get data object
            for (int i = 0; i < stations.length(); i++) {
                JSONObject station = stations.getJSONObject(i);
                String locationName = station.getString("name");
                Log.d(LOG_TAG, "FOUND LOCATION ! name is " + locationName);
                ContentValues values = new ContentValues();
                values.put(LocationsTable.COLUMN_NAME, locationName);
                mCallbacks.onLocationRetrieved(values);
            }
            mCallbacks.onLocationsLoaded();
        } catch (JSONException e) {
            Log.d(LOG_TAG, "Cannot parse locations query response.", e);
            mCallbacks.onLocationsFailed();
        }
    }

    @Override
    protected String getUrl() throws UnsupportedEncodingException {
        return String.format(
            "http://transport.opendata.ch/v1/locations?query=%s",
            URLEncoder.encode(mQueryStr, "UTF-8")
        );
    }
}

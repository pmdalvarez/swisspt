package de.erasys.paolo.swisspt.content;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.erasys.paolo.swisspt.content.model.Connection;

/**
 * Created by paolo on 23.04.15.
 */
public class ModelFactory {

    private static final String LOG_TAG = ModelFactory.class.getSimpleName();

    public static ArrayList<Connection> getConnectionsFromJsonString(String jsonString) {
        ArrayList<Connection> connections = new ArrayList<>();
        try {
            JSONObject stationboardJsonObj  = new JSONObject(jsonString); // json
            JSONArray stations = stationboardJsonObj.getJSONArray("stationboard"); // get data object
            String originStation = stationboardJsonObj.getJSONObject("station").getString("name");

            // assume adapter is already clear
            for (int i = 0; i < stations.length(); i++) {
                JSONObject connJsonObj = stations.getJSONObject(i);
                JSONObject stopJsonObj = connJsonObj.getJSONObject("stop");
                String departureTime = stopJsonObj.isNull("departure") ? "" : getTimeFromTimestamp(stopJsonObj.getString("departure"));
                String arrivalTime = stopJsonObj.isNull("arrival") ? "" : getTimeFromTimestamp(stopJsonObj.getString("arrival"));
                String destinationStation = connJsonObj.getString("to");
                Connection connection = new Connection(
                        connJsonObj.getString("name"),
                        originStation,
                        departureTime,
                        destinationStation,
                        arrivalTime
                );
                connections.add(connection);
                Log.d(LOG_TAG, "FOUND CONNECTION ! name is " + connection.name + " at " + connection.departure);
            }
        } catch (JSONException e) {
            // fail silently and return what we have managed to parse so far
        }
        return connections;
    }

    private static String getTimeFromTimestamp(String datetime) {
        try {
            Date dateObj  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(datetime);
            return new SimpleDateFormat("HH:mm").format(dateObj);
        } catch (ParseException e) {
            // don't format the date - return empty string
            Log.d(LOG_TAG, "setViewValue PARSE ERROR datetime string  = " + datetime + " " + e.getMessage() + " " + e.getStackTrace());
            return "";
        }
    }

}

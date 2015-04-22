package de.erasys.paolo.swisspt;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import de.erasys.paolo.swisspt.content.model.Connection;
import de.erasys.paolo.swisspt.content.provider.LocationsContentProvider;
import de.erasys.paolo.swisspt.content.provider.LocationsTable;


public class MainActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private class LoadConnectionsTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            // params comes from the execute() call: params[0] is the queryString.
            try {
                String result = getConnections(params[0]);
                return result;
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject resultJsonObj  = new JSONObject(result); // json
                JSONArray stations = resultJsonObj.getJSONArray("stationboard"); // get data object
                String originStation = resultJsonObj.getJSONObject("station").getString("name");

                // assume adapter is already clear
                for (int i = 0; i < stations.length(); i++) {
                    JSONObject connJsonObj = stations.getJSONObject(i);
                    JSONObject stopJsonObj = connJsonObj.getJSONObject("stop");
                    String departureTime = getTimeFromTimestamp(stopJsonObj.getString("departure"));
                    String arrivalTime = getTimeFromTimestamp(stopJsonObj.getString("arrival"));
                    String destinationStation = connJsonObj.getString("to");
                    Connection connection = new Connection(
                        connJsonObj.getString("name"),
                        originStation,
                        departureTime,
                        destinationStation,
                        arrivalTime
                    );
                    Log.d(LOG_TAG, "FOUND CONNECTION ! name is " + connection.name + " at " + connection.departure);
                    mConnectionsAdapter.add(connection);
                }
                // hide loadingView + show listView
                TextView loadingView = (TextView) findViewById(R.id.loading);
                loadingView.setVisibility(View.GONE);
                ListView stationboard = (ListView) findViewById(R.id.stationboard);
                stationboard.setVisibility(View.VISIBLE);

                // call adapter notify data set changed method
                mConnectionsAdapter.notifyDataSetChanged();
            } catch (JSONException e) {
               // fail silently
            }
        }

        private String getTimeFromTimestamp(String datetime) {
            try {
                Date dateObj  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(datetime);
                return new SimpleDateFormat("HH:mm").format(dateObj);
            } catch (ParseException e) {
                // don't format the date
                Log.d(this.getClass().getName(), "setViewValue PARSE ERROR datetime string  = " + datetime + " " + e.getMessage() + " " + e.getStackTrace());
            }
            return "";
        }
    }

    private class LoadLocationsTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {

            // params comes from the execute() call: params[0] is the queryString.
            try {
                String result = getLocations(params[0]);
                JSONObject jObject  = new JSONObject(result); // json
                JSONArray stations = jObject.getJSONArray("stations"); // get data object
                for (int i = 0; i < stations.length(); i++) {
                    JSONObject station = stations.getJSONObject(i);
                    String locationName = station.getString("name");
                    Log.d(LOG_TAG, "FOUND LOCATION ! name is " + locationName);
                    ContentValues values = new ContentValues();
                    values.put(LocationsTable.COLUMN_NAME, locationName);
                    getContentResolver().insert(LocationsContentProvider.CONTENT_URI, values);
                }
                return result;
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
            } catch (JSONException e) {
                return "Cannot parse locations query response.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            mLocationsAdapter.notifyDataSetChanged(); // correct?
        }
    }

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // this is the Adapter being used to display the location suggestions of the autocompletetextview
    LocationsAdapter mLocationsAdapter = null;

    // this is the Adapter being used to display the stationboard
    ConnectionsAdapter mConnectionsAdapter = null;

    private String getConnections(String queryString) throws IOException {
        InputStream is = null;

        try {
            String urlStr =  String.format(
                "http://transport.opendata.ch/v1/stationboard?station=%s",
                URLEncoder.encode(queryString, "UTF-8")
            );
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(LOG_TAG, "GETTING CONNECTIONS!! The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = convertInputStreamToString(is);
            Log.d(LOG_TAG, "GETTING CONNECTIONS!! The content is: " + contentAsString);

            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private String getLocations(String queryString) throws IOException {
        InputStream is = null;

        try {
            String urlStr =  String.format(
                "http://transport.opendata.ch/v1/locations?query=%s",
                URLEncoder.encode(queryString, "UTF-8")
            );
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(LOG_TAG, "GETTING LOCATIONS!! The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = convertInputStreamToString(is);
            Log.d(LOG_TAG, "GETTING LOCATIONS!! The content is: " + contentAsString);

            return contentAsString;

            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private String convertInputStreamToString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String line;
        String result = "";
        while ((line = reader.readLine()) != null)  result += line;
        stream.close();
        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupAutoCompleteTextView();
        setupStationboard();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public Loader onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                LocationsTable.COLUMN_ID,
                LocationsTable.COLUMN_NAME };
        // TODO - if string is empty then return empty cursor without running query
        CursorLoader cursorLoader = new CursorLoader(this, LocationsContentProvider.CONTENT_URI, projection, LocationsTable.COLUMN_ID + " = 0 ", null, LocationsTable.COLUMN_NAME);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mLocationsAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mLocationsAdapter.swapCursor(null);
    }

    private void setupAutoCompleteTextView() {
Log.d(LOG_TAG, "fillData");
        getSupportLoaderManager().initLoader(0, null, this);
        mLocationsAdapter = new LocationsAdapter(this, null, LocationsAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        final AutoCompleteTextView locationSearchView = (AutoCompleteTextView) findViewById(R.id.autoCompleteLocationSearch);
        if (locationSearchView != null)  {
            locationSearchView.setAdapter(mLocationsAdapter);

            locationSearchView.addTextChangedListener(new TextWatcher() {
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // to make more efficient make query only if string length = 3
                    if (s != null && s.length() == 3) {
                        Log.d(LOG_TAG, "TEXT CHANGED!!! Querying swiss PT API");
                        final StringBuilder sb = new StringBuilder();
                        sb.append(s);
                        final String[] params = {sb.toString()};
                        LoadLocationsTask task = new LoadLocationsTask();
                        task.execute(params);
                    }
                }
                public void beforeTextChanged(CharSequence s, int start, int count,
                                              int after) {
                    // do nothing
                }
                public void afterTextChanged(Editable s) {
                    // do nothing
                }
            });
            // add listener for when user chooses a location
            locationSearchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View itemView, int pos,
                                        long id) {
                    // 1/3: clear list
                    mConnectionsAdapter.clearData();
                    // 2/3: show loading textView + hide listview
                    TextView loadingView = (TextView) findViewById(R.id.loading);
                    loadingView.setVisibility(View.VISIBLE);
                    ListView stationboard = (ListView) findViewById(R.id.stationboard);
                    stationboard.setVisibility(View.GONE);

                    // 3/3: load connections
                    TextView textView = (TextView) itemView.findViewById(R.id.autoCompleteItemTextView);
                    final String[] params = {(String)textView.getText()};
                    LoadConnectionsTask task = new LoadConnectionsTask();
                    task.execute(params);
                }
            });
        }
    }

    private void setupStationboard() {
        final ListView listView = (ListView) findViewById(R.id.stationboard);
        mConnectionsAdapter = new ConnectionsAdapter(this, new ArrayList<Connection>());
        listView.setAdapter(mConnectionsAdapter);
    }

}

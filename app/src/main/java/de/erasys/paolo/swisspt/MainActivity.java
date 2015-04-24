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
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import de.erasys.paolo.swisspt.adapters.ConnectionsAdapter;
import de.erasys.paolo.swisspt.adapters.LocationsAdapter;
import de.erasys.paolo.swisspt.content.ModelFactory;
import de.erasys.paolo.swisspt.content.model.Connection;
import de.erasys.paolo.swisspt.content.provider.LocationsContentProvider;
import de.erasys.paolo.swisspt.content.provider.LocationsTable;
import de.erasys.paolo.swisspt.helpers.HttpRequestHelper;


public class MainActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private class LocationsLoader extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            // params comes from the execute() call: params[0] is the queryString.
            try {
                Log.d(LOG_TAG, "SEARCHING FOR LOCATIONS!!");
                String result = HttpRequestHelper.getLocations(params[0]);
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
            Log.d(LOG_TAG, "LOCATIONS LOADER: notifying mLocationsAdapter data set changed");
            mLocationsAdapter.notifyDataSetChanged();
            AutoCompleteTextView locationSearch = (AutoCompleteTextView) findViewById(R.id.autoCompleteLocationSearch);
            locationSearch.showDropDown();
        }
    }

    private class StationboardLoader extends AsyncTask<String, Void, ArrayList<Connection>> {

        @Override
        protected void onPreExecute() {
            // hide list and show loading view
            ProgressBar loadingView = (ProgressBar) findViewById(R.id.loading);
            loadingView.setVisibility(View.VISIBLE);
            ListView stationboard = (ListView) findViewById(R.id.stationboard);
            stationboard.setVisibility(View.GONE);
        }

        @Override
        protected ArrayList<Connection> doInBackground(String... params) {

            // params comes from the execute() call: params[0] is the queryString.
            try {
                String result = HttpRequestHelper.getStationboard(params[0]);
                // parsing on background thread rather than UI thread as to not overburden it
                ArrayList<Connection> connections = ModelFactory.getConnectionsFromJsonString(result);

                return connections;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Connection> connections) {
            if (connections == null) return;

            // update contents of adapter with new list of connections
            mConnectionsAdapter.setValues(connections);
            mConnectionsAdapter.notifyDataSetChanged();

            // hide loadingView + show listView
            ProgressBar loadingView = (ProgressBar) findViewById(R.id.loading);
            loadingView.setVisibility(View.GONE);
            ListView stationboard = (ListView) findViewById(R.id.stationboard);
            stationboard.setVisibility(View.VISIBLE);
        }

    }

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // this is the Adapter being used to display the location suggestions of the autocompletetextview
    private LocationsAdapter mLocationsAdapter = null;

    // this is the Adapter being used to display the stationboard
    private ConnectionsAdapter mConnectionsAdapter = null;

    private Timer mStationboardReloader = null;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupAutoCompleteTextView();
        setupStationboard();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mStationboardReloader != null)  {
            Log.d(LOG_TAG, "Application on pause!! cancelling StationboardReloader");
            mStationboardReloader.cancel();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mStationboardReloader != null)  {
            Log.d(LOG_TAG, "Application resumed and cancelled mStationboardReloader exists!! restarting StationboardReloader");
            startStationboardReloader();
        }
    }

    private void setupStationboard() {
        final ListView listView = (ListView) findViewById(R.id.stationboard);
        mConnectionsAdapter = new ConnectionsAdapter(this, new ArrayList<Connection>());
        listView.setAdapter(mConnectionsAdapter);
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
                    // 1/3: Query Locations. To make more efficient make query only if string length = 3
                    if (s != null && s.length() == 3) {
                        Log.d(LOG_TAG, "TEXT CHANGED!!! Querying swiss PT API");
                        final StringBuilder sb = new StringBuilder();
                        sb.append(s);
                        final String[] params = {sb.toString()};
                        LocationsLoader task = new LocationsLoader();
                        task.execute(params);
                    }

                    // 2/3: If there is a StationboardReloader instance
                    // -  cancel, to stop reloading
                    // -  make null, to indicate that we don't wanna restart it onResume
                    if (mStationboardReloader != null) {
                        mStationboardReloader.cancel();
                        mStationboardReloader = null;
                    }

                    // 3/3: Clear stationboard table if not already empty
                    if (!mConnectionsAdapter.isEmpty()) {
                        mConnectionsAdapter.clear();
                        mConnectionsAdapter.notifyDataSetChanged();
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
                public void onItemClick(AdapterView<?> parent, final View itemView, int pos,
                                        long id) {

                    // cancel existing timer if exists
                    if (mStationboardReloader != null)  {
                        Log.d(LOG_TAG, "ITEM CHOSEN!!! stopping current StationboardReloader");
                        mStationboardReloader.cancel();
                    }

                    startStationboardReloader();
                }
            });
        }
    }

    private void startStationboardReloader() {
        // now create a new timer  and assign to member variable mTimer
        // so that it can be cancelled next time item is selected
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                // asynctasks are meant to be run only on ui thread and hence need runOnUiThread method
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView) findViewById(R.id.autoCompleteLocationSearch);
                        final String[] params = {textView.getText().toString()};
                        StationboardLoader task = new StationboardLoader();
                        task.execute(params);
                    }
                });
            }
        };
        mStationboardReloader = new Timer();
        mStationboardReloader.scheduleAtFixedRate(timerTask, 0, 15 * 1000);
    }

}

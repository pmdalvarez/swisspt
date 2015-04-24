package de.erasys.paolo.swisspt;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Handler;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.erasys.paolo.swisspt.adapters.ConnectionsAdapter;
import de.erasys.paolo.swisspt.adapters.LocationsAdapter;
import de.erasys.paolo.swisspt.content.ModelFactory;
import de.erasys.paolo.swisspt.content.model.Connection;
import de.erasys.paolo.swisspt.content.provider.LocationsContentProvider;
import de.erasys.paolo.swisspt.content.provider.LocationsTable;
import de.erasys.paolo.swisspt.helpers.HttpRequestHelper;


public class MainActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private class LocationsLoader implements Runnable {

        private String mQueryStr;

        public LocationsLoader(String queryStr) {
            this.mQueryStr = queryStr;
        }

        public void run() {
            try {
                Log.d(LOG_TAG, "SEARCHING FOR LOCATIONS!!");
                String result = HttpRequestHelper.getLocations(mQueryStr);
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

                // go to ui thread to tell adapter we have new data
                runOnUiThread(new Runnable() {
                    public void run() {mLocationsAdapter.notifyDataSetChanged();}
                });
            } catch (IOException e) {
                Log.d(LOG_TAG, "Unable to retrieve web page. URL may be invalid.");
            } catch (JSONException e) {
                Log.d(LOG_TAG, "Cannot parse locations query response.");
            }
        }
    }

    private class StationboardLoader implements Runnable {

        private String mQueryStr;

        public StationboardLoader(String queryStr) {
            this.mQueryStr = queryStr;
        }

        public void run() {
            runOnUiThread(new Runnable() {
                public void run() {setStationboardVisibility(false);}
            });

            final ArrayList<Connection> connections;
            try {
                String result = HttpRequestHelper.getStationboard(mQueryStr);
                connections = ModelFactory.getConnectionsFromJsonString(result);
            } catch (IOException e) {
                Log.d(LOG_TAG, "IOException trying to get connections from stationboard http call");
                runOnUiThread(new Runnable() {
                    public void run() {setStationboardVisibility(true);}
                });
                return;
            }

            runOnUiThread(new Runnable() {
                public void run() {
                    // update contents of adapter with new list of connections
                    mConnectionsAdapter.setValues(connections);
                    mConnectionsAdapter.notifyDataSetChanged();
                    setStationboardVisibility(true);
                }
            });
        }

        private void setStationboardVisibility(boolean visible) {
            // hide loadingView + show listView
            ProgressBar spinner = (ProgressBar) findViewById(R.id.loading);
            ListView stationboard = (ListView) findViewById(R.id.stationboard);
            if (visible) {
                spinner.setVisibility(View.GONE);
                stationboard.setVisibility(View.VISIBLE);
            } else {
                spinner.setVisibility(View.VISIBLE);
                stationboard.setVisibility(View.GONE);
            }
        }

    }

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final int RELOAD_TIME_SECS = 15;

    // this is the Adapter being used to display the location suggestions of the autocompletetextview
    private LocationsAdapter mLocationsAdapter = null;

    // this is the Adapter being used to display the stationboard
    private ConnectionsAdapter mConnectionsAdapter = null;

    private ExecutorService mExecutorService = null;

    private final Handler mHandler = new Handler();

    private Runnable mReloader = null;

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
        if (mExecutorService == null) {
            // have several parallel threads so that locations can be searched while stationboard is searched at same time
            mExecutorService = Executors.newFixedThreadPool(10);
        }
        setupAutoCompleteTextView();
        setupStationboard();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (mReloader != null)  {
            Log.d(LOG_TAG, "Application on pause!! cancelling mReloader");
            mHandler.removeCallbacks(mReloader);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mReloader != null)  {
            Log.d(LOG_TAG, "Application resumed and cancelled mReloader exists!! restarting reloader");
            mHandler.postDelayed(mReloader, RELOAD_TIME_SECS * 1000);
        }
    }

    @Override
    protected void onDestroy() {
        if (mExecutorService != null) mExecutorService.shutdown();
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
                    // 1/3: Query Locations. To make more efficient make query only if string length = 2
                    if (s != null && s.length() == 3) {
                        Log.d(LOG_TAG, "TEXT CHANGED!!! Querying swiss PT API");
                        final StringBuilder sb = new StringBuilder();
                        sb.append(s);
                        mExecutorService.execute(new LocationsLoader(sb.toString()));
                    }

                    // 2/3: If there is a Stationboard Reloader instance
                    // -  cancel, to stop reloading
                    // -  make null, to indicate that we don't wanna restart it onResume
                    if (mReloader != null) {
                        mHandler.removeCallbacks(mReloader);
                        mReloader = null;
                    }

                    // 3/3: Clear stationboard table if not already empty
                    if (!mConnectionsAdapter.isEmpty()) {
                        mConnectionsAdapter.clear();
                        mConnectionsAdapter.notifyDataSetChanged();
                    }

                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // do nothing
                }
                public void afterTextChanged(Editable s) {
                    // do nothing
                }
            });
            // Add listener for when user chooses a location
            locationSearchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, final View itemView, int pos,
                                        long id) {

                    // cancel existing reloader if exists
                    if (mReloader != null)  {
                        Log.d(LOG_TAG, "ITEM CHOSEN!!! stopping current StationboardReloader");
                        mHandler.removeCallbacks(mReloader);
                    }

                    startStationboardReloader();
                }
            });
        }
    }

    private void startStationboardReloader() {
        TextView textView = (TextView) findViewById(R.id.autoCompleteLocationSearch);
        final String stationName = textView.getText().toString();
        mReloader = new Runnable() {
            public void run() {
                mExecutorService.execute(new StationboardLoader(stationName));
                mHandler.postDelayed(mReloader, RELOAD_TIME_SECS * 1000);
            }
        };
        mHandler.post(mReloader);
    }

}

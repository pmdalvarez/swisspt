package de.erasys.paolo.swisspt;

import android.content.ContentValues;
import android.database.Cursor;
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

import java.util.ArrayList;

import de.erasys.paolo.swisspt.adapters.ConnectionsAdapter;
import de.erasys.paolo.swisspt.adapters.LocationsAdapter;
import de.erasys.paolo.swisspt.content.model.Connection;
import de.erasys.paolo.swisspt.content.provider.LocationsContentProvider;
import de.erasys.paolo.swisspt.content.provider.LocationsTable;
import de.erasys.paolo.swisspt.networking.CommonQueueExecutor;
import de.erasys.paolo.swisspt.networking.LocationsCallbacks;
import de.erasys.paolo.swisspt.networking.LocationsLoader;
import de.erasys.paolo.swisspt.networking.ScheduledTaskExecutor;
import de.erasys.paolo.swisspt.networking.StationboardCallbacks;
import de.erasys.paolo.swisspt.networking.StationboardLoader;


public class MainActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    class MyLocationsCallbacks implements LocationsCallbacks {

        @Override
        public void onLocationsLoaded() {
            runOnUiThread(new Runnable() {
                public void run() {
                    mLocationsAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onLocationsFailed() {
        }

        @Override
        public void onLocationRetrieved(ContentValues values) {
            getContentResolver().insert(LocationsContentProvider.CONTENT_URI, values);
        }
    }

    class MyStationboardCallbacks implements StationboardCallbacks {

        @Override
        public void onConnectionsLoading() {
            runOnUiThread(new Runnable() {
                public void run() {
                    setStationboardVisibility(false);
                }
            });
        }

        @Override
        public void onConnectionsLoaded(final ArrayList<Connection> connections) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mLocationsAdapter.notifyDataSetChanged();
                    mConnectionsAdapter.setValues(connections);
                    mConnectionsAdapter.notifyDataSetChanged();
                    setStationboardVisibility(true);
                }
            });
        }

    }

    private static final int RELOAD_TIME_SECS = 15;

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // this is the Adapter being used to display the location suggestions of the autocompletetextview
    private LocationsAdapter mLocationsAdapter = null;

    // this is the Adapter being used to display the stationboard
    private ConnectionsAdapter mConnectionsAdapter = null;

    private StationboardLoader mStationboardLoader = null;

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
        if (mStationboardLoader != null)  {
            Log.d(LOG_TAG, "Application on pause!! cancelling mReloader");
            ScheduledTaskExecutor.getInstance().removeTask(mStationboardLoader);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mStationboardLoader != null)  {
            Log.d(LOG_TAG, "Application resumed and cancelled mReloader exists!! restarting reloader");
            ScheduledTaskExecutor.getInstance().addTask(mStationboardLoader, RELOAD_TIME_SECS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CommonQueueExecutor.getInstance().shutdown();
        mStationboardLoader = null;
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
                        LocationsLoader locationsLoader = new LocationsLoader(sb.toString(), new MyLocationsCallbacks());
                        CommonQueueExecutor.getInstance().addRequest(locationsLoader);
                    }

                    // 2/3: If there is a Stationboard Reloader instance
                    // -  cancel, to stop reloading
                    // -  make null, to indicate that we don't wanna restart it onResume
                    if (mStationboardLoader != null) {
                        ScheduledTaskExecutor.getInstance().removeTask(mStationboardLoader);
                        mStationboardLoader = null;
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
                    if (mStationboardLoader != null)  {
                        Log.d(LOG_TAG, "ITEM CHOSEN!!! stopping current StationboardReloader");
                        ScheduledTaskExecutor.getInstance().removeTask(mStationboardLoader);
                    }

                    // reinitialise StationboardReloader
                    TextView textView = (TextView) findViewById(R.id.autoCompleteLocationSearch);
                    final String stationName = textView.getText().toString();
                    StationboardLoader stationboardLoader = new StationboardLoader(stationName, new MyStationboardCallbacks());
                    ScheduledTaskExecutor.getInstance().addTask(stationboardLoader, RELOAD_TIME_SECS);
                }
            });
        }
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

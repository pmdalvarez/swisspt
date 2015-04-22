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
import android.widget.TextView;
import android.widget.Toast;

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

import de.erasys.paolo.swisspt.content.LocationsContentProvider;
import de.erasys.paolo.swisspt.content.LocationsTable;


public class MainActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private class LocationQueryTask extends AsyncTask<String, Void, String> {
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
        }
    }

    // this is the Adapter being used to display the chat history data
    LocationsAdapter mAdapter = null;

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

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

            // TODO - parse string to list of stations
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
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private void setupAutoCompleteTextView() {
Log.d(LOG_TAG, "fillData");
        getSupportLoaderManager().initLoader(0, null, this);
        mAdapter = new LocationsAdapter(this, null, LocationsAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        final AutoCompleteTextView locationSearchView = (AutoCompleteTextView) findViewById(R.id.autoCompleteLocationSearch);
        if (locationSearchView != null)  {
            locationSearchView.setAdapter(mAdapter);

            locationSearchView.addTextChangedListener(new TextWatcher() {
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    // to make more efficient make query only if string length = 2
                    if (s != null && s.length() == 1) {
                        Log.d(LOG_TAG, "TEXT CHANGED!!! Querying swiss PT API");
                        final StringBuilder sb = new StringBuilder();
                        sb.append(s);
                        final String[] params = {sb.toString()};
                        LocationQueryTask task = new LocationQueryTask();
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
                    TextView textView = (TextView) itemView.findViewById(R.id.autoCompleteItemTextView);
                    String text = (String)textView.getText();
                    Toast.makeText(getApplicationContext(), "Selected text " + text, Toast.LENGTH_LONG).show();
                }
            });
        }
    }



}

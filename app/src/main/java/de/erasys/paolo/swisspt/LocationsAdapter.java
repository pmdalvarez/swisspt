package de.erasys.paolo.swisspt;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FilterQueryProvider;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.Arrays;

import de.erasys.paolo.swisspt.content.LocationsContentProvider;
import de.erasys.paolo.swisspt.content.LocationsTable;

/**
 * Created by paolo on 17.04.15.
 */
public class LocationsAdapter  extends CursorAdapter implements Filterable {

    private static class ViewHolder {
        TextView itemView;
    }

    private static final String LOG_TAG = LocationsAdapter.class.getSimpleName();

    private ContentResolver mContentResolver;

    public LocationsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.autocomplete_item, parent, false);
        ViewHolder holder = new ViewHolder();
        holder.itemView = (TextView) v.findViewById(R.id.autoCompleteItemTextView);
        v.setTag(holder);
        return v;
    }

    @Override
    public void bindView(View v, Context context, Cursor c) {
        String location = c.getString(c.getColumnIndexOrThrow(LocationsTable.COLUMN_NAME));
        ViewHolder holder = (ViewHolder) v.getTag();
        holder.itemView.setText(location);
    }

    @Override
    public String convertToString(Cursor cursor) {
        // returns name column (ie. location name) into textview after location from drop-down list is selected.
        return cursor.getString(cursor.getColumnIndexOrThrow(LocationsTable.COLUMN_NAME));
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        FilterQueryProvider filter = getFilterQueryProvider();
        if (filter != null) {
            return filter.runQuery(constraint);
        }

        String[] projection = {
                LocationsTable.COLUMN_ID,
                LocationsTable.COLUMN_NAME };

        if (constraint != null && constraint.length() != 0) {
            String selection = LocationsTable.COLUMN_NAME + " LIKE ? ";
            final StringBuilder sb = new StringBuilder();
            sb.append(constraint);
            String[] selectionArgs = {sb.toString() + "%"};
            Log.d(LOG_TAG, "SELECTION  IS " + selection);
            Log.d(LOG_TAG, "SELECTION ARGS IS " + Arrays.toString(selectionArgs));
            Log.d(LOG_TAG, "PROJECTION IS " + Arrays.toString(projection));
            Cursor cursor = mContentResolver.query(LocationsContentProvider.CONTENT_URI, projection, selection, selectionArgs, LocationsTable.COLUMN_NAME + " ASC");
            Log.d(LOG_TAG, "CURSOR COUNT IS " + cursor.getCount());
            return cursor;
        }

        // TODO - if string is empty then return empty cursor without running query
        Log.d(LOG_TAG, "QUERYING FOR EMPTY CURSOR ");
        return mContentResolver.query(LocationsContentProvider.CONTENT_URI, projection, LocationsTable.COLUMN_ID + " = 0 ", null, null);
    }

}
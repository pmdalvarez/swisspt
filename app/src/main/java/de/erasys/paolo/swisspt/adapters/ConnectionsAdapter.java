package de.erasys.paolo.swisspt.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import de.erasys.paolo.swisspt.R;
import de.erasys.paolo.swisspt.content.model.Connection;

/**
 * Created by paolo on 13.03.15.
 */
public class ConnectionsAdapter extends BaseAdapter {

    private static final String LOG_TAG = ConnectionsAdapter.class.getSimpleName();

    private final Context context;
    private final ArrayList<Connection> values;

    public ConnectionsAdapter(Context context, ArrayList<Connection> values) {
        this.context = context;
        this.values = values;
    }

    @Override
    public int getCount() {
        return values.size();
    }

    @Override
    public Object getItem(int i) {
        return values.get(i);
    }

    @Override
    public long getItemId(int i)
    {
        // used by onclick listener
        return i;
    }

    public void add(Connection connection) {
        values.add(connection);
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.stationboard_connection, parent, false);
        }

        TextView timeCol = (TextView) convertView.findViewById(R.id.connectionDeparture);
        TextView connNameCol = (TextView) convertView.findViewById(R.id.connectionName);
        TextView destinationCol = (TextView) convertView.findViewById(R.id.connectionDestination);

        timeCol.setText(values.get(position).departure.time);
        connNameCol.setText(values.get(position).name);
        destinationCol.setText(values.get(position).arrival.location.name);
//        Log.d(LOG_TAG, "values:" + values.toString() + " value:" + values.get(position));

        return convertView;
    }

    public void setValues(ArrayList<Connection> values) {
        this.values.clear();
        for (int i = 0; i < values.size(); i++) {
            this.values.add(values.get(i));
        }
    }

}


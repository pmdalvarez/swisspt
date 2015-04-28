package de.erasys.paolo.swisspt.networking;

import android.content.ContentValues;

/**
 * Created by paolo on 24.04.15.
 */
public interface LocationsCallbacks {

    public void onLocationsLoaded();
    public void onLocationsFailed();
    public void onLocationRetrieved(ContentValues values);

}

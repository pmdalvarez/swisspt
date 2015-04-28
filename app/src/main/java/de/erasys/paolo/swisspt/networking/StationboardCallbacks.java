package de.erasys.paolo.swisspt.networking;

import java.util.ArrayList;

import de.erasys.paolo.swisspt.content.model.Connection;

/**
 * Created by paolo on 24.04.15.
 */
public interface StationboardCallbacks {

    public void onConnectionsLoading();

    public void onConnectionsLoaded(ArrayList<Connection> connections);

}

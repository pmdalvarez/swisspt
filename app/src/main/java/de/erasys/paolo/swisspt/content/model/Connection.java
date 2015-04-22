package de.erasys.paolo.swisspt.content.model;

import android.webkit.WebStorage;

/**
 * Created by paolo on 22.04.15.
 */
public class Connection {

    public class Rendezvous {
        public Location location;
        public String time;
    }

    /**
     * Connection Name eg. S5, RE739
     */
    public String name;
    public Rendezvous departure;
    public Rendezvous arrival;

    public Connection(String name, String origin, String departureTime, String destination, String arrivalTime) {
        this.name = name;
        this.departure = new Rendezvous();
        this.departure.location = new Location();
        this.departure.location.name = origin;
        this.departure.time = departureTime;
        this.arrival = new Rendezvous();
        this.arrival.location = new Location();
        this.arrival.location.name = destination;
        this.arrival.time = arrivalTime;
    }

}

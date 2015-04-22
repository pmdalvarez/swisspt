package de.erasys.paolo.swisspt.content.model;

/**
 * Created by paolo on 22.04.15.
 */
public class Connection {
    /**
     * Connection Name eg. S5, RE739
     */
    public String name;

    public Location origin;

    public Location destination;

    /**
     * Time of departure
     */
    public String departure;

}

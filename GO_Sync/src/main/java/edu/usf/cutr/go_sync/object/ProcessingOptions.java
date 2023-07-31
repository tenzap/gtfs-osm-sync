/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Enum.java to edit this template
 */
package edu.usf.cutr.go_sync.object;

/**
 *
 * @author tenzap
 */
public enum ProcessingOptions {
    // General

    // For Stops
    SKIP_GTFS_STATIONS,

    // For Routes
    DONT_REPLACE_EXISING_OSM_ROUTE_COLOR,
    DONT_ADD_GTFS_ROUTE_TEXT_COLOR_TO_ROUTE,
    DONT_ADD_GTFS_AGENCY_ID_TO_ROUTE,

    // For Route Members
    SKIP_NODES_HAVING_ROLE_EMPTY,
    SKIP_NODES_HAVING_ROLE_STOP_ALL,
    SKIP_NODES_HAVING_ROLE_STOP_WITHOUT_MATCHING_PLATFORM,
    CREATE_ROUTE_AS_PTV2,
    SKIP_NODES_HAVING_ROLE_PLATFORM_NOT_IN_GTFS_TRIP_FROM_OSM_RELATION;

}

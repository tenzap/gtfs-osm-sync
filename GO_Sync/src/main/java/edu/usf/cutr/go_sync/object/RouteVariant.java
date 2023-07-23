/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.usf.cutr.go_sync.object;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 *
 * @author tenzap
 */
public class RouteVariant {

    String trip_id;
    String route_id;
    String route_short_name;
    private String route_long_name;
    List<String> same_trip_sequences;
    TreeMap<Integer, RouteVariantStop> stops;

    public RouteVariant(String trip_id) {
        this.trip_id = trip_id;
        stops = new TreeMap<>();
        same_trip_sequences = new ArrayList<String>();
    }

    public String getTrip_id() {
        return trip_id;
    }

    public TreeMap<Integer, RouteVariantStop> getStops() {
        return stops;
    }

    public void setStops(TreeMap<Integer, RouteVariantStop> stops) {
        this.stops = stops;
    }

    public void addStop(Integer sequence_id, String stop_id, String name, String pickup_type, String drop_off_type) {
        RouteVariantStop rvs = new RouteVariantStop(stop_id, name, pickup_type, drop_off_type);
        stops.put(sequence_id, rvs);
    }

    public String toText() {
        String s = "";
        s += String.format("Trip_id [%s] | route_id [%s] | route_short_name [%s] | route_long_name [%s]\n", trip_id, route_id, route_short_name, route_long_name);
        s += String.format(" Same as: %s\n", same_trip_sequences.toString());
        for (Map.Entry<Integer, RouteVariantStop> stop : stops.entrySet()) {
            Integer key = stop.getKey();
            RouteVariantStop value = stop.getValue();
            s += String.format(" num: %d, %s, %s, %s\n", key, value.getStop_id(), value.getPickup_type(), value.getDrop_off_type());
        }
        return s;
    }

    public List<String> getSame_trip_sequence() {
        return same_trip_sequences;
    }

    public void addSame_trip_sequence(String trip_id) {
        this.same_trip_sequences.add(trip_id);
    }

    public String getRoute_id() {
        return route_id;
    }

    public void setRoute_id(String route_id) {
        this.route_id = route_id;
    }

    public String getRoute_short_name() {
        return route_short_name;
    }

    public void setRoute_short_name(String route_short_name) {
        this.route_short_name = route_short_name;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + Objects.hashCode(this.stops);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RouteVariant other = (RouteVariant) obj;
        return Objects.equals(this.stops, other.stops);
    }

    public boolean equalsSequenceOf(RouteVariant obj) {
        final RouteVariant other = (RouteVariant) obj;
        return Objects.equals(this.stops, other.stops);
    }

    public String getOsmValue(String key_name) {
        switch (key_name) {
            case "ref":
                return this.route_short_name;
            case "name":
                return String.format("Bus %s: %s => %s",
                        this.route_short_name,
                        stops.firstEntry().getValue().getName(),
                        stops.lastEntry().getValue().getName());
            case "from":
                return stops.firstEntry().getValue().getName();
            case "to":
                return stops.lastEntry().getValue().getName();
            case "gtfs:route_id":
                return this.route_id;
            case "gtfs:trip_id:sample":
                return this.trip_id;
            case "gtfs:name":
            case "gtfs_name":
                return this.route_long_name;
            default:
                break;
        }
        return "";
    }

    public String getRoute_long_name() {
        return route_long_name;
    }

    /*public String getVariantIdForDisplay() {
        return getRoute_short_name() + "|" + trip_id;
    }*/

}

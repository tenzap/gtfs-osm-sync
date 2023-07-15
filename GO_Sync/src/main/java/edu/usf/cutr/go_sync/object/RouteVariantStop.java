/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.usf.cutr.go_sync.object;

import java.util.Objects;

/**
 *
 * @author tenzap
 */
public class RouteVariantStop {

    String stop_id, pickup_type, drop_off_type, name;

    public RouteVariantStop(String stop_id, String pickup_type, String drop_off_type) {
        this.stop_id = stop_id;
        this.name = stop_id;
        this.pickup_type = pickup_type;
        this.drop_off_type = drop_off_type;
    }

    public String getStop_id() {
        return stop_id;
    }

    public String getPickup_type() {
        return pickup_type;
    }

    public String getDrop_off_type() {
        return drop_off_type;
    }

    public String toText() {
        return String.format("stop: [%s], pickup_type[%s], drop_off_type[%s]",
                stop_id, pickup_type, drop_off_type);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        final RouteVariantStop other = (RouteVariantStop) obj;
        if (!Objects.equals(this.stop_id, other.stop_id)) {
            return false;
        }
        if (!Objects.equals(this.pickup_type, other.pickup_type)) {
            return false;
        }
        return Objects.equals(this.drop_off_type, other.drop_off_type);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + Objects.hashCode(this.stop_id);
        hash = 61 * hash + Objects.hashCode(this.pickup_type);
        hash = 61 * hash + Objects.hashCode(this.drop_off_type);
        return hash;
    }

}
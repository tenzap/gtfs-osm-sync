/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package edu.usf.cutr.go_sync.tools.parser;

import java.util.ArrayList;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 *
 * @author todo
 */
public class NodeWayAttr extends AttributesImpl {
    
    String osmPrimitiveType; // node or way
    String lat;
    String lon;
    
    ArrayList<String> wayNds = new ArrayList<>();

    NodeWayAttr(Attributes attr, String object_type) {
        super(attr);
        this.osmPrimitiveType = object_type;
    }

    public ArrayList<String> getWayNds() {
        return wayNds;
    }

    public void setWayNds(ArrayList<String> wayNds) {
        //System.out.println("Adding " + wayNds.size() + " nds to way.");
        //System.out.println(wayNds.toString());
        this.wayNds = wayNds;
    }
    
    public void setLat(String lat) {
        if (osmPrimitiveType.equals("way")) {
            this.lat = lat;
        }
    }

    public void setLon(String lon) {
        if (osmPrimitiveType.equals("way")) {
            this.lon = lon;
        }
    }
    
    public String getLat() {
        if (osmPrimitiveType.equals("way")) {
            return lat;
        }
        return getValue("lat");
    }

    public String getLon() {
        if (osmPrimitiveType.equals("way")) {
            return lon;
        }
        return getValue("lon");
    }
    
    public String geOsmPrimitiveType() {
        return osmPrimitiveType;
    }
    
    public boolean shouldSaveGeoData() {
        return !osmPrimitiveType.equals("way");
    }

    @Override
    public String toString() {
        String a = "primitive type: [%s]\t- id: [%s]\t - lat: [%s]\t- lon: [%s]\t - " +
                "nodes content: %s";
        return String.format(a, osmPrimitiveType, getValue("id"), lat, lon, wayNds.toString());
    }
}

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.usf.cutr.go_sync.object;

import java.util.ArrayList;

/**
 *
 * @author tenzap
 */
public class NetexStopPlace {
    String id;
    String town;
    String name;
    String parentSiteRef;
    String childSiteRef;
    String altName;

    ArrayList<String> quayRefs;

    public NetexStopPlace(String id) {
        this.id = id;
        parentSiteRef = null;
        quayRefs = new ArrayList<String>();
    }    

    public String getId() {
        return id;
    }
        
    public void setId(String id) {
        this.id = id;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParentSiteRef() {
        return parentSiteRef;
    }
    
    public String getGtfsEquivalentId() {
        String[] base_id;
        if (parentSiteRef != null) {
            base_id = parentSiteRef.split(":");
            return base_id[3].replace("log", "S");
        } else {
            //if (childSiteRef != null) {
                // Case when we are on a logical StopPlace or a parentStopPlace
                base_id = id.split(":");
                return base_id[3].replace("log", "S");
            //}
        }
        //return "Unexpected case: TODO";
    }

    public void setParentSiteRef(String parentSiteRef) {
        this.parentSiteRef = parentSiteRef;
    }

    public String getChildSiteRef() {
        return childSiteRef;
    }

    public void setChildSiteRef(String childSiteRef) {
        this.childSiteRef = childSiteRef;
    }

    public ArrayList<String> getQuayRefs() {
        return quayRefs;
    }

    public void setQuayRefs(ArrayList<String> quayRefs) {
        this.quayRefs = quayRefs;
    }
    
    public void addQuayRef(String quayRef) {
        this.quayRefs.add(quayRef);
    }
    
    public String getAltName() {
        if (altName == null)
            return "";
        return altName;
    }

    public void setAltName(String altName) {
        this.altName = altName;
    }
    
    public String printContent() {
        return String.format("id: [%s] name: [%s] altNames: %s town: [%s] parentSiteRef: [%s] childSiteRef: [%s] quayRefs: %s", id, name, altName, town, parentSiteRef, childSiteRef, quayRefs.toString());
    }
}

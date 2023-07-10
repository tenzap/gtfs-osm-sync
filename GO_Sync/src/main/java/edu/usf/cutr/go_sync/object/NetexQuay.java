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
public class NetexQuay {

    String id;
    String town;
    String name;
    ArrayList<String> altNames;

    public NetexQuay(String id) {
        this.id = id;
        altNames = new ArrayList<String>();
    }

    public String getId() {
        return id;
    }

    public String getIdAsGtfs() {
        return id.split(":")[3];
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

    public ArrayList<String> getAltNames() {
        return altNames;
    }

    public String getAltNamesJoined() {
        return String.join(";", altNames);
    }

    public void setAltNames(ArrayList<String> altNames) {
        this.altNames = altNames;
    }

    public void addAltName(String altName) {
        this.altNames.add(altName.replace(';', '_'));
    }

    public String printContent() {
        return String.format("id: [%s] name: [%s] altNames: %s town: [%s]", id, name, altNames.toString(), town);
    }

}

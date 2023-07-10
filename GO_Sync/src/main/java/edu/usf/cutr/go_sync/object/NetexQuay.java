/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.usf.cutr.go_sync.object;

/**
 *
 * @author tenzap
 */
public class NetexQuay extends NetexStopElement {

    public NetexQuay(String id) {
        super(id);
    }

    public String getIdAsGtfs() {
        return id.split(":")[3];
    }

    public String printContent() {
        return String.format("id: [%s] name: [%s] altNames: %s town: [%s]", id, name, altNames.toString(), town);
    }

}

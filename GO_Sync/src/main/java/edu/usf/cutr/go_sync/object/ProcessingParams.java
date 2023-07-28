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
public class ProcessingParams {
    ArrayList<String> stopCities;
    Double stopMinLat, stopMaxLat;
    Double stopMinLon, stopMaxLon;

    public ArrayList<String> getStopCities() {
        return stopCities;
    }

    public void setStopCities(ArrayList<String> stopCities) {
        this.stopCities = stopCities;
    }

    public Double getStopMinLat() {
        return stopMinLat;
    }

    public void setStopMinLat(String stopMinLat) {
        this.stopMinLat = Double.valueOf(stopMinLat);
    }

    public Double getStopMaxLat() {
        return stopMaxLat;
    }

    public void setStopMaxLat(String stopMaxLat) {
        this.stopMaxLat = Double.valueOf(stopMaxLat);
    }

    public Double getStopMinLon() {
        return stopMinLon;
    }

    public void setStopMinLon(String stopMinLon) {
        this.stopMinLon = Double.valueOf(stopMinLon);
    }

    public Double getStopMaxLon() {
        return stopMaxLon;
    }

    public void setStopMaxLon(String stopMaxLon) {
        this.stopMaxLon = Double.valueOf(stopMaxLon);
    }
}

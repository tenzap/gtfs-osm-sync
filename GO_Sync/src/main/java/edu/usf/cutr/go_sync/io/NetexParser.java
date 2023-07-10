/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package edu.usf.cutr.go_sync.io;

import edu.usf.cutr.go_sync.object.NetexQuay;
import edu.usf.cutr.go_sync.object.NetexStopPlace;
import edu.usf.cutr.go_sync.tools.parser.NodeWayAttr;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.imageio.metadata.IIOMetadataFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author tenzap
 */
public class NetexParser extends DefaultHandler {

    HashMap<String, NetexQuay> quayList;
    List<NetexStopPlace> stopPlaceList;
    List<NetexStopPlace> parentSiteList;
    HashMap<String, NetexStopPlace> logicalSiteList;

    NetexQuay quay = null;
    NetexStopPlace stopPlace = null;

    private StringBuilder tagTextContent = new StringBuilder();

    List<String> xPath;
    String pathBegin = "PublicationDelivery/dataObjects/GeneralFrame/members/";

    public NetexParser() {
        xPath = new ArrayList<String>();
        quayList = new HashMap<String, NetexQuay>();
        stopPlaceList = new ArrayList<NetexStopPlace>();
        parentSiteList = new ArrayList<NetexStopPlace>();
        logicalSiteList = new HashMap<String, NetexStopPlace>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody

        xPath.add(qName);

        if (qName.equals("Quay")) {
            quay = new NetexQuay(attributes.getValue("id"));
        }

        if (qName.equals("StopPlace")) {
            stopPlace = new NetexStopPlace(attributes.getValue("id"));
        }

        if (qName.equals("Name")) {
            if (String.join("/", xPath).equals(pathBegin + "Quay/Name")) {
                tagTextContent.setLength(0);
            }

            if (String.join("/", xPath).equals(pathBegin + "Quay/alternativeNames/AlternativeName/Name")) {
                tagTextContent.setLength(0);
            }

            if (String.join("/", xPath).equals(pathBegin + "StopPlace/Name")) {
                tagTextContent.setLength(0);
            }
        }

        if (qName.equals("Town")) {
            if (String.join("/", xPath).equals(pathBegin + "Quay/PostalAddress/Town")) {
                tagTextContent.setLength(0);

            }
            if (String.join("/", xPath).equals(pathBegin + "StopPlace/PostalAddress/Town")) {
                tagTextContent.setLength(0);
            }
        }

        if (qName.equals("alternativeNames")) {
            // Quay/alternativeNames
        }

        if (qName.equals("AlternativeName")) {
            // Quay/alternativeNames/AlternativeName
        }

        if (qName.equals("PostalAddress")) {
            // Quay/PostalAddress

            // StopPlace/PostalAddress
        }

        if (qName.equals("quays")) {
            // StopPlace/quays
        }

        if (qName.equals("QuayRef")) {
            if (String.join("/", xPath).equals(pathBegin + "StopPlace/quays/QuayRef")) {
                stopPlace.addQuayRef(attributes.getValue("ref"));
            }
        }

        if (qName.equals("ParentSiteRef")) {
            if (String.join("/", xPath).equals(pathBegin + "StopPlace/ParentSiteRef")) {
                stopPlace.setParentSiteRef(attributes.getValue("ref"));
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
        tagTextContent.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody

        if (qName.equals("Quay")) {
            quayList.put(quay.getIdAsGtfs(), quay);
        }

        if (qName.equals("StopPlace")) {
            // Store stopPlace to different list depending on whether it is a parentSite or not.
            if (stopPlace.getParentSiteRef() == null) {
                parentSiteList.add(stopPlace);
            } else {
                stopPlaceList.add(stopPlace);
            }
        }

        if (qName.equals("Name")) {
            if (String.join("/", xPath).equals(pathBegin + "Quay/Name")) {
                quay.setName(tagTextContent.toString());
            }

            if (String.join("/", xPath).equals(pathBegin + "Quay/alternativeNames/AlternativeName/Name")) {
                quay.addAltName(tagTextContent.toString());
            }

            if (String.join("/", xPath).equals(pathBegin + "StopPlace/Name")) {
                stopPlace.setName(tagTextContent.toString());
            }
        }

        if (qName.equals("Town")) {
            if (String.join("/", xPath).equals(pathBegin + "Quay/PostalAddress/Town")) {
                quay.setTown(tagTextContent.toString());

            }
            if (String.join("/", xPath).equals(pathBegin + "StopPlace/PostalAddress/Town")) {
                stopPlace.setTown(tagTextContent.toString());
            }
        }

        if (xPath.get(xPath.size() - 1).equals(qName)) {
            xPath.remove(xPath.size() - 1);
        }

    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody

        boolean found = false;
        // create logical stop place based on the stopPlaceList
        for (NetexStopPlace p : parentSiteList) {
            found = false;
            for (NetexStopPlace c : stopPlaceList) {
                if (p.getId().equals(c.getParentSiteRef())) {
                    found = true;
                    // Save current Id to childSiteRef
                    c.setChildSiteRef(c.getId());
                    c.setParentSiteRef(null);
                    // Use id of parentSite
                    c.setId(p.getId());
                    // Use name of parentSite as altName
                    c.setAltName(p.getName());
                    logicalSiteList.put(c.getGtfsEquivalentId(), c);
                    break;
                }
            }
            if (!found) {
                // Case where no match found:
                System.out.println("No child stopPlace in netext for : " + p.getId());
                logicalSiteList.put(p.getGtfsEquivalentId(), p);
            }
        }

        //printResult();
    }

    public void printResult() {
        System.out.println("All logical stopPlaces after parsing:");
        for (Map.Entry<String, NetexStopPlace> set : logicalSiteList.entrySet()) {
            System.out.println(set.getKey() + "> " + set.getValue().printContent());
        }
        System.out.println("All Quay after parsing:");
        for (Map.Entry<String, NetexQuay> set : quayList.entrySet()) {
            System.out.println(set.getKey() + "> " + set.getValue().printContent());
        }
    }

    public HashMap<String, NetexQuay> getQuayList() {
        return quayList;
    }

    public HashMap<String, NetexStopPlace> getLogicalSiteList() {
        return logicalSiteList;
    }

}

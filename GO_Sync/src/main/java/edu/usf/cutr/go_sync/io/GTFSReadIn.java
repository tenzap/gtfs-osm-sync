/**
Copyright 2010 University of South Florida

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

**/
package edu.usf.cutr.go_sync.io;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import edu.usf.cutr.go_sync.tag_defs;
import edu.usf.cutr.go_sync.object.OperatorInfo;
import edu.usf.cutr.go_sync.object.Route;
import edu.usf.cutr.go_sync.object.RouteVariant;
import edu.usf.cutr.go_sync.object.Stop;
import edu.usf.cutr.go_sync.tools.OsmFormatter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

public class GTFSReadIn {
    private static Hashtable<String, Route> allRoutes;
    private static final String ROUTE_KEY = "route_ref";
    private static final String NTD_ID_KEY = "ntd_id";

    private List<Stop> stops;

    public GTFSReadIn() {
        stops = new ArrayList<Stop>();
        allRoutes = new Hashtable<String, Route>();
//        readBusStop("C:\\Users\\Khoa Tran\\Desktop\\Summer REU\\Khoa_transit\\stops.txt");
    }

    public static Set<String> getAllRoutesID(){
        return allRoutes.keySet();
    }

//TODO handle multiple agencies
    public String readAgency(String agency_fName){
        try {
            BufferedReader br = new BufferedReader(new FileReader(agency_fName));
            CSVParser parser = CSVParser.parse(br, CSVFormat.DEFAULT.withHeader());

            for (CSVRecord csvRecord : parser) {
                String agencyName;
                if (csvRecord.get(tag_defs.GTFS_NETWORK_KEY) == null ||
                    csvRecord.get(tag_defs.GTFS_NETWORK_KEY).isEmpty())
                    agencyName = csvRecord.get(tag_defs.GTFS_NETWORK_ID_KEY);
                else agencyName = csvRecord.get(tag_defs.GTFS_NETWORK_KEY);
                br.close();
                return agencyName;
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
            return null;
        }
        return null;
    }

    public List<Stop> readBusStop(String fName, String agencyName, String routes_fName, String trips_fName, String stop_times_fName){
        long tStart = System.currentTimeMillis();
        Hashtable<String, HashSet<Route>> id = matchRouteToStop(routes_fName, trips_fName, stop_times_fName);
        Hashtable<String, HashSet<Route>> stopIDs = new Hashtable<String, HashSet<Route>>(id);

        String thisLine;
        String [] elements;
        int stopIdKey=-1, stopNameKey=-1, stopLatKey=-1, stopLonKey=-1;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(fName)),"UTF-8"));
            HashMap<String,Integer> keysIndex = new HashMap<String,Integer> ();
            thisLine = br.readLine();
            StringReader sr = new StringReader(thisLine);
            CSVParser headerParser = CSVParser.parse(sr, CSVFormat.DEFAULT.withHeader(
                    //"route_id","route_short_name","route_long_name","route_desc","route_type","route_url","color","route_text_color"
            ));

            List<String> CSVkeysList = headerParser.getHeaderNames();
            ArrayList<String> CSVkeysListNew = new ArrayList<>(CSVkeysList);
            String[] keys =  new String[CSVkeysList.size()];
            keys = CSVkeysList.toArray(keys);{
                    for(int i=0; i<keys.length; i++) {
                        switch (keys[i]) {
                            case "stop_id":
                                stopIdKey = i;
                                break;
                            case "stop_name":
                                stopNameKey = i;
                                break;
                            case "stop_lat":
                                stopLatKey = i;
                                break;
                            case "stop_lon":
                                stopLonKey = i;
                                break;
                            case tag_defs.GTFS_STOP_URL_KEY:
                                keysIndex.put(tag_defs.OSM_URL_KEY, i);
                                break;
                            case tag_defs.GTFS_ZONE_KEY:
                                keysIndex.put(tag_defs.OSM_ZONE_KEY, i);
                                break;
                            case tag_defs.GTFS_STOP_TYPE_KEY:
                                keysIndex.put(tag_defs.OSM_STOP_TYPE_KEY, i);
                                break;
                            case tag_defs.GTFS_WHEELCHAIR_KEY:
                                keysIndex.put(tag_defs.OSM_WHEELCHAIR_KEY, i);
                                break;
                            default:
                                String t = "gtfs_" + keys[i];
                                keysIndex.put(t, i);
                        }
                    }
                    System.out.println(keysIndex.toString());
//                    System.out.println(stopIdKey+","+stopNameKey+","+stopLatKey+","+stopLonKey);
                }
            CSVParser parser = CSVParser.parse(br, CSVFormat.DEFAULT.withHeader(keys));
            for (CSVRecord csvRecord : parser) {
                Iterator<String> iter = csvRecord.iterator();
                Map<String,String> hm = csvRecord.toMap();
                elements =  new String[hm.size()];
                elements = hm.values().toArray(elements);
                 //add leading 0's to gtfs_id
                    String tempStopId = OsmFormatter.getValidBusStopId(elements[stopIdKey]);
                    Stop s = new Stop(tempStopId, agencyName, elements[stopNameKey],elements[stopLatKey],elements[stopLonKey]);
                    HashSet<String> keysn = new HashSet<String>(keysIndex.keySet());
                    Iterator it = keysn.iterator();
                    try {
                        while(it.hasNext()) {
                        	String k = (String)it.next();

                            String v = null;
                            //if(!lastIndexEmpty) v = elements[(Integer)keysIndex.get(k)];
                            if(keysIndex.get(k) < elements.length) v = elements[keysIndex.get(k)];
                            if ((v!=null) && (!v.isEmpty())) {
                                if (k.equals(tag_defs.OSM_STOP_TYPE_KEY)) {
                                    switch(Integer.parseInt(v)) {
                                        // https://developers.google.com/transit/gtfs/reference/stops-file
                                        case 0: v="platform";break;
                                        case 1: v="station"; break;
                                        default: break;
                                    }
                                }
                                if (k.equals(tag_defs.OSM_WHEELCHAIR_KEY)) {
                                    String parent = "";

                                    if (keysn.contains("gtfs_parent_station"))
                                        parent = elements[keysIndex.get(k)];
                                    if (parent.isEmpty()) {
                                        switch (Integer.parseInt(v)) {
                                            // https://developers.google.com/transit/gtfs/reference/stops-file
                                            case 0:
                                                v = "";
                                                break;
                                            case 1:
                                                v = "limited";
                                                break;
                                            case 2:
                                                v = "no";
                                                break;
                                            default:
                                                break;
                                        }
                                        s.addTag(k, v);
                                    }
                                } else
                                    s.addTag(k, v);
                            }
                            //System.out.print(k+":" + v +" ");
                        }
//                        s.addTag(NTD_ID_KEY, OperatorInfo.getNTDID());
//                        s.addTag("url", s.getTag("stop_url"));

// disable source tag
//                        s.addTag("source", "http://translink.com.au/about-translink/reporting-and-publications/public-transport-performance-data");
//                        if (!tempStopId.contains("place")) s.addTag("url", "http://translink.com.au/stop/"+tempStopId);

                    } catch(Exception e) {
                        System.out.println("Error occurred! Please check your GTFS input files");
                        System.out.println(e.toString());
                        System.exit(0);
                    }
                    // TODO use routes to determine stop tags
                   // System.err.println(s.getTags());
                    String r = getRoutesInTextByBusStop(stopIDs.get(tempStopId));

//             generate tag for routes using stop
                    if (!r.isEmpty()) s.addTag(ROUTE_KEY, r);
                    HashSet<Route> asdf = stopIDs.get(tempStopId);
                    if(asdf!=null)s.addRoutes(stopIDs.get(tempStopId));

                    stops.add(s);

                    HashMap<String,String> modes = getModeTagsByBusStop(stopIDs.get(tempStopId));
                    if (!r.isEmpty()) s.addTags(modes);
//                    System.out.println(thisLine);
                }
//            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }
        long tDelta = System.currentTimeMillis() - tStart;
//        this.setMessage("Completed in "+ tDelta /1000.0 + "seconds");
        System.out.println("GTFSReadIn Completed in "+ tDelta /1000.0 + "seconds");
        return stops;
    }

    public Hashtable<String, Route> readRoutes(String routes_fName){
        Hashtable<String, Route> routes = new Hashtable<String, Route>();
        String thisLine;
        String [] elements;
        int routeIdKey=-1, routeShortNameKey=-1,routeLongNameKey=-1;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(routes_fName)), "UTF-8"));
            HashMap<String,Integer> keysIndex = new HashMap<String,Integer> ();
            thisLine = br.readLine();
            StringReader sr = new StringReader(thisLine);
            CSVParser headerParser = CSVParser.parse(sr, CSVFormat.DEFAULT.withHeader(
                    //"route_id","route_short_name","route_long_name","route_desc","route_type","route_url","color","route_text_color"
            ));
            List<String> CSVkeysList = headerParser.getHeaderNames();
            ArrayList<String> CSVkeysListNew = new ArrayList<>(CSVkeysList);
            String[] keysn =  new String[CSVkeysList.size()];
            keysn = CSVkeysList.toArray(keysn);
            for(int i=0; i<keysn.length; i++) {
                //read keys
                switch (keysn[i]) {
                    case tag_defs.GTFS_ROUTE_ID_KEY:
                        routeIdKey = i;
                        break;
                    case tag_defs.GTFS_ROUTE_URL_KEY:
                        keysIndex.put(tag_defs.OSM_URL_KEY, i);
                        break;
                    case "route_type":
                        keysIndex.put(tag_defs.OSM_ROUTE_TYPE_KEY, i);
                        break;
                    case tag_defs.GTFS_COLOUR_KEY:
                    case tag_defs.GTFS_COLOR_KEY:
                        keysIndex.put(tag_defs.OSM_COLOUR_KEY, i);
                        break;
                    case tag_defs.GTFS_ROUTE_NUM:
                        routeShortNameKey = i;
                        break;
                    case tag_defs.GTFS_ROUTE_NAME:
                        routeLongNameKey = i;
                        break;
                    default:
                        String t = "gtfs_" + keysn[i];
                        keysIndex.put(t, i);
                        break;
                }
            }
            if (routeLongNameKey != -1)
                keysIndex.put("gtfs:name",routeLongNameKey);
//                    System.out.println(stopIdKey+","+stopNameKey+","+stopLatKey+","+stopLonKey);

            {
                final Pattern colourPattern = Pattern.compile("^[a-fA-F0-9]+$");
                CSVParser parser = CSVParser.parse(br, CSVFormat.DEFAULT.withHeader(keysn));
                for (CSVRecord csvRecord : parser) {

                    Iterator<String> iter = csvRecord.iterator();
                    Map<String,String> hm = csvRecord.toMap();
                    elements =  new String[hm.size()];
                    elements = hm.values().toArray(elements);

                    String routeName;
                    if(elements[routeShortNameKey]==null || elements[routeShortNameKey].isEmpty()) routeName = elements[routeIdKey];
                    else routeName = elements[routeShortNameKey];
                    Route r = new Route(elements[routeIdKey], routeName, OperatorInfo.getFullName());
                    HashSet<String> keys = new HashSet<String>(keysIndex.keySet());
                    Iterator<String> it = keys.iterator();
                    try {
                        while(it.hasNext()) {
                            String k = it.next();
                            String v = null;
                            int ki = keysIndex.get(k);
                            if(/*!(lastIndexEmpty && */ki <elements.length) v = elements[ki];
                            if ((v!=null) && (!v.isEmpty())) {
                                if (k.equals(tag_defs.OSM_ROUTE_TYPE_KEY)) {
                                    String route_value;
                                    switch(Integer.parseInt(v)) {
                                        // TODO allow drop down finetuning selection on report viewer
                                        // https://developers.google.com/transit/gtfs/reference/routes-file
                                        // https://wiki.openstreetmap.org/wiki/Relation:route#Route_types_.28route.29
                                        case 0: route_value = "light_rail";	break;// 0: Tram, Streetcar, Light rail. Any light rail or street level system within a metropolitan area.
                                        case 1:	route_value = "subway";     break;	// Subway, Metro. Any underground rail system within a metropolitan area.
                                        case 2: route_value = "train";      break;	// Rail. Used for intercity or long-distance travel.
                                        case 3: route_value = "bus";        break;	// Bus. Used for short- and long-distance bus routes.
                                        case 4: route_value = "ferry";      break;	// Ferry. Used for short- and long-distance boat service.
                                        case 5: route_value = "tram";       break;	// Cable car. Used for street-level cable cars where the cable runs beneath the car.
                                        case 6: k = "aerialway";
                                                route_value = "yes";        break;	// Gondola, Suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable.
                                        // TODO use railway=funicular
                                        case 7: k = "railway";
                                                route_value = "funicular";  break;	// Funicular. Any rail system designed for steep inclines.
                                        default: route_value = v; break;
                                    }
                                    v = route_value;
                                }
                                //prepend hex colours
//                                if (k.equals(tag_defs.OSM_COLOUR_KEY))
//                                    System.out.println(tag_defs.OSM_COLOUR_KEY + " "+ v + " #"+v);
                                if (k.equals(tag_defs.OSM_COLOUR_KEY) && ((v.length() == 3 || v.length() == 6) && colourPattern.matcher(v).matches()))/*^[a-fA-F0-9]+$")))*/ {
                                    v = "#".concat(v);
                                }
                                r.addTag(k, v);
                            }
                        }
                    } catch(Exception e){
                        System.out.println("Error occurred! Please check your GTFS input files");
                        System.out.println(e.toString());
                        System.exit(0);
                    }
                    routes.put(elements[routeIdKey], r);
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }
        return routes;
    }

    private void insertRouteVariantToAllRouteVariants(String prev_trip_id, RouteVariant rv, HashMap<String, RouteVariant> allRouteVariants) {
        String duplicate_route = null;
        // If we start reading a new trip, save the previous trip to allRouteVariants

        // Search if there is any RouteVariant in allRouteVariants having the same sequence.
        for (HashMap.Entry<String, RouteVariant> rv_check : allRouteVariants.entrySet()) {
            String key = rv_check.getKey();
            RouteVariant value = rv_check.getValue();

            if (value.equalsSequenceOf(rv)) {
                duplicate_route = key;
                //System.out.println(String.format("Duplicate trips: Existing:%s vs New:%s", key, rv.getTrip_id()));
                break;
            }
        }
        if (duplicate_route == null) {
            allRouteVariants.put(prev_trip_id, rv);
        } else {
            //System.out.println(String.format("Adding equal %s to existing trip %s", rv.getTrip_id(), duplicate_route));
            allRouteVariants.get(duplicate_route).addSame_trip_sequence(rv.getTrip_id());
        }
    }

    public HashMap<String, RouteVariant> readRouteVariants(String stop_times_fName, String trips_fName, String routes_fName) {
        HashMap<String, RouteVariant> allRouteVariants = new HashMap<String, RouteVariant>();
        String thisLine;

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(stop_times_fName)), "UTF-8"));
            HashMap<String, Integer> keysIndex = new HashMap<String, Integer>();
            thisLine = br.readLine();
            StringReader sr = new StringReader(thisLine);
            CSVParser headerParser = CSVParser.parse(sr, CSVFormat.DEFAULT.withHeader( //"route_id","route_short_name","route_long_name","route_desc","route_type","route_url","color","route_text_color"
                    ));
            List<String> CSVkeysList = headerParser.getHeaderNames();
            String[] keysn = new String[CSVkeysList.size()];
            keysn = CSVkeysList.toArray(keysn);
            for (int i = 0; i < keysn.length; i++) {
                //read keys
                switch (keysn[i]) {
                    default:
                        keysIndex.put(keysn[i], i);
                        break;
                }
            }

            String prev_trip_id = null;
            String trip_id = null;
            RouteVariant rv = null;

            CSVParser parser = CSVParser.parse(br, CSVFormat.DEFAULT.withHeader(keysn));
            for (CSVRecord csvRecord : parser) {
                // Create route variant if it doesn't exist, or fetch the existing one.
                trip_id = csvRecord.get(keysIndex.get("trip_id"));

                if (prev_trip_id == null) {
                    rv = new RouteVariant(trip_id);
                } else {
                    if (!trip_id.equals(prev_trip_id)) {
                        insertRouteVariantToAllRouteVariants(prev_trip_id, rv, allRouteVariants);
                        rv = new RouteVariant(trip_id);
                    }
                }

                prev_trip_id = trip_id;

                Integer sequence_id = Integer.valueOf(csvRecord.get(keysIndex.get("stop_sequence")));
                String stop_id = csvRecord.get(keysIndex.get("stop_id"));
                String pickup_type = csvRecord.get(keysIndex.get("pickup_type"));
                String drop_off_type = csvRecord.get(keysIndex.get("drop_off_type"));

                rv.addStop(sequence_id, stop_id, pickup_type, drop_off_type);
            }
            // We finished reading the file, save the last trip we read.
            insertRouteVariantToAllRouteVariants(prev_trip_id, rv, allRouteVariants);

            // Read trips & routes files
            Hashtable<String, Route> routes = readRoutes(routes_fName);
            HashMap<String, String> tripIDs = getTripIDs(trips_fName);

            // Now fill the routeVariants with the route_id & route_short_name
            for (HashMap.Entry<String, RouteVariant> rv_entry : allRouteVariants.entrySet()) {
                RouteVariant current_rv = rv_entry.getValue();
                String route_id = tripIDs.get(current_rv.getTrip_id());
                current_rv.setRoute_id(route_id);
                current_rv.setRoute_short_name(routes.get(route_id).getRouteRef());
            }

        } catch (IOException e) {
            System.err.println("Error: " + e);
        }

        return allRouteVariants;
    }

    public HashMap<String, String> getTripIDs(String trips_fName) {
        HashMap<String, String> tripIDs = new HashMap<>();

        // trips.txt read-in
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(trips_fName)),"UTF-8"));
            CSVParser parser = CSVParser.parse(br, CSVFormat.DEFAULT.withHeader());
            for (CSVRecord csvRecord : parser) {

                String tripId = csvRecord.get(tag_defs.GTFS_TRIP_ID_KEY);
                // not sure if tripId is unique in trips.txt, e.g. can 1 trip_id has multiple route_id
                if (tripIDs.containsKey(tripId)) {
                    System.out.println("Repeat "+tripId);
                }
                tripIDs.put(tripId, csvRecord.get(tag_defs.GTFS_ROUTE_ID_KEY));
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }
        return tripIDs;
    }

    public Hashtable<String, HashSet<Route>> matchRouteToStop(String routes_fName, String trips_fName, String stop_times_fName){
        allRoutes.putAll(readRoutes(routes_fName));
        HashMap<String,String> tripIDs = getTripIDs(trips_fName);

        // hashtable String(stop_id) vs. HashSet(routes)
        Hashtable<String, HashSet<Route>> stopIDs = new Hashtable<String, HashSet<Route>>();
        // stop_times.txt read-in
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new BOMInputStream(new FileInputStream(stop_times_fName)), "UTF-8"));

            CSVParser parser = CSVParser.parse(br, CSVFormat.DEFAULT.withHeader());

            for (CSVRecord csvRecord : parser) {
                // This seems to be the fastest method using csvparser
                String trip = csvRecord.get(tag_defs.GTFS_TRIP_ID_KEY);
                HashSet<Route> routes = new HashSet<Route>();
                Route tr = null;
                if (tripIDs.get(trip) != null) tr = allRoutes.get(tripIDs.get(trip));
                if (tr != null) routes.add(tr);
                String sid = OsmFormatter.getValidBusStopId(csvRecord.get(tag_defs.GTFS_TRIPS_STOP_ID_KEY));
                if (stopIDs.containsKey(sid)) {
                    routes.addAll(stopIDs.get(sid));
                    stopIDs.remove(sid);
                }
                stopIDs.put(sid, routes);
            }
        }
        catch (IOException e) {
            System.err.println("Error: " + e);
        }
        return stopIDs;
    }

    //TODO implement  this
    // https://wiki.openstreetmap.org/wiki/Public_transport
    public HashMap<String,String> getModeTagsByBusStop(HashSet<Route> r) {
        HashMap<String,String> keys = new HashMap<String,String>();
        if (r!=null) {
            //convert from hashset to arraylist
            ArrayList<Route> routes = new ArrayList<Route>(r);
            for (Route rr:routes) {
                if (rr.containsKey(tag_defs.OSM_ROUTE_TYPE_KEY)) {
                    keys.put(rr.getTag(tag_defs.OSM_ROUTE_TYPE_KEY), "yes");
                    if (rr.getTag(tag_defs.OSM_ROUTE_TYPE_KEY) == "ferry")
                        keys.put("amenity","ferry_terminal");
                }
                if (rr.containsKey("aerialway"))
                     keys.put("aerialway","station");
                if (rr.containsKey("railway") && rr.getTag("railway") == "funicular") {
                    keys.put("railway","station");
                    keys.put("station","funicular");
                }
            }
        }
        return keys;
    }

    private class hashCodeCompare implements  Comparator
    {
        @Override
        public int compare(Object o, Object t1) {
            return o.hashCode() - t1.hashCode();
        }
    }

    public String getRoutesInTextByBusStop(HashSet<Route> r) {
        String text="";

        if (r!=null) {
            TreeSet<String> routeRefSet = new TreeSet<String>(new hashCodeCompare());
            //convert from hashset to arraylist
            ArrayList<Route> routes = new ArrayList<Route>(r);
            for (Route rr:routes) {
                routeRefSet.add(rr.getRouteRef());
            }
            text = String.join(";",routeRefSet);
        }
        return text;
    }
}
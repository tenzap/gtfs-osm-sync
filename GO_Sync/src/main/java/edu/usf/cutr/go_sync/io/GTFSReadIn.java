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
import edu.usf.cutr.go_sync.object.NetexQuay;
import edu.usf.cutr.go_sync.object.NetexStopPlace;
import javax.xml.parsers.SAXParserFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;

public class GTFSReadIn {
    private static Hashtable<String, Route> allRoutes;
    private static final String ROUTE_KEY = "route_ref";
    private static final String NTD_ID_KEY = "ntd_id";

    // Since we don't save all trip_id in RouteVariant, we have to maintain a list that says which RouteVariant
    // has the same trip as the Gtfs trip_id in file
    HashMap<String, String> gtfsTripIdToRouteVariantMap = new HashMap<>();

    private List<Stop> stops;
    private HashMap<String, Stop> stopsMap;
    HashMap <String, NetexQuay> netexQuays;
    HashMap <String, NetexStopPlace> netexSites;

    public GTFSReadIn() {
        stops = new ArrayList<Stop>();
        stopsMap = new HashMap<String, Stop>();
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
                if (csvRecord.get(tag_defs.GTFS_AGENCY_NAME_KEY) == null ||
                    csvRecord.get(tag_defs.GTFS_AGENCY_NAME_KEY).isEmpty())
                    agencyName = csvRecord.get(tag_defs.GTFS_AGENCY_ID_KEY);
                else agencyName = csvRecord.get(tag_defs.GTFS_AGENCY_NAME_KEY);
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

    public List<Stop> readBusStop(String fName, String agencyName, String routes_fName, String trips_fName, String stop_times_fName, String netexStopsFilename){
        long tStart = System.currentTimeMillis();
        Hashtable<String, HashSet<Route>> id = matchRouteToStop(routes_fName, trips_fName, stop_times_fName);
        Hashtable<String, HashSet<Route>> stopIDs = new Hashtable<String, HashSet<Route>>(id);

        if (netexStopsFilename != null && !netexStopsFilename.isEmpty()) {
            readNetexStopsFile(netexStopsFilename);
        }

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
                                keysIndex.put(tag_defs.OSM_STOP_URL_KEY, i);
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
                String public_transport_type = "";
                 //add leading 0's to gtfs_id
                    String tempStopId = OsmFormatter.getValidBusStopId(elements[stopIdKey]);
                    //System.out.println("Reading stop from gtfs: " + tempStopId.toString());
                    Stop s = new Stop("node", tempStopId, agencyName, elements[stopNameKey],elements[stopLatKey],elements[stopLonKey], getNetexQuayName(tempStopId), getNetexQuayAltNames(tempStopId));
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
                                        case 0:
                                            v = public_transport_type = "platform";
                                            break;
                                        case 1:
                                            v = public_transport_type = "station";
                                            break;
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
                    stopsMap.put(tempStopId,s);

                    HashMap<String, String> modes = getModeTagsByBusStop(stopIDs.get(tempStopId), public_transport_type);
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
                        keysIndex.put(tag_defs.OSM_ROUTE_URL_KEY, i);
                        break;
                    case "route_type":
                        keysIndex.put(tag_defs.OSM_ROUTE_TYPE_KEY, i);
                        break;
                    case tag_defs.GTFS_COLOUR_KEY:
                    case tag_defs.GTFS_COLOR_KEY:
                        keysIndex.put(tag_defs.OSM_COLOUR_KEY, i);
                        break;
                    case tag_defs.GTFS_ROUTE_NUM_KEY:
                        routeShortNameKey = i;
                        break;
                    case tag_defs.GTFS_ROUTE_NAME_KEY:
                        keysIndex.put(tag_defs.OSM_ROUTE_NAME_KEY, i);
                        break;
                    default:
                        String t = "gtfs_" + keysn[i];
                        keysIndex.put(t, i);
                        break;
                }
            }

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
                                if (k.equals("gtfs_route_text_color") ||
                                        k.equals("gtfs_agency_id")) {
                                    // Don't add "gtfs_route_text_color" to the Route
                                    // Don't add "gtfs_agency_id" to the Route
                                    continue;
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
            gtfsTripIdToRouteVariantMap.put(prev_trip_id, prev_trip_id);
        } else {
            //System.out.println(String.format("Adding equal %s to existing trip %s", rv.getTrip_id(), duplicate_route));
            allRouteVariants.get(duplicate_route).addSame_trip_sequence(rv.getTrip_id());
            gtfsTripIdToRouteVariantMap.put(prev_trip_id, duplicate_route);
        }
    }

    public HashMap<String, RouteVariant> readRouteVariants(String stop_times_fName, String trips_fName, String routes_fName) {
        assert (!stopsMap.isEmpty()) : "no stops. Is this called after having read the stops.txt file?";

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
                String arrival_time = csvRecord.get(keysIndex.get("arrival_time"));
                String departure_time = csvRecord.get(keysIndex.get("departure_time"));
                String pickup_type = csvRecord.get(keysIndex.get("pickup_type"));
                String drop_off_type = csvRecord.get(keysIndex.get("drop_off_type"));

                rv.addStop(sequence_id, stop_id, stopsMap.get(stop_id).getStopName(), arrival_time, departure_time, pickup_type, drop_off_type);
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
                current_rv.setRoute_long_name(routes.get(route_id).getTag("gtfs:name"));
            }

        } catch (IOException e) {
            System.err.println("Error: " + e);
        }

        return allRouteVariants;
    }

    public static HashMap<String, ArrayList<RouteVariant>> getAllRouteVariantsByRoute(HashMap<String, RouteVariant> allRouteVariants) {
        HashMap<String, ArrayList<RouteVariant>> allRouteVariantsByRoute = new HashMap<>();

        for (HashMap.Entry<String, RouteVariant> rv_entry : allRouteVariants.entrySet()) {
            RouteVariant current_rv = rv_entry.getValue();
            String route_id = current_rv.getRoute_id();

            if (allRouteVariantsByRoute.containsKey(route_id)) {
                allRouteVariantsByRoute.get(route_id).add(current_rv);
            } else {
                ArrayList rvlist = new ArrayList<>();
                rvlist.add(current_rv);
                allRouteVariantsByRoute.put(route_id, rvlist);
            }
        }
        return allRouteVariantsByRoute;
    }

    public HashMap<String, String> getGtfsTripIdToRouteVariantMap() {
        System.out.println(String.format("gtfsTripIdToRouteVariantMap content: %s", gtfsTripIdToRouteVariantMap.toString()));
        return gtfsTripIdToRouteVariantMap;
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
                String sid = OsmFormatter.getValidBusStopId(csvRecord.get(tag_defs.GTFS_STOP_ID_KEY));
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
    public HashMap<String,String> getModeTagsByBusStop(HashSet<Route> r, String public_transport_type) {
        HashMap<String,String> keys = new HashMap<String,String>();
        if (r!=null) {
            //convert from hashset to arraylist
            ArrayList<Route> routes = new ArrayList<Route>(r);
            for (Route rr : routes) {
                if (public_transport_type.equals("platform")) {
                    if (rr.containsKey(tag_defs.OSM_ROUTE_TYPE_KEY)) {
                        switch (rr.getTag(tag_defs.OSM_ROUTE_TYPE_KEY)) {
                            case "bus":
                            case "trolley_bus":
                            case "share_taxi":
                                keys.put("highway", "bus_stop");
                                break;
                            case "railway":
                            case "tram":
                            case "subway":
                            case "light_rail":
                                keys.put("railway", "paltform");
                                break;
                            default:
                                break;
                        }
                    }

                } else if (public_transport_type.equals("stop_position")) {
                    keys.put(rr.getTag(tag_defs.OSM_ROUTE_TYPE_KEY), "yes");
                } else if (public_transport_type.equals("station")) {
                    if (rr.containsKey(tag_defs.OSM_ROUTE_TYPE_KEY)) {
                        switch (rr.getTag(tag_defs.OSM_ROUTE_TYPE_KEY)) {
                            case "bus":
                                keys.put("amenity", "bus_station");
                                break;
                            case "railway":
                            case "tram":
                            case "subway":
                            case "light_rail":
                                keys.put("railway", "station");
                                break;
                            case "ferry":
                                keys.put("amenity", "ferry_terminal");
                                break;
                            default:
                                break;
                        }
                    }
                    if (rr.containsKey("railway") && rr.getTag("railway").equals("funicular")) {
                        keys.put("railway", "station");
                        keys.put("station", "funicular");
                    }
                    if (rr.containsKey("aerialway")) {
                        keys.put("aerialway", "station");
                    }
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

    public String getNetexQuayName(String gtfsId) {
        //System.out.println("getName for " + gtfsId);
        if (gtfsId.startsWith("S")) {
            if (netexSites == null) {
                return null;
            } else {
                return netexSites.get(gtfsId).getName();
            }
        } else {
            if (netexQuays == null) {
                return null;
            } else {
                return netexQuays.get(gtfsId).getName();
            }
        }
    }

    public List<String> getNetexQuayAltNames(String gtfsId) {
        //System.out.println("getAlt Name for " + gtfsId);
        if (gtfsId.startsWith("S")) {
            if (netexSites == null) {
                return null;
            } else {
                List<String> altNames = new ArrayList<String>();
                altNames.add(netexSites.get(gtfsId).getAltName());
                return altNames;
            }
        } else {
            if (netexQuays == null) {
                return null;
            } else {
                return netexQuays.get(gtfsId).getAltNames();
            }
        }
    }

    private void readNetexStopsFile(String netexFilePath) {
        try {
            File nextFile = new File(netexFilePath);
            NetexParser netexParser = new NetexParser();
            SAXParserFactory.newInstance().newSAXParser().parse(nextFile, netexParser);
            netexQuays = netexParser.getQuayList();
            netexSites = netexParser.getLogicalSiteList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

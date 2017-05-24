package com.example.bing2013.draft2_linear_acceleration;

import android.location.Location;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static android.content.ContentValues.TAG;

/**
 * Created by bing2013 on 5/23/17.
 */

public class OSM {

    List<String> fileContent = new ArrayList<>();
    Set<Integer> ids = new HashSet<>();
    Random rand = new Random(System.currentTimeMillis());

    private File file;

    public OSM(File f){
        this.file = f;
    }

    // add curb cut
    public Integer addNode(Location l){
        String lon = String.valueOf(l.getLongitude());
        String lat = String.valueOf(l.getLatitude());

        Integer id;
        do{
            id = rand.nextInt();
        }while(ids.contains(id));
        ids.add(id);
        id *= -1;

        String nodeContent = "<node id=\"" + id + "\" lon=\"" + lon + "\" lat=\"" + lat + "\" visible=\"true\"/>\n";
        fileContent.add(nodeContent);
        return id;
    }

    // add crossing
    public void addWay(Location l1, Location l2){
        Integer nodeID1 = addNode(l1);
        Integer nodeID2 = addNode(l2);

        Integer wayID;
        do{
            wayID = rand.nextInt();
        }while(ids.contains(wayID));
        ids.add(wayID);
        wayID *= -1;

        fileContent.add("<way id=\"" + wayID + "\" action=\"modify\" visible=\"true\">\n");
        fileContent.add("<nd ref=\"" + nodeID1 + "\"/>\n");
        fileContent.add("<nd ref=\"" + nodeID2 + "\"/>\n");
        fileContent.add("<tag k=\"highway\" v=\"footway\"/>\n");
        fileContent.add("<tag k=\"footway\" v=\"crossing\"/>\n");
        fileContent.add("<tag k=\"crossing\" v=\"unmarked\"/>\n");
        fileContent.add("<tag k=\"wheelchair\" v=\"yes\"/>\n");
        fileContent.add("<tag k=\"project\" v=\"OpenSidewalks\"/>\n");
        fileContent.add("</way>");
    }

    public void writePath(){
        String xmlHeader = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
        String osmHeader = "<osm version=\"0.6\" generator=\"PCApp\">\n";

        try {
            FileWriter writer = new FileWriter(file, false);
            writer.append(xmlHeader);
            writer.append(osmHeader);
            for(String s : fileContent){
                writer.append(s);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "Error Writting Path",e);
        }
    }

}

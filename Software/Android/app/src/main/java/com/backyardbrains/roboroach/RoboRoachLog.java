package com.backyardbrains.roboroach;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import static android.support.v4.app.ActivityCompat.startActivity;

/**
 * Created by Amy on 12/1/2014.
 */
public class RoboRoachLog {
    long startTime;

    private class Event{
        long timestamp;
        int accX;
        int accY;
        int accZ;
        String description;
        Event(String description, int accX, int accY, int accZ){
            timestamp = System.currentTimeMillis() - startTime;
            this.description = description;
            this.accX = accX;
            this.accY = accY;
            this.accZ = accZ;
        }
        public String toString(){
            return timestamp + "," + accX + "," + accY + "," + accZ + "," + description;
        }
    }
    private ArrayList<Event> events = new ArrayList();
    RoboRoachLog(){
        startTime = System.currentTimeMillis();
    }
    public void write(String desc){
        Event last = events.get(events.size()-1);
        events.add(new Event(desc, last.accX, last.accY, last.accZ));
    }
    public void write(Accelerometer accel){
        events.add(new Event("Acc. Chg.", accel.getX(), accel.getY(), accel.getZ()));
    }
    public String toString() {
        String columnString = "\"Timestamp\",\"Accelerometer X\",\"Accelerometer Y\",\"Accelerometer Z\",\"Description\"\n";
        String data = columnString;
        for (Event e : events) {
            data += e.toString();
            data += "\n";
        }
        return data;
    }


}

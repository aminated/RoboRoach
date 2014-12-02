package com.backyardbrains.roboroach;

import com.androidplot.xy.XYSeries;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

import java.util.ArrayList;
import java.util.LinkedList;

import java.util.List;
import java.util.Observable;
import java.util.Observer;


/**
 * Created by Amy on 11/13/2014.
 */

public class Accelerometer {
   // private double time_increment = 1;
   // private double time = 0;
    private int x = 0;
    private int z = 0;
    private ArrayList<Integer> xList = new ArrayList();
    private ArrayList<Integer> yList = new ArrayList();
    private ArrayList<Integer> zList = new ArrayList();

    {
        for(int i=0;i<10;i++){
            xList.add(0); yList.add(0); zList.add(0);
        }
    }
    public int getX() { return x;}
    public int getY() { return y;}
    public int getY(int index) {
        int val = 0;

        try{
            val = yList.get(index);
        }
        catch(Exception ex){
            while(yList.size() < 10)
                yList.add(y);
        }
        return val;
    }

    public int getX(int index) {
        int val = 0;

        try{
            val = xList.get(index);
        }
        catch(Exception ex){
            while(xList.size() < 10)
                xList.add(x);
        }
        return val;
    }
    public int getZ() { return z;}
    public int getZ(int index) {
        int val = 0;

        try{
            val = zList.get(index);
        }
        catch(Exception ex){
            while(zList.size() < 10)
                zList.add(z);
        }
        return val;
    }
    private int y =0;

    private RoboRoachActivity parent;
    public void pushX(int x){
       //xSeries.appendData(new GraphView.GraphViewData(time,(double)x), false, 100);
        if(x>= 128) x = x-255;
        this.x = x;
        shiftValues();
        parent.onAccelUpdated();
    }
    public void pushY(int y){
       // ySeries.appendData(new GraphView.GraphViewData(time,(double)y), false, 100);
        if(y>= 128) y = y-255;
        this.y = y;
        shiftValues();
        parent.onAccelUpdated();
    }
    public void pushZ(int z){
        if(z>= 128) z = z-255;
        this.z =z;
    }
    public void shiftValues(){
        yList.remove(0);
        while(yList.size() < 10)
            yList.add(y);

        xList.remove(0);
        while(xList.size() < 10)
            xList.add(x);

        zList.remove(0);
        while(zList.size() < 10)
            zList.add(z);
    }
    public Accelerometer( RoboRoachActivity parent){

        this.parent = parent;
    }
}

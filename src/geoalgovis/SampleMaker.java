/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis;

import java.awt.Point;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author s159102
 */
public class SampleMaker {
    
    int minNumberOfCornersRegion = 4;    // Minimal number of corners per region
    int maxNumberOfCornersRegion = 4;    // Maximum number of corners per region
    
    int spread;
    
    int minSizeRegion;             // Minimum region size
    int maxSizeRegion;             // Maximum region size

    int minRadiusCircle;             // Minimum radius circles
    int maxRadiusCircle;           // Maximum radius circles
    
    boolean allowOverlap;

    ArrayList<int[]> filled;
    
    public void createSamples(int numberOfSamples, int numberOfRegions, int spread, int minSizeRegion, int maxSizeRegion, int minRadiusCircle, int maxRadiusCircle, boolean allowOverlap, String sampleFolder){
        this.spread = spread;
        this.minSizeRegion = minSizeRegion;
        this.maxSizeRegion = maxSizeRegion;
        this.minRadiusCircle = minRadiusCircle;
        this.maxRadiusCircle = maxRadiusCircle;
        this.allowOverlap = allowOverlap;
        
        for (int i = 1; i <= numberOfSamples; i++) { 
            filled = new ArrayList();
            try {
                int indexSample = i; // Number of sample

                // Create new file with unused index
                File newFile = new File(sampleFolder+"generatedSample"+indexSample+".txt");
                while(!newFile.createNewFile()) {
                    indexSample++;
                    newFile = new File(sampleFolder+"generatedSample"+indexSample+".txt");
                }
                System.out.println(sampleFolder+"generatedSample"+indexSample+".txt generated");

                // Create random sample
                try (FileWriter writer = new FileWriter(sampleFolder+"generatedSample"+indexSample+".txt")) {
                    for (int j = 0; j < numberOfRegions; j++) {
                        String regionLine = randomRegionLine();
                        String circleLine = randomCircleLine(regionLine);
                        writer.write(circleLine+"\n"+regionLine+"\n");
                    }
                    System.out.println("Successfully generated sample.");
                }

            } catch (IOException e) {
                System.out.println("An error occurred.");
            }
        }
    }
    
    // Creates random area text line in the form: 'x_1, y_1, .... , x_n, y_n'
    private String randomRegionLine(){
        boolean overlap = true;
        String toReturn = "";
        int timesToTry = 10000;
        
        int moveX = (int)Math.round(Math.random()*spread);
        int moveY = (int)Math.round(Math.random()*spread);
        int xMax = 0;
        int yMax = 0;
        int xMin = 0;
        int yMin = 0;
        
        while(overlap && timesToTry > 0){
            moveX = (int)Math.round(Math.random()*spread);
            moveY = (int)Math.round(Math.random()*spread);
            xMin = 999999;
            yMin = 999999;
            xMax = -999999;
            yMax = -999999;
            toReturn = "";
            
            int numberOfPoints = (int)Math.round(Math.random()*(maxNumberOfCornersRegion-minNumberOfCornersRegion))+minNumberOfCornersRegion;
            for (int i = 0; i < numberOfPoints; i++) {
                int x = (int) ((int)Math.round((Math.random()*(maxSizeRegion-minSizeRegion))))+minSizeRegion+moveX;
                int y = (int) ((int)Math.round((Math.random()*(maxSizeRegion-minSizeRegion))))+minSizeRegion+moveY;
                toReturn = toReturn + x + "\t" + y + "\t";

                if (x>xMax){ xMax = x; }
                if (y>yMax){ yMax = y; }
                if (x<xMin){ xMin = x; }
                if (y<yMin){ yMin = y; }
            }
            
            if (!overlapping(xMin, xMax, yMin, yMax) || allowOverlap){
                overlap = false;
            } else {
                timesToTry--;
                if (timesToTry == 0){
                    System.out.println("Overlapping! But that is not allowed! No free spot can be found, change parameters!");
                    System.exit(1);
                }
            }
        }

        filled.add(new int[]{xMin, xMax, yMin, yMax});

        toReturn = toReturn.substring(0, toReturn.length() - 1);
        return toReturn;
    }
    
    private boolean overlapping(int xMin, int xMax, int yMin, int yMax){
        for(int[] regio : filled){
            if (doOverlap(xMin, yMax, xMax, yMin, regio[0], regio[3], regio[1], regio[2])){
                return true;
            }
        }
        return false;
    }
    
    boolean doOverlap(int l1x, int l1y, int r1x, int r1y, int l2x, int l2y, int r2x, int r2y) { 
        // If one rectangle is on left side of other  
        if (l1x >= r2x || l2x >= r1x) { 
            return false; 
        } 
  
        // If one rectangle is above other  
        if (l1y <= r2y || l2y <= r1y) { 
            return false; 
        } 
  
        return true; 
    } 
    
    // Creates random circle text line in the form: 'Label  xCenter yCenter radius 1'
    private String randomCircleLine(String regionLine){
        String[] arrayPoints = regionLine.split("\t");

        int Xsum = 0;
        int Ysum = 0;
        for (int i = 0;i <= (arrayPoints.length /2) - 1;i++){
            Xsum += Integer.parseInt(arrayPoints[i*2]);
            Ysum += Integer.parseInt(arrayPoints[(i*2)+1]);
        }
        
        int radius = (int)Math.round(Math.random()*(maxRadiusCircle-minRadiusCircle))+minRadiusCircle;
        int x = (int)Xsum/(arrayPoints.length /2);
        int y = (int)Ysum/(arrayPoints.length /2);
        return getAlphaNumericString(3)+"\t"+x+".0\t"+y+".0\t"+radius+"\t1";
    }
    
    // function to generate a random string of length n 
    private String getAlphaNumericString(int n) 
    { 
  
        // chose a Character random from this String 
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  
        // create StringBuffer size of AlphaNumericString 
        StringBuilder sb = new StringBuilder(n); 
  
        for (int i = 0; i < n; i++) { 
  
            // generate a random number between 
            // 0 to AlphaNumericString variable length 
            int index 
                = (int)(AlphaNumericString.length() 
                        * Math.random()); 
  
            // add Character one by one in end of sb 
            sb.append(AlphaNumericString 
                          .charAt(index)); 
        } 
  
        return sb.toString(); 
    } 
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author s159102
 */
public class SampleMaker {
        
    String sampleFolder = "C:\\Project-Algorithms-for-geographic-data\\inputs\\generated\\";
    int numberOfSamples = 50;            // Number of samples to generate
    
    int minNumberOfRegions = 50;         // Minimal number of regions to generate
    int maxNumberOfRegions = 50;        // Maximum number of regions to generate
    
    int horizontalSpread = 2000;         // Larger number means less overlapping
    int verticalSpread = 2000;           // Larger number means less overlapping
    
    int minNumberOfCornersRegion = 4;    // Minimal number of corners per region
    int maxNumberOfCornersRegion = 4;    // Maximum number of corners per region
    
    int minSizeRegion = 100;              // Minimum region size
    int maxSizeRegion = 700;             // Maximum region size
    
    int minRadiusCircle = 0;             // Minimum radius circles
    int maxRadiusCircle = 100;           // Maximum radius circles
    
    boolean allowOverlap = false;        // Allow overlapping regions
    
    ArrayList<int[]> filled;
    
    public void createSamples(){
        
        
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
                    int numberOfRegions = (int)Math.round(Math.random()*(maxNumberOfRegions-minNumberOfRegions))+minNumberOfRegions;
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
        int timesToTry = 100;
        
        int moveX = (int)Math.round(Math.random()*horizontalSpread);
        int moveY = (int)Math.round(Math.random()*verticalSpread);
        int x = 0;
        int y = 0;
        int xRange = 0;
        int yRange = 0;
        
        while(overlap && timesToTry > 0){
            moveX = (int)Math.round(Math.random()*horizontalSpread);
            moveY = (int)Math.round(Math.random()*verticalSpread);
            x = 0;
            y = 0;
            xRange = 0;
            yRange = 0;
            toReturn = "";
            
            int numberOfPoints = (int)Math.round(Math.random()*(maxNumberOfCornersRegion-minNumberOfCornersRegion))+minNumberOfCornersRegion;
            for (int i = 0; i < numberOfPoints; i++) {
                x = (int) ((int)Math.round((Math.random()*(maxSizeRegion-minSizeRegion))))+minSizeRegion+moveX;
                y = (int) ((int)Math.round((Math.random()*(maxSizeRegion-minSizeRegion))))+minSizeRegion+moveY;
                toReturn = toReturn + x + "\t" + y + "\t";

                if (x>xRange){ xRange = x; }
                if (y>yRange){ yRange = y; }
            }
            
            if (!overlapping(x, xRange, y, yRange) || allowOverlap){
                overlap = false;
            } else {
                timesToTry--;
                if (timesToTry == 0){
                    System.out.println("Overlapping! But that is not allowed! No free spot can be found, change parameters!");
                    System.exit(1);
                }
            }
        }

        filled.add(new int[]{moveX, xRange, moveY, yRange});

        toReturn = toReturn.substring(0, toReturn.length() - 1);
        return toReturn;
    }
    
    private boolean overlapping(int x, int xRange, int y, int yRange){
        for(int[] regio : filled){
            if ((x >= regio[0] && x <= regio[1]) || (xRange >= regio[0] && xRange <= regio[1])){
                if ((y >= regio[0] && y <= regio[3]) || (yRange >= regio[2] && yRange <= regio[3])){
                    return true;
                }
            }
        }
        return false;
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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * This file generates random samples in the given folder
 */
public class SampleGenerator {
    public static void main(String[] args) {
        int numberOfSamples = 1000;
        
        String sampleFolder = "C:\\Project-Algorithms-for-geographic-data\\inputs\\generated\\"; // Folder for samples
        
        for (int i = 1; i <= numberOfSamples; i++) { 
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
                    int numberOfRegions = (int)Math.round(Math.random()*49)+1;
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
    static String randomRegionLine(){
        String toReturn = "";
        
        int moveX = (int)Math.round(Math.random()*100);
        int moveY = (int)Math.round(Math.random()*100);
        int numberOfPoints = (int)Math.round(Math.random()*4)+4;
        for (int i = 0; i < numberOfPoints; i++) {
            int x = (int) ((int)Math.round((Math.random()*49)-25))+moveX;
            int y = (int) ((int)Math.round((Math.random()*49)-25))+moveY;
            toReturn = toReturn + x + "\t" + y + "\t";
        }
        toReturn = toReturn.substring(0, toReturn.length() - 1);
        return toReturn;
    }
    
    // Creates random circle text line in the form: 'Label  xCenter yCenter radius 1'
    static String randomCircleLine(String regionLine){
        String[] arrayPoints = regionLine.split("\t");

        int Xsum = 0;
        int Ysum = 0;
        for (int i = 0;i <= (arrayPoints.length /2) - 1;i++){
            Xsum += Integer.parseInt(arrayPoints[i*2]);
            Ysum += Integer.parseInt(arrayPoints[(i*2)+1]);
        }
        
        int radius = (int)Math.round(Math.random()*25);
        int x = (int)Xsum/(arrayPoints.length /2);
        int y = (int)Ysum/(arrayPoints.length /2);
        return getAlphaNumericString(3)+"\t"+x+".0\t"+y+".0\t"+radius+"\t1";
    }
    
    // function to generate a random string of length n 
    static String getAlphaNumericString(int n) 
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

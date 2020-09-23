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
        
        String sampleFolder = "C:\\Users\\Mart\\Downloads\\Project-Algorithms-for-geographic-data\\inputs\\generated\\"; // Folder for samples
        
        for (int i = 1; i <= 100; i++) { 
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
                        int x = (int)Math.round(Math.random()*50);
                        int y = (int)Math.round(Math.random()*50);
                        writer.write(randomCircleLine(x,y)+"\n"+randomRegionLine(x,y)+"\n");
                    }
                    System.out.println("Successfully generated sample.");
                }

            } catch (IOException e) {
                System.out.println("An error occurred.");
            }
        }
        
    }
    
    // Creates random circle text line in the form: 'Label  xCenter yCenter radius 1'
    static String randomCircleLine(int x, int y){
        int radius = (int)Math.round(Math.random()*10);
        return getAlphaNumericString(3)+"	"+x+".0	"+y+".0	"+radius+"	1";
    }
    
    // Creates random area text line in the form: 'x_1, y_1, .... , x_n, y_n'
    static String randomRegionLine(int xBase, int yBase){
        String toReturn = "";
            
        int numberOfPoints = (int)Math.round(Math.random()*4)+1;
        for (int i = 0; i < 4; i++) {
            int x = (int) ((int)xBase + Math.round((Math.random()*49)-25));
            int y = (int) ((int)yBase + Math.round((Math.random()*49)-25));
            toReturn = toReturn + x + "\t" + y + "\t";
        }
        toReturn = toReturn.substring(0, toReturn.length() - 1);
        return toReturn;
        //return "5.0	5.0	10.0	5.0	10.0	10.0	5.0	10.0";
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

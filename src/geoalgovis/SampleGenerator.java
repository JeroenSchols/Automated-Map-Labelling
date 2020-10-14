/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis;

import java.io.File;

/**
 *
 * This file generates random samples in the given folder
 */

public class SampleGenerator {
    static SampleMaker maker = new SampleMaker();
    
    public static void main(String[] args) {
        
        int numberOfSamples = 100;
        int minRadiusCircle = 200;
        int maxRadiusCircle = 500;  
        int numberOfRegions = 50;
        int minSizeRegion = 100;
        int maxSizeRegion = 700;
        String sampleFolder = "C:\\Users\\Mart\\Downloads\\Project-Algorithms-for-geographic-data\\inputs\\generated\\Large spread & no overlap\\";
        
        
        int spread = 4000;
        boolean allowOverlap = false;
        maker.createSamples(numberOfSamples, numberOfRegions, spread, minSizeRegion, maxSizeRegion, minRadiusCircle, maxRadiusCircle, allowOverlap, sampleFolder);
        
        
        sampleFolder = "C:\\Users\\Mart\\Downloads\\Project-Algorithms-for-geographic-data\\inputs\\generated\\Large spread & with overlap\\";
        allowOverlap = true;
        maker.createSamples(numberOfSamples, numberOfRegions, spread, minSizeRegion, maxSizeRegion, minRadiusCircle, maxRadiusCircle, allowOverlap, sampleFolder);
        
        
        sampleFolder = "C:\\Users\\Mart\\Downloads\\Project-Algorithms-for-geographic-data\\inputs\\generated\\Small spread & with overlap\\";
        spread = 2000;
        allowOverlap = true;
        maker.createSamples(numberOfSamples, numberOfRegions, spread, minSizeRegion, maxSizeRegion, minRadiusCircle, maxRadiusCircle, allowOverlap, sampleFolder);    
        
        sampleFolder = "C:\\Users\\Mart\\Downloads\\Project-Algorithms-for-geographic-data\\inputs\\generated\\Small spread & no overlap\\";
        spread = 2500;
        allowOverlap = false;
        maker.createSamples(numberOfSamples, numberOfRegions, spread, minSizeRegion, maxSizeRegion, minRadiusCircle, maxRadiusCircle, allowOverlap, sampleFolder);   
    }
}


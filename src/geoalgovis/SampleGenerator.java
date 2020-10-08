/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis;

/**
 *
 * This file generates random samples in the given folder
 */

public class SampleGenerator {
    public static void main(String[] args) {
        SampleMaker maker = new SampleMaker();
        maker.createSamples();
    }
}


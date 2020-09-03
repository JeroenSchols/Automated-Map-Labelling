/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis.symbolplacement;

import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.Polygon;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class Symbol extends Circle {

    private Region region;

    public Symbol(Region region, Vector center, double radius) {
        super(center, radius);
        this.region = region;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public double distanceToRegion() {
        return region.distanceToRegion(getCenter());
    }
    
    public Vector vectorToRegion() {
        return region.vectorToRegion(getCenter());
    }

}

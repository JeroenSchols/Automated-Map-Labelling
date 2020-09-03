/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis.symbolplacement;

import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.mix.GeometryGroup;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class Region extends GeometryGroup<Polygon> {

    private String name = "";
    private double weight = 1;
    private int index;
    private Vector anchor = Vector.origin();

    public String getName() {
        return name;
    }

    public Vector getAnchor() {
        return anchor;
    }

    public void setAnchor(Vector anchor) {
        this.anchor = anchor;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public double distanceToRegion(Vector v) {
        Vector vec = vectorToRegion(v);
        if (vec == null) {
            return 0;
        }
        return vec.length();
    }

    public Vector vectorToRegion(Vector v) {
        boolean inside = false;
        Vector mindist = null;
        for (Polygon p : getParts()) {
            if (p.contains(v)) {
                inside = !inside;
            }
            Vector d = p.closestPoint(v).clone();
            d.translate(-v.getX(), -v.getY());
            if (mindist == null || d.squaredLength() < mindist.squaredLength()) {
                mindist = d;
            }
        }
        if (inside) {
            return null;
        } else {
            return mindist;
        }

    }

}

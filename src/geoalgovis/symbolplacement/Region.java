/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis.symbolplacement;

import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.mix.GeometryGroup;

import java.util.*;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class Region extends GeometryGroup<Polygon> {

    private String name = "";
    private double weight = 1;
    private int index;
    private Vector anchor = Vector.origin();
    private List<Vector> extremaAnchors = null;
    private List<Vector> sampleAnchors = null;
    private List<Vector> allAnchors = null;
    private Double min_x = null;
    private Double min_y = null;
    private Double max_x = null;
    private Double max_y = null;
    private Double x_step = null;
    private Double y_step = null;

    public String getName() {
        return name;
    }

    public Vector getAnchor() {
        return anchor;
    }

    public void setAnchor(Vector anchor) {
        if (this.allAnchors != null) {
            if (!this.extremaAnchors.contains(this.anchor) && !this.sampleAnchors.contains(this.anchor)) this.allAnchors.remove(this.anchor);
            this.anchor = anchor;
            this.allAnchors.add(this.anchor);
        } else {
            this.anchor = anchor;
        }
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

    public List<Vector> getExtremaAnchors() {
        if (this.extremaAnchors != null) return this.extremaAnchors;

        List<Vector> boundaryVectors = new ArrayList<>();
        for (Polygon part : this.getParts()) boundaryVectors.addAll(part.vertices());

        Vector min_x = boundaryVectors.get(0);
        Vector max_x = boundaryVectors.get(0);
        Vector min_y = boundaryVectors.get(0);
        Vector max_y = boundaryVectors.get(0);
        for (Vector v : boundaryVectors) {
            if (min_x.getX() > v.getX()) min_x = v;
            if (max_x.getX() < v.getX()) max_x = v;
            if (min_y.getY() > v.getY()) min_y = v;
            if (max_y.getY() < v.getY()) max_y = v;
        }

        this.extremaAnchors = new ArrayList<>();
        this.extremaAnchors.add(min_x);
        this.extremaAnchors.add(max_x);
        this.extremaAnchors.add(min_y);
        this.extremaAnchors.add(max_y);
        this.min_x = min_x.getX();
        this.max_x = max_x.getX();
        this.min_y = min_y.getY();
        this.max_y = max_y.getY();
        this.x_step = 0.2 * (this.max_x - this.min_x);
        this.y_step = 0.2 * (this.max_y - this.min_y);

        return this.extremaAnchors;
    }

    public List<Vector> getSampleAnchors() {
        if (this.sampleAnchors != null) return this.sampleAnchors;
        if (this.extremaAnchors == null) this.getExtremaAnchors();

        this.sampleAnchors = new ArrayList<>();
        for (double x = this.min_x; x <= this.max_x; x += x_step) {
            for (double y = this.min_y; y <= this.max_y; y += y_step) {
                Vector vector = new Vector(x, y);
                if (this.distanceToRegion(vector) == 0) sampleAnchors.add(vector);
            }
        }

        return this.sampleAnchors;
    }

    public List<Vector> getAllAnchors() {
        if (this.allAnchors != null) return this.allAnchors;
        if (this.sampleAnchors == null) this.getSampleAnchors();
        this.allAnchors = new ArrayList<>();
        this.allAnchors.addAll(this.extremaAnchors);
        this.allAnchors.addAll(this.sampleAnchors);
        this.allAnchors.add(this.anchor);
        return this.allAnchors;
    }

}

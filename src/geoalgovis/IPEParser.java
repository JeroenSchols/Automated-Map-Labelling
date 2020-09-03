/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis;

import geoalgovis.symbolplacement.Input;
import geoalgovis.symbolplacement.Region;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.GeometryType;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.mix.GeometryGroup;
import nl.tue.geometrycore.io.ReadItem;
import nl.tue.geometrycore.io.ipe.IPEReader;
import nl.tue.geometrycore.util.DoubleUtil;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class IPEParser {

    public static void main(String[] args) throws IOException {

        IPEReader read = IPEReader.clipboardReader();
        List<ReadItem> items = read.read();
        Input input = new Input();
        input.regions = new ArrayList();
        mainloop:
        for (ReadItem i : items) {
            BaseGeometry g = i.toGeometry();
            switch (g.getGeometryType()) {
                case POLYGON: {
                    Region region = new Region();
                    region.setName("NONAME");
                    region.setWeight(1);
                    region.getParts().add((Polygon) g);
                    region.setIndex(input.regions.size());
                    input.regions.add(region);
                    break;
                }
                case GEOMETRYGROUP: {
                    Region region = new Region();
                    region.setName("NONAME");
                    region.setWeight(1);
                    GeometryGroup<? extends BaseGeometry> grp = (GeometryGroup) g;
                    for (BaseGeometry part : grp.getParts()) {
                        if (part.getGeometryType() != GeometryType.POLYGON) {
                            System.out.println("ERR: group that does not consist of polygons? " + part.getGeometryType());
                            continue mainloop;
                        } else {
                            region.getParts().add((Polygon) part);
                        }
                    }
                    region.setIndex(input.regions.size());
                    input.regions.add(region);
                    break;
                }
                case CIRCLE:
                case VECTOR: {
                    // skip for now
                    break;
                }
                default:
                    System.out.println("ERR: item that is not a polygon, group, or composed path? " + g.getGeometryType());
                    break;
            }
        }
        for (ReadItem i : items) {
            BaseGeometry g = i.toGeometry();
            switch (g.getGeometryType()) {
                case POLYGON:
                case GEOMETRYGROUP: {
                    // skip for now
                    break;
                }
                case CIRCLE: {
                    Circle c = (Circle) g;
                    for (Region r : input.regions) {
                        if (r.distanceToRegion(c.getCenter()) < DoubleUtil.EPS) {
                            r.setWeight(c.getRadius());
                            break;
                        }
                    }
                    break;
                }
                case VECTOR: {
                    // skip for now
                    Vector v = (Vector) g;
                    for (Region r : input.regions) {
                        if (r.distanceToRegion(v) < DoubleUtil.EPS) {
                            r.setName(i.getString());
                            r.setAnchor(v);
                            break;
                        }
                    }
                    break;
                }
                default:
                    System.out.println("ERR: item that is not a polygon, group, or composed path? " + g.getGeometryType());
                    break;
            }
        }
        System.out.println("Found "+input.regions.size()+" regions");
        System.out.println("Copy everything below the line as a template for an input file");
        System.out.println("--------------------------------------------------------------");
        System.out.println(input.writeProblem());

    }
}

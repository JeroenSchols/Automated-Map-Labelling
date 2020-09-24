package geoalgovis.algorithms;

import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.Vector;
import geoalgovis.symbolplacement.Input;
import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;
import geoalgovis.symbolplacement.SymbolPlacementAlgorithm;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.HalfLine;
import nl.tue.geometrycore.geometry.linear.Polygon;

import java.util.*;

public class CenterSpreadAlgorithm extends SymbolPlacementAlgorithm {

    private Random random = new Random(0);

    /*
     * For a given point (default the average center of all symbols) place all symbols
     * as close as possible to this point while not moving passed another obstacle.
     * Symbols must be placed on the line between the current position and the center
     * point. Placement occurs in order of  least distance to the center point.
     * Only avoid already re-placed symbols.
     *
     * The main idea is to remove overlap while having minimum displacements occuring.
     * Approach is intended to be used after a good yet somewhat overlapping alignment
     * is found.
     */

    @Override
    public Output doAlgorithm(Input input) {
        Output output = new Output(input);
        centerSpread(output.symbols, Util.getAvgCenter(output.symbols));
        return output;
    }

    /**
     * realigns all points multiple times around points in a central area
     * the central area is avgAnchor + [(-delta_x, -delta_y), (delta_x, delta_y)]
     * where delta_x = (max_x - min_x) * centersize / 2 (similar for delta_y
     *
     * @param symbols the symbols to be aligned
     * @param centersize the relative size of the central area
     * @param iter the number of iterations the central area is aligned on
     */
    void centerAreaSpread(List<Symbol> symbols, Double centersize, Integer iter) {
        if (centersize == null) centersize = 0.05;
        if (iter == null) iter = 100;

        double min_x = Float.MAX_VALUE;
        double max_x = -Float.MAX_VALUE;
        double min_y = Float.MAX_VALUE;
        double max_y = -Float.MAX_VALUE;
        for (Symbol s : symbols) {
            for (Polygon p : s.getRegion().getParts()) {
                for (Vector v : p.vertices()) {
                    min_x = Math.min(min_x, v.getX());
                    max_x = Math.max(max_x, v.getX());
                    min_y = Math.min(min_y, v.getY());
                    max_y = Math.max(max_y, v.getY());
                }
            }
        }

        double delta_x = (max_x - min_x) * centersize;
        double delta_y = (max_y - min_y) * centersize;

        Vector avgAnchor = Util.getAvgAnchor(symbols);
        centerSpread(symbols, avgAnchor);
        for (int i = 1; i < iter; i++) {
            Vector deltaAnchor = Vector.add(avgAnchor, new Vector(delta_x * (random.nextDouble() - 0.5), delta_y * (random.nextDouble() - 0.5)));
            centerSpread(symbols, deltaAnchor);
        }

    }


    /**
     * re-align symbols around the center point
     *
     * @param symbols the symbols to be aligned
     * @param center the center point to be aligned to
     */
    void centerSpread(List<Symbol> symbols, Vector center) {
        Util.sortAroundPoint(symbols, center); // sort on increasing distance to the center point
        ArrayList<Symbol> placed = new ArrayList<>(); // list of placed symbols

        for (Symbol current : symbols) {
            HalfLine ray = HalfLine.byThroughpoint(center, current.getCenter()); // ray from center point to symbol center
            Vector furthestIntersect = null; // the intersection of any circle on ray with maximum distance
            double max_dist = Float.MIN_VALUE;
            for (Symbol s : placed) {
                List<BaseGeometry> intersect = (new Circle(s.getCenter(), current.getRadius() + s.getRadius())).intersect(ray);
                for (BaseGeometry geom : intersect) {
                    Vector sect = (Vector) geom;
                    double dist = sect.distanceTo(center);
                    if (dist > max_dist) {
                        max_dist = dist;
                        furthestIntersect = sect;
                    }
                }
            }
            if (furthestIntersect != null) current.getCenter().set(furthestIntersect); // place symbol at furthest intersection
            placed.add(current);
        }
    }
}

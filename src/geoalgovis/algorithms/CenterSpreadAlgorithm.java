package geoalgovis.algorithms;

import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.Vector;
import geoalgovis.symbolplacement.Input;
import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;
import geoalgovis.symbolplacement.SymbolPlacementAlgorithm;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.HalfLine;

import java.util.*;

public class CenterSpreadAlgorithm extends SymbolPlacementAlgorithm {

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
     * re-align symbols around the center point
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

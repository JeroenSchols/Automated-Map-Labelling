package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.util.Pair;

import java.util.*;

class Util {

    /**
     * place all points far away from all regions to start with a clean slate
     */
    static Output placeAway(Output output) {
        Rectangle boundingBox = output.getBoundingBox();
        double x = boundingBox.width() + boundingBox.getRight();
        double y = boundingBox.height() + boundingBox.getTop();
        for (Symbol s : output.symbols) {
            s.getCenter().set(x + s.getRadius(), y);
            x += 2*s.getRadius();
        }
        return output;
    }

    /**
     * get the average anchor point
     */
    static Vector getAvgAnchor(List<Symbol> symbols) {
        Vector avg_anchor = new Vector(0,0);
        for (Symbol s : symbols) avg_anchor = Vector.add(avg_anchor, s.getRegion().getAnchor());
        return Vector.divide(avg_anchor, symbols.size());
    }

    /**
     * get the average center point
     */
    static Vector getAvgCenter(List<Symbol> symbols) {
        Vector avg_center = new Vector(0,0);
        for (Symbol s : symbols) avg_center = Vector.add(avg_center, s.getCenter());
        return Vector.divide(avg_center, symbols.size());
    }

    /**
     * sort symbols on distance around a point
     */
    static void sortAroundPoint(List<Symbol> symbols, Vector point) {
        symbols.sort((s1, s2) -> (int) (s1.getCenter().distanceTo(point) - s2.getCenter().distanceTo(point)));
    }

    /**
     * gives a minor deplacement to a symbol when it has a center equal to another symbol
     */
    static void removeOverlappingCenters(List<Symbol> symbols) {
        boolean valid;
        do {
            valid = true;
            outer: for (Symbol s1 : symbols) {
                for (Symbol s2 : symbols) {
                    if (s1.hashCode() >= s2.hashCode()) continue;
                    if (s1.getCenter().distanceTo(s2.getCenter()) == 0) {
                        s1.getCenter().translate(0.0001, 0.0001);
                        valid = false;
                        break outer;
                    }
                }
            }
        } while(!valid);
    }

    /**
     * check whether two symbols overlap
     */
    static boolean checkOverlap(Symbol a, Symbol b) {
        return a.getRadius() + b.getRadius() - a.getCenter().distanceTo(b.getCenter()) > 0;
    }

    /**
     * calculate the amount of overlap
     */
    static double calcOverlap(Symbol a, Symbol b) {
        return a.getRadius() + b.getRadius() - a.getCenter().distanceTo(b.getCenter());
    }

    /**
     * checks whether a symbols has any overlap
     */
    static boolean hasOverlap(Symbol current, List<Symbol> symbols) {
        for (Symbol s : symbols) {
            if (s == current) continue;
            if (checkOverlap(current, s)) return true;
        }
        return false;
    }

    /**
     * sort a region on a direction
     */
    static List<Pair<Symbol, Vector>> sortRegionDir(List<Symbol> symbols, RegionSortDirection dir, Vector origin) {
        if (origin == null) origin = getAvgAnchor(symbols); // pick as default the average anchor as origin
        ArrayList<Pair<Symbol, Vector>> symbolsValuated = new ArrayList<>();

        // set weights to coordinates dependent on direction
        double x = 0;
        double y = 0;
        switch (dir) {
            case North:         x = 0; y = 1; break;
            case NorthEast:     x = 1; y = 1; break;
            case East:          x = 1; y = 0; break;
            case SouthEast:     x = 1; y = -1; break;
            case South:         x = 0; y = -1; break;
            case SouthWest:     x = -1; y = -1; break;
            case West:          x = -1; y = 0; break;
            case NorthWest:     x = -1; y = 1; break;
        }
        double finalX = x;
        double finalY = y;

        for (Symbol s : symbols) {
            List<Vector> vertices = new ArrayList<>();
            for (Polygon a : s.getRegion().getParts()) vertices.addAll(a.vertices());
            Vector relBest = Vector.subtract(vertices.get(0), origin);
            for (Vector vertex : vertices) {
                Vector relVertex = Vector.subtract(vertex, origin);
                if (x * relVertex.getX() + y * relVertex.getY() > x * relBest.getX() + y * relBest.getY()) relBest = relVertex;
            }
            Vector best = Vector.add(relBest, origin);
            symbolsValuated.add(new Pair<>(s, best));
        }

        symbolsValuated.sort(Comparator.comparingDouble(s -> - finalX * s.getSecond().getX() - finalY * s.getSecond().getY()));
        return symbolsValuated;
    }

    enum RegionSortDirection {
        North,
        NorthEast,
        East,
        SouthEast,
        South,
        SouthWest,
        West,
        NorthWest
    }

    enum MirrorDirection {
        None,
        X,
        Y,
        XY
    }

    enum CandidateGoals {
        Anchor,
        Sample,
        Extrema,
        All
    }
}

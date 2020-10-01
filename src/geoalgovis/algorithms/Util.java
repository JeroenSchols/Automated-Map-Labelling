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
        symbols.sort(Comparator.comparingDouble(s -> s.getCenter().distanceTo(point)));
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

    /**
     * partition the symbol set into hor x ver boxes of even size
     */
    static Pair<ArrayList<Symbol>, Pair<Pair<Double, Double>, Pair<Double, Double>>>[][] partition(List<Symbol> symbols, int hor, int ver) {
        double min_x = Float.MAX_VALUE;
        double max_x = -Float.MAX_VALUE;
        double min_y = Float.MAX_VALUE;
        double max_y = -Float.MAX_VALUE;
        for (Symbol s : symbols) {
            min_x = Math.min(min_x, s.getCenter().getX());
            max_x = Math.max(max_x, s.getCenter().getX());
            min_y = Math.min(min_y, s.getCenter().getY());
            max_y = Math.max(max_y, s.getCenter().getY());
        }

        double delta_x = (max_x - min_x) / hor;
        double delta_y = (max_y - min_y) / ver;

        Pair<ArrayList<Symbol>, Pair<Pair<Double, Double>, Pair<Double, Double>>>[][] partitions = new Pair[hor][ver];
        Pair[][] boundaries = new Pair[hor][ver];

        for (int i = 0; i < hor; i++){
            for (int j = 0; j < ver; j++) {
                ArrayList<Symbol> partsymbols = new ArrayList<>();
                Pair<Pair<Double, Double>, Pair<Double, Double>> range = new Pair<>(new Pair<>(min_x + i * delta_x, min_y + j * delta_y), new Pair<>(min_x + (i+1) * delta_x, min_y + (j+1) * delta_y));
                partitions[i][j] = new Pair<>(partsymbols, range);
            }
        }

        for (Symbol s : symbols) {
            double x = s.getRegion().getAnchor().getX();
            double y = s.getRegion().getAnchor().getY();
            for (int i = 1; i <= hor; i++) {
                if (x - min_x <= i * delta_x) {
                    for (int j = 1; j <= ver; j++) {
                        if (y - min_y <= j * delta_y) {
                            partitions[i-1][j-1].getFirst().add(s);
                            break;
                        }
                    }
                    break;
                }
            }
        }

        return partitions;
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

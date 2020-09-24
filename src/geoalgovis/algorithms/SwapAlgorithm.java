package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Region;
import geoalgovis.symbolplacement.Symbol;
import nl.tue.geometrycore.geometry.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

class SwapAlgorithm {

    /**
     * swaps all symbols of equal size when this improves the score
     *
     * @param symbols the symbols to be swapped
     */
    void swap_equal_size(Collection<Symbol> symbols) {
        HashMap<Double, Collection<Symbol>> groups = new HashMap<>();
        for (Symbol s : symbols) {
            if (!groups.containsKey(s.getRadius())) groups.put(s.getRadius(), new ArrayList<>());
            groups.get(s.getRadius()).add(s);
        }

        for (Collection<Symbol> group : groups.values()) {
            if (group.size() == 1) continue;
            boolean improved;
            do {
                improved = false;
                for (Symbol s1 : group) {
                    for (Symbol s2 : group) {
                        if (s1.hashCode() >= s2.hashCode()) continue;
                        if (Math.pow(s1.distanceToRegion(), 2) + Math.pow(s2.distanceToRegion(), 2) > Math.pow(s1.getRegion().distanceToRegion(s2.getCenter()), 2) + Math.pow(s2.getRegion().distanceToRegion(s1.getCenter()), 2)) {
                            Vector c = s1.getCenter().clone();
                            s1.setCenter(s2.getCenter());
                            s2.setCenter(c);
                            improved = true;
                        }
                    }
                }
            } while (improved);
        }
    }

    /**
     * swaps all symbols when this layout remains valid and improves the score
     *
     * @param symbols the symbols to be swapped
     */
    void swap(Collection<Symbol> symbols) {

        // create a map[x,y] = r that denotes the maximum valid radius of a center at x,y
        HashMap<Symbol, Double> max_sizes = new HashMap<>();
        for (Symbol s1 : symbols) {
            double min_dist = Float.MAX_VALUE;
            for (Symbol s2 : symbols) if (s1 != s2) min_dist = Math.min(s1.getCenter().distanceTo(s2.getCenter()) - s2.getRadius(), min_dist);
            max_sizes.put(s1, min_dist);
        }

        HashMap<Symbol, HashMap<Region, Double>> distanceToRegion = new HashMap<>();
        for (Symbol s1 : symbols) {
            HashMap<Region, Double> distance = new HashMap<>();
            for (Symbol s2 : symbols) {
                distance.put(s2.getRegion(), Math.pow(s2.getRegion().distanceTo(s1.getCenter()), 2));
            }
            distanceToRegion.put(s1, distance);
        }

        boolean improved;
        do {
            improved = false;
            for (Symbol s1 : symbols) {
                double r1 = max_sizes.get(s1);
                for (Symbol s2 : symbols) {
                    if (s1.hashCode() >= s2.hashCode()) continue;
                    double r2 = max_sizes.get(s2);
                    if (r1 >= s2.getRadius() && r2 >= s1.getRadius() && distanceToRegion.get(s1).get(s1.getRegion()) + distanceToRegion.get(s2).get(s2.getRegion()) > distanceToRegion.get(s2).get(s1.getRegion()) + distanceToRegion.get(s1).get(s2.getRegion())) {
                        // perform a swap and update the data structures
                        Vector c = s1.getCenter().clone();
                        HashMap<Region, Double> m = distanceToRegion.get(s1);
                        s1.setCenter(s2.getCenter());
                        distanceToRegion.put(s1, distanceToRegion.get(s2));
                        max_sizes.put(s1, r2);
                        s2.setCenter(c);
                        distanceToRegion.put(s2, m);
                        max_sizes.put(s2, r1);
                        r1 = r2;
                        improved = true;
                    }
                }
            }
        } while (improved);
    }
}

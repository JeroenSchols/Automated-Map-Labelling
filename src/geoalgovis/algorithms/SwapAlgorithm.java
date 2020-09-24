package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Input;
import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;
import geoalgovis.symbolplacement.SymbolPlacementAlgorithm;
import nl.tue.geometrycore.geometry.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

class SwapAlgorithm {

    /**
     * swaps all symbols of equal size when this improves the score
     *
     * @param symbols the symbols to be swapped
     * @return whether this improved the score
     */
    boolean swap_equal_size(Collection<Symbol> symbols) {
        HashMap<Double, Collection<Symbol>> groups = new HashMap<>();
        for (Symbol s : symbols) {
            if (!groups.containsKey(s.getRadius())) groups.put(s.getRadius(), new ArrayList<>());
            groups.get(s.getRadius()).add(s);
        }

        boolean improved_once = false;
        for (Collection<Symbol> group : groups.values()) {
            if (group.size() == 1) continue;
            boolean improved;

            do {
                improved = false;
                for (Symbol s1 : group) {
                    for (Symbol s2 : group) {
                        if (s1.hashCode() >= s2.hashCode()) continue;
                        if (Math.pow(s1.distanceToRegion(), 2) + Math.pow(s2.distanceToRegion(), 2) > Math.pow(s1.getRegion().distanceToRegion(s2.getCenter()), 2) + Math.pow(s2.getRegion().distanceToRegion(s1.getCenter()), 2)) {
                            swap_centers(s1, s2);
                            improved = true;
                            improved_once = true;
                        }
                    }
                }
            } while (improved);
        }
        return improved_once;
    }

    /**
     * swaps all symbols when this layout remains valid and improves the score
     *
     * @param symbols the symbols to be swapped
     * @return whether this improved the score
     */
    boolean swap(Collection<Symbol> symbols) {

        // create a map[x,y] = r that denotes the maximum valid radius of a center at x,y
        HashMap<Double, HashMap<Double, Double>> max_sizes = new HashMap<>();
        for (Symbol s : symbols) max_sizes.put(s.getCenter().getX(), new HashMap<>());
        for (Symbol s1 : symbols) {
            double min_dist = Float.MAX_VALUE;
            for (Symbol s2 : symbols) if (s1 == s2) min_dist = Math.min(s1.distanceTo(s2.getCenter()) - s2.getRadius(), min_dist);
            max_sizes.get(s1.getCenter().getX()).put(s1.getCenter().getY(), min_dist);
        }

        boolean improved_once = false;
        boolean improved;
        do {
            improved = false;
            for (Symbol s1 : symbols) {
                for (Symbol s2 : symbols) {
                    if (s1.hashCode() >= s2.hashCode()) continue;
                    if (max_sizes.get(s1.getCenter().getX()).get(s1.getCenter().getY()) < s2.getRadius() && max_sizes.get(s2.getCenter().getX()).get(s2.getCenter().getY()) < s1.getRadius() && Math.pow(s1.distanceToRegion(), 2) + Math.pow(s2.distanceToRegion(), 2) > Math.pow(s1.getRegion().distanceToRegion(s2.getCenter()), 2) + Math.pow(s2.getRegion().distanceToRegion(s1.getCenter()), 2)) {
                        swap_centers(s1, s2);
                        improved = true;
                        improved_once = true;
                    }
                }
            }
        } while (improved);

        return improved_once;
    }

    /**
     * swaps the center of two symbols
     */
    private void swap_centers(Symbol a, Symbol b) {
        Vector c = a.getCenter().clone();
        a.setCenter(b.getCenter());
        b.setCenter(c);
    }
}

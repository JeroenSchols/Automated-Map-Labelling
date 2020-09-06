package geoalgovis.algorithms;

import geoalgovis.symbolplacement.*;
import nl.tue.geometrycore.geometry.Vector;

import java.util.HashMap;
import java.util.Map;

public class GravitationalAlgorithm extends SymbolPlacementAlgorithm {

    @Override
    public Output doAlgorithm(Input input) {

        Output output = new Output(input);


        for (int step = 0; step < 100; step++) {
            HashMap<Symbol, Vector> transMap = new HashMap<>(); // contains all translations taking place this step

            for (Symbol s : output.symbols) { // calculate the translation for each symbol
                Vector trans = new Vector(0, 0);
                double dist = s.distanceToRegion();
                if (dist < 0) { // when the symbol is not on its region, add a vector towards this region
                    Vector v = Vector.subtract(s.getCenter(), s.getRegion().getAnchor());
                    trans = Vector.add(trans, Vector.multiply(1/10, v));
                }

                for (Symbol s2 : output.symbols) {
                    if (s == s2) continue;
                    dist = s.getCenter().distanceTo(s2.getCenter()) - s.getRadius() - s2.getRadius();
                    if (dist < 0) { // when two symbols overlap, add a vector away from this symbol
                        Vector v = Vector.subtract(s.getCenter(), s2.getCenter());
                        v.normalize();
                        trans = Vector.add(trans, Vector.multiply((-dist+1)/10, v));
                    }
                }
                transMap.put(s, trans);
            }

            // perform translations
            for (Map.Entry<Symbol, Vector> e : transMap.entrySet()) {
                e.getKey().translate(e.getValue());
            }
        }

        // and make sure to return the result
        return output;
    }
}

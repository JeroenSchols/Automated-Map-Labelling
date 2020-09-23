package geoalgovis.algorithms;

import geoalgovis.symbolplacement.*;
import nl.tue.geometrycore.geometry.Vector;

import java.util.*;

import static geoalgovis.algorithms.Util.calcOverlap;

@SuppressWarnings("Duplicates")
public class PushAlgorithm extends SymbolPlacementAlgorithm {

    @Override
    public Output doAlgorithm(Input input) {
        Output output = new Output(input);

        double n_step = 100;
        for (double step = 0.0; step <= 5 * n_step && !output.isValid(); step++) {
            HashMap<Symbol, Vector> transMap = new HashMap<>();
            for (Symbol current : output.symbols) {
                if (current.distanceToRegion() == 0) {
                    transMap.put(current, new Vector(0,0));
                } else {
                    transMap.put(current, Vector.multiply(Math.max(0.0, 1.0 - step / n_step), Vector.subtract(current.getRegion().getAnchor(), current.getCenter())));
                }
            }

            for (Symbol s1 : output.symbols) {
                double s1d2 = Math.pow(s1.distanceToRegion(), 2);
                for (Symbol s2 : output.symbols) {
                    if (s1.hashCode() >= s2.hashCode()) continue;
                    double overlap = step / n_step * calcOverlap(s1, s2);
                    if (overlap <= 0 ) continue;
                    double s2d2 = Math.pow(s2.distanceToRegion(), 2);
                    Vector dir = Vector.subtract(s1.getCenter(), s2.getCenter());
                    dir.normalize();
                    if (s1d2 == 0 && s2d2 == 0) {
                        transMap.get(s1).translate(Vector.multiply(overlap / 2, dir));
                        transMap.get(s2).translate(Vector.multiply(-overlap / 2, dir));
                    } else if (s1d2 == 0) {
                        transMap.get(s1).translate(Vector.multiply(overlap, dir));
                    } else if (s2d2 == 0) {
                        transMap.get(s2).translate(Vector.multiply(-overlap, dir));
                    } else {
                        transMap.get(s1).translate(Vector.multiply(overlap * s2d2 / (s1d2 + s2d2), dir));
                        transMap.get(s2).translate(Vector.multiply(-overlap * s1d2 / (s1d2 + s2d2), dir));
                    }
                }
            }

            for (Map.Entry<Symbol, Vector> e : transMap.entrySet()) {
                e.getKey().getCenter().translate(e.getValue());
            }
        }

        output.symbols.sort(Comparator.comparingDouble(Symbol::distanceToRegion));
        new PullBackAlgorithm().pullBack(output, true, null, null, 180.0);
        output.symbols.sort(Comparator.comparingDouble(Symbol::distanceToRegion));
        new PullBackAlgorithm().pullBack(output, true, null, null, null);

        // and make sure to return the result
        return output;
    }
}
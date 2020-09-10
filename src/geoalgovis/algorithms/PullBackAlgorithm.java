package geoalgovis.algorithms;

import geoalgovis.symbolplacement.*;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.*;
import sun.text.normalizer.SymbolTable;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class PullBackAlgorithm extends SymbolPlacementAlgorithm {

    @Override
    public Output doAlgorithm(Input input) {

        Output output = new Output(input);

        for (int step = 0; step < 100; step++) {
            HashMap<Symbol, Vector> transMap = new HashMap<>(); // contains all translations taking place this step

            for (Symbol s : output.symbols) { // calculate the translation for each symbol
                Vector trans = new Vector(0, 0);
                for (Symbol s2 : output.symbols) {
                    if (s == s2) continue;
                    double dist = s.getCenter().distanceTo(s2.getCenter()) - s.getRadius() - s2.getRadius();
                    if (dist <= 0) { // when two symbols overlap, add a vector away from this symbol
                        Vector v = Vector.subtract(s.getCenter(), s2.getCenter());
                        v.normalize();
                        trans = Vector.add(trans, Vector.multiply(-2.5 * dist, v));
                    }
                }
                transMap.put(s, trans);
            }
            // perform translations
            for (Map.Entry<Symbol, Vector> e : transMap.entrySet()) {
                e.getKey().translate(e.getValue());
            }
        }

        double avg_x = 0;
        double avg_y = 0;
        for (Symbol s : output.symbols) {
            avg_x += s.getRegion().getAnchor().getX();
            avg_y += s.getRegion().getAnchor().getY();
        }
        avg_x = avg_x / output.symbols.size();
        avg_y = avg_y / output.symbols.size();
        Vector avg = new Vector(avg_x, avg_y);

        TreeMap<Double, Symbol> queue = new TreeMap<>();
        for (Symbol s : output.symbols) {
            queue.put(s.getCenter().distanceTo(s.getRegion().getAnchor()), s);
        }

        boolean valid = false;
        for (int step = 0; step < 100 && !valid; step++) {


            for (Symbol s : queue.values()) {
                if (s.distanceToRegion() == 0) continue;
                LineSegment ray = new LineSegment(s.getCenter(), s.getRegion().getAnchor());
                double best_dist = s.getCenter().distanceTo(s.getRegion().getAnchor());
                Vector best = s.getRegion().getAnchor();
                for (Symbol s2 : output.symbols) {
                    if (s == s2) continue;
                    Circle c = new Circle(s2.getCenter(), s.getRadius() + s2.getRadius() + 1);
                    for (BaseGeometry intersect : c.intersect(ray)) {
                        Vector v = (Vector) intersect;
                        double dist = v.distanceTo(s.getCenter());
                        if (dist < best_dist) {
                            best_dist = dist;
                            best = v;
                        }
                    }
                }
                s.getCenter().set(best);
            }

            double max_r = 0;
            for (Symbol s : output.symbols) max_r = Math.max(max_r, s.getCenter().distanceTo(avg) + s.getRadius());
            Vector cur_false = new Vector(0, max_r);

            valid = true;
            for (Symbol s : output.symbols) {
                for (Symbol s2 : output.symbols) {
                    if (s == s2) continue;
                    if (s.getCenter().distanceTo(s2.getCenter()) < s.getRadius() + s2.getRadius()) {
                        s.getCenter().set(Vector.add(cur_false, avg));
                        cur_false = Vector.rotate(cur_false, 10);
                        valid = false;
                        break;
                    }
                }
            }
        }

        // and make sure to return the result
        return output;
    }
}

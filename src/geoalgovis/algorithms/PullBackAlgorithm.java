package geoalgovis.algorithms;

import geoalgovis.symbolplacement.*;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.*;

import java.util.*;

public class PullBackAlgorithm extends SymbolPlacementAlgorithm {

    @Override
    public Output doAlgorithm(Input input) {
        LineupAlgorithm la = new LineupAlgorithm();
        Output output = la.doAlgorithm(input);
        output.symbols.sort(Comparator.comparingDouble(Circle::getRadius));

        double improved;
        int timeout = 0;
        do {
            timeout++;
            improved = 0;
            for (Symbol s : output.symbols) {
                double dist_before = s.distanceToRegion();
                if (dist_before == 0) continue;
                Vector opt = findClosest(s, s.getRegion().getAnchor(), output.symbols);
                s.getCenter().set(opt);
                improved += dist_before - s.distanceToRegion();
            }
        } while (improved > 0 && timeout < 100);

        if (timeout == 100) {
            System.err.println(input.generalName() + " timed out");
        }

        return output;
    }


    private Vector findClosest(Symbol current, Vector goal, Collection<Symbol> obstacles) {
        Vector dir = Vector.subtract(goal, current.getCenter());
        Vector best = current.getCenter();

        for (int deg = 0; deg < 360; deg += 36) {
            Vector closest = findClosest(current, goal, Vector.rotate(dir, deg), obstacles);
            if (current.getRegion().distanceToRegion(closest) < current.getRegion().distanceToRegion(best)) best = closest;
        }

        return best;
    }

    private Vector findClosest(Symbol current, Vector goal, Vector dir, Collection<Symbol> obstacles) {
        Line line = new Line(goal, dir);
        List<Event> events = new ArrayList<>();
        for (Symbol s : obstacles) {
            if (s == current) continue;
            Circle obstacle = new Circle(s.getCenter(), current.getRadius() + s.getRadius());
            List<BaseGeometry> intersect = obstacle.intersect(line);
            if (intersect.size() == 2) {
                Vector v0 = Vector.subtract((Vector) intersect.get(0), goal);
                Vector v1 = Vector.subtract((Vector) intersect.get(1), goal);
                double d0 = Vector.dotProduct(v0, dir);
                double d1 = Vector.dotProduct(v1, dir);
                if (d0 < d1) {
                    events.add(new Event(d0, 1));
                    events.add(new Event(d1, -1));
                } else {
                    events.add(new Event(d1, 1));
                    events.add(new Event(d0, -1));
                }
            }
        }
        events.add(new Event(0, 0));
        events.sort(Comparator.comparingDouble(event -> event.t));

        double best = Float.MAX_VALUE;
        int count = 0;
        for (Event event : events) {
            if (count == 0 && Math.abs(event.t) < Math.abs(best)) best = event.t;
            count += event.delta;
            if (count == 0 && Math.abs(event.t) < Math.abs(best)) best = event.t;
        }

        return Vector.add(goal, Vector.multiply(best, dir));
    }

    private class Event {

        double t;
        int delta;

        Event(double t, int delta) {
            this.t = t;
            this.delta = delta;
        }
    }
}

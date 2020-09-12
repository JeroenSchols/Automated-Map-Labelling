package geoalgovis.algorithms;

import geoalgovis.symbolplacement.*;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.*;

import java.util.*;

public class PullBackAlgorithm extends SymbolPlacementAlgorithm {

    /*
     * PullBack algorithm considers lines through the anchorpoint to place the symbol on
     * It does so by finding intersections between other symbols and these lines and creating intervals over these lines
     * In case there is a place where no interval is present, it considers placing the circle here
     * Of all lines, we place a symbol closest to its anchor
     */

    @Override
    public Output doAlgorithm(Input input) {
        Output output = Util.placeAway(new Output(input));
        output.symbols.sort(Comparator.comparingDouble(Circle::getRadius));
        output = pullBack(output, true, null, null, null);
        return output;
    }

    /**
     * Apply the pullBack optimization max_iter times or until convergence is reached.
     * Applies this to all sub-optimally placed or overlapping symbols.
     * Try a total of 180/radi_step lines to align on.
     *
     * @param output any output
     * @param is_valid whether the current output is non-overlapping, if null tests for validity
     * @param max_iter the maximum number of iterations to apply pullBack, terminates earlier when converged
     * @param min_delta the minimum relative change to consider until convergence is not reached
     * @param radi_step 180/radi_step is the number of alignment-lines to consider
     */
    public Output pullBack(Output output, Boolean is_valid, Integer max_iter, Double min_delta, Double radi_step) {
        // set default values
        if (is_valid == null) is_valid = output.isValid();
        if (max_iter == null) max_iter = 25;
        if (min_delta == null) min_delta = 0.0001;
        if (radi_step == null) radi_step = 1d;

        double current_quality = output.computeQuality();
        double prev_quality = 2*current_quality;
        for (int iter = 0; iter < max_iter && (1+min_delta)*current_quality < prev_quality; iter++) {
            prev_quality = current_quality;
            for (Symbol s : output.symbols) {
                if (s.distanceToRegion() == 0 && is_valid) continue; // only re-align if non-optimal or overlapping
                Vector opt = findClosest(s, s.getRegion().getAnchor(), output.symbols, radi_step);
                s.getCenter().set(opt);
            }
            is_valid = true;
            current_quality = output.computeQuality();
        }

        // indicate when this run did not converge properly
        if ((1+min_delta)*current_quality < prev_quality) System.out.println(
                "pullBack did not converge on " + output.getName() +
                ", with max_iter = " + max_iter +
                ", and min_delta = " + min_delta
        );

        return output;
    }

    /**
     * For a current symbol try and align as close as possible to the goal point without overlapping with obstacles
     * Try a total of 180/radi_step lines to align one.
     *
     * @param current the current symbol to be aligned
     * @param goal the goal to be aligned to
     * @param obstacles all symbols (is allowed to contain current, yet filtered out)
     * @param radi_step 180/radi_step is the number of alignment-lines to consider
     * @return the closest point on an alignment-line for current to goal to be located on without overlapping obstacles
     */
    private Vector findClosest(Symbol current, Vector goal, Collection<Symbol> obstacles, double radi_step) {
        Vector dir = Vector.subtract(goal, current.getCenter());
        Vector best = current.getCenter();

        for (int deg = 0; deg < 180; deg += radi_step) {
            Vector closest = findClosest(current, goal, Vector.rotate(dir, deg), obstacles);
            if (current.getRegion().distanceToRegion(closest) < current.getRegion().distanceToRegion(best)) best = closest;
        }

        return best;
    }

    /**
     * For a current symbol try and align as close as possible to the goal point without
     * overlapping with obstacles while being located on dir vector from goal.
     *
     * @param current the current symbol to be aligned
     * @param goal the goal to be aligned to
     * @param dir the direction vector of the line through the goal which current can be aligned on
     * @param obstacles all symbols (is allowed to contain current, yet filtered out)
     * @return the closest point on dir from goal for current to be located on without overlapping obstacles
     */
    private Vector findClosest(Symbol current, Vector goal, Vector dir, Collection<Symbol> obstacles) {
        Line line = new Line(goal, dir); // the line to be aligned on (line through goal with direction dir)
        List<Event> events = new ArrayList<>(); // will store all events being starts/ends of intervals

        for (Symbol s : obstacles) {
            if (s == current) continue; // skip current as an obstacle
            // get the points on the line, which when center points of current result in current and s touching
            List<BaseGeometry> intersect = (new Circle(s.getCenter(), current.getRadius()+s.getRadius())).intersect(line);
            if (intersect.size() == 2) { // circles for which current touches once when moving over the line are ignored
                Vector v0 = Vector.subtract((Vector) intersect.get(0), goal);
                Vector v1 = Vector.subtract((Vector) intersect.get(1), goal);
                double d0 = Vector.dotProduct(v0, dir); // project these center points on the dir vector
                double d1 = Vector.dotProduct(v1, dir);
                if (d0 < d1) { // let the smallest value denote the start of the interval and the largest then end
                    events.add(new Event(d0, 1));
                    events.add(new Event(d1, -1));
                } else {
                    events.add(new Event(d1, 1));
                    events.add(new Event(d0, -1));
                }
            }
        }
        events.add(new Event(0, 0)); // add an event to consider placing symbol on top of its anchor
        events.sort(Comparator.comparingDouble(event -> event.t)); // sort all events on t

        double best = Float.MAX_VALUE; // the smallest distance considered where current can be placed from goal
        int count = 0; // the number of currently 'active' intervals
        for (Event event : events) {
            if (count == 0 && Math.abs(event.t) < Math.abs(best)) best = event.t; // no intervals present check event.t
            count += event.delta; // update the number of currently 'active' intervals
            if (count == 0 && Math.abs(event.t) < Math.abs(best)) best = event.t; // no intervals present check event.t
        }

        return Vector.add(goal, Vector.multiply(best, dir)); // return the point closest to goal on line
    }

    // class corresponding to start and end points of intervals on lines
    private class Event {

        double t;
        int delta;

        Event(double t, int delta) {
            this.t = t;
            this.delta = delta;
        }
    }
}

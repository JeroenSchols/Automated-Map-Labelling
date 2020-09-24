package geoalgovis.algorithms;

import geoalgovis.symbolplacement.*;
import nl.tue.geometrycore.geometry.BaseGeometry;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.*;
import nl.tue.geometrycore.util.Pair;

import java.util.*;

public class PullBackAlgorithm extends SymbolPlacementAlgorithm {

    private Random random = new Random(0);

    /*
     * PullBack algorithm considers lines through the anchorpoint to place the symbol on
     * It does so by finding intersections between other symbols and these lines and creating intervals over these lines
     * In case there is a place where no interval is present, it considers placing the circle here
     * Of all lines, we place a symbol closest to its anchor
     */

    @Override
    public Output doAlgorithm(Input input) {
        Output bestOutput = Util.placeAway(new Output(input));

        for (Util.RegionSortDirection dir : Util.RegionSortDirection.values()) {
            System.out.println("checking :" + dir.name());
            Output output = Util.placeAway(new Output(input));
            List<Pair<Symbol, Vector>> symbolsValuated = Util.sortRegionDir(output.symbols, dir, null);
            for (int i = 0; i < symbolsValuated.size(); i++) {
                output.symbols.set(i, symbolsValuated.get(i).getFirst());
                symbolsValuated.get(i).getFirst().getRegion().setAnchor(symbolsValuated.get(i).getSecond());
            }
            pullBack(output, 1, null, null, null);
            if (output.isValid() && output.computeQuality() < bestOutput.computeQuality()) bestOutput = output;
        }

        return bestOutput;
    }

    /**
     * Apply the pullBack optimization max_iter times or until convergence is reached.
     * Applies this to all sub-optimally placed or overlapping symbols.
     * Try a total of 180/radi_step lines to align on.
     *
     * @param output any output
     * @param max_iter the maximum number of iterations to apply pullBack, terminates earlier when converged
     * @param min_delta the minimum relative change to consider until convergence is not reached
     * @param radi_step 180/radi_step is the number of alignment-lines to consider
     * @param cGoals which goals to try pulling towards
     */
    void pullBack(Output output, Integer max_iter, Double min_delta, Double radi_step, Util.CandidateGoals cGoals) {
        // set default values
        if (max_iter == null) max_iter = 25;
        if (min_delta == null) min_delta = 0.0001;
        if (radi_step == null) radi_step = 1d;
        if (cGoals == null) cGoals = Util.CandidateGoals.All;

        double current_quality = output.computeQuality();
        double prev_quality = 2*current_quality;
        for (int iter = 0; iter < max_iter && (1+min_delta)*current_quality < prev_quality; iter++) {
            prev_quality = current_quality;
            for (Symbol s : output.symbols) {
                Vector opt = findClosest(s, output.symbols, radi_step, cGoals);
                s.getCenter().set(opt);
            }
            current_quality = output.computeQuality();
        }

        // indicate when this run did not converge properly
        if ((1+min_delta)*current_quality < prev_quality) System.err.println(
                "pullBack did not converge on " + output.getName() +
                ", with max_iter = " + max_iter +
                ", and min_delta = " + min_delta
        );
    }


    /**
     * For a current symbol try and align as close as possible to any goal point without overlapping with obstacles
     * Try a total of 180/radi_step lines to align on. A goal point is either the anchor or a point on the boundary
     *
     * @param current the current symbol to be aligned
     * @param obstacles all symbols (is allowed to contain current, yet filtered out)
     * @param radi_step 180/radi_step is the number of alignment-lines to consider
     * @return the closest point on an alignment-line for current to goal to be located on without overlapping obstacles
     * @param cGoals which goals to try pulling towards
     */
    private Vector findClosest(Symbol current, Collection<Symbol> obstacles, double radi_step, Util.CandidateGoals cGoals) {
        Vector best = current.getCenter();

        List<Vector> goals = null;
        switch (cGoals) {
            case All: goals = current.getRegion().getAllAnchors(); break;
            case Anchor: goals = new ArrayList<>(); goals.add(current.getRegion().getAnchor()); break;
            case Sample: goals = current.getRegion().getSampleAnchors(); break;
            case Extrema: goals = current.getRegion().getExtremaAnchors(); break;
        }

        Collections.shuffle(goals, random);
        for (int i = 0; i < goals.size(); i++) {
            if (i % 2 == 1) continue; // we randomly sample which goals to optimize on
            Vector goal = goals.get(i);
            Vector closest = findClosest(current, goal, obstacles, radi_step);
            if (current.getRegion().distanceToRegion(closest) <= current.getRegion().distanceToRegion(best)) best = closest;
        }

        return best;
    }


    /**
     * For a current symbol try and align as close as possible to the goal point without overlapping with obstacles
     * Try a total of 180/radi_step lines to align on.
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

        for (double deg = 0; deg < 180; deg += radi_step) {
            Vector closest = findClosest(current, goal, Vector.rotate(dir, deg), obstacles);
            if (goal.distanceTo(closest) < goal.distanceTo(best) && current.getRegion().distanceToRegion(closest) <= current.getRegion().distanceToRegion(best)) best = closest;
//            if (current.getRegion().distanceToRegion(closest) <= current.getRegion().distanceToRegion(best)) best = closest;
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
        events.add(new Event(0, 0));
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

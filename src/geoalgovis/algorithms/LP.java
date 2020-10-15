package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;

import gurobi.*;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

class LP {

    private GRBEnv env;

    LP () {
        try {
            this.env = new GRBEnv();
            env.set(GRB.IntParam.LogToConsole, 0);
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    /**
     * solve as a linear program where the area is separated into hor x ver areas that are
     * independently solved
     *
     * @param output the output to be run on
     * @param orderAnchor whether ordering occurs on the anchor or the center points
     * @param hor number of horizontal partitions
     * @param ver number of vertical partitions
     */
    void partitionedLpSolve(Output output, Boolean orderAnchor, int hor, int ver) {
        Pair<ArrayList<Symbol>, Pair<Pair<Double, Double>, Pair<Double, Double>>>[][] partition = Util.partition(output.symbols, hor, ver);
        for (int i = 0; i < hor; i++) {
            for (int j = 0; j < ver; j++) {
                rectangleSolve(partition[i][j].getFirst(), orderAnchor, partition[i][j].getSecond(), 0, false);
            }
        }
    }

    /**
     * run the lp program for different mirroring orientations and take the best
     *
     * @param output the output to be run on
     * @param orderAnchor whether ordering occurs on the anchor or the center points
     */
    void lpSolve(Output output, Boolean orderAnchor) {
        if (orderAnchor == null) orderAnchor = true;

        double bestQuality = output.isValid() ? output.computeQuality() : Float.MAX_VALUE;
        HashMap<Symbol, Pair<Double, Double>> best = new HashMap<>();
        for (Symbol s : output.symbols) best.put(s, new Pair<>(s.getCenter().getX(), s.getCenter().getY()));

        for (Util.MirrorDirection dir : Util.MirrorDirection.values()) {
            lpSolve(output, dir, orderAnchor);
            double quality = output.computeQuality();
            if (quality < bestQuality) {
                for (Symbol s : output.symbols) best.put(s, new Pair<>(s.getCenter().getX(), s.getCenter().getY()));
                bestQuality = quality;
            }
        }

        for (Symbol s : output.symbols) {
            s.getCenter().setX(best.get(s).getFirst());
            s.getCenter().setY(best.get(s).getSecond());
        }
    }

    /**
     * run the lp program for select mirroring orientation
     *
     * @param output the output to be run on
     * @param dir which mirroring orientation
     * @param orderAnchor whether ordering occurs on the anchor or the center points
     */
    void lpSolve(Output output, Util.MirrorDirection dir, Boolean orderAnchor) {
        if (orderAnchor == null) orderAnchor = true;
        mirror(output, dir);
        rectangleSolve(output.symbols, orderAnchor, null, 0, true);
        mirror(output, dir);
    }

    /**
     * mirrors the symbols their anchor and center point
     *
     * @param output the output which symbols should be mirrored
     * @param dir the direction in which to mirror
     */
    private void mirror(Output output, Util.MirrorDirection dir) {
        for (Symbol s : output.symbols) {
            Vector anchor = s.getRegion().getAnchor();
            Vector center = s.getCenter();
            if (dir == Util.MirrorDirection.X || dir == Util.MirrorDirection.XY) {
                anchor.setX(-anchor.getX());
                center.setX(-center.getX());
            }
            if (dir == Util.MirrorDirection.Y || dir == Util.MirrorDirection.XY) {
                anchor.setY(-anchor.getY());
                center.setY(-center.getY());
            }
        }
    }

    /**
     * run the linear program for rectangles as symbols
     *
     * @param symbols the symbols to be placed
     * @param orderAnchor whether ordering occurs on the anchor or the center points
     * @param box a bounding box in which all symbol centers need to be placed (can be null)
     * @param margin the amount of slack is allowed to be used on the bounding box
     * @param valid whether the symbols should be guaranteed non-overlapping
     */
    private void rectangleSolve(List<Symbol> symbols, boolean orderAnchor, Pair<Pair<Double, Double>, Pair<Double, Double>> box, double margin, boolean valid) {
        try {
            GRBModel model = new GRBModel(env);

            HashMap<Symbol, Vector> anchor = new HashMap<>(); // stores for each symbol the anchor vector
            HashMap<Symbol, Pair<GRBVar, GRBVar>> center = new HashMap<>(); // stores for each symbol the variable center <x, y>

            // variable denoting the upper-bound on all distances between anchor and variable center
            GRBVar d = model.addVar(0, GRB.INFINITY, 2 * symbols.size(), GRB.CONTINUOUS, "d");

            for (Symbol s : symbols) {
                anchor.put(s, s.getRegion().getAnchor());

                // create a variable x, y to correspond to the new center coordinates
                GRBVar x, y;
                if (box != null) {
                    double dx = margin * box.getSecond().getFirst() - box.getFirst().getFirst();
                    double dy = margin * box.getSecond().getSecond() - box.getFirst().getSecond();
                    x = model.addVar(box.getFirst().getFirst() - dx, box.getSecond().getFirst() + dx, 0, GRB.CONTINUOUS, s.getRegion().getName() + "_x");
                    y = model.addVar(box.getFirst().getSecond() - dy, box.getSecond().getSecond() + dy, 0, GRB.CONTINUOUS, s.getRegion().getName() + "_y");
                } else {
                    x = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, s.getRegion().getName() + "_x");
                    y = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, s.getRegion().getName() + "_y");
                }
                center.put(s, new Pair<>(x, y));

                // limit the distance between anchor and new center to be at most d
                GRBVar dl = model.addVar(0, GRB.INFINITY, 1, GRB.CONTINUOUS, s.getRegion().getName() + "_d");
                model.addConstr(dl, GRB.LESS_EQUAL, d, null);
                int[] c = {-1, 1};
                for (int xc : c) {
                    for (int yc : c) {
                        GRBLinExpr dist = new GRBLinExpr();
                        dist.addTerm(xc, x);
                        dist.addTerm(yc, y);
                        dist.addTerm(1, dl);
                        model.addConstr(dist, GRB.GREATER_EQUAL, xc * anchor.get(s).getX() + yc * anchor.get(s).getY(), s.getRegion().getName() + "_dist");
                    }
                }
            }

            // add the constraint that orthogonality in x is maintained
            if (orderAnchor) {
                symbols.sort(Comparator.comparingDouble(s -> s.getRegion().getAnchor().getX()));
            } else {
                symbols.sort(Comparator.comparingDouble(s -> s.getCenter().getX()));
            }
            for (int i = 0; i < symbols.size() - 1; i++) {
                Symbol s1 = symbols.get(i);
                Symbol s2 = symbols.get(i + 1);
                GRBLinExpr order = new GRBLinExpr();
                order.addTerm(-1, center.get(s1).getFirst());
                order.addTerm(1, center.get(s2).getFirst());
                model.addConstr(order, GRB.GREATER_EQUAL, 0, s1.getRegion().getName() + "-" + s2.getRegion().getName() + "_xorder");

            }

            // add the constraint that orthogonality in y is maintained
            if (orderAnchor) {
                symbols.sort(Comparator.comparingDouble(s -> s.getRegion().getAnchor().getY()));
            } else {
                symbols.sort(Comparator.comparingDouble(s -> s.getCenter().getY()));
            }
            for (int i = 0; i < symbols.size() - 1; i++) {
                Symbol s1 = symbols.get(i);
                Symbol s2 = symbols.get(i + 1);
                GRBLinExpr order = new GRBLinExpr();
                order.addTerm(-1, center.get(s1).getSecond());
                order.addTerm(1, center.get(s2).getSecond());
                model.addConstr(order, GRB.GREATER_EQUAL, 0, s1.getRegion().getName() + "-" + s2.getRegion().getName() + "_yorder");

            }

            // add the non overlapping constraint
            for (Symbol s1 : symbols) {
                for (Symbol s2 : symbols) {
                    if (s1 == s2) continue;
                    double dist = valid ? Math.sqrt(2 * (Math.pow(s1.getRadius() + s2.getRadius(), 2))) : s1.getRadius() + s2.getRadius();
                    if ((orderAnchor && anchor.get(s1).getX() <= anchor.get(s2).getX() && anchor.get(s1).getY() <= anchor.get(s2).getY()) || (!orderAnchor && s1.getCenter().getX() <= s2.getCenter().getX() && s1.getCenter().getY() <= s2.getCenter().getY())){
                        GRBLinExpr order = new GRBLinExpr();
                        order.addTerm(-1, center.get(s1).getFirst());
                        order.addTerm(-1, center.get(s1).getSecond());
                        order.addTerm(1, center.get(s2).getFirst());
                        order.addTerm(1, center.get(s2).getSecond());
                        model.addConstr(order, GRB.GREATER_EQUAL, dist, s1.getRegion().getName() + "-" + s2.getRegion().getName() + "_overlap");
                    }
                    if ((orderAnchor && anchor.get(s1).getX() <= anchor.get(s2).getX() && anchor.get(s1).getY() >= anchor.get(s2).getY()) || (!orderAnchor && s1.getCenter().getX() <= s2.getCenter().getX() && s1.getCenter().getY() >= s2.getCenter().getY())){
                        GRBLinExpr order = new GRBLinExpr();
                        order.addTerm(-1, center.get(s1).getFirst());
                        order.addTerm(1, center.get(s1).getSecond());
                        order.addTerm(1, center.get(s2).getFirst());
                        order.addTerm(-1, center.get(s2).getSecond());
                        model.addConstr(order, GRB.GREATER_EQUAL, dist, s1.getRegion().getName() + "-" + s2.getRegion().getName() + "_overlap");
                    }
                }
            }

            model.optimize();

            // when the model has found a solution get this solution, otherwise recurse with more slack
            if (model.get(GRB.IntAttr.Status) == 2) {
                for (Symbol s : symbols) {
                    double x = center.get(s).getFirst().get(GRB.DoubleAttr.X);
                    double y = center.get(s).getSecond().get(GRB.DoubleAttr.X);
                    s.setCenter(new Vector(x, y));
                }
                model.dispose();
            } else {
                model.dispose();
                rectangleSolve(symbols, orderAnchor, box, margin + 0.1, valid);
            }

        } catch (GRBException e) {
            e.printStackTrace();
        }
    }
}

package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Input;
import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;
import geoalgovis.symbolplacement.SymbolPlacementAlgorithm;

import gurobi.*;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.util.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class LP extends SymbolPlacementAlgorithm {
    @Override
    public Output doAlgorithm(Input input) {
        Output output = new Output(input);
        this.partitionedLpSolve(output, true, 10, 10);
        new PushAlgorithm().pushRun(output, Util.CandidateGoals.Anchor, 1000d, 0.9, 3d);
        new SwapAlgorithm().swap(output.symbols);
        new CenterSpreadAlgorithm().centerAreaSpread(output.symbols, 0.25, null);
        new PostProcessAlgorithm().postprocess(output);
        return output;
    }

    void partitionedLpSolve(Output output, Boolean orderAnchor, int hor, int ver) {
        ArrayList<Symbol>[][] partition = Util.partition(output.symbols, hor, ver);
        for (int i = 0; i < hor; i++) {
            for (int j = 0; j < ver; j++) {
                rectangleSolve(partition[i][j], orderAnchor);
            }
        }
    }

    void lpSolve(Output output, Boolean orderAnchor) {
        if (orderAnchor == null) orderAnchor = true;

        double bestQuality = output.isValid() ? output.computeQuality() : Float.MAX_VALUE;
        HashMap<Symbol, Pair<Double, Double>> best = new HashMap<>();
        for (Symbol s : output.symbols) best.put(s, new Pair<>(s.getCenter().getX(), s.getCenter().getY()));

        for (Util.MirrorDirection dir : Util.MirrorDirection.values()) {
            lpSolve(output, dir, orderAnchor);
            double quality = output.computeQuality();
            System.out.println(quality);
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

    void lpSolve(Output output, Util.MirrorDirection dir, Boolean orderAnchor) {
        if (orderAnchor == null) orderAnchor = true;
        mirror(output, dir);
        rectangleSolve(output.symbols, orderAnchor);
        mirror(output, dir);
    }

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

    private void rectangleSolve(List<Symbol> symbols, boolean orderAnchor) {
        try {
            GRBEnv env = new GRBEnv();
            env.set(GRB.IntParam.LogToConsole, 0);
            GRBModel model = new GRBModel(env);

            HashMap<Symbol, Vector> anchor = new HashMap<>();
            HashMap<Symbol, Pair<GRBVar, GRBVar>> center = new HashMap<>();
            GRBVar d = model.addVar(0, GRB.INFINITY, 2 * symbols.size(), GRB.CONTINUOUS, "d");

            for (Symbol s : symbols) {
                anchor.put(s, s.getRegion().getAnchor());

                GRBVar x = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, s.getRegion().getName() + "_x");
                GRBVar y = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, s.getRegion().getName() + "_y");
                GRBVar dl = model.addVar(0, GRB.INFINITY, 1, GRB.CONTINUOUS, s.getRegion().getName() + "_d");
                model.addConstr(dl, GRB.LESS_EQUAL, d, null);

                center.put(s, new Pair<>(x, y));

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

            for (Symbol s1 : symbols) {
                for (Symbol s2 : symbols) {
                    if (s1 == s2) continue;
                    double dist = Math.sqrt(2 * (Math.pow(s1.getRadius() + s2.getRadius(), 2)));
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

            for (Symbol s : symbols) {
                double x = center.get(s).getFirst().get(GRB.DoubleAttr.X);
                double y = center.get(s).getSecond().get(GRB.DoubleAttr.X);
                s.setCenter(new Vector(x, y));
            }

            model.dispose();
            env.dispose();

        } catch (GRBException e) {
            e.printStackTrace();
        }
    }
}

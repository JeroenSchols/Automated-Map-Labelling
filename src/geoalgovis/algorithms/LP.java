package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Input;
import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;
import geoalgovis.symbolplacement.SymbolPlacementAlgorithm;

import gurobi.*;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.util.Pair;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class LP extends SymbolPlacementAlgorithm {
    @Override
    public Output doAlgorithm(Input input) {
        Output output = new Output(input);
        lpSolve(output, Util.MirrorDirection.None);
        System.out.println("lp done -- starting postprocessing");
        new SwapAlgorithm().swap(output.symbols);
        new PostProcessAlgorithm().postprocess(output);
        return output;
    }

    void lpSolve(Output output) {
        double bestQuality = output.isValid() ? output.computeQuality() : Float.MAX_VALUE;
        HashMap<Symbol, Pair<Double, Double>> best = new HashMap<>();
        for (Symbol s : output.symbols) best.put(s, new Pair<>(s.getCenter().getX(), s.getCenter().getY()));

        for (Util.MirrorDirection dir : Util.MirrorDirection.values()) {
            lpSolve(output, dir);
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

    void lpSolve(Output output, Util.MirrorDirection dir) {
        for (Symbol s : output.symbols) {
            Vector anchor = s.getRegion().getAnchor();
            if (dir == Util.MirrorDirection.X || dir == Util.MirrorDirection.XY) {
                anchor.setX(-anchor.getX());
            }
            if (dir == Util.MirrorDirection.Y || dir == Util.MirrorDirection.XY) {
                anchor.setY(-anchor.getY());
            }
        }

        rectangleSolve(output.symbols);

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

    private void rectangleSolve(List<Symbol> symbols) {
        try {
            GRBEnv env = new GRBEnv();
            env.set(GRB.IntParam.LogToConsole, 0);
            GRBModel model = new GRBModel(env);

            HashMap<Symbol, Vector> anchor = new HashMap<>();
            HashMap<Symbol, Pair<GRBVar, GRBVar>> center = new HashMap<>();
            HashMap<Symbol, GRBVar> centerDist = new HashMap<>();
            for (Symbol s : symbols) {
                anchor.put(s, s.getRegion().getAnchor());

                GRBVar x = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, s.getRegion().getName() + "_x");
                GRBVar y = model.addVar(-GRB.INFINITY, GRB.INFINITY, 0, GRB.CONTINUOUS, s.getRegion().getName() + "_y");
                GRBVar d = model.addVar(0, GRB.INFINITY, 1, GRB.CONTINUOUS, s.getRegion().getName() + "_d");

                center.put(s, new Pair<>(x, y));
                centerDist.put(s, d);

                int[] c = {-1, 1};
                for (int xc : c) {
                    for (int yc : c) {
                        GRBLinExpr dist = new GRBLinExpr();
                        dist.addTerm(xc, x);
                        dist.addTerm(yc, y);
                        dist.addTerm(1, d);
                        model.addConstr(dist, GRB.GREATER_EQUAL, xc * anchor.get(s).getX() + yc * anchor.get(s).getY(), s.getRegion().getName() + "_dist");
                    }
                }
            }

            symbols.sort(Comparator.comparingDouble(s -> s.getRegion().getAnchor().getX()));
            for (int i = 0; i < symbols.size() - 1; i++) {
                Symbol s1 = symbols.get(i);
                Symbol s2 = symbols.get(i + 1);
                GRBLinExpr order = new GRBLinExpr();
                order.addTerm(-1, center.get(s1).getFirst());
                order.addTerm(1, center.get(s2).getFirst());
                model.addConstr(order, GRB.GREATER_EQUAL, 0, s1.getRegion().getName() + "-" + s2.getRegion().getName() + "_xorder");

            }

            symbols.sort(Comparator.comparingDouble(s -> s.getRegion().getAnchor().getY()));
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
                    if (anchor.get(s1).getX() <= anchor.get(s2).getX() && anchor.get(s1).getY() <= anchor.get(s2).getY()) {
                        GRBLinExpr order = new GRBLinExpr();
                        order.addTerm(-1, center.get(s1).getFirst());
                        order.addTerm(-1, center.get(s1).getSecond());
                        order.addTerm(1, center.get(s2).getFirst());
                        order.addTerm(1, center.get(s2).getSecond());
                        model.addConstr(order, GRB.GREATER_EQUAL, dist, s1.getRegion().getName() + "-" + s2.getRegion().getName() + "_overlap");
                    }
                    if (anchor.get(s1).getX() <= anchor.get(s2).getX() && anchor.get(s1).getY() >= anchor.get(s2).getY()) {
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

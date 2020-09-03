/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis.symbolplacement;

import geoalgovis.problem.Solution;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.LineSegment;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.GeometryRenderer;
import nl.tue.geometrycore.geometryrendering.glyphs.ArrowStyle;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.geometryrendering.styling.SizeMode;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;
import nl.tue.geometrycore.util.DoubleUtil;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class Output extends Solution {

    public Input input;
    public List<Symbol> symbols;

    public Output(Input input) {
        this.input = input;
        symbols = new ArrayList();
        for (Region r : input.regions) {
            symbols.add(new Symbol(r, r.getAnchor().clone(), r.getWeight()));
        }
    }

    @Override
    public boolean isValid() {
        if (symbols.size() != input.regions.size()) {
            return false;
        }
        sortToIndex();
        for (int i = 0; i < symbols.size(); i++) {
            Symbol s = symbols.get(i);
            if (!DoubleUtil.close(s.getRadius(), s.getRegion().getWeight())) {
                return false;
            }
            if (input.regions.get(s.getRegion().getIndex()) != s.getRegion()) {
                return false;
            }
            for (int j = i + 1; j < symbols.size(); j++) {
                Symbol ss = symbols.get(j);
                if (ss.getCenter().squaredDistanceTo(s.getCenter())
                        < s.getRadius() + ss.getRadius() - DoubleUtil.EPS) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public double computeQuality() {
        double dists = 0;
        for (Symbol s : symbols) {
            dists += Math.pow(s.distanceToRegion(), 2);
        }
        return dists;
    }

    public void sortToIndex() {
        int i = 0;
        while (i < symbols.size()) {
            Symbol s = symbols.get(i);
            int si = s.getRegion().getIndex();
            if (si == i) {
                i++;
            } else {
                symbols.set(i, symbols.get(si));
                symbols.set(si, s);
            }
        }
    }

    @Override
    public String toString() {
        String result = "";
        for (Symbol s : symbols) {
            result += s.getCenter().getX() + "\t" + s.getCenter().getY() + "\n";
        }
        return result;
    }

    @Override
    public void draw(GeometryRenderer render) {
        input.draw(render, false);
        render.setSizeMode(SizeMode.VIEW);
        render.setStroke(Color.blue, 2, Dashing.SOLID);
        render.setFill(null, Hashures.SOLID);
        render.draw(symbols);
        render.setTextStyle(TextAnchor.CENTER, 12);
        render.setFill(null, Hashures.SOLID);
        render.setForwardArrowStyle(ArrowStyle.LINEAR, 3);
        for (Symbol s : symbols) {
            Vector c = s.getCenter();
            render.draw(c, s.getRegion().getName());
            Vector dir = s.vectorToRegion();
            if (dir != null) {
                render.draw(LineSegment.byStartAndOffset(c, dir));
            }
        }
        render.setForwardArrowStyle(null, 0);
    }

    @Override
    public Rectangle getBoundingBox() {
        Rectangle R = Rectangle.byBoundingBox(symbols);
        R.includeGeometry(input.getBoundingBox());
        return R;
    }
}

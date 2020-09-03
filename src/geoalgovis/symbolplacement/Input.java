/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package geoalgovis.symbolplacement;

import geoalgovis.problem.Problem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.curved.Circle;
import nl.tue.geometrycore.geometry.linear.Polygon;
import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.GeometryRenderer;
import nl.tue.geometrycore.geometryrendering.styling.Dashing;
import nl.tue.geometrycore.geometryrendering.styling.ExtendedColors;
import nl.tue.geometrycore.geometryrendering.styling.Hashures;
import nl.tue.geometrycore.geometryrendering.styling.SizeMode;
import nl.tue.geometrycore.geometryrendering.styling.TextAnchor;

/**
 *
 * @author Wouter Meulemans (w.meulemans@tue.nl)
 */
public class Input extends Problem<Output> {

    public List<Region> regions;

    @Override
    public String generalName() {
        return "Symbol Placement";
    }

    public void sortToIndex() {
        int i = 0;
        while (i < regions.size()) {
            Region r = regions.get(i);
            int ri = r.getIndex();
            if (ri == i) {
                i++;
            } else {
                regions.set(i, regions.get(ri));
                regions.set(ri, r);
            }
        }
    }

    @Override
    public void loadProblem(String f) {
        String[] lines = f.split("\n");
        regions = new ArrayList();
        Region curr = null;
        int k = 0;
        for (int i = 0; i < lines.length; i++) {
            String[] splitline = lines[i].split("\t");
            if (k == 0) {
                curr = new Region();
                curr.setIndex(regions.size());
                regions.add(curr);
                curr.setName(splitline[0]);
                curr.setAnchor(new Vector(
                        Double.parseDouble(splitline[1]), 
                        Double.parseDouble(splitline[2])));
                curr.setWeight(Double.parseDouble(splitline[3]));
                k = Integer.parseInt(splitline[4]);
            } else {
                Polygon poly = new Polygon();
                curr.getParts().add(poly);
                for (int j = 0; j < splitline.length; j += 2) {
                    Vector v = new Vector(
                            Double.parseDouble(splitline[j]),
                            Double.parseDouble(splitline[j + 1]));
                    poly.addVertex(v);
                }
                k--;
            }
        }
    }

    @Override
    public String writeProblem() {

        sortToIndex();
        String result = "";
        for (Region r : regions) {
            result += r.getName() + "\t" + r.getAnchor().getX() + "\t" + r.getAnchor().getY() + "\t" + r.getWeight() + "\t" + r.getParts().size() + "\n";
            for (Polygon p : r.getParts()) {
                boolean first = true;
                for (Vector v : p.vertices()) {
                    if (first) {
                        first = false;
                    } else {
                        result += "\t";
                    }
                    result += v.getX() + "\t" + v.getY();
                }
                result += "\n";
            }
        }
        return result;
    }

    @Override
    public Output loadSolution(String solution) {
        Output output = new Output(this);
        String[] lines = solution.split("[\t\n]");
        for (int i = 0; i < lines.length; i += 2) {
            output.symbols.get(i / 2).getCenter().set(
                    Double.parseDouble(lines[i]),
                    Double.parseDouble(lines[i + 1])
            );
        }
        return output;
    }

    @Override
    public void draw(GeometryRenderer render) {
        draw(render, true);
    }

    public void draw(GeometryRenderer render, boolean showCircles) {
        render.setSizeMode(SizeMode.VIEW);
        render.setStroke(Color.black, 2, Dashing.SOLID);
        render.setFill(ExtendedColors.lightGray, Hashures.SOLID);
        render.draw(regions);
        render.setTextStyle(TextAnchor.CENTER, 12);
        render.setFill(null, Hashures.SOLID);
        for (Region r : regions) {
            Vector c = r.getAnchor();
            render.draw(c, r.getName());
            if (showCircles) {
                render.setStroke(Color.red, 2, Dashing.SOLID);
                render.draw(new Circle(c, r.getWeight()));
                render.setStroke(Color.black, 2, Dashing.SOLID);
            }
        }

    }

    @Override
    public Rectangle getBoundingBox() {
        return Rectangle.byBoundingBox(regions);
    }

    @Override
    public boolean isValidInstance() {
        return regions != null && regions.size() > 0;
    }
}

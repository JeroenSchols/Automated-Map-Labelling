package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;
import nl.tue.geometrycore.geometry.Vector;
import nl.tue.geometrycore.geometry.linear.Rectangle;

import java.util.List;

class Util {

    /**
     * place all points far away from all regions to start with a clean slate
     */
    static Output placeAway(Output output) {
        Rectangle boundingBox = output.getBoundingBox();
        double x = boundingBox.width() + boundingBox.getRight();
        double y = boundingBox.height() + boundingBox.getTop();
        for (Symbol s : output.symbols) {
            s.getCenter().set(x + s.getRadius(), y);
            x += 2*s.getRadius();
        }
        return output;
    }

    /**
     * get the average anchor point
     */
    static Vector getAvgAnchor(List<Symbol> symbols) {
        Vector avg_anchor = new Vector(0,0);
        for (Symbol s : symbols) avg_anchor = Vector.add(avg_anchor, s.getRegion().getAnchor());
        return Vector.divide(avg_anchor, symbols.size());
    }

    /**
     * get the average center point
     */
    static Vector getAvgCenter(List<Symbol> symbols) {
        Vector avg_center = new Vector(0,0);
        for (Symbol s : symbols) avg_center = Vector.add(avg_center, s.getCenter());
        return Vector.divide(avg_center, symbols.size());
    }

    /**
     * sort symbols on distance around a point
     */
    static void sortAroundPoint(List<Symbol> symbols, Vector point) {
        symbols.sort((s1, s2) -> (int) (s1.getCenter().distanceTo(point) - s2.getCenter().distanceTo(point)));
    }
}

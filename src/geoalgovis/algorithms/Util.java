package geoalgovis.algorithms;

import geoalgovis.symbolplacement.Output;
import geoalgovis.symbolplacement.Symbol;
import nl.tue.geometrycore.geometry.linear.Rectangle;

public class Util {

    /**
     * place all points far away from all regions to start with a clean slate
     */
    public static Output placeAway(Output output) {
        Rectangle boundingBox = output.getBoundingBox();
        double x = boundingBox.width() + boundingBox.getRight();
        double y = boundingBox.height() + boundingBox.getTop();
        for (Symbol s : output.symbols) {
            s.getCenter().set(x + s.getRadius(), y);
            x += 2*s.getRadius();
        }
        return output;
    }
}

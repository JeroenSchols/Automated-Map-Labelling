package geoalgovis.gui;

import nl.tue.geometrycore.geometry.linear.Rectangle;
import nl.tue.geometrycore.geometryrendering.GeometryRenderer;

public interface Viewable {

    void draw(GeometryRenderer render);

    Rectangle getBoundingBox();

}

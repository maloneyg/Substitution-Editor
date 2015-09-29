//package Project;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.concurrent.*;
import java.io.*;
import java.lang.Math.*;
import java.util.Map;
import java.util.Stack;
import java.util.AbstractMap.*;
import java.io.PrintWriter;

import math.geom2d.polygon.SimplePolygon2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.Point2D;

import java.util.Arrays;

/*************************************************************************
 * An interface representing a rhomb in a patch.  
 * These rhombs may move relative to one another.  
 *************************************************************************/
public interface Rhomb {

    /**
     * Return an integer indicating the congruence class of rhomb.  
     * @return The congruence class of the shape this represents.  It is the smallest angle in
     * the rhomb, expressed as an integer multiple of pi/{@link Point#N()}.
     */
    public int getType();

    /**
     * Return an integer indicating the angle of the rhomb.  
     * @return The angle of rotation of this.  
     */
    public int getAngle();

    /**
     * Getter method.  
     * @return The point.  
     */
    public Point getPoint();
    
    /**
     * Determine if this Rhomb is on the edge of the patch.  
     * @return true if it's on the edge of the patch.  
     */
    public boolean onEdge();

    /**
     * Remove this Rhomb.  This method should only be used if this is on an edge.  
     */
    public void collapse();

    /**
     * Shift the Rhomb by a given vector.
     * @param vector The vector by which we shift this.  
     */
    public void shift(Point vector);

    /**
     * Getter method.  
     * @return A list of polygon objects containing the 2d rhomb that this Rhomb represents.  (2 coordinates.)  
     */
    public SimplePolygon2D getRhomb();

    /**
     * Set the scale for drawing.  
     * @param s The new scale used to draw this rhomb.  
     */
    public void setScale(double s);

    /**
     * Get the scale for drawing.  
     * @return The scale for drawing.  
     */
    public double getScale();

    /**
     * Produce a lightweight version of this.  
     * @return A SimpleRhomb containing all the essential data from this.  
     */
    public SimpleRhomb createSimpleRhomb();

    /**
     * String representation for gap.  
     * @return A String containing various internal variables, formatted for use in gap.  
     */
    public String gapString();

    /**
     * String representation for postscript.  
     * @return A String giving instructions for how to draw this in Postscript.  
     */
    public String postscriptString();

} // end of interface Rhomb

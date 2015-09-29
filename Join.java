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
 * A class representing the intersection of two {@link Yarn}s (pseudolines).  
 * The Joins may shift about relative to one another, but each Join always
 * refers to the same two Yarns.  
 *************************************************************************/
public class Join implements Rhomb, Serializable {

    /** For serialization.  */
    public static final long serialVersionUID = 5508L;

    /**
     * For plotting in the plane.  
     * A reference point indicating the position of the corner of the rhomb
     * with even angles that has two outward-pointing arrows.  
     */
    private Point p;
    /**
     * The congruence class of rhomb.  
     * Given by the difference of the angles of its Yarns.  
     */
    public final int type;
    /** Orientation.  */
    private final int angle;
    /** The first Yarn incident with this point.  */
    private final Yarn y1;
    /** The second Yarn incident with this point.  */
    private final Yarn y2;

    /** A polygon for drawing.  */
    private transient SimplePolygon2D rhomb;
    /** The scale for drawing the rhomb.  */
    private double scale = RhombDisplay.SCALE;
    /** And the same polygon as a list of Points.  */
    private Point[] vertices;

    /** Method for saving.  */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject(p);
        stream.writeObject(vertices);
    }

    /** Method for restoring a saved Join.  */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        p = (Point)          stream.readObject();
        vertices = (Point[]) stream.readObject();
        rhomb = null;
        setRhomb();
    }

    /** Method for restoring a saved Join.  Empty.  */
    private void readObjectNoData() throws ObjectStreamException {
    }

    /**
     * Public constructor.  
     * @param p A reference point indicating the corner of the rhomb with even angle and two outward-pointing arrows.  
     * @param y1 The first Yarn incident with this Join.  
     * @param y2 The second Yarn incident with this Join.  
     */
    public Join(Point p, Yarn y1, Yarn y2) {
        this.p = p;
        this.y1 = y1;
        this.y2 = y2;
        vertices = new Point[4];
        // compute angle and type
        int temp = Math.abs(y1.getStartAngle()-y2.getStartAngle());
        if (temp >= Point.N()) temp = 2*Point.N()-temp;
        type = Point.N()/2 + 1 - (temp/2);
        angle = (((y1.ccwStart(y2)) ? y2.getStartAngle() : y1.getStartAngle()) + Point.N()) % (2*Point.N());
        // now make the rhomb
        setRhomb();
    }

    /**
     * Public static factory method.  
     * @param p A reference point indicating the corner of the rhomb with even angle and two outward-pointing arrows.  
     * @param y1 The first Yarn incident with this Join.  
     * @param y2 The second Yarn incident with this Join.  
     */
    public static Join createJoin(Point p, Yarn y1, Yarn y2) {
        return new Join(p,y1,y2);
    }

    /**
     * Produce a SimpleRhomb representation of the same rhomb.  
     * @return A SimpleRhomb version of this.  
     */
    public SimpleRhomb createSimpleRhomb() {
        return new SimpleRhomb(p,Point.createPoint(angle),Point.createPoint(angle-2*(Point.N()/2+1-type)),type,angle);
    }

    /**
     * Two Joins are equal if they represent the intersection of the same two Yarns.  
     */
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        Join t = (Join) obj;
        return ((this.y1.equals(t.y1)&&this.y2.equals(t.y2))||(this.y1.equals(t.y2)&&this.y2.equals(t.y1)));
    }

    /**
     * Use the sum of the hasCodes of the two Yarns.  
     */
    public int hashCode() {
        return y1.hashCode()+y2.hashCode();
    }

    /**
     * Getter method.  
     * @return The first Yarn.  
     */
    public Yarn getY1() {
        return y1;
    }

    /**
     * Getter method.  
     * @return The second Yarn.  
     */
    public Yarn getY2() {
        return y2;
    }

    /**
     * Getter method.  
     * @return The type.  
     */
    public int getType() {
        return type;
    }

    /**
     * Determine if this Join is on the edge of a pseudoline arrangement.  
     * @return true if, for each of the two Yarns, this is either the first or last Join.  
     */
    public boolean onEdge() {
        return ((this.equals(y1.lastJoin())||this.equals(y1.firstJoin()))&&(this.equals(y2.lastJoin())||this.equals(y2.firstJoin())));
    }

    /**
     * Remove this Join.  This method should only be used if this is on an edge.  
     */
    public void collapse() {
        y1.collapse(this);
    }

    /**
     * Shift the point by a given vector.  This means that, when we draw the rhomb 
     * represented by this Join, it will be shifted.  
     * @param vector The vector by which we shift point.  
     */
    public void shift(Point vector) {
        p = p.plus(vector);
        setRhomb();
    }

    /**
     * Reset the rhomb whenever it changes.  
     * The rhomb is determined entirely by other internal variables; this method resets 
     * the rhomb if those variables have changed.  
     */
    private void setRhomb() {
        double[] x = new double[4];
        double[] y = new double[4];
        double[] t = new double[2];
        Point temp = p;
        vertices[0] = temp;
        t = temp.project();
        x[0] = scale*t[0];
        y[0] = scale*t[1];
        temp = temp.minus(Point.createPoint(2*type - 1 + angle));
        vertices[1] = temp;
        t = temp.project();
        x[1] = scale*t[0];
        y[1] = scale*t[1];
        temp = temp.plus(Point.createPoint(angle));
        vertices[2] = temp;
        t = temp.project();
        x[2] = scale*t[0];
        y[2] = scale*t[1];
        temp = temp.plus(Point.createPoint(2*type - 1 + angle));
        vertices[3] = temp;
        t = temp.project();
        x[3] = scale*t[0];
        y[3] = scale*t[1];
        rhomb = new SimplePolygon2D(x,y);
    }

    /**
     * Getter method.  
     * @return A polygon object containing the 2d rhomb that this Join represents.  (2 coordinates.)  
     */
    public SimplePolygon2D getRhomb() {
        return rhomb;
    }

    /**
     * Set the scale for drawing.  
     * @param s The new scale used to draw this rhomb.  
     */
    public void setScale(double s) {
        scale = s;
        setRhomb();
    }

    /**
     * Get the scale for drawing.  
     * @return The scale for drawing.  
     */
    public double getScale() {
        return scale;
    }

    /**
     * Getter method.  
     * @return The angle that the base of this rhomb makes with the positive x-axis.  
     */
    public int getAngle() {
        return angle;
    }

    /**
     * Getter method.  
     * @return A list of Points describing the 2d rhomb that this Join represents.  ({@link Point#N()}-1 coordinates.)  
     */
    public Point[] getVertices() {
        return vertices;
    }

    /**
     * Getter method.  
     * @return The Point #p.
     */
    public Point getPoint() {
        return p;
    }

    /**
     * Getter method.  Broken.  Only here for interface compliance.  
     */
    public Point getV1() {
        return p;
    }

    /**
     * Getter method.  Broken.  Only here for interface compliance.  
     */
    public Point getV2() {
        return p;
    }

    /**
     * Basic String representation.  
     * @return A String containing j, followed by the unique identifying number of this Join.  
     */
    public String toString() {
        return "join : \n     yarn 1 : " + y1 + "\n    yarn 2 : " + y2;
    }

    /**
     * String representation for gap.  
     * @return A String containing various internal variables, formatted for use in gap.  
     */
    public String gapString() {
        return "        MkSubtile" + Point.N() + "( t, T, " + p + ", " + type + ", " + angle + " )";
    }

    /**
     * String representation for Postscript.  
     * @return A String giving instructions for how to draw this in Postscript.  
     */
    public String postscriptString() {
        return "gsave " + p.postscriptString() + Point.order() + "orth translate " + (angle*(180.0/Point.N())) + " rotate t" + type + " grestore";
    }

} // end of class Join

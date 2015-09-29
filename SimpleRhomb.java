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
 * A class representing a rhomb in a patch.  
 * The SimpleRhombs may shift about relative to one another.  
 *************************************************************************/
public class SimpleRhomb implements Rhomb, Serializable {

    /** For serialization.  */
    public static final long serialVersionUID = 5607L;

    /**
     * For plotting in the plane.  
     * A reference point indicating the position of the corner of the rhomb
     * with even angles that has two outward-pointing arrows.  
     */
    public Point p;
    /** A vector indicating the first outward-pointing edge from {@link #p}.  */
    public final Point v1;
    /** A vector indicating the second outward-pointing edge from {@link #p}. */
    public final Point v2;
    /** The congruence class of rhomb.  */
    public final int type;
    /** Orientation.  */
    public final int angle;
    /** The four vertices, represented as integer vectors of length {@link Point#N()}-1.  */
    private Point[] vertices = new Point[4];
    /** The scale for drawing.  */
    private double scale = RhombDisplay.SCALE;

    /**
     * The rhombs that share edges in common with this one.  This is at most
     * four rhombs.  If some edges are on the boundary, we indicate that by 
     * putting nulls there.  
     * The rhombs that share a v1-edge are listed at positions 0 and 3, the rhombs
     * that share a v2-edge are at 1 and 2.  The ones that share an edge of one 
     * and lie at the bases of the edges of the other type are listed at 0 and 1.  
     */
    public final SimpleRhomb[] adjacent = new SimpleRhomb[4];

    /** A polygon for drawing.  */
    private transient SimplePolygon2D rhomb;

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

    /** Method for restoring a saved SimpleRhomb.  Empty.  */
    private void readObjectNoData() throws ObjectStreamException {
    }

    /**
     * Public constructor.  
     * @param p A reference point indicating the corner of the rhomb with even angle and two outward-pointing arrows.  
     * @param v1 A vector representation of the first outward-pointing edge from p.  
     * @param v2 A vector representation of the second outward-pointing edge from p.  
     * @param type A number indicating the congruence class of the rhomb.  
     * @param angle An integer multiple of pi/{@link Point#N()} representing the
     * angle the rhomb makes with the positive x-axis.
     */
    public SimpleRhomb(Point p, Point v1, Point v2, int type, int angle) {
        this.p = p;
        this.v1 = v1;
        this.v2 = v2;
        this.type = type;
        this.angle = angle;
        vertices[0] = p;
        vertices[1] = p.plus(v2);
        vertices[2] = vertices[1].plus(v1);
        vertices[3] = p.plus(v1);
        // now make the rhomb
        setRhomb();
    }

    /**
     * Public static factory method.  
     * @param p A reference point indicating the corner of the rhomb with even angle and two outward-pointing arrows.  
     * @param v1 A vector representation of the first outward-pointing edge from p.  
     * @param v2 A vector representation of the second outward-pointing edge from p.  
     * @param type A number indicating the congruence class of the rhomb.  
     * @param angle An integer multiple of pi/{@link Point#N()} representing the
     * angle the rhomb makes with the positive x-axis.
     */
    public static SimpleRhomb createSimpleRhomb(Point p, Point v1, Point v2, int type, int angle) {
        return new SimpleRhomb(p,v1,v2,type,angle);
    }

    /**
     * This already is a SimpleRhomb, so return this.  
     */
    public SimpleRhomb createSimpleRhomb() {
        return this;
    }

    /**
     * Two SimpleRhombs are equal if they have the same type, the same angle, and the same corner.  
     */
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        SimpleRhomb t = (SimpleRhomb) obj;
        return ((this.p.equals(t.p))&&(this.type==t.type)&&(this.angle==t.angle));
    }

    /**
     * Use the corner, the type, and the angle.  
     */
    public int hashCode() {
        return p.hashCode() + 19*type + 361*angle;
    }

    /**
     * Getter method.  
     * @return The type.  
     */
    public int getType() {
        return type;
    }
    /**
     * Getter method.  
     * @return The point.  
     */
    public Point getPoint(){
    	return p;
    }

    /**
     * Getter method.  
     * @return The vertices.  
     */
    public Point[] getVert(){
    	return vertices;
    }

    /**
     * Rotate a SimpleRhomb by the given angle
     * @param rotationAngle
     * @return rotated SimpleRhomb
     */
    public SimpleRhomb rotate(int rotationAngle) {
    	 Point newV1 = v1.rotate(rotationAngle);
    	 Point newV2 = v2.rotate(rotationAngle);
    	 Point newP  =  p.rotate(rotationAngle);
    	 int newAngle = (angle + rotationAngle)%(2*Point.N());
    	 return new SimpleRhomb(newP, newV1, newV2, type, newAngle);
    	}

    /**
     * Replace the given SimpleRhomb with null in the adjacent list for this.  
     * Do this when a rhomb is collapsed.  
     * @param neighbour The adjacent rhomb that is being removed.  If it's null, 
     * there will be trouble.  
     */
    private void dropNeighbour(SimpleRhomb neighbour) {
        for (int i = 0; i < 3; i++) {
            if (neighbour.equals(adjacent[i])) {
                adjacent[i] = null;
                break;
            }
        }
    }

    /**
     * Determine if this SimpleRhomb is on the edge of a pseudoline arrangement.  
     * @return true if, for each of the two Yarns, this is either the first or last SimpleRhomb.  
     */
    public boolean onEdge() {
        return true;
    }

    /**
     * Remove this SimpleRhomb.  This method should only be used if this is on an edge.  
     */
    public void collapse() {
        for (int i = 0; i < 3; i++) if (adjacent[i]!=null) adjacent[i].dropNeighbour(this);
    }

    /**
     * Shift the point by a given vector.  This means that, when we draw the rhomb 
     * represented by this SimpleRhomb, it will be shifted.  
     * @param vector The vector by which we shift point.  
     */
    public void shift(Point vector) {
        p = p.plus(vector);
        for (int i = 0; i < 4; i++) vertices[i] = vertices[i].plus(vector);
        setRhomb();
    }

    /**
     * Flip the positions of the given three rhombs.  
     * We assume, but do not check, that they form a hex.  
     * This method is called in {@link SimpleHex#flip}.  The rhombs aren't shifted here; we shift 
     * them in that method instead.  
     * @param triple The three rhombs that we flip.  
     * @param directions For each rhomb in triple, do we shift it by the positive or the negative
     * of the direction vector common to the two other rhombs?
     * @return A list of {@link SimpleHex}es created or destroyed by this flip.  
     */
    public static List<Hex> flip(SimpleRhomb[] triple,boolean[] directions) {
        List<Hex> output = new LinkedList<>();
        // stores indices in the adjacent lists of the elements of triple.  
        // the number at [i][j] is the index in the adjacent list of triple[i] 
        // of triple[i+1+j (mod 3)].  
        int[][] r = new int[3][2];
        // iterate over adjacent indices
        for (int i = 0; i < 4; i++) {
            // iterate over rhombs in the triple
            for (int j = 0; j < 3; j++) {
                if (triple[(j+1)%3].equals(triple[j].adjacent[i])) r[j][0] = i;
                else if (triple[(j+2)%3].equals(triple[j].adjacent[i])) r[j][1] = i;
            }
        }
        // we've made note of the outside rhombs.  Now make switches.  
        // iterate over pairs of rhombs (there are three, each with 
        // indices of the form (i,i+1)).
        for (int i = 0; i < 3; i++) {
            // record the SimpleHexes that will be destroyed. 
            if (triple[i].adjacent[3-r[i][0]]!=null&&triple[i].adjacent[3-r[i][0]].shareEdge(triple[i].adjacent[3-r[i][1]])) {
                output.add(new SimpleHex(triple[i],triple[i].adjacent[3-r[i][0]],triple[i].adjacent[3-r[i][1]]));
            }
        }

        for (int i = 0; i < 3; i++) {
            if (triple[i].adjacent[3-r[i][0]]!=null) triple[i].adjacent[3-r[i][0]].swapAdjacent(triple[i],triple[(i+1)%3]);
            if (triple[(i+1)%3].adjacent[3-r[(i+1)%3][1]]!=null) triple[(i+1)%3].adjacent[3-r[(i+1)%3][1]].swapAdjacent(triple[(i+1)%3],triple[i]);
            triple[i].adjacent[r[i][0]] = triple[(i+1)%3].adjacent[3-r[(i+1)%3][1]];
            triple[(i+1)%3].adjacent[r[(i+1)%3][1]] = triple[i].adjacent[3-r[i][0]];
            triple[i].adjacent[3-r[i][0]] = triple[(i+1)%3];
            triple[(i+1)%3].adjacent[3-r[(i+1)%3][1]] = triple[i];
            // shift the rhomb
            triple[i].shift((directions[i]) ? triple[(i+1)%3].commonEdge(triple[(i+2)%3]) : Point.ZERO().minus(triple[(i+1)%3].commonEdge(triple[(i+2)%3])));
        }
        for (int i = 0; i < 3; i++) {
            // record the SimpleHexes that will be created. 
            if (triple[i].adjacent[r[i][0]]!=null&&triple[i].adjacent[r[i][0]].shareEdge(triple[i].adjacent[r[i][1]])) {
                output.add(new SimpleHex(triple[i],triple[i].adjacent[r[i][0]],triple[i].adjacent[r[i][1]]));
            }
        }
        return output;
    }

    /**
     * Reset the rhomb whenever it changes.  
     * The rhomb is determined entirely by other internal variables; this method resets 
     * the rhomb if those variables have changed.  
     */
    private void setRhomb() {
        rhomb = new SimplePolygon2D(new Point2D[] {vertices[0].getPoint2D().scale(scale),vertices[1].getPoint2D().scale(scale),vertices[2].getPoint2D().scale(scale),vertices[3].getPoint2D().scale(scale)});
    }

    /**
     * Reset the scale for drawing the Rhomb.  
     * @param s The new scale.  
     */
    public void setScale(double s) {
        scale = s;
        setRhomb();
    }

    /**
     * Get the scale for drawing the Rhomb.  
     * @return The scale for drawing the Rhomb.  
     */
    public double getScale() {
        return scale;
    }

    /**
     * Find the edge direction vector that this and another rhomb have in common.  
     * @param other The other rhomb.  
     * @return The edge direction vector that this has in common with other.  
     * Return null if there is no common vector.  
     */
    public Point commonEdge(SimpleRhomb other) {
        if (other==null) return null;
        if (this.v1.equals(other.v1)||this.v1.equals(other.v2)) return v1;
        else if (this.v2.equals(other.v1)||this.v2.equals(other.v2)) return v2;
        else return null;
    }

    /**
     * Determine if this shares an edge with another SimpleRhomb.  
     * @param other The other rhomb.  
     * @return true if this and other share an edge.  
     */
    public boolean shareEdge(SimpleRhomb other) {
        if (other==null) return false;
        boolean oneInCommon = false;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (this.vertices[i].equals(other.vertices[j])) {
                    if (oneInCommon) return true;
                    else oneInCommon = true;
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Getter method.  
     * @return A polygon object containing the 2d rhomb that this SimpleRhomb represents.  (2 coordinates.)  
     */
    public SimplePolygon2D getRhomb() {
        return rhomb;
    }

    /**
     * Getter method.  
     * @return The angle that the base of this rhomb makes with the positive x-axis.  
     */
    public int getAngle() {
        return angle;
    }

    /**
     * Add another SimpleRhomb to the adjacency list.  
     * @param other The rhomb we add.  
     * @param a The angle that other makes with the positive x-axis.  
     * @param first Tells us if other is listed before this in a Yarn.  
     */
    public void addAdjacent(SimpleRhomb other, int a, boolean first) {
        if ((a%(2*Point.N()))==angle) {
            if (first) adjacent[0] = other;
            else adjacent[3] = other;
        } else {
            if (first) adjacent[1] = other;
            else adjacent[2] = other;
        }
    }

    /**
     * Replace a SimpleRhomb in the adjacency list with another one.  
     * @param out The rhomb we remove from the list.  
     * @param in The rhomb with which we replace it.  
     */
    private void swapAdjacent(SimpleRhomb out, SimpleRhomb in) {
        for (int i = 0; i < 4; i++) {
            if (out.equals(adjacent[i])) {
                adjacent[i] = in;
                break;
            }
        }
    }

    /**
     * Method for debugging.  
     * Determine if this is doubly-adjacent to any other rhomb (it shouldn't be).  
     * @return true if this has another rhomb more than once in its adjacency list.  
     */
    private boolean duplicateAdjacent() {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if (i==j) continue;
                if (adjacent[i]!=null&&adjacent[i].equals(adjacent[j])) return true;
            }
        }
        return false;
    }

    /**
     * String representation for Postscript.  
     * @return A String giving instructions for how to draw this in Postscript.  
     */
    public String postscriptString() {
        return "gsave " + p.postscriptString() + Point.order() + "orth translate " + (angle*(180.0/Point.N())) + " rotate t" + type + " grestore";
    }

    /**
     * String representation for gap.  
     * @return A String containing various internal variables, formatted for use in gap.  
     */
    public String gapString() {
        return "        MkSubtile" + Point.N() + "( t, T, " + p + ", " + type + ", " + angle + " )";
    }

    /**
     * String representation of this.  
     * @return A String representing this.  
     */
    public String toString() {
        return "SimpleRhomb point : " + p + ". angle : " + angle + ". type: " + type + ".";
    }

    /**
     * String representation of this, including all the rhombs to which it's adjacent.  
     * @return A String representing this and all its adjacent rhombs.  
     */
    public String adjacencyString() {
        return this + "\nadjacent to : \n    " + adjacent[0] + "\n    " + adjacent[1] + "\n    " + adjacent[2] + "\n    " + adjacent[3];
    }

    /**
     * 
     */
    public SimpleRhomb transform(int rot, Point move){
    	SimpleRhomb r = rotate(rot).translate(move);
    	return r;
    }

    /**
     * Shift the point by a given vector.  This means that, when we draw the rhomb 
     * represented by this SimpleRhomb, it will be shifted.  
     * @param move The vector by which we shift point.  
     */
    public SimpleRhomb translate(Point move) {
        Point newP = p.plus(move);
        SimpleRhomb r = createSimpleRhomb(newP, v1, v2, type, angle);
        return r;
    }

    /**
     * Return a List of {@link Point}s representing the inflation of this rhomb 
     * by the given factor, with edges distorted according to the given 
     * edge sequence.  
     * @param infl The inflation factor, represented as a matrix.  
     * @param edge The edge sequence used to distort the edges.  
     * @return infl * this, with its edges distorted according to the 
     * rule specified by edge.  
     */
    public List<Point> supertile(Point[] infl, int[] edge) {
        int even = Point.N()+1-2*type;
        List<Point> output = new ArrayList<>();
        Point current = vertices[0].multiply(infl);
        for (int i = 0; i < edge.length; i++) {
            output.add(current);
            current = current.plus(Point.createPoint(this.angle+edge[i]));
        }
        for (int i = 0; i < edge.length; i++) {
            output.add(current);
            current = current.plus(Point.createPoint(this.angle-even+edge[i]));
        }
        for (int i = 0; i < edge.length; i++) {
            output.add(current);
            current = current.plus(Point.createPoint(this.angle+Point.N()+edge[edge.length-i-1]));
        }
        for (int i = 0; i < edge.length; i++) {
            output.add(current);
            current = current.plus(Point.createPoint(this.angle-even+Point.N()+edge[edge.length-i-1]));
        }
        return output;
    }

    /**
     * Return a SimlePolygon2D representing the inflation of this rhomb 
     * by the given factor, with edges distorted according to the given 
     * edge sequence.  
     * @param infl The inflation factor, represented as a matrix.  
     * @param edge The edge sequence used to distort the edges.  
     * @return infl * this, with its edges distorted according to the 
     * rule specified by edge.  
     */
    public SimplePolygon2D outline(Point[] infl, int[] edge) {
        List<Point2D> output = new ArrayList<>();
        for (Point p : supertile(infl,edge)) output.add(p.getPoint2D().scale(scale));
        return new SimplePolygon2D(output);
    }

} // end of class SimpleRhomb

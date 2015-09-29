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

/**
 * Three {@link Yarn}s that all intesect each other.  
 * Any one of these Yarns intersects the other two consecutively, with no other Yarn
 * intersections ({@link Join}s) between.  
 * This corresponds to a trio of rhombs, all of which share a common point and any two
 * of which share a common edge.  The union of three such rhombs is a hexagon, and that
 * hexagon can be flipped by reversing the order of all pairs of Yarn intersections.  
 * We do not check for these properties on construction; we merely assume that they
 * are satisfied.  
 */
public class Triple implements Hex, Serializable {

    /** For serialization. */
    public static final long serialVersionUID = 5512L;

    /** The three Yarns. */
    private final Yarn[] y;
    /** The three Joins. */
    public Join[] j;
    /** A representation of this as a hexagon. */
    private transient SimplePolygon2D hex;


    /** Private constructor.  */
    private Triple(Yarn y0, Yarn y1, Yarn y2) {
        y = new Yarn[] {y0,y1,y2};
        j = new Join[] {y0.joinWith(y1),y1.joinWith(y2),y2.joinWith(y0)};
        hex = makeHex();
    }

    /**
     * Public static factory method.  
     * The three Yarns are assumed to Join each other, with no other Yarns in between Joins.  
     * This property is not enforced.  
     * @param y0 The first Yarn.  
     * @param y1 The second Yarn.  
     * @param y2 The third Yarn.  
     */
    public static Triple createTriple(Yarn y0, Yarn y1, Yarn y2) {
        return new Triple(y0,y1,y2);
    }

    /**
     * Public static factory method.  
     * Create a Triple from an array of three Yarns.  We don't check to make 
     * sure the array is the right length, or the Yarns have the right 
     * adjacency properties.  
     * @param yy The array of three Yarns from which we build a Triple.  
     */
    public static Triple createTriple(Yarn[] yy) {
        Yarn y0 = yy[0];
        Yarn y1 = yy[1];
        Yarn y2 = yy[2];
        return new Triple(y0,y1,y2);
    }

    /** Method for saving.  */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
    }

    /** Method for restoring a saved Join.  */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        hex = makeHex();
    }

    /** Method for restoring a saved Join.  Empty.  */
    private void readObjectNoData() throws ObjectStreamException {
    }

    /**
     *  Getter method.  
     *  @return A list of the three Joins between the three Yarns.  
     */
    public Rhomb[] getJoins() {
        return j;
    }

    /**
     *  Getter method.  
     *  I hope to remove this soon.
     *  @return A list of the three Yarns that constitute this Triple.  
     */
    public Yarn[] getYarns() {
        return y;
    }

    /**
     *  Getter method.  
     *  @return A polygon equal to the union of the three rhombs represented by the three Joins.  
     */
    public SimplePolygon2D getHex() {
        return hex;
    }

    /**
     *  Set the hexagon.
     *  @return A polygon equal to the union of the three rhombs represented by the three Joins.  
     */
    private SimplePolygon2D makeHex() {
        Point[] p0 = j[0].getVertices();
        Point[] p1 = j[1].getVertices();
        Point[] p2 = j[2].getVertices();
        int i1 = -1;
        int j1 = -1;
        int i2 = -1;
        int j2 = -1;
        try {
            // find the indices where p0 and p1 overlap
            outerloop:
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (p0[i].equals(p1[j])) {
                        i1 = i;
                        j1 = j;
                        break outerloop;
                    }
                }
            }
            // make sure these are listed such that the p0-vertices
            // appear consecutively in a counter-clockwise traversal
            if (p0[(i1==0) ? 3 : i1-1].equals(p1[(j1+1)%4])) {
                i2 = i1;
                i1 = (i1==0) ? 3 : i1-1;
                j2 = j1;
                j1 = (j1+1)%4;
            } else {
                i2 = (i1+1)%4;
                j2 = (j1==0) ? 3 : j1-1;
            }
            Point2D[] output = new Point2D[6];
            for (int k = 0; k < 4; k++) {
                if (p2[k].equals(p0[i2])) {
                    output[0] = p0[(i2+1)%4].getPoint2D();
                    output[1] = p0[(i2+2)%4].getPoint2D();
                    output[2] = p1[(j2+1)%4].getPoint2D();
                    output[3] = p1[(j2+2)%4].getPoint2D();
                    output[4] = p2[(k+1)%4].getPoint2D();
                    output[5] = p2[(k+2)%4].getPoint2D();
                    return new SimplePolygon2D(output);
                } else if (p2[k].equals(p0[i1])) {
                    output[0] = p1[(j1+1)%4].getPoint2D();
                    output[1] = p1[(j1+2)%4].getPoint2D();
                    output[2] = p0[(i1+1)%4].getPoint2D();
                    output[3] = p0[(i1+2)%4].getPoint2D();
                    output[4] = p2[(k+1)%4].getPoint2D();
                    output[5] = p2[(k+2)%4].getPoint2D();
                    return new SimplePolygon2D(output);
                }
            }
            throw new IllegalArgumentException("Something's wrong with this hex.");
        } catch (Exception e) {
            return null;
        }
    }

    /**
     *  Determine if this represents a hexagon--i.e., if the three Yarns still satisfy the adjacency conditions.  
     *  @return true if the three Yarns satisfy the adjacency conditions, false otherwise.  
     */
    public boolean valid() {
        return (y[0].consecutive(y[1],y[2])&&y[1].consecutive(y[2],y[0])&&y[2].consecutive(y[0],y[1]));
    }

    /**
     *  Determine if this contains a given Rhomb.  
     *  This only contains Joins, so any other type of Rhomb will return false.
     *  @param jj The Rhomb we're looking for.  
     *  @return true if this contains jj, false otherwise.  
     */
    public boolean contains(Rhomb jj) {
        return (j[0].equals(jj)||j[1].equals(jj)||j[2].equals(jj));
    }

    /**
     *  Determine if this contains a given Yarn.  
     *  @param yy The Yarn we're looking for.  
     *  @return true if this contains yy, false otherwise.  
     */
    public boolean contains(Yarn yy) {
        return (y[0].equals(yy)||y[1].equals(yy)||y[2].equals(yy));
    }

    /**
     *  Determine if this contains a given pair of Yarns (order doesn't matter).  
     *  @param yy The first Yarn we're looking for.  
     *  @param z  The second Yarn we're looking for.  
     *  @return true if this contains yy and z, false otherwise.  
     */
    public boolean contains(Yarn yy, Yarn z) {
        if (y[0].equals(yy)) return (y[1].equals(z)||y[2].equals(z));
        if (y[1].equals(yy)) return (y[2].equals(z)||y[0].equals(z));
        if (y[2].equals(yy)) return (y[0].equals(z)||y[1].equals(z));
        return false;
    }

    /**
     *  Determine if this contains two Yarns in common with another Triple.  
     *  @param t  The other Triple.  
     *  @return true if this contains two Yarns that t also contains.  
     */
    public boolean doubleOverlap(Triple t) {
        int overlaps = 0;
        for (int i = 0; i < y.length; i++) {
            for (int j = 0; j < y.length; j++) {
                if (this.y[i].equals(t.y[j])) overlaps++;
            }
        }
        return overlaps>1;
    }

    /**
     *  Two Triples are equal if they contain the same three Yarns.  
     */
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        Triple t = (Triple) obj;
        int count1 = 0;
        int count2 = 0;
        for (int i = 0; i < y.length; i++) {
            for (int j = 0; j < y.length; j++) {
                if (this.y[i].equals(t.y[j])) {
                    count1 += i+1;
                    count2 += j+1;
                    break;
                }
            }
        }
        return (count1==6&&count2==6);
    }

    /**
     *  Pass in a list of Joins that contains the three Joins in this Triple.  
     *  Then create a SimpleHex out of the three SimpleRhombs at the corresponding 
     *  positions in a list of SimpleRhombs.  
     *  @param allJoins A list of joins that contains the three Joins that constitute this hex.  
     *  @param newJoins A list of SimpleRhombs that correspond to the Joins in allJoins.  
     *  @return A SimpleHex made from the three SimpleRhombs in newJoins that lie at the
     *  same indices as the Joins in this lie in allJoins.  
     */
    public SimpleHex createSimpleHex(List<Rhomb> allJoins, List<Rhomb> newJoins) {
        int i = allJoins.indexOf(j[0]);
        SimpleRhomb r0 = (SimpleRhomb)newJoins.get(i);
        i = allJoins.indexOf(j[1]);
        SimpleRhomb r1 = (SimpleRhomb)newJoins.get(i);
        i = allJoins.indexOf(j[2]);
        SimpleRhomb r2 = (SimpleRhomb)newJoins.get(i);
        return new SimpleHex(r0,r1,r2);
    }

    /**
     *  Suppose this Triple has just been flipped.  
     *  Create a list of new Triples that are created as a result.  
     *  @return A list of Triples that share a Join (two Yarns) with this one.  
     */
    public List<Triple> newTriples() {
        List<Triple> output = new LinkedList<>();
        Yarn z1 = y[0].nextYarn(y[1],y[2]);
        Yarn z2 = y[2].nextYarn(y[1],y[0]);
        if (z1!=null&&z1.equals(z2)) output.add(new Triple(z1,y[0],y[2]));
        z1 = y[0].nextYarn(y[2],y[1]);
        z2 = y[1].nextYarn(y[2],y[0]);
        if (z1!=null&&z1.equals(z2)) output.add(new Triple(z1,y[0],y[1]));
        z1 = y[1].nextYarn(y[0],y[2]);
        z2 = y[2].nextYarn(y[0],y[1]);
        if (z1!=null&&z1.equals(z2)) output.add(new Triple(z1,y[1],y[2]));
        return output;
    }

    /**
     *  Flip the Yarns in this Triple.  
     *  Each Yarn has a list of other Yarns that it meets; this method reverses
     *  the positions of any two of the Yarns in this Triple in the list of the
     *  third Yarn.  
     *  @return A list of all Triples created or destroyed by this flip.  
     */
    public List<Hex> flip() {
        if (y[0].hits(y[1],y[2])==y[0].ccwStart(y[2])) {
            y[0].joinWith(y[1]).shift(Point.createPoint(y[2].getStartAngle()));
        } else {
            y[0].joinWith(y[1]).shift(Point.createPoint(y[2].getEndAngle()));
        }
        if (y[1].hits(y[2],y[0])==y[1].ccwStart(y[0])) {
            y[1].joinWith(y[2]).shift(Point.createPoint(y[0].getStartAngle()));
        } else {
            y[1].joinWith(y[2]).shift(Point.createPoint(y[0].getEndAngle()));
        }
        if (y[2].hits(y[0],y[1])==y[2].ccwStart(y[1])) {
            y[2].joinWith(y[0]).shift(Point.createPoint(y[1].getStartAngle()));
        } else {
            y[2].joinWith(y[0]).shift(Point.createPoint(y[1].getEndAngle()));
        }
        y[0].swap(y[1],y[2]);
        y[1].swap(y[2],y[0]);
        y[2].swap(y[0],y[1]);
        return Yarn.surroundingTriples(y[0],y[1],y[2]);
    }

} // end of class Triple

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
 * This corresponds to a trio of rhombs, all of which share a common point and any two
 * of which share a common edge.  The union of three such rhombs is a hexagon, and that
 * hexagon can be flipped by shifting all the rhombs to opposite sides of the hex.  
 */
public class SimpleHex implements Hex, Serializable {

    /** For serialization. */
    public static final long serialVersionUID = 7513L;

    /** A representation of this as a hexagon. */
    private final SimplePolygon2D hex;

    /** The three rhombs in this hex. */
    public final SimpleRhomb[] rhombs = new SimpleRhomb[3];

    /**
     * A set of instructions for flipping the hex.  
     * For each rhomb in triple, do we shift it by the positive or the negative
     * of the direction vector common to the two other rhombs?
     */
    private final boolean[] directions = new boolean[3];

    /**
     *  Public constructor.  No checks. 
     *  We make a hexagon out of the three given rhombs.  
     */
    public SimpleHex(SimpleRhomb r0, SimpleRhomb r1, SimpleRhomb r2) {
        rhombs[0] = r0;
        rhombs[1] = r1;
        rhombs[2] = r2;
        // Set the shift directions.
        // There are four possibilities, depending on the orientations of the three edges
        // that meet at the common vertex of all three rhombs.  
        // 1) They all point away from the common vertex.  
        // 2) Two point away, one points toward.  
        // 3) One points away, two point toward.  
        // 4) They all point toward.  
        // This can be determined by counting the number of distinct corner points p 
        // of the three rhombs.  
        // 1) Only one corner means that all edges point away.  
        // 2) Two distinct corners means that two point away, one points toward.  
        // Three distinct corners means either: 
        // 3) One points away, two point toward; or
        // 4) they all point toward.  
        int corners = 3; // the number of distinct corners of the three rhombs
        // the rhomb with a corner that doesn't match the other two, if there is such a rhomb: 
        int oddOne = -1;
        if (rhombs[0].p.equals(rhombs[1].p)) {
            corners--;
            directions[0] = true;
            directions[1] = true;
            oddOne = 2;
        }
        if (rhombs[0].p.equals(rhombs[2].p)) {
            corners--;
            directions[0] = true;
            directions[2] = true;
            oddOne = 1;
        }
        if (rhombs[1].p.equals(rhombs[2].p)) {
            corners--;
            directions[1] = true;
            directions[2] = true;
            oddOne = 0;
        }
        // At this point there are three possible values for corners:
        // 0: All three rhombs have the same corner.  
        // 2: Two rhombs share a corner, and the third rhomb has a different one.  
        // 3: All three rhombs have different corners.  
        // We make the hex differently in each case.  
        Point2D[] output = new Point2D[6];
        Point current = Point.ZERO();
        if (corners==0) {
            current = rhombs[0].p.plus(rhombs[0].v2);
            output[0] = current.getPoint2D();
            current = current.plus(rhombs[0].v1);
            output[1] = current.getPoint2D();
            current = rhombs[0].p.plus(rhombs[0].v1);
            output[2] = current.getPoint2D();
            current = current.plus(rhombs[1].commonEdge(rhombs[2]));
            output[3] = current.getPoint2D();
            current = rhombs[0].p.plus(rhombs[1].commonEdge(rhombs[2]));
            output[4] = current.getPoint2D();
            current = current.plus(rhombs[0].v2);
            output[5] = current.getPoint2D();
        } else if (corners==2) {
            current = rhombs[oddOne].p.plus(rhombs[oddOne].v2);
            output[0] = current.getPoint2D();
            current = current.plus(rhombs[oddOne].v1);
            output[1] = current.getPoint2D();
            current = rhombs[oddOne].p.plus(rhombs[oddOne].v1);
            output[2] = current.getPoint2D();
            current = current.minus(rhombs[(oddOne+1)%3].commonEdge(rhombs[(oddOne+2)%3]));
            output[3] = current.getPoint2D();
            current = current.minus(rhombs[oddOne].v1);
            output[4] = current.getPoint2D();
            current = current.plus(rhombs[oddOne].v2);
            output[5] = current.getPoint2D();
        } else if (corners==3) {
            // Now we have to identify whether we're in case 3) or 4).
            boolean allIn = true;
            for (oddOne = 0; oddOne < 3; oddOne++) {
                // we're doing our first assignment of current here
                current = rhombs[oddOne].p.plus(rhombs[oddOne].v1);
                if (current.equals(rhombs[(oddOne+1)%3].p)||current.equals(rhombs[(oddOne+2)%3].p)) {
                    allIn = false;
                    directions[oddOne] = true;
                    break;
                }
            }
            oddOne = (oddOne<3) ? oddOne : 2;
            output[0] = current.getPoint2D();
            current = rhombs[oddOne].p;
            output[1] = current.getPoint2D();
            current = rhombs[oddOne].p.plus(rhombs[oddOne].v2);
            output[2] = current.getPoint2D();
            // here's the difference between case 3) and case 4).
            current = (allIn) ? current.minus(rhombs[(oddOne+1)%3].commonEdge(rhombs[(oddOne+2)%3])) : current.plus(rhombs[(oddOne+1)%3].commonEdge(rhombs[(oddOne+2)%3]));
            output[3] = current.getPoint2D();
            current = current.plus(rhombs[oddOne].v1);
            output[4] = current.getPoint2D();
            current = current.minus(rhombs[oddOne].v2);
            output[5] = current.getPoint2D();
        } else {
            throw new IllegalArgumentException("Strange hex.");
        }
        hex = new SimplePolygon2D(output);
    } // end of constructor

    /**
     *  Getter method.  
     *  @return A list of the three rhombs in this hex.  
     */
    public Rhomb[] getJoins() {
        return rhombs;
    }

    /**
     *  Getter method.  
     *  @return A polygon equal to the union of the three rhombs.
     */
    public SimplePolygon2D getHex() {
        return hex;
    }

    /**
     *  Simplification method.  
     *  @return This is already a SimpleHex, so just return this.  
     */
    public SimpleHex createSimpleHex(List<Rhomb> allJoins, List<Rhomb> newJoins) {
        return this;
    }

    /**
     *  Determine if this represents a hexagon--i.e., if the three rhombs all touch one another.
     *  @return true if the three rhombs all touch one another.  
     */
    public boolean valid() {
        int[] counts = new int[3];
        for (int i = 0; i < 3; i++) {
            if (rhombs[1].equals(rhombs[0].adjacent[i])||rhombs[2].equals(rhombs[0].adjacent[i]))
                counts[0]++;
            if (rhombs[0].equals(rhombs[1].adjacent[i])||rhombs[2].equals(rhombs[1].adjacent[i]))
                counts[1]++;
            if (rhombs[0].equals(rhombs[2].adjacent[i])||rhombs[1].equals(rhombs[2].adjacent[i]))
                counts[2]++;
        }
        return (counts[0]==2&&counts[1]==2&&counts[2]==2);
    }

    /**
     *  Determine if this contains a given Rhomb.  
     *  This only contains {@link SimpleRhomb}s, so any other type of Rhomb will return false.
     *  @param jj The Rhomb we're looking for.  
     *  @return true if this contains jj, false otherwise.  
     */
    public boolean contains(Rhomb jj) {
        return (rhombs[0].equals(jj)||rhombs[1].equals(jj)||rhombs[2].equals(jj));
    }

    /**
     *  Determine if this contains a {@link SimpleRhomb} in common with another SimpleHex.  
     *  @param t  The other SimpleHex.  
     *  @return true if this contains a SimpleRhomb that t also contains.  
     */
    public boolean doubleOverlap(SimpleHex t) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (this.rhombs[i].equals(t.rhombs[j])) return true;
            }
        }
        return false;
    }

    /**
     *  Two SimpleHexes are equal if they contain the same three SimpleRhombs.  
     */
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        SimpleHex t = (SimpleHex) obj;
        int count1 = 0;
        int count2 = 0;
        for (int i = 0; i < rhombs.length; i++) {
            for (int j = 0; j < rhombs.length; j++) {
                if (this.rhombs[i].equals(t.rhombs[j])) {
                    count1 += i+1;
                    count2 += j+1;
                    break;
                }
            }
        }
        return (count1==6&&count2==6);
    }

    /**
     *  Flip the Rhombs in this SimpleHex.  
     *  @return A list of all SimpleHexes created or destroyed by this flip.  
     */
    public List<Hex> flip() {
        List<Hex> output = SimpleRhomb.flip(rhombs,directions);
        // change the directions for the next flip
        directions[0] = !directions[0];
        directions[1] = !directions[1];
        directions[2] = !directions[2];
//        for (int i = 0; i < 3; i++) {
//            System.out.println(rhombs[i] + " Adjacent to : ");
//            for (int j = 0; j < 4; j++) {
//                System.out.println(rhombs[i].adjacent[j]);
//            }
//        }
        return output;
    }

} // end of class SimpleHex

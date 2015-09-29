import java.lang.Math.*;
import java.util.Map;
import java.util.Stack;
import java.util.AbstractMap.*;
import java.io.*;
import java.io.PrintWriter;
import math.geom2d.polygon.SimplePolygon2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.Point2D;
import java.util.Arrays;

/*************************************************************************
 * A class representing the end of a {@link Yarn} (which is a pseudoline).  
 * Every Yarn begins and ends at a Terminus, and each Terminus is the
 * beginning or ending point of exactly one Yarn.  
 * Each Terminus has an associated angle, which is the angle of the family
 * of parallel rhomb edges that define the Yarn.  
 *************************************************************************/
public class Terminus implements Serializable {

    /** For serialization */
    public static final long serialVersionUID = 5507L;

    /**
     * The position of this in the big list of termini. 
     * This is for outside classes to keep track of termini.  
     */
    private final int index;

    /**
     * The angle of the boundary subedge where this is located.
     */
    private int angle;

    /**
     * Private constructor.  
     * @param i The index of this in a big list of Termini.  
     * @param a The angle.
     */
    private Terminus(int i, int a) {
        angle = a%(2*Point.N());
        if (angle<=0) angle += 2*Point.N();
        index = i;
    }

    /**
     * Public static factory method
     * @param i The index of this in a big list of Termini.  
     * @param a The angle.
     */
    public static Terminus createTerminus(int i, int a) {
        return new Terminus(i,a);
    }


    /**
     * Two Termini are equal if they have the same indices and angles.
     */
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        Terminus t = (Terminus) obj;
        return (this.index==t.index&&this.angle==t.angle);
    }

    /**
     * hashCode override.  
     */
    public int hashCode() {
        int prime = 123;
        int result = 13;
        result += this.angle*prime;
        result += this.index*prime;
        return result;
    }

    /**
     * A getter method.  
     * @return The index.  
     */
    public int getIndex() {
        return index;
    }

    /**
     * A getter method.  
     * @return The angle.  
     */
    public int getAngle() {
        return angle;
    }

    /**
     * Simple toString method
     * @return A String with the index and angle.  
     */
    public String toString() {
        return ((index<10) ? "  " : " ") + index + " " + angle;
    }

    /**
     * Swap the angles of two Termini.  
     * They're assumed to be adjacent, but we don't check.  
     * @param t The other Terminus.  
     */
    public void swapAngles(Terminus t) {
        int k = t.angle;
        t.angle = this.angle;
        this.angle = k;
    }

} // end of class Terminus

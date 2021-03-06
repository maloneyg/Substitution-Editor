//package Project;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.io.*;
import java.lang.Math.*;
import java.util.Map;
import java.util.Stack;
import java.util.AbstractMap.*;
import java.io.PrintWriter;
import math.geom2d.Point2D;

/*************************************************************************
 * Represents a point in the module generated by the nth roots of unity 
 * in the complex plane.  
 * Each Point can be expressed as an integer combination of the vectors
 * (cos (k pi/n), sin(k pi/n)).  The main field is an int array, the kth
 * entry of which is the coefficient of (cos (k pi/n), sin(k pi/n)).
 *
 *************************************************************************/
public class Point implements Serializable {

    /** For serialization. */
    public static final long serialVersionUID = 5511L;

    /** All angles are integer multiples of 180/N. */
    private static int N;

    /** An array of coefficients. */
    private final int[] point;

    /** An array of all points that are a distance of 1 from the origin. */
    private static Point[] STAR;

    /** cos(i pi/N) */ 
    private static double[] COS;
    /** sin(i pi/N) */
    private static double[] SIN;

    /** The zero vector. */
    private static Point Z;

    /** A String representing the order of symmetry. */
    private static String ORDER;

    static { // initialize N
        setN(7);
    }

    /**
     * Set the order of symmetry. 
     * @param n The new order of symmetry.  
     */
    public static void setN(int n) {
        if (n<5) throw new IllegalArgumentException("Trying to set N = " + n + ", which is too low (n >= 5 is required).");
        N = n;

        Point[] preStar = new Point[2*N];
        for (int i = 0; i < N-1; i++) {
            int[] plus = new int[N-1];
            int[] minus = new int[N-1];
            for (int j = 0; j < N-1; j++) {
                plus[j] = (j==i) ? 1 : 0;
                minus[j] = (j==i) ? -1 : 0;
            }
            preStar[i] = new Point(plus);
            preStar[N+i] = new Point(minus);
        }
        int[] plus = new int[N-1];
        int[] minus = new int[N-1];
        for (int j = 0; j < N-1; j++) {
            minus[j] = (j%2==1) ? -1 : 1;
            plus[j] = (j%2==1) ? 1 : -1;
        }
        preStar[N-1] = new Point(plus);
        preStar[2*N-1] = new Point(minus);
        STAR = preStar;

        double[] preCos = new double[N-1];
        double[] preSin = new double[N-1];
        for (int i = 0; i < N-1; i++) {
            preCos[i] = Math.cos(i*Math.PI/((double)N));
            preSin[i] = Math.sin(i*Math.PI/((double)N));
        }
        COS = preCos;
        SIN = preSin;

        Z = new Point(new int[N-1]);

        // change the prefix string for postscript output
        switch (N) {
            case  5:  ORDER = "pent";
                      break;
            case  7:  ORDER = "hept";
                      break;
            case  9:  ORDER = "nine";
                      break;
            case 11:  ORDER = "elf";
                      break;
            default:  ORDER = "unknown";
                      break;
        }
        // Change the colour palette
        ColourPalette.setN(N);
    } // end of static initialization

    /**
     * Get the order of symmetry. 
     * @return N.  
     */
    public static int N() {
        return N;
    }

    /**
     * Get the zero vector.  
     * @return A point representing the origin.  
     */
    public static Point ZERO() {
        return Z;
    }

    /**
     * Private constructor. 
     * @param p The coefficients of the Point we create.  
     */
    private Point(int[] p) {
        if (p.length!=N-1) throw new IllegalArgumentException("Trying to create a Point with " + (N-1) + " entries, but only giving " + p.length + " entries as input.");
        this.point = p;
    }

    /**
     * Public static factory method.  
     * Returns the element of the unit circle that makes an angle of 
     * i pi/N with the positive x-axis.  
     * @param i The angle between the Point returned and the positive x-axis.  
     * @return The point on the unit circle that makes an angle of i with the positive x-axis.  
     */
    public static Point createPoint(int i) {
        i = i%(2*N);
        if (i < 0) i = i + 2*N;
        return STAR[i];
    }

    /**
     * Output a String consisting of the coefficients of the Point in a  
     * comma-separated list, enclosed in square brackets.  
     * @return The coefficients of the Point in a comma-separated list enclosed in square brackets.  
     */
    public String toString() {
        String output = "[ ";
        for (int i = 0; i < (N-2); i++) output += point[i] + ", ";
        return output + point[N-2] + " ]";
    } // end of toString

    /**
     * Output a String consisting of the coefficients of the Point in a  
     * space-separated list.  
     * @return The coefficients of the Point in a space-separated list.
     */
    public String postscriptString() {
        String output = "";
        for (int i = 0; i < (N-1); i++) output += point[i] + " ";
        return output;
    } // end of postscriptString

    /**
     * Output a String describing the order of symmetry.  
     * @return "pent" if N == 5, "hept" if N == 11, etc.
     */
    public static String order() {
        return ORDER;
    } // end of order

    /**
     * Output a String consisting of the coefficients of the Point in a  
     * comma-separated list with no brackets.  
     * @return The coefficients of the Point in a comma-separated list.  
     */
    public String matrixString() {
        String output = "";
        for (int i = 0; i < (N-2); i++) output += point[i] + ", ";
        return output + point[N-2] + "";
    } // end of matrixString

    /**
     * Output 2d Cartesian coordinates.  
     * @return An array of two doubles representing x- and y-coordinates of this point.  
     */
    public double[] project() {
        double[] output = new double[2];
        for (int i = 0; i < N-1; i++) {
            output[0] += COS[i]*point[i];
            output[1] += SIN[i]*point[i];
        }
        return output;
    } // end of project

    /**
     * Output this Point as a Point2D.  
     * @return A Point2D representation of this point.  
     */
    public Point2D getPoint2D() {
        double x = 0.0;
        double y = 0.0;
        for (int i = 0; i < N-1; i++) {
            x += COS[i]*point[i];
            y += SIN[i]*point[i];
        }
        return new Point2D(x,y);
    } // end of getPoint2D

    /**
     * Output a gap-readable String representing an inflation matrix.  
     * This square matrix has N-1 rows, each enclosed in square brackets, 
     * with comma-separated entries.  
     * @param angles A list of angles, given as integer multiples of pi/N.  
     * The sum of the points on the unit circle corresponding to these angles
     * is the inflation factor of the matrix.  
     * @return The matrix representation of multiplication by the inflation
     * factor, expressed in terms of the N-1 vectors (cos(k pi/n), sin(k pi/n)).
     */
    public static String gapString(int[] angles) {
        boolean first = true;
        String output = "[ ";
        for (int i = 0; i < (N-1); i++) {
            Point p = Z;
            for (int j : angles) {
                int index = (i+j)%(2*N);
                p = p.plus(STAR[(index>=0) ? index : index + 2*N]);
            }
            if (!first) {
                output += ", ";
            } else {
                first = false;
            }
            output += p;
        }
        return output + " ]";
    } // end of gapString

    /**
     * Same as {@link #gapString(int[])}, but with row breaks
     * indicated by new lines instead of square brackets.  
     */
    public static String matrixString(int[] angles) {
        String output = "";
        for (int i = 0; i < (N-1); i++) {
            Point p = Z;
            for (int j : angles) {
                int index = (i+j)%(2*N);
                p = p.plus(STAR[(index>=0) ? index : index + 2*N]);
            }
            output += p.matrixString() + "\n";
        }
        return output + "";
    } // end of matrixString

    /**
     * Output a gap-readable String representing a rotation matrix.  
     * This square matrix has N-1 rows, each enclosed in square brackets, 
     * with comma-separated entries.  
     * @return The matrix representation of rotation by pi/n, 
     * expressed in terms of the N-1 vectors (cos(k pi/n), sin(k pi/n)).
     */
    public static String gapString() {
        boolean first = true;
        String output = "[ ";
        for (int i = 1; i < (N); i++) {
            if (!first) {
                output += ", ";
            } else {
                first = false;
            }
            output += STAR[i];
        }
        return output + " ]";
    } // end of gapString

    /**
     * Two Points are equal if they are of the same length and have the 
     * same entries in each position.  
     */
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        Point l = (Point) obj;
        if (l.point.length!=this.point.length) return false;
        for (int i = 0; i < N-1; i++) if (this.point[i]!=l.point[i]) return false;
        return true;
    }

    /**
     * Currently broken.  
     */
    public int hashCode() {
        return 19;
    }

    /**
     * Add another point to this one and return the result.  
     * The two original Points remain unchanged.  
     * @param p The Point we add to this one.  
     * @return The sum of this an p.  
     */
    public Point plus(Point p) {
        int[] output = new int[N-1];
        for (int i = 0; i < output.length; i++) output[i] = this.point[i]+p.point[i];
        return new Point(output);
    }

    /**
     * Subtract another point from this one and return the result.  
     * The two original Points remain unchanged.  
     * @param p The Point we subtract from this one.  
     * @return The difference of this an p.  
     */
    public Point minus(Point p) {
        int[] output = new int[N-1];
        for (int i = 0; i < output.length; i++) output[i] = this.point[i]-p.point[i];
        return new Point(output);
    }

    /**
     * Rotate this Point about the origin by the given angle.  
     * @param angle  The angle of rotation.  
     * @return The Point that results from the rotation.  
     */
    public Point rotate(int angle) {
        int[] output = new int[N-1];
        for (int i = 0; i < output.length; i++) {
            for (int j = 0; j < output.length; j++) {
                output[i] += this.point[j]*STAR[(angle+j)%(2*N)].point[i];
            }
        }
        return new Point(output);
    }

    /**
     * Produce an inflation matrix (i.e., a sequence of {@link #N()}-1 Points) 
     * corresponding to a given edge sequence.  
     * @param seq A sequence of angles representing a vector 
     * reached by taking steps of unit length, starting at the origin.  
     * This vector should lie on the positive x-axis, and therefore 
     * represent a length.  
     * @return A matrix (array of Points), multiplication by which 
     * corresponds to inflation by the length represented by the given
     * sequence.  
     */
    public static Point[] inflation(int[] seq) {
        Point[] output = new Point[N-1];
        Point base = Point.Z;
        for (int i = 0; i < seq.length; i++) {
            base = base.plus(STAR[(seq[i]%(2*N)<0) ? (seq[i]%(2*N))+2*N : (seq[i]%(2*N))]);
        }
        for (int i = 0; i < N-1; i++) {
            output[i] = base.rotate(i);
        }
        return output;
    }

    /**
     * View the given array of Points as a matrix, and multiply this Point
     * by it.  Intended for use in computing inflated Points.  
     * @param mat  An Array of Points representing an inflation matrix.  
     * @return  The inflation of this point by the given matrix.  
     */
    public Point multiply(Point[] mat) {
        if (mat.length!=N-1) throw new IllegalArgumentException("Trying to multiply by a matrix of size " + mat.length + " when a matrix of size " + (N-1) + " is required.");
        int[] output = new int[N-1];
        for (int i = 0; i < output.length; i++) {
            for (int j = 0; j < output.length; j++) {
                output[i] += this.point[j]*mat[j].point[i];
            }
        }
        return new Point(output);
    }


    /** For testing.  */
    public static void main(String[] args) {

        System.out.println(gapString());
        int[] ii = new int[] {0,0,0,1,-1,1,-1,1,-1,2,-2,2,-2,2,-2,3,-3,3,-3,4,-4};

        System.out.println("Testing rotation:");
        System.out.println(matrixString(ii));
        Point test = createPoint(0);
        for (int i = 0; i < STAR.length; i++) {
          System.out.println(test.rotate(i).matrixString());
        }
        System.out.println("Testing inflation:");
        for (Point p : Point.inflation(new int[] {0,1,-1,2,-2})) {
            System.out.println(p.matrixString());
        }
        System.out.println("Testing multiplication:");
        System.out.println(createPoint(3).plus(createPoint(5)).matrixString());
        System.out.println(createPoint(3).plus(createPoint(5)).multiply(Point.inflation(new int[] {0,1,-1,2,-2})).matrixString());

    }

} // end of class Point

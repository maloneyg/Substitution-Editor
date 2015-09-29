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
 *  A class representing a pseudoline.  
 *  Each such pseudoline corresponds to a connected path of rhombs, all edges parallel to a given edge.  
 *  Each Yarn begins and ends at a {@link Terminus}.  
 *  The intersection of two Yarns is a {@link Join}.  
 */
public class Yarn implements Comparable<Yarn>, Serializable {

    /** For serialization. */
    public static final long serialVersionUID = 5509L;

    /**  The Terminus where this Yarn begins.  Should have an even angle.  */
    private Terminus start;
    /**  The Terminus where this Yarn ends.  Should have an odd angle if N is even.  */
    private Terminus end;
    /**  A List of all Yarns that cross this one.  */
    private LinkedList<Yarn> cross;
    /**  A List of all crossings.  */
    private LinkedList<Join> joins;
    /**
     *  The image of this Yarn under a 180-degree rotation of the whole 
     *  configuration, if there is such a symmetry.  Otherwise this is null.  
     */
    private Yarn opposite = null;

    /**  Private constructor.  */
    private Yarn(Terminus s, Terminus e) {
        start = s;
        end = e;
        cross = new LinkedList<>();
        joins = new LinkedList<>();
    }

    /**
     *  Public static factory method.  
     *  @param s The start.  
     *  @param e The end.  
     */
    public static Yarn createYarn(Terminus s, Terminus e) {
        return new Yarn(s,e);
    }

    /**
     *  Getter method.  
     *  @return The first intersection with another Yarn, determined by proximity to the starting Terminus.  null if there are no Joins.  
     */
    public Join firstJoin() {
        if (joins.isEmpty()) return null;
        return joins.get(0);
    }

    /**
     *  Getter method.  
     *  @return The last intersection with another Yarn, determined by proximity to the starting Terminus.  null if there are no Joins.  
     */
    public Join lastJoin() {
        if (joins.isEmpty()) return null;
        return joins.get(joins.size()-1);
    }

    /**
     *  Add a Yarn to the end of the list of Yarns that this one crosses.  
     *  @param y The Yarn we add.  
     */
    public void add(Yarn y) {
        cross.add(y);
    }

    /**
     *  Add a Yarn to the beginning of the list of Yarns that this one crosses.  
     *  @param y The Yarn we add.  
     */
    public void addFirst(Yarn y) {
        cross.addFirst(y);
    }

    /**
     *  Add a Yarn to the end of the list of Yarns that this one crosses.  
     *  @param y The Yarn we add.  
     */
    public void addLast(Yarn y) {
        cross.addLast(y);
    }

    /**
     *  Add a Join to the end of the list of Joins.  
     *  @param j The Join we add.  
     */
    public void add(Join j) {
        joins.add(j);
    }

    /**
     *  Add a Join to the beginning of the list of Joins.  
     *  @param j The Join we add.  
     */
    public void addFirst(Join j) {
        joins.addFirst(j);
    }

    /**
     *  Add a Join to the end of the list of Joins.  
     *  @param j The Join we add.  
     */
    public void addLast(Join j) {
        joins.addLast(j);
    }

    /**
     *  Add a Yarn at the end of the list of Yarns and a Join at the end of the list of Joins.  
     *  @param y The Yarn we add.  
     *  @param j The Join we add.  
     */
    public void add(Yarn y, Join j) {
        cross.add(y);
        joins.add(j);
    }

    /**
     *  Remove a Yarn from the list of Yarns that cross this one.  
     *  @param y The Yarn we remove.  
     */
    public void remove(Yarn y) {
        cross.remove(y);
    }

    /**
     *  Remove a Join from the list of Joins between this Yarn and other Yarns.  
     *  @param j The Join we remove.  
     */
    public void remove(Join j) {
        joins.remove(j);
    }

    /**
     *  Remove the first Yarn that this one crosses, along with the corresponding Join.  
     */
    public void removeFirst() {
        joins.removeFirst();
        cross.removeFirst();
    }

    /**
     *  Remove the last Yarn that this one crosses, along with the corresponding Join.  
     */
    public void removeLast() {
        joins.removeLast();
        cross.removeLast();
    }

    /**
     *  Set the Yarn opposite this one.  
     *  The opposite Yarn is the image of this one under a 180-degree rotation.  
     *  @param y The Yarn that we are declaring to be opposite this one.  
     */
    public void setOpposite(Yarn y) {
        if (!y.equals(opposite)) {
            opposite = y;
            y.setOpposite(this);
        }
    }

    /**
     *  Determine if this Yarn is its own opposite.  
     *  @return true if this Yarn is invariant under a 180-degree rotation.  
     */
    public boolean symmetric() {
        return this.equals(opposite);
    }

    /**
     *  Call this method when the list of Yarns crossing this one has been
     *  partly filled, and likewise for its opposite.  
     *  This method fills the list for this one the rest of the way, by 
     *  adding the entries from the list of the crossings of the opposite
     *  Yarn to the end of the list for this Yarn, in reverse order.  
     */
    public void symmetrize() {
        if (opposite==null) return;
        @SuppressWarnings("unchecked")
        List<Yarn> c1 = (List<Yarn>)this.cross.clone();
        @SuppressWarnings("unchecked")
        List<Yarn> c2 = (List<Yarn>)opposite.cross.clone();
        if (c1.isEmpty()) for (Yarn y : c2) addFirst(y.getOpposite());
        for (int i = 0; i < c2.size(); i++) {
            if (c1.contains(c2.get(i).getOpposite())) {
                // add everything before this to c1
                for (int j = i-1; j > -1; j--) addLast(c2.get(j).getOpposite());
                break;
            }
        }
        for (int i = c2.size()-1; i > -1; i--) {
            if (c1.contains(c2.get(i).getOpposite())) {
                // add everything after this to c1
                for (int j = i+1; j < c2.size(); j++) addFirst(c2.get(j).getOpposite());
                break;
            }
        }
    }

    /**
     *  Remove a Join from the list of Joins on this Yarn.  
     *  Do this if we are eliminating a Join entirely, because it
     *  is being removed form the edge.  
     *  @param j The Join that we remove.  
     */
    public void collapse(Join j) {
        boolean f1 = false;
        boolean f2 = false;
        Terminus t1 = null;
        Terminus t2 = null;
        Yarn y2 = cross.get(joins.indexOf(j));
        if (j.equals(lastJoin())) {
            t1 = end;
            removeLast();
        } else {
            f1 = true;
            t1 = start;
            removeFirst();
        }
        if (j.equals(y2.lastJoin())) {
            t2 = y2.end;
            y2.removeLast();
        } else {
            f2 = true;
            t2 = y2.start;
            y2.removeFirst();
        }
        t1.swapAngles(t2);
        if (f1) start = t2;
        else end = t2;
        if (f2) y2.start = t1;
        else y2.end = t1;
    }

    /**
     *  Getter method.  
     *  @return The index in the global list of Termini of the starting Terminus.  
     */
    public int getStartIndex() {
        return start.getIndex();
    }

    /**
     *  Getter method.  
     *  @return The index in the global list of Termini of the ending Terminus.  
     */
    public int getEndIndex() {
        return end.getIndex();
    }

    /**
     *  Getter method.  
     *  @return The angle of the starting Terminus.  
     */
    public int getStartAngle() {
        return start.getAngle();
    }

    /**
     *  Getter method.  
     *  @return The angle of the ending Terminus.  
     */
    public int getEndAngle() {
        return end.getAngle();
    }

    /**
     *  Getter method.  
     *  @return The image of this Yarn under a 180-degree rotation.  This image must be
     *  determined and set manually, and it is initialized to null.  Also, if the set
     *  of all Yarns is not symmetric under such a rotation, then this method returns null.  
     */
    public Yarn getOpposite() {
        return opposite;
    }

    /**
     *  Compare to another Yarn on the basis of the start and end indices of their Termini.  
     *  @param other The Yarn to which we compare this one.  
     *  @return The difference between the minimum of the indices of the two Termini of
     *  this Yarn and the minimum of the indices of the two termini of other.  
     */
    public int compareTo(Yarn other) {
        int thisIndex = Math.min(start.getIndex(),end.getIndex());
        int thatIndex = Math.min(other.start.getIndex(),other.end.getIndex());
        return thisIndex - thatIndex;
    }

    /**
     *  Check to see if this hits two other Yarns in a row.  
     *  @param y1 The first Yarn for which we check for an intersection with this one.  
     *  @param y2 The second Yarn for which we check for an intersection with this one.  
     *  @return true if this hits y1 followed immediately by y2.  
     */
    public boolean hits(Yarn y1, Yarn y2) {
        int i = cross.indexOf(y1);
        if (i<0||i==cross.size()-1) return false;
        return cross.get(i+1).equals(y2);
    }

    /**
     *  Swap the positions of two Yarns that cross this one.  
     *  This should only be done if this crosses the two Yarns one immediately after the other.  
     *  @param y1 The first of the two Yarns we swap.  
     *  @param y2 The second of the two Yarns we swap.  
     *  @see #hits  
     */
    public void swap(Yarn y1, Yarn y2) {
        int index1 = cross.indexOf(y1);
        int index2 = cross.indexOf(y2);
        if (index1<0||index2<0) throw new IllegalArgumentException("Can't swap Yarns " + y1 + " and " + y2 + " because one of them doesn't cross " + this + ".");
        if (Math.abs(index1-index2)!=1) throw new IllegalArgumentException("Can't swap Yarns that don't appear in consecutive positions.");
        Collections.swap(cross,index1,index2);
        Collections.swap(joins,index1,index2);
    }

    /**
     *  Check to see if this hits two other Yarns in a row.  
     *  @param y1 The first Yarn for which we check for an intersection with this one.  
     *  @param y2 The second Yarn for which we check for an intersection with this one.  
     *  @return true if this hits y1 followed immediately by y2 or y2  
     *  followed immediately by y1.  
     */
    public boolean consecutive(Yarn y1, Yarn y2) {
        int index1 = cross.indexOf(y1);
        if (index1<0) return false;
        int index2 = cross.indexOf(y2);
        if (index2<0) return false;
        return (Math.abs(index1-index2)==1);
    }

    /**
     *  @return The number of other Yarns this crosses.  
     */
    public int crossCount() {
        return cross.size();
    }

    /**
     *  Check if this crosses a particular Yarn.  
     *  @param other The other Yarn that we are checking for a cross with this.  
     *  @return true if other crosses this.  
     */
    public boolean crosses(Yarn other) {
        return cross.contains(other);
    }

    /**
     *  Getter method.  
     *  @return A list of all the Yarns that cross this one.  
     */
    public List<Yarn> getCross() {
        return cross;
    }

    /**
     *  A method for use shortly after construction.  
     *  The list of other Yarns crossing this one has not been populated yet;
     *  this method determines if a given Yarn should be in that list.  
     *  This is done by calculating a cross-ratio of the two Yarns; if it
     *  is negative, then they should cross.  
     *  @param other A Yarn that we are checking to see if it should cross this.  
     *  @return true if other should cross this.  
     */
    public boolean shouldCross(Yarn other) {
        int cr = this.crossRatio(other);
        return (cr < 0);
    }

    /**
     *  Compute the sign of the cross ratio between this and another Yarn.  
     *  This cross ratio is calculated by taking products of differences of
     *  the starting and ending indices of their Termini.  
     *  @param other The Yarn for which we are computing the cross ratio with this.  
     *  @return The sign of the cross ratio of this and other.  
     */
    public int crossRatio(Yarn other) {
        if (this.equals(other)) return 0;
        int s1 = this.start.getIndex();
        int s2 = other.start.getIndex();
        int e1 = this.end.getIndex();
        int e2 = other.end.getIndex();
        int cr = (((s1-s2)*(e1-s2)))*(((e1-e2)*(s1-e2)));
        return (cr < 0) ? -1 : ((cr > 0) ? 1 : 0);
    }

    /**
     *  Move counterclockwise through the Termini, starting with the start
     *  of this.  We will eventually hit the start or the end of other.  
     *  If we hit the start first, return true; otherwise return false.  
     *  @param other The Yarn for which we are comparing Termini positions with this.  
     *  @return true if we hit the start of other before the end of other in a ccw traversal of Termini, starting with the start of this.  
     */
    public boolean ccwStart(Yarn other) {
        if (this.equals(other)) return false;
        int s1 = this.start.getIndex();
        int s2 = other.start.getIndex();
        int e1 = this.end.getIndex();
        return ((s2-s1)*(e1-s2)*(e1-s1)>0);
    }

    /**
     *  Check for validity of crossings.  
     *  Iterate through all the Yarns crossing this one, and compute 
     *  differences of angles of the Termini.  If these differences are all 
     *  less than {@link Point#N()} (mod 2N) then the crossings are valid; otherwise they're
     *  invalid.  
     *  @return true if all crossings with this Yarn are valid.  
     */
    public boolean valid() {
        int thisIndex = Math.min(this.start.getIndex(),this.end.getIndex());
        Terminus thisFirst = (this.start.getIndex()==thisIndex) ? this.start : this.end;
        for (Yarn y : cross) {
            int thatIndex = Math.min(y.start.getIndex(),y.end.getIndex());
            Terminus thatFirst = (y.start.getIndex()==thatIndex) ? y.start : y.end;
            int difference = (thisIndex>thatIndex) ? thisFirst.getAngle()-thatFirst.getAngle() : thatFirst.getAngle()-thisFirst.getAngle();
            int pos = (difference<=0) ? difference + 2*Point.N() : difference;
            if (pos>=Point.N()) return false;
        }
        return true;
    }

    /**
     *  Assuming that this crosses a given Yarn, return the corresponding 
     *  Join.  If the given Yarn does not cross this one, return null.  
     *  @param other The Yarn whose Join with this we are seeking.  
     *  @return The Join between other and this.  
     */
    public Join joinWith(Yarn other) {
        int index = cross.indexOf(other);
        if (index < 0) return null;//throw new IllegalArgumentException("Looking for the Join of two Yarns that don't cross.");
        return joins.get(index);
    }

    /**
     *  Assume that y1 and y2 intersect this consecutively.  
     *  Then return the Yarn that comes after, on the side of y1.  
     *  Return null if there is no such Yarn.  
     *  @param y1 The first of two Yarns that intersect this consecutively.  
     *  @param y2 The second of two Yarns that intersect this consecutively.  
     *  @return The Yarn, other than y2, that intersects this consecutively 
     *  with y1.  Return null if there is not such Yarn.  
     */
    public Yarn nextYarn(Yarn y1, Yarn y2) {
        int index1 = cross.indexOf(y1);
        int index2 = cross.indexOf(y2);
        if (index1 < 0||index2 < 0) throw new IllegalArgumentException("Looking for the next Yarn, but the given two aren't on this one.");
        if (Math.abs(index1-index2)!=1) throw new IllegalArgumentException("Looking for the next Yarn, but these two aren't consecutive.");
        int newIndex = 2*index1-index2;
        if (newIndex>=cross.size()||newIndex<0) return null;
        return cross.get(newIndex);
    }

    /**
     *  Assume that y0, y1 and y2 intersect in a triple.  
     *  Then return a list of all Triples that would be created or destroyed by
     *  flipping that Triple.  
     *  @param y0 One of three Yarns.  
     *  @param y1 One of three Yarns.  
     *  @param y2 One of three Yarns.  
     *  @return All Triples that would be created or destroyed by flipping the Triple on y0, y1, and y2.
     */
    public static List<Hex> surroundingTriples(Yarn y0, Yarn y1, Yarn y2) {
        List<Hex> output = new LinkedList<>();
        int y01 = y0.cross.indexOf(y1);
        int y02 = y0.cross.indexOf(y2);
        int y10 = y1.cross.indexOf(y0);
        int y12 = y1.cross.indexOf(y2);
        int y20 = y2.cross.indexOf(y0);
        int y21 = y2.cross.indexOf(y1);
        int i = y01+y01-y02;
        int j = y10+y10-y12;
        if (i>=0&&i<y0.cross.size()&&j>=0&&j<y1.cross.size()&&y0.cross.get(i).equals(y1.cross.get(j))) output.add(Triple.createTriple(y0.cross.get(i),y0,y1));
        i = y02+y02-y01;
        j = y12+y12-y10;
        if (i>=0&&i<y0.cross.size()&&j>=0&&j<y1.cross.size()&&y0.cross.get(i).equals(y1.cross.get(j))) output.add(Triple.createTriple(y0.cross.get(i),y0,y1));
        i = y21+y21-y20;
        j = y12+y12-y10;
        if (i>=0&&i<y2.cross.size()&&j>=0&&j<y1.cross.size()&&y2.cross.get(i).equals(y1.cross.get(j))) output.add(Triple.createTriple(y2.cross.get(i),y1,y2));
        i = y20+y20-y21;
        j = y10+y10-y12;
        if (i>=0&&i<y2.cross.size()&&j>=0&&j<y1.cross.size()&&y2.cross.get(i).equals(y1.cross.get(j))) output.add(Triple.createTriple(y2.cross.get(i),y1,y2));
        i = y20+y20-y21;
        j = y02+y02-y01;
        if (i>=0&&i<y2.cross.size()&&j>=0&&j<y0.cross.size()&&y2.cross.get(i).equals(y0.cross.get(j))) output.add(Triple.createTriple(y2.cross.get(i),y0,y2));
        i = y21+y21-y20;
        j = y01+y01-y02;
        if (i>=0&&i<y2.cross.size()&&j>=0&&j<y0.cross.size()&&y2.cross.get(i).equals(y0.cross.get(j))) output.add(Triple.createTriple(y2.cross.get(i),y0,y2));
        return output;
    }

    /**
     *  A basic String describing this Yarn.  
     *  @return A String giving the indices of the start and end Termini.  
     */
    public String toString() {
        return "start: " + getStartIndex() + ". end: " + getEndIndex() + ".";
    }

    /**
     *  A String listing all Yarns this crosses.  
     *  @return A String consisting of all the Yarns that cross this, 
     *  separated by newlines.  
     */
    public String crossString() {
        String output = "";
        for (Yarn y : cross) {
            output += "  " + y + "\n";
        }
        return output;
    }

    /**
     *  @return A List of all the Yarns that cross this one.  
     */
    public List<Yarn> cross() {
        return cross;
    }

    /**
     *  @return An Iterable of all the Joins on this Yarn.  
     */
    public Iterable<Join> joins() {
        return joins;
    }

} // end of class Yarn

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
 *  A class representing a patch of rhombic tiles.  
 *  This is equivalent to a pseudoline arrangement.  Here the pseudolines 
 *  are represented by {@link Yarn}s and their intersections by {@link SimpleRhomb}s.  
 *  Each SimpleRhomb corresponds to a rhomb, and each Yarn to a sequence of rhombs,
 *  each of which shares an edge with the last, and all of which have two edges
 *  in the same family of parallel edges.  
 *  This class contains methods for creating and modifying such arrangements.  
 */
public class RhombBoundary implements Serializable {

    /** For serialization. */
    public static final long serialVersionUID = 5510L;

    /** Number of permutations in a WorkUnit.  */
    public static final int WORK_UNIT_LENGTH = 1000;
    /** Number of permutations we create before stopping to output interim results.  */
    public static final int BATCH_SIZE = 100*WORK_UNIT_LENGTH;

    /** The number of subedges.  */
    private int l;
    /**
     * A field for keeping track of the value of {@link Point#N()} that was used when 
     * this was created.  This can be checked against the current value
     * of N during de-serialization to make sure it's consistent.  
     */
    private int n;

    /**
     * The angles of the subedges (a subedge is a {@link Terminus}) 
     * read in counterclockwise order.  
     */
    private Terminus[] termini;

    /**
     * A List of all the Yarns in the pattern.  
     */
    private Yarn[] yarns;

    /**
     * All the Joins of all the Yarns in the pattern.  
     */
    private List<Rhomb> allJoins;
    /**
     * All the Joins that have two rhomb edges on the boundary.  
     */
    private List<Rhomb> edgeJoins;

    /**
     * All Triples of Yarns that cross each other, with no intervening
     * Yarns in their crossing lists.  
     */
    private List<Hex> triples;

    /**
     * Does this represent a valid tiling with no overlapping rhombs?  
     */
    private final boolean valid;

    /**
     * Public constructor.  
     * @param angles A list of angle differences.  These should be integers
     * between -N/2 and +N/2.  They correspond to the angles (expressed as 
     * integer multiples of pi/{@link Point#N()}) of a sequence of unit-length line segments
     * that constitute the boundary of the patch.  
     * @param sym Tells us if the patch should be made symmetric under
     * a mirror reflection.  This means that we change the signs of the angles
     * on opposite sides to make indentations exdentations and exdentaions
     * indentations.  
     */
    public RhombBoundary(int[] angles, boolean sym) {
        n = Point.N();
        l = angles.length;
        termini = new Terminus[l];
        allJoins = new LinkedList<>();
        edgeJoins = new LinkedList<>();
        for (int j = 0; j < l; j++) {
            termini[j] = Terminus.createTerminus(j,angles[j]);
        }

        placeYarns(sym);
        if (sym) symmetricThreadYarnsKKS();
        else threadYarnsKKS();
        valid = getValid();
        if (!valid) return;
        setJoins();
        setEdgeJoins();
        setTriples();
    } // end of constructor

    /**
     * Private constructor.  
     * We just specify every field.  
     */
    private RhombBoundary(int l,int n,Terminus[] termini,Yarn[] yarns,List<Rhomb> allJoins,List<Rhomb> edgeJoins,List<Hex> triples,boolean valid) {
        this.l = l;
        this.n = n;
        this.termini = termini;
        this.yarns = yarns;
        this.allJoins = allJoins;
        this.edgeJoins = edgeJoins;
        this.triples = triples;
        this.valid = valid;
    } // end of constructor

    /** Method for saving and restoring.  */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.writeObject(n);
        stream.defaultWriteObject();
    }

    /** Method for saving and restoring.  */
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        n = (int) stream.readObject();
        Point.setN(n);
        stream.defaultReadObject();
    }

    /**
     * A class for creating and checking the validity of many RhombBoundaries.  
     * Produces {@link BoundaryResult}s.  
     */
    private static class BoundaryWorkUnit implements WorkUnit
    {
        /** Produces permutations of a multiset.  */
        private MultiSetLinkedList m;
        /** The number of permutations to search.  */
        private final int max;
        /** Do we search symmetric boundaries?  */
        private final boolean sym;
        /** A sequence that we prepend to the permutation.  */
        private int[] prefix = null;
        /** A sequence that we append to the permutation.  */
        private int[] suffix = null;
        /**
         * A number substitution rule that allows us to store 
         * subsequences as single numbers, thereby reducing the
         * total number of permutations we need to search if we
         * know that only certain subsequences will occur.  
         */
        private List<List<Integer>> rule = null;

        /**
         * Public constructor.  
         * @param M The object we use to produce permutations.  
         * @param max The maximum number of permutations we'll create.  
         * @param sym Tells us if the RhombBoundaries that we create 
         * and search should be symmetric.  
         */
        public BoundaryWorkUnit(MultiSetLinkedList M, int max, boolean sym) {
            this.m = M;
            this.max = max;
            this.sym = sym;
        }

        /**
         * Public constructor.  
         * Same as {@link #BoundaryWorkUnit(MultiSetLinkedList,int,boolean)}, except it includes an integer substitution rule to abgreviate sequences.  
         * @param M The object we use to produce permutations.  
         * @param max The maximum number of permutations we'll create.  
         * @param r An integer substitution rule that tells us how to encode 
         * subsequences so that a proper subset of the set of all permutations
         * can be searched.  
         * @param sym Tells us if the RhombBoundaries that we create 
         * and search should be symmetric.  
         */
        public BoundaryWorkUnit(MultiSetLinkedList M, int max, boolean sym, List<List<Integer>> r) {
            this.m = M;
            this.max = max;
            this.sym = sym;
            this.rule = r;
        }

        /**
         * Public constructor.  
         * Same as {@link #BoundaryWorkUnit(MultiSetLinkedList,int,boolean,List<List<Integer>>)}, except it includes a prefix and suffix.  
         * @param M The object we use to produce permutations.  
         * @param max The maximum number of permutations we'll create.  
         * @param r An integer substitution rule that tells us how to encode 
         * subsequences so that a proper subset of the set of all permutations
         * can be searched.  
         * @param sym Tells us if the RhombBoundaries that we create 
         * and search should be symmetric.  
         * @param pre A list of ints to prepend to each permutation.  
         * @param suf A list of ints to append to each permutation.  
         */
        public BoundaryWorkUnit(MultiSetLinkedList M, int max, boolean sym, List<List<Integer>> r, int[] pre, int[] suf) {
            this.m = M;
            this.max = max;
            this.sym = sym;
            this.rule = r;
            this.prefix = pre;
            this.suffix = suf;
        }

        /**
         * Given an int array input and an Integer List rule, 
         * return a new int array in which each entry of input 
         * has been replaced with the sequence at the 
         * corresponding position in rule (or the negative of 
         * the absolute value of the corresponding position).  
         * @param input The sequence we are decoding.  
         * @param rule The rule that tells us what subsequences we use 
         * to replace the various symbols in input.  
         * @return The sequence input, with each symbol replaced by the 
         * subsequence that appears at the corresponding position in rule.  
         */
        public static int[] substitute(int[] input, List<List<Integer>> rule)
        {
            // first make a List
            List<Integer> output = new LinkedList<>();
            for (int i = 0; i < input.length; i++) {
                if (Math.abs(input[i])<rule.size()) {
                    for (Integer j : rule.get(Math.abs(input[i]))) output.add(j);
                } else {
                    output.add(input[i]);
                }
            }
            // now convert it into an array
            int[] o = new int[output.size()];
            for (int i = 0; i < o.length; i++) o[i] = output.get(i);
            return o;
        }

        /**
         * Concatenate two int arrays.  
         * @param first The first int array.  
         * @param second The second int array.  
         * @return The array obtained by concatenating first and second.  
         */
        public static int[] concatenate(int[] first, int[] second) {
            int[] output = new int[first.length+second.length];
            for (int i = 0; i < first.length; i++) output[i] = first[i];
            for (int i = 0; i < second.length; i++) output[i+first.length] = second[i];
            return output;
        }

        /**
         * Call this work unit.  
         * @return A result without much content.  The actual information 
         * we want is output to the terminal.  This output consists of a 
         * list of all permutations of the starting multiset that produce
         * valid RhombBoundaries.  
         */
        public BoundaryResult call()
        {
            List<int[]> output = new LinkedList<>();
            for (int i = 0; i < max; i++) {
                int[] current = m.getArray();
                if (prefix!=null) current = concatenate(prefix,current);
                if (suffix!=null) current = concatenate(current,suffix);
                if (rule!=null) current = substitute(current,rule);
                boolean valid = true;
                for (int j = Point.N()-1; j > 0; j = j-2) {
                    RhombBoundary RB = (sym) ? createSymmetricRhombBoundary(j,current) : createRhombBoundary(j,current);
                    if (!RB.valid()) {
                        valid = false;
                        break;
                    }
                }
                if (valid) //return current;
                {
                    output.add(current);
                }
                m.iterate();
            }
            return new BoundaryResult(output);
        }
    } // end of class BoundaryWorkUnit

    /**
     * A basic class returned by a {@link BoundaryWorkUnit}.  
     * It doesn't contain much--most of the real information is 
     * output to the terminal in {@link BoundaryWorkUnit.call()}.  
     */
    private static class BoundaryResult implements Result
    {
        /** The main field.  */
        private List<int[]> valids;

        /** Public constructor.  */
        public BoundaryResult(List<int[]> v)
        {
            this.valids = v;
        }

        /** Getter method.  */
        public List<int[]> getValids()
        {
            return valids;
        }

    } // end of class BoundaryResult 

    /**
     * Check if each {@link Terminus} has a match.  
     * This is chiefly for use in the constructor.  
     * Go through all of them and make sure that they can be partitioned 
     * into pairs of angles that differ by pi (N pi /N).  
     * @return true if the Termini can be partitioned into pairs of angles 
     * that differ by pi.  
     */
    private boolean endsMatch() {
        int[] sums = new int[Point.N()];
        for (Terminus t : termini) {
                int a = t.getAngle();
                boolean first = a<=Point.N();
                int angle = (first) ? a : a-Point.N();
                sums[angle-1] += (first) ? 1 : -1;
        }
        for (int j = 0; j < Point.N(); j++) {
            if (sums[j]!=0) return false;
        }
        return true;
    }

    /**
     * Create all of the {@link Yarn}s in this RhombBoundary.  
     * We do this by creating Yarns connecting {@link Terminus} pairs 
     * with angles that differ by pi.  
     * There might be many such pairs, but we assume that this 
     * RhombBoundary has the following property by construction: 
     * all Termini with angles k pi/{@link Point.N()} occur as a subsequence of the 
     * Termini, and without any intervening Termini with angles ({@link Point.N()}-k)pi/{@link Point.N()}.  
     * This has the consequence that there is a unique way to pair off the
     * Termini such that no two Yarns with the same ending angles cross 
     * each other.  
     * This method is for use in the constructor.  
     * @param sym Tells us if we are constructing a symmetric RhombBoundary 
     * or not.  This has no effect on how the Yarns are placed, but if the 
     * RhombBoundary is symmetric, then we set the opposites of all the Yarns
     * at the end using {@link Yarn.setOpposite(Yarn)}.  
     */
    private void placeYarns(boolean sym) {
        if (!endsMatch()) throw new IllegalArgumentException("The ends don't match.");
        yarns = new Yarn[l/2];
        int yarnCount = 0;
        for (int i = 1; i <= Point.N(); i++) {
            int start = (i%2==1) ? i : i+Point.N();
            int end = (i%2==1) ? i+Point.N() : i;
            // the starts and ends hit so far
            Stack<Terminus> starts = new Stack<>();
            Stack<Terminus> ends = new Stack<>();
            for (Terminus t : termini) {
                    if (t.getAngle()==start) {
                        if (ends.isEmpty()) {
                            starts.push(t);
                        } else {
                            Terminus pop = ends.pop();
                            yarns[yarnCount] = Yarn.createYarn(t,pop);
                            yarnCount++;
                        }
                    } // end of t.getAngle()==start
                    if (t.getAngle()==end) {
                        if (starts.isEmpty()) {
                            ends.push(t);
                        } else {
                            Terminus pop = starts.pop();
                            yarns[yarnCount] = Yarn.createYarn(pop,t);
                            yarnCount++;
                        }
                    } // end of t.getAngle()==end
            }
        }

        // if we are filling the rhomb symmetrically
        if (sym) {
            for (int i = 0; i < l/2; i++) {
                yarnOn(i).setOpposite(yarnOn(l-1-i));
            }
        }
    } // end of placeYarns

    /**
     * Once we've placed the Yarns, add their crossings.  
     * Use the Kannan-Kenyon-Soroker algorithm.  
     */
    private void threadYarnsKKS() {
        // make an array in which each yarn appears twice
        // once at its start index and once at its end index
        Yarn[] yarnIndex = new Yarn[l];
        for (Yarn y : yarns) {
            yarnIndex[y.getStartIndex()] = y;
            yarnIndex[y.getEndIndex()] = y;
        }
        // a list of Yarns we've done already
        List<Yarn> discards = new LinkedList<>();
        // now iterate through the array repeatedly
        boolean done = false;
        while (!done) {
            done = true;
            for (int i = 0; i < yarnIndex.length; i++) {
                Yarn y = yarnIndex[i];
                if (!discards.contains(y)) {
                    done = false;
                    int otherEnd = (y.getStartIndex()!=i) ? y.getStartIndex() : y.getEndIndex();
                    // now check to see if there are any full
                    // Yarns between the start and end of this one
                    // that do not cross it
                    boolean simpleYarn = true;
                    for (int j = i+1; j < otherEnd; j++) {
                         if (!discards.contains(yarnIndex[j])) {
                             if (!y.shouldCross(yarnIndex[j])) {
                                 simpleYarn = false;
                                 break;
                             }
                         }
                    } // end of check for simpleYarn
                    if (simpleYarn) {
                        boolean forward = y.getStartIndex()==i;
                        for (int j = i+1; j < otherEnd; j++) {
                            Yarn other = yarnIndex[j];
                            if (!discards.contains(other)) {
                                if (forward) {
                                    y.addLast(other);
                                } else {
                                    y.addFirst(other);
                                }
                                if (j==other.getStartIndex()) {
                                    other.addLast(y);
                                } else {
                                    other.addFirst(y);
                                }
                            }
                        }
                        discards.add(y);
                        break;
                    } // end of if(simpleYarn)
                } // end of if (!discards.contains(y))
            } // end of iteration through yarnIndex
        } // end of while(!done)
    } // end of threadYarnsKKS

    /**
     * Once we've placed the Yarns, add their crossings.  
     * Use the Kannan-Kenyon-Soroker algorithm, but symmetrize it.  
     */
    private void symmetricThreadYarnsKKS() {
        // make an array in which each yarn appears twice
        // once at its start index and once at its end index
        Yarn[] yarnIndex = new Yarn[l];
        for (Yarn y : yarns) {
            yarnIndex[y.getStartIndex()] = y;
            yarnIndex[y.getEndIndex()] = y;
        }
        // a list of Yarns we've done already
        List<Yarn> discards = new LinkedList<>();
        // now iterate through the array repeatedly
        boolean done = false;
        while (!done) {
            done = true;
            for (int i = 0; i < l/2; i++) {
                Yarn y = yarnIndex[i];
                if (!discards.contains(y)) {
                    done = false;
                    int otherEnd = (y.getStartIndex()!=i) ? y.getStartIndex() : y.getEndIndex();
                    int bound = (otherEnd>l/2) ? l/2 : otherEnd;
                    // now check to see if there are any full
                    // Yarns between the start and end of this one
                    // that do not cross it
                    boolean simpleYarn = true;
                    for (int j = i+1; j < bound; j++) {
                         if (!discards.contains(yarnIndex[j])) {
                             if (!y.shouldCross(yarnIndex[j])) {
                                 simpleYarn = false;
                                 break;
                             }
                         }
                    } // end of check for simpleYarn
                    if (simpleYarn) {
                        boolean forward = y.getStartIndex()==i;
                        for (int j = i+1; j < bound; j++) {
                            Yarn other = yarnIndex[j];
                            if (!discards.contains(other)) {
                                if (forward) {
                                    y.addLast(other);
                                } else {
                                    y.addFirst(other);
                                }
                                if (j==other.getStartIndex()) {
                                    other.addLast(y);
                                } else {
                                    other.addFirst(y);
                                }
                            }
                        }
                        discards.add(y);
                        break;
                    } // end of if(simpleYarn)
                } // end of if (!discards.contains(y))
            } // end of iteration through yarnIndex
        } // end of while(!done)
        discards.clear();
        for (Yarn y : yarnIndex) {
            if (!discards.contains(y)) {
                y.symmetrize();
                discards.add(y);
            }
        }
    } // end of symmetricThreadYarnsKKS

    /**
     * Set the Joins of all the Yarns.  
     */
    public void setJoins() {
        List<Yarn> discards = new LinkedList<>();
        for (Yarn y : yarns) {
            Point base = base(y.getStartIndex());
            for (Yarn z : y.cross()) {
                Point shift = Point.createPoint(z.getStartAngle());
                Point corner = (y.ccwStart(z)) ? base.plus(shift) : base;
                if (discards.contains(z)) {
                    y.addLast(z.joinWith(y));
                } else {
                    Join newJoin = Join.createJoin(corner,y,z);
                    y.addLast(newJoin);
                    allJoins.add(newJoin);
                }
                if (y.ccwStart(z)) {
                    base = base.plus(shift);
                } else {
                    base = base.minus(shift);
                }
            }
            discards.add(y);
        }
    }

    /**
     * Fill the list of all rhombs that have two
     * edges on the boundary.  
     * Clear this list first, as it might be out of date.  
     */
    public void setEdgeJoins() {
        edgeJoins.clear();
        for (Rhomb j : allJoins) if (j.onEdge()) edgeJoins.add(j);
    }

    /**
     * Fill the list of {@link Triple}s.  
     */
    private void setTriples() {
        List<Yarn> discards = new LinkedList<>();
        triples = new LinkedList<>();
        Yarn y1 = null;
        Yarn y2 = null;
        for (Yarn y : yarns) {
            for (int i = 0; i < y.cross().size()-1; i++) {
                Yarn z1 = y.cross().get(i);
                Yarn z2 = y.cross().get(i+1);
                if (!(discards.contains(z1)||discards.contains(z2))) {
                    if (z1.consecutive(z2,y)&&z2.consecutive(y,z1)) triples.add(Triple.createTriple(y,z1,z2));
                }
            }
            discards.add(y);
        }
    } // end of setTriples

    /**
     * Return the Yarn that starts or ends at a given index.  
     * @param i The index of the {@link Terminus} for which we wish to find the Yarn.  
     * @return The Yarn at index i.  
     */
    private Yarn yarnOn(int i) {
        for (Yarn y : yarns) {
            if (y.getStartIndex()==i||y.getEndIndex()==i) return y;
        }
        throw new IllegalArgumentException("No Yarn at position " + i + ".");
    }

    /**
     * Flip one of the Triples.  
     * A Triple is a hexagon consisting of three rhombs, any two of which 
     * meet at a common edge.  This method shifts each rhombs across 
     * to the side of the hexagon opposite where it currently is.  
     * @param t The triple we flip.  
     * @return A List of all Triples that are created or destroyed by this flip.  
     */
    public List<Hex> flipTriple(Hex t) {
        List<Hex> changes = t.flip();
        for (Hex u : changes) {
            if (!triples.remove(u)) triples.add(u);
        }
        return changes;
    } // end of flipTriple

    /**
     * Remove the rhomb represented by j, if it is on the edge.  
     * Careful! We assume that j is on the edge, but we don't check.  
     * @param j The join representing the rhomb that we wish to remove.  
     */
    public void collapse(Rhomb j) {
        if (!allJoins.contains(j)) return;
        if (j.onEdge()) {
            Hex removeMe = null;
            for (Hex t : triples) if (t.contains(j)) removeMe = t;
            if (removeMe!=null) triples.remove(removeMe);
            j.collapse();
            allJoins.remove(j);
            setEdgeJoins();
        }
    } // end of flipTriple

    /**
     * Getter method.  
     * @return A list of all the Joins inside this boundary.  
     */
    public List<Rhomb> getJoins() {
        return allJoins;
    }

    /**
     * Getter method.  
     * @return A list of all the Triples inside this boundary.  
     */
    public List<Hex> getTriples() {
        return triples;
    }

    /**
     * Getter method.  
     * @return A list of all the Joins inside this boundary that 
     * represent rhombs having two or more edges on the boundary.  
     */
    public List<Rhomb> getEdgeRhombs() {
        return edgeJoins;
    }

    /**
     * How many Triples are there?  
     * @return The number of Triples.  
     */
    public int numTriples() {
        return triples.size();
    }

    /**
     * Public static factory method.  
     * Calls {@link #createRhombBoundary(int,int[],int[],boolean)} with angles repeated 
     * twice and with the boolean variable sym set to false.  
     * @param even One of the angles of the big rhomb that we create, 
     * expressed as an integer multiple of pi/{@link Point#N()}.  
     * @param angles A sequence of angle differences that determines the
     * shape of the four edges of the rhomb.  
     * @return A RhombBoundary created by applying the sequence angles to 
     * each edge of the rhomb that has angles even*pi/{@link Point#N()} and ({@link Point#N()}-even)*pi/{@link Point#N()}.  
     * It will probably not be tiled symmetrically.  
     */
    public static RhombBoundary createRhombBoundary(int even, int[] angles) {
        return createRhombBoundary(even,angles,angles,false);
    }

    /**
     * Public static factory method.  
     * Same as {@link #createRhombBoundary(int,int[])} except it symmetrizes the boundary.  
     * This entails reversing the edge shapes on opposite sides.  
     */
    public static RhombBoundary createSymmetricRhombBoundary(int even, int[] angles) {
        int[] a = new int[angles.length];
        for (int i = 0; i < a.length; i++) a[i] = -angles[i];
        return createRhombBoundary(even,angles,a,true);
    }

    /**
     * Public static factory method.  
     * Calls {@link #createRhombBoundary(int,int[],int[],boolean)} with the boolean 
     * variable sym set to false.  
     * @param even One of the angles of the big rhomb that we create, 
     * expressed as an integer multiple of pi/{@link Point#N()}.  
     * @param angles1 A sequence of angle differences that determines the
     * shape of the top and left edges of the rhomb.  
     * @param angles2 A sequence of angle differences that determines the
     * shape of the bottom and right edges of the rhomb.  
     * @return A RhombBoundary created by applying the sequences angles1 and
     * angles2 to the top and left (respectively bottom and right) edges of 
     * the rhomb that has angles even*pi/N and (N-even)*pi/N.  
     */
    public static RhombBoundary createRhombBoundary(int even, int[] angles1, int[] angles2) {
        return createRhombBoundary(even,angles1,angles2,false);
    }

    /**
     * Public static factory method.  
     * This is the root method that many of the others call.  
     * It builds a rhomb boundary for the inflated rhomb with angles 
     * even*pi/{@link Point#N()} and (N-even)*pi/N.  
     * @param even One of the angles of the big rhomb that we create, 
     * expressed as an integer multiple of pi/N.  
     * @param angles1 A sequence of angle differences that determines the
     * shape of the top and left edges of the rhomb.  
     * @param angles2 A sequence of angle differences that determines the
     * shape of the bottom and right edges of the rhomb.  
     * @param sym Tells us whether or not to symmetrize.  When we symmetrize 
     * we reverse the edge shapes on opposite sides.  This should only be 
     * done if angles1 and angles2 are the same.  
     * @return A RhombBoundary created by applying the sequences angles1 and
     * angles2 to the top and left (respectively bottom and right) edges of 
     * the rhomb that has angles even*pi/N and (N-even)*pi/N.  
     */
    public static RhombBoundary createRhombBoundary(int even, int[] angles1, int[] angles2, boolean sym) {
        int l1 = angles1.length;
        int l2 = angles2.length;
        if (even<1||even>Point.N()-1) throw new IllegalArgumentException(even + " is not between 0 and " + (Point.N()-1) + ".");
//        for (int j = 0; j < l1; j++) {
//            if (Math.abs(angles1[j]) > Point.N()/2) throw new IllegalArgumentException("Please input a list of angles between " + (-Point.N()/2) + " and " + (Point.N()/2) + ".");
//        }
//        for (int j = 0; j < l2; j++) {
//            if (Math.abs(angles2[j]) > Point.N()/2) throw new IllegalArgumentException("Please input a list of angles between " + (-Point.N()/2) + " and " + (Point.N()/2) + ".");
//        }
        int[] b1 = new int[angles1.length];
        int[] b2 = new int[angles2.length];
        if (sym) for (int i = 0; i < b1.length; i++) b1[i] = -angles1[i];
        else b1 = angles1;
        if (sym) for (int i = 0; i < b2.length; i++) b2[i] = -angles2[i];
        else b2 = angles2;
        int ll = 2*(l1+l2);
        int[] a = new int[ll];
        for (int j = 0; j < l1; j++) {
            a[ll-1-j] = Point.N()+angles1[j];
            a[j] = (even>=b1[j]) ? 2*Point.N()-even+b1[j] : b1[j]-even;
        }
        for (int j = 0; j < l2; j++) {
            a[l1+j] = (angles2[j]>0) ? angles2[j] : 2*Point.N()+angles2[j];
            a[l1+2*l2-1-j] = (Point.N()-even+b2[j]>0) ? Point.N()-even+b2[j] : 3*Point.N()-even+b2[j];
        }
        return new RhombBoundary(a,sym);
    }

    /**
     * Public static factory method.  
     * This creates the boundary of a triangle with distorted edges, and then 
     * tiles it with rhombs.  
     * @param a1 A sequence of angle differences that determines the
     * shape of the first edge of the triangle.  
     * @param a2 A sequence of angle differences that determines the
     * shape of the second edge of the triangle.  
     * @param a3 A sequence of angle differences that determines the
     * shape of the third edge of the triangle.  
     * @param triangle A list of three integers that represent the angles of 
     * the triangle, expressed as integer multiples of pi/{@link Point#N()}.  
     * @return A triangle with edges distorted according to a1, a2, and a3, tiled
     * with rhombs.  
     */
    public static RhombBoundary createTriangleBoundary(int[] a1, int[] a2, int[] a3, int[] triangle) {
        int l1 = a1.length;
        int l2 = a2.length;
        int l3 = a3.length;
//        for (int j = 0; j < l1; j++) {
//            if (Math.abs(a1[j]) > Point.N()/2) throw new IllegalArgumentException("Please input a list of angles between " + (-Point.N()/2) + " and " + (Point.N()/2) + ".");
//        }
//        for (int j = 0; j < l2; j++) {
//            if (Math.abs(a2[j]) > Point.N()/2) throw new IllegalArgumentException("Please input a list of angles between " + (-Point.N()/2) + " and " + (Point.N()/2) + ".");
//        }
//        for (int j = 0; j < l3; j++) {
//            if (Math.abs(a3[j]) > Point.N()/2) throw new IllegalArgumentException("Please input a list of angles between " + (-Point.N()/2) + " and " + (Point.N()/2) + ".");
//        }
        int ll = l1+l2+l3;
        int[] a = new int[ll];
        for (int j = 0; j < l1; j++) {
            a[j] = (a1[j]<=0) ? 2*Point.N()+a1[j] : a1[j];
        }
        for (int j = 0; j < l2; j++) {
            int t = Point.N()-triangle[2]+a2[j];
            if (t<0) t = t+2*Point.N();
            if (t>2*Point.N()) t = t-2*Point.N();
            a[l1+j] = t;
        }
        for (int j = 0; j < l3; j++) {
            int t = Point.N()+triangle[1]+a3[j];
            if (t<0) t = t+2*Point.N();
            if (t>2*Point.N()) t = t-2*Point.N();
            a[l1+l2+j] = t;
        }
        return new RhombBoundary(a,false);
    }

    /**
     * Public static factory method.  
     * This creates the boundary of a region with 2*{@link Point#N()}-fold rotational symmetry.  
     * Each edge of this region is constructed using edge sequence i.  
     * There are no checks to make sure that i is well-formed.  
     * @param i A sequence of angle differences that determines the
     * shape of the first edge of the region.  
     * @return A planar region with 2*N-fold rotational symmetry tiled with rhombs.  
     * with rhombs.  
     */
    public static RhombBoundary createStarBoundary(int[] i) {
        int angleTotal = 0;
        int[] boundary = new int[2*Point.N()*i.length];
        for (int j = 0; j < i.length; j++) {
            // checks
//            if (Math.abs(i[j]) > Point.N()/2) throw new IllegalArgumentException("Please input a list of angles between " + (-Point.N()/2) + " and " + (Point.N()/2) + ".");
            angleTotal += i[j];
            // fill the array
            for (int k = 0; k < 2*Point.N(); k++) {
                boundary[k*i.length+j] = i[j]+k;
                if (boundary[k*i.length+j]<=0) boundary[k*i.length+j] += 2*Point.N();
                else if (boundary[k*i.length+j]>2*Point.N()) boundary[k*i.length+j] -= 2*Point.N();
            }
        }
        if (angleTotal!=0) throw new IllegalArgumentException("The entries of " + vectorString(i) + " do not sum to 0.");
//        System.out.println(Arrays.toString(boundary));
        return new RhombBoundary(boundary,false);
    }

    /**
     * Public static factory method.  
     * This creates the boundary of a basic prototile with the given type, plus 1.  
     * @param i The type of the prototile that is created (less 1).  
     * @return The canonical prototile of type i+1.  
     */
    public static RhombBoundary createPrototile(int i) {
        if (i<0||i>=Point.N()/2) throw new IllegalArgumentException("Cannot create a prototile of type " + (i+1) + ".");
        return RhombBoundary.createRhombBoundary(Point.N()-2*i-1,new int[] {0});
    }

    /**
     * Public static factory method.  
     * This creates a list of all the prototiles, each in canonical position.  
     * @return A list of RhombBoundaries, each containing one prototile.  
     */
    public static List<RhombBoundary> prototileList() {
        List<RhombBoundary> output = new ArrayList<RhombBoundary>();
        int[] noSub = new int[] {0};
        for (int i = 0; i < Point.N()/2; i++) {
            output.add(RhombBoundary.createRhombBoundary(Point.N()-2*i-1,noSub));
        }
        return output;
    }

    /**
     * Produce a RhombBoundary representing the same data, but with simpler fields.  
     * @return Another RhombBoundary that uses {@link SimpleRhomb} instead of
     * {@link Join} and {@link SimpleHex} instead of {@link Triple}.  
     */
    public RhombBoundary simplify() {
        List<Rhomb> newJoins = new ArrayList<>(allJoins.size());
        List<Hex> newTriples = new ArrayList<>(triples.size());
        for (int i = 0; i < allJoins.size(); i++) newJoins.add(allJoins.get(i).createSimpleRhomb());
        for (Yarn y : yarns) {
            Join current = null;
            Join previous = null;
            int a = y.getEndAngle();
            for (Join jj : y.joins()) {
                current = jj;
                // add previous to the adjacency list of current
                ((SimpleRhomb)newJoins.get(allJoins.indexOf(current))).addAdjacent((previous==null) ? null : ((SimpleRhomb)newJoins.get(allJoins.indexOf(previous))),a,true);
                // add current to the adjacency list of previous 
                if (previous!=null) ((SimpleRhomb)newJoins.get(allJoins.indexOf(previous))).addAdjacent(((SimpleRhomb)newJoins.get(allJoins.indexOf(current))),a,false);
                previous = current;
            }
            ((SimpleRhomb)newJoins.get(allJoins.indexOf(previous))).addAdjacent(null,a,false);
        }
        for (int i = 0; i < triples.size(); i++) newTriples.add(triples.get(i).createSimpleHex(allJoins,newJoins));
        return new RhombBoundary(l,n,new Terminus[0],new Yarn[0],newJoins,newJoins,newTriples,valid);
    }

    /**
     * Check for the validity of the crossings.  
     * @return true if any two Yarns that cross are valid (see {@link Yarn#valid()}).  
     */
    public boolean valid() {
        return valid;
    }

    /**
     * Determine whether or not this represents a valid tiling by rhombs.  
     * This method is for use in the constructor.  
     * @return true if this represents a valid tiling by rhombs.  
     */
    private boolean getValid() {
        for (Yarn y : yarns) if (!y.valid()) return false;
        return true;
    }

    /**
     * Output a String.  
     * @return A String representing this RhombBoundary.  List all the {@link Yarn}s and their crossings.  
     */
    public String toString() {
        String output = "";
        for (int i = 0; i < l/2; i++) output += yarns[i] + " crossings: \n" + yarns[i].crossString();
        return output;
    } // end of toString

    /**
     * Currently broken.  Any two RhombBoundaries are equal.  
     */
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        RhombBoundary l = (RhombBoundary) obj;
        return true;
    }

    /**
     * Currently broken.  
     * @return 19.  
     */
    public int hashCode() {
        int prime = 59;
        int result = 19;
        return result;
    }

    /**
     * Find the base of the rhomb at the boundary subedge with this index.  
     * We have a set of prototile rhombs that all have a vertex at the origin.  
     * This vertex has an even angle on it, with two edges emanating from it
     * in the positive orientation.  One of those edges lies on the positive
     * x-axis.  All of the rhombs in a tiling represented by a RhombBoundary 
     * are obtained from these prototiles by rotation and translation.  This 
     * method takes a rhomb in a tiling, and tells us which of its vertices is
     * the image of the origin under the transformation that carries the 
     * associated prototile onto it.  
     * @param i An index of an element in the {@link Terminus} list.  
     * @return The base vertex of the rhomb in this tiling that has an 
     * edge on Terminus number i.  
     */
    public Point base(int i) {
        if (i<0||i>l) throw new IllegalArgumentException("Trying to find a basepoint at index " + i + ", which is not between 0 and " + l + ".");
        Point output = Point.ZERO();
        for (int j = 0; j <= i; j++) output = output.plus(Point.createPoint(termini[j].getAngle()));
        return output;
    }

    /**
     * Output a list of subrhombs in gap-readable format.  
     * @return A String containing gap-readable representations of all the 
     * rhombs in this tiling.  
     */
    public String gapString() {
        List<Yarn> discards = new LinkedList<>();
        String output = "";
        boolean first = true;
        for (Yarn y : yarns) {
            for (Yarn z : y.cross()) {
                if (!discards.contains(z)) {
                    // add a comma if we're not on the first line
                    if (!first) {
                        output += ",\n";
                    } else {
                        first = false;
                    }
                    // add the next line
                    output += y.joinWith(z).gapString();
                }
            }
            discards.add(y);
        }
        return output;
    }

    /**
     * Output a gap file with a full complement of substitutions rules.  
     * This method constructs a bunch of RhombBoundaries using edge sequences 
     * i1 and i2, then calls {@link #gapString} on each of them to produce its 
     * associated substitution rule.  
     * It writes all of these substitution rules, with appropriate preamble, to 
     * the file called fileName.  
     * @param i1 The edge sequence for the top and left edges of the rhombs.  
     * @param i2 The edge sequence for the bottom and right edges of the rhombs.  
     * @param ref Tells us whether or not we're making different substitution 
     * rules for the reflected versions of the prototiles.  This is deprecated.  
     * @param fileName The name of the file to which we write the gap output.  
     */
    public static void gapString(int[] i1, int[] i2, boolean ref, String fileName) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(fileName);

            // preamble
            out.write("test := rec(\n\n  inf := ");
            out.write(Point.gapString(i1) + ",\n\n");
            out.write("  rot := List( [1.." + (2*Point.N()) + "], i->Rot" + Point.N() + "^(i-1) ),\n\n");
            out.write("  seed := [ rec( pos := " + Point.ZERO() + ", typ := 1, orient := 0) ],\n\n");
            out.write("  basis := List( [0.." + (Point.N()-2) + "], i->[Cos(i*Phi" + Point.N() + "), Sin(i*Phi" + Point.N() + ")] ),\n\n");
            out.write("  prototiles := [\n");

            // prototiles
            Point Z = Point.ZERO();
            Point O = Point.createPoint(0);
            for (int i = 1; i < Point.N(); i = i+2) {
                Point I = Point.createPoint(i);
                for (int j = 0; j < ((ref) ? 2 : 1); j++) {
                    out.write("    rec( vertices := [ " + Z + ", " + O + ", " + O.minus(I) + ", " + Z.minus(I) + "],\n");
                    out.write("           angles := [ " + (Point.N()-i) + "/" + (2*Point.N()) + ", " + i + "/" + (2*Point.N()) + ", " + (Point.N()-i) + "/" + (2*Point.N()) + ", " + i + "/" + (2*Point.N()) + " ] )");
                    if (!(i+2>Point.N()&&j==((ref) ? 1 : 0))) out.write(",");
                    out.write("\n");
                }
            }
            out.write("  ],\n\n");

            // substitutions
            out.write("  subst_tiles := [\n");
            for (int i = 1; i < Point.N(); i = i+2) {
                for (int j = 0; j < ((ref) ? 2 : 1); j++) {
                    out.write("               [\n");
                    out.write("    function(t,T) return\n      [\n");
                    RhombBoundary RB = createRhombBoundary(Point.N()-i,(j==0) ? i1 : i2,(j==0) ? i2 : i1,false);
                    out.write(RB.gapString());
                    out.write("\n      ];\n    end\n");
                    out.write("               ]\n");
                    if (!(i+2>Point.N()&&j==((ref) ? 1 : 0))) out.write(",");
                    out.write("\n");
                }
            }
            out.write("  ],\n\n");

            // header file
            String order = ((Point.N()==5) ? "pent" : ((Point.N()==7) ? "hept" : "elf"));
            out.write("  header := \"" + order + "_rhomb_header.ps\",\n\n");

            // drawing functions
            out.write("  drawfuncs := [\n");
            double ang = 180.0/((double)Point.N());
            for (int i = 1; i < Point.N(); i = i+2) {
                for (int j = 0; j < ((ref) ? 2 : 1); j++) {
                    out.write("    function( tile, psfile )\n");
                    out.write("      AppendTo( psfile, \"gsave \",\n");
                    out.write("             ");
                    for (int k = 0; k < Point.N()-1; k++) out.write("tile.pos[" + (k+1) + "],\" \",");
                    out.write("\n             ");
                    out.write("\"" + order + "orth translate \",tile.orient*" + ang + ", ");
                    out.write("\n             ");
                    out.write("\" rotate t" + ((i/2)+1) + " grestore\\n\");\n");
                    out.write("    end");
                    if (!(i+2>Point.N()&&j==((ref) ? 1 : 0))) out.write(",");
                    out.write("\n");
                }
            }
            out.write("  ]\n\n);");

        } catch ( Exception e ) {
        } finally {
            try {
                if ( out != null)
                out.close( );
            } catch ( Exception e ) {
            }
        }
    } // end of static gapString

    /**
     * Write all of the Rhombs to a postscript file.  
     * @param fileName The name of the file to which we write the postscript output.  
     */
    public void postscriptString(String fileName) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(fileName);
            out.write("\n");
            for (Rhomb r : allJoins) out.write(r.postscriptString() + "\n");
            out.write("showpage");
        } catch ( Exception e ) {
        } finally {
            try {
                if ( out != null)
                out.close( );
            } catch ( Exception e ) {
            }
        }
    } // end of postscriptString

    /**
     * Output a gap file with a full complement of substitutions rules.  
     * Read the rules from a list of saved files.  Order matters.  The saved 
     * files represent inflated rhombs, and must be entered in order of 
     * decreasing even angles.  
     * Writes the results to a gap-readable file.  
     * @param i1 An edge sequence.  It is only used to compute the inflation 
     * matrix, not to make substitution rules.  It should match the inflation 
     * factors for the rhombs in the saved files, but we don't enforce this.  
     * @param saveFiles A list of names of saved .rb files containing 
     * serialized RhombBoundaries.  
     * @param outName The name of the output file.  
     */
    public static void gapString(int[] i1, String[] saveFiles, String outName) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(outName);

            // preamble
            out.write("test := rec(\n\n  inf := ");
            out.write(Point.gapString(i1) + ",\n\n");
            out.write("  rot := List( [1.." + (2*Point.N()) + "], i->Rot" + Point.N() + "^(i-1) ),\n\n");
            out.write("  seed := [ rec( pos := " + Point.ZERO() + ", typ := 1, orient := 0) ],\n\n");
            out.write("  basis := List( [0.." + (Point.N()-2) + "], i->[Cos(i*Phi" + Point.N() + "), Sin(i*Phi" + Point.N() + ")] ),\n\n");
            out.write("  prototiles := [\n");

            // prototiles
            Point Z = Point.ZERO();
            Point O = Point.createPoint(0);
            for (int i = 1; i < Point.N(); i = i+2) {
                Point I = Point.createPoint(i);
                out.write("    rec( vertices := [ " + Z + ", " + O + ", " + O.minus(I) + ", " + Z.minus(I) + "],\n");
                out.write("           angles := [ " + (Point.N()-i) + "/" + (2*Point.N()) + ", " + i + "/" + (2*Point.N()) + ", " + (Point.N()-i) + "/" + (2*Point.N()) + ", " + i + "/" + (2*Point.N()) + " ] )");
                if (!(i+2>Point.N())) out.write(",");
                out.write("\n");
            }
            out.write("  ],\n\n");

            // substitutions
            out.write("  subst_tiles := [\n");
            for (int i = 0; i < saveFiles.length; i++) {
                out.write("               [\n");
                out.write("    function(t,T) return\n      [\n");
                RhombBoundary RB = null;

                String fileName = saveFiles[i];
                if ( ! new File(fileName).isFile() ) {
                    System.out.println(fileName + " not found!");
                    System.exit(1);
                }
                try {
                    FileInputStream fileIn = new FileInputStream(fileName);
                    ObjectInputStream in = new ObjectInputStream(fileIn);
                    RB = ((RhombBoundary)in.readObject());
                    if (RB==null) System.exit(1);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(1);
                }

                out.write(RB.gapString());
                out.write("\n      ];\n    end\n");
                out.write("               ]\n");
                if (!(i==saveFiles.length-1)) out.write(",");
                out.write("\n");
            }
            out.write("  ],\n\n");

            // header file
            String order = ((Point.N()==5) ? "pent" : ((Point.N()==7) ? "hept" : ((Point.N()==11) ? "elf" : "nine")));
            out.write("  header := \"" + order + "_rhomb_header.ps\",\n\n");

            // drawing functions
            out.write("  drawfuncs := [\n");
            double ang = 180.0/((double)Point.N());
            for (int i = 1; i < Point.N(); i = i+2) {
                out.write("    function( tile, psfile )\n");
                out.write("      AppendTo( psfile, \"gsave \",\n");
                out.write("             ");
                for (int k = 0; k < Point.N()-1; k++) out.write("tile.pos[" + (k+1) + "],\" \",");
                out.write("\n             ");
                out.write("\"" + order + "orth translate \",tile.orient*" + ang + ", ");
                out.write("\n             ");
                out.write("\" rotate t" + ((i/2)+1) + " grestore\\n\");\n");
                out.write("    end");
                if (!(i+2>Point.N())) out.write(",");
                out.write("\n");
            }
            out.write("  ]\n\n);");

        } catch ( Exception e ) {
        } finally {
            try {
                if ( out != null)
                out.close( );
            } catch ( Exception e ) {
            }
        }
    } // end of static gapString

    /**
     * Output a gap file with a full complement of substitutions rules.  
     * Same as {@link #gapString(int[],int[],boolean,String)}, except the boolean variable ref is 
     * set to false, and the same edge sequence i is input for both i1 and i2.  
     */
    public static void gapString(int[] i, String fileName) {
        gapString(i,i,false, fileName);
    }

    /**
     * Create the reverse of an array of ints.  
     * @param i The array that we want to reverse.  
     * @return The reverse of i.  
     */
    public static int[] reverse(int[] i) {
        int l = i.length;
        int[] output = new int[l];
        for (int j = 0; j < l; j++) output[j] = i[l-1-j];
        return output;
    }

    /**
     * Check all permutations of an input list for 
     * compatibility with all rhombs.  
     * sym tells us whether or not to symmetrize the boundaries.  
     * This method writes to stdout all permutations of i that yield 
     * valid tilings for each inflated prototile.  
     * @param i The seed that we use to produce edge sequences.  All edge 
     * sequences are obtained by permuting i and then applying rule to replace 
     * symbols.  
     * @param sym Tells us whether or not to symmetrize the RhombBoundaries 
     * that we construct.  
     * @param rule A list of lists.  The nth entry tells us how to replace the 
     * number n wherever it appears in the sequence i, or any of its permutations.  
     * @param pre A list of ints to prepend to each permutation.  
     * @param suf A list of ints to append to each permutation.  
     */
    public static void allValid(int[] i,boolean sym,List<List<Integer>> rule,int[] pre, int[] suf) {
        System.out.println(vectorString(i));
        Integer[] ii = new Integer[i.length];
        for (int j = 0; j < ii.length; j++) ii[j] = i[j];
        MultiSetLinkedList perm = MultiSetLinkedList.createMultiSetLinkedList(ii);
        int[] first = perm.getArray();
        int[] current = first;
        boolean done = false;
        boolean pastFirst = false;
        LinkedList<Future<Result>> listOfFutures = new LinkedList<>();
        int searchCount = 0;

        // keep making work units until we've searched everything
        while (!done) {
            // new batch: reset the search count
            searchCount = 0;
            // search up to BATCH_SIZE permutations at one time, until we're done
            while ((!done)&&searchCount<BATCH_SIZE) {
                // create work units
                MultiSetLinkedList clone = perm.deepCopy();
                int j = 0;
                for (; j < WORK_UNIT_LENGTH; j++) {
                    if (Arrays.equals(first,perm.getArray())) {
                        if (pastFirst) {
                            done = true;
                            break;
                        } else {
                            pastFirst = true;
                        }
                    }
                    if (searchCount==BATCH_SIZE) break;
                    perm.iterate();
                    searchCount++;
                }
                if (j > 0) {
                    BoundaryWorkUnit wu = (rule==null) ? new BoundaryWorkUnit(clone,j,sym) : new BoundaryWorkUnit(clone,j,sym,rule,pre,suf);
                    listOfFutures.add(GeneralThreadService.INSTANCE.getExecutor().submit(wu));
                }
            } // end of while(searchCount<BATCH_SIZE)

            // poll the futures
            while (true) {
                // poll every 250 milliseconds
                try {
                    Thread.sleep(250L);
                }
                catch (InterruptedException e) {
                }

                int numberComplete = 0;
                for (Future<Result> thisFuture : listOfFutures) {
                    if ( thisFuture.isDone() )
                        numberComplete++;
                }
                System.out.print(String.format("%d of %d work units complete\r", numberComplete, listOfFutures.size()));
                if ( numberComplete == listOfFutures.size() )
                    break;
            }
            System.out.println();

            // count how many edges have been added
            for (Future<Result> thisFuture : listOfFutures) {
                try {
                    List<int[]> nextInt = ((BoundaryResult)thisFuture.get()).getValids();
                    if (nextInt.size()>0) {
                        for (int[] k : nextInt) {
                            System.out.print("result: ");
                            for (int t : k) {
                                System.out.print(t + " ");
                            }
                            System.out.print("\n");
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            listOfFutures.clear();
        } // end of big while loop

        System.out.println("all done!");
    } // end of allValid()

    /**
     * Check all permutations of an input list for 
     * compatibility with all rhombs.  
     * This is the same as {@link #allValid(int[],boolean,List,int[],int[])}, except it does not involve
     * a prefix or suffix.  
     * sym tells us whether or not to symmetrize the boundaries.  
     * This method writes to stdout all permutations of i that yield 
     * valid tilings for each inflated prototile.  
     * @param i The seed that we use to produce edge sequences.  All edge 
     * sequences are obtained by permuting i and then applying rule to replace 
     * symbols.  
     * @param sym Tells us whether or not to symmetrize the RhombBoundaries 
     * that we construct.  
     * @param rule A list of lists.  The nth entry tells us how to replace the 
     * number n wherever it appears in the sequence i, or any of its permutations.  
     */
    public static void allValid(int[] i,boolean sym,List<List<Integer>> rule) {
        allValid(i,sym,rule,null,null);
    }

    /**
     * Check all permutations of an input list for 
     * compatibility with all rhombs.  
     * This is the same as {@link #allValid(int[],boolean,List)}, except it does not involve a substitution.  
    
     * This method writes to stdout all permutations of i that yield 
     * valid tilings for each inflated prototile.  
     * @param i The seed that we use to produce edge sequences.  All edge 
     * sequences are obtained by permuting i.  
     * symbols.  
     * @param sym Tells us whether or not to symmetrize the RhombBoundaries 
     * that we construct.  
     */
    public static void allValid(int[] i,boolean sym) {
        allValid(i,sym,null);
    }

    /**
     * Represent an integer vector as a String.  
     * @param i The integer vector for which we produce a String representation.  
     * @return A String containing the elements of i, separated by commas and enclosed 
     * in square brackets.  
     */
    public static String vectorString(int[] i) {
        String output = "[";
        for (int j = 0; j < i.length; j++) output += i[j] + ((j==i.length-1) ? "]" : ",");
        return output;
    }

    /**
     * Get the number of tiles of each type.  
     * @return An int array, the ith entry of which is the number of rhombs of type i appearing 
     * in this tiling.  
     */
    public int[] tileNumbers() {
        int[] output = new int[Point.N()/2];
        for (Rhomb j : allJoins) output[j.getType()-1]++;
        return output;
    }

    /**
     * Produce a substitution matrix for the set of all rhombic prototiles with the given 
     * edge sequence.  
     * The substituted rhombs are RhombBoundaries constructed using {@link #createRhombBoundary(int,int[])}.  
     * @return An {@link Point#N()}/2 x N/2 int matrix, the (i,j)th entry of which is the number of 
     * tiles of type i appearing in the substituted image of tile j.  
     */
    public static int[][] substitutionMatrix(int[] i1) {
        int[][] output = new int[Point.N()/2][Point.N()/2];
        int[] column = new int[Point.N()/2];
        RhombBoundary RB = null;
        for (int i = 0; i < Point.N()/2; i++) {
            RB = createRhombBoundary(Point.N()-2*i-1,i1);
            column = RB.tileNumbers();
            for (int j = 0; j < Point.N()/2; j++) output [j][i] = column[j];
        }
        return output;
    }

    /**
     * Convert a two-dimensional array of ints to a String.  
     * @param i1 a matrix of ints that we represent as a String.  
     * @return A string containing all the entries of i1.  Rows are separated by 
     * newlines; columns are represented by commas.  
     */
    public static String matrixString(int[][] i1) {
        String output = "";
        for (int i = 0; i < i1.length; i++) {
            for (int j = 0; j < i1[i].length; j++) {
                output += i1[i][j] + ((j==i1[i].length-1) ? "\n" : ", ");
            }
        }
        return output;
    }

    /**
     * Output two lists of lists: the first is a list of pairs of 
     * boundary indices, indicating the boundary segments to which each
     * Yarn is connected.  The second is a list of lists of indices.  
     * Each list contains the indices of the Yarns that the Yarn at that 
     * position crosses.  
     * @return Two arrays of ints, the first boundary indices for all the  
     * Yarns, the second containing crossing lists for all the Yarns.  
     */
    public int[][][] yarnDump() {
        int len = termini.length;
        int[][][] output = new int[2][yarns.length][];
        List<Yarn> l = Arrays.asList(yarns);
        for (int i = 0; i < yarns.length; i++) {
            output[0][i] = new int[] {len - 1 - yarns[i].getStartIndex(),len - 1 - yarns[i].getEndIndex()};
            List<Yarn> cross = yarns[i].getCross();
            output[1][i] = new int[cross.size()];
            for (int j = cross.size()-1; j >= 0; j--) {
                output[1][i][j] = l.indexOf(cross.get(j));
            }
        }
        return output;
    }

    /**
     * Output the results of {@link #yarnDump()} as a String.  
     * @return A String representing all of the Yarn intersection data.  
     */
    public String yarnString() {
        String output = "[[[";
        int[][][] dump = yarnDump();
        for (int i = 0; i < dump[0].length; i++) {
            output += dump[0][i][0] + "," + dump[0][i][1] + "]";
            output += ((i==dump[0].length-1) ? "],[[" : ",[");
        }
        for (int i = 0; i < dump[1].length; i++) {
            for (int j = 0; j < dump[1][i].length; j++) {
                output += dump[1][i][j];
                output += ((j==dump[1][i].length-1) ? "]" : ",");
            }
            output += ((i==dump[1].length-1) ? "]]" : ",[");
        }
        return output;
    }

    /**
     * Output the angles in termini in reverse order, replacing 2*{@link Point#N()} with 0.
     * @return The angles in termini in reverse order, replacing 2*N with 0.
     */
    public String boundaryString() {
        String output = "[";
        for (int i = termini.length-1; i >= 0; i--) {
            output += (termini[i].getAngle()==2*Point.N()) ? 0 : termini[i].getAngle();
            output += ((i==0) ? "]" : ",");
        }
        return output;
    }


    /**
     * For testing.  
     */
    public static void main(String[] args) {


        int[] i1 = new int[] {1,-1,1,-1,3,-3,4,-4,1,-1,3,-3,1,-1,2,0,-2,2,0,-2};


        List<Integer> JJ0 = new LinkedList<>();
        JJ0.add(0);
        List<Integer> JJ1 = new LinkedList<>();
        JJ1.add(1);
        JJ1.add(-1);
        List<Integer> JJ2 = new LinkedList<>();
        JJ2.add(2);
        JJ2.add(-2);
        List<Integer> JJ3 = new LinkedList<>();
        JJ3.add(3);
        JJ3.add(-3);
        List<Integer> JJ4 = new LinkedList<>();
        JJ4.add(4);
        JJ4.add(-4);
        List<Integer> JJ5 = new LinkedList<>();
        JJ5.add(5);
        JJ5.add(-5);
        List<Integer> JJ6 = new LinkedList<>();
        JJ6.add(6);
        JJ6.add(-6);
        List<Integer> JJ7 = new LinkedList<>();
        JJ6.add(7);
        JJ6.add(-7);
        List<List<Integer>> allBumps = new LinkedList<>();
        allBumps.add(JJ0);
        allBumps.add(JJ1);
        allBumps.add(JJ2);
        allBumps.add(JJ3);
        allBumps.add(JJ4);
        allBumps.add(JJ5);
        allBumps.add(JJ6);
        allBumps.add(JJ7);

        List<Integer> II0 = new LinkedList<>();
        II0.add(0);
        List<Integer> II1 = new LinkedList<>();
        II1.add(1);
        II1.add(-1);
        List<Integer> II2 = new LinkedList<>();
        II2.add(-2);
        II2.add(0);
        II2.add(2);
        List<Integer> II3 = new LinkedList<>();
        II3.add(3);
        II3.add(1);
        II3.add(-1);
        II3.add(-3);
        List<Integer> II4 = new LinkedList<>();
        II4.add(-4);
        II4.add(-2);
        II4.add(0);
        II4.add(2);
        II4.add(4);
        List<Integer> II5 = new LinkedList<>();
        II5.add(5);
        II5.add(3);
        II5.add(1);
        II5.add(-1);
        II5.add(-3);
        II5.add(-5);
        List<List<Integer>> bigBumps = new LinkedList<>();
        bigBumps.add(II0);
        bigBumps.add(II1);
        bigBumps.add(II2);
        bigBumps.add(II3);
        bigBumps.add(II4);
        bigBumps.add(II5);

//        try this next one overnight
//        int[] ttt = new int[] {0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,4};
//        int[] ttt = new int[] {0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,5};
//        int[] ttt = new int[] {0,0,1,1,2,2,3,3,4,4,5};
//        int[] ttt = new int[] {0,0,0,1,1,1,2,2,2,3,3,4};
//        int[] ttt = new int[] {0,0,0,1,1,1,1,2,2,2,3,3,4};

//        int[] ttt = new int[] {0,1,1,2,2,2,3,3,3,4,4,5};
//        int[] ttt = new int[] {1,2,2,3,3,4};
//        int[] ttt = new int[] {2,3,3,4,4,5};
//        int[] ttt = new int[] {3,2,2,1,1,1,0,0,0};
//        int[] ttt = new int[] {1,2,2,2,3,3,3,4};
//        int[] jjj = allValid(ttt,false,bigBumps);
//        int[] ttt = new int[] {2,3,3,4};
//        int[] ttt = new int[] {0,0,1,1,2,2,3,3,4,4,5,5,6,6,7};
        int[] ttt = new int[] {3,2,2,1,1,0};
        ttt = new int[] {3,2,2,1,1,1,0,0,0};
        ttt = new int[] {5,4,4,3,3,2};
//        ttt = new int[] {5,4,4,3,3,2,2,1};
        ttt = new int[] {5,4,4,3,3,2,2,1,1,0}; // Pisot, 11, bigBumps
//        ttt = new int[] {5,4,4,3,3,3,2,2,2,1,1,0};
//        ttt = new int[] {5,4,4,3,3,3,2,2,1};
//        ttt = new int[] {5,4,4,4,3,3,3,2};
        ttt = new int[] {5,4,4,3,3,3,2,2,2,2,1,1,1,1,1,0,0,0,0,0}; // Pisot, 11, allBumps
        ttt = new int[] {4,3,3,2,2,2,1,1,1,1,0,0,0,0}; // not Pisot, 11, allBumps
        ttt = new int[] {5,4,4,3}; // not Pisot, 11, bigBumps
        ttt = new int[] {5,4,4,3,3,2}; // not Pisot, 11, bigBumps
        ttt = new int[] {5,4,4,3,3,2,2,1}; // not Pisot, 11, bigBumps
        ttt = new int[] {5,4,3,2,1}; // no dice
        ttt = new int[] {4,3,3,2,2,2,1,1,0}; // no dice, 11 or 9
        ttt = new int[] {4,3,3,2,2,1}; // not Pisot, 11 or 9, bigBumps
        ttt = new int[] {4,3,3,2,2,1,1,0}; // not Pisot, 11, yes Pisot, 9
//        ttt = new int[] {4,3,3,2}; // not Pisot, 11
        ttt = new int[] {1,3,1,3,0,2,5,3,1,2,0,4,2,0};


        ttt = new int[] {5,4,3,3,2,2}; // no dice, 11, bigBumps
        ttt = new int[] {5,4,4,3,3,2}; // no dice, 11, bigBumps
        ttt = new int[] {1,3,1,3,0,2,5,4,3,1,2,0,4,2,0}; // nice try, but no
        ttt = new int[] {4,4,3,3,3,2,2,1}; // no dice, 11, bigBumps
        ttt = new int[] {5,4,3,3,3,2,2,1}; // no dice, 11, bigBumps
        ttt = new int[] {5,4,3,3,2,2,1,0}; // no dice, 11, bigBumps
        ttt = new int[] {4,4,3,3,2,2,1,1}; // no dice, 11, bigBumps
//        ttt = new int[] {-1,1,-3,3,-5,5,-7,7,-9,9


        ttt = new int[] {1,3,0,2,1,0,4,2,5,3,1,2,3,0,1,4,2,0};
        ttt = new int[] {1,3,0,2,1,0,5,3,1,4,2,0,1,2,3,0,4,1,2,0};
        ttt = new int[] {1,3,1,3,0,2,5,3,4,1,2,0,4,2,0};
        ttt = new int[] {1,3,1,3,0,2,5,3,1,0,2,4,1,2,0,4,2,0};
        ttt = new int[] {1,3,0,2,1,0,4,2,0,1,3,5,0,1,0,1,2,3,0,1,4,2,0}; // only works for 4 and 6, N = 11
        int[] pre = new int[] {1,3,0,2,1,0,4,2,0,1,3,5};
        int[] suf = new int[] {0,1,2,3,0,1,4,2,0};
        ttt = new int[] {3,1,2,0,4,2,0,1,0,5,3,1};

        ttt = new int[] {4,3,3,2,2,2,1,1,1,1,0,0,0,0}; // yes Pisot, 9, allBumps
        ttt = new int[] {0,0,0,1,1,1,2,2,3}; // should work with N = 7
        ttt = new int[] {0,0,1,1,2}; // should work with N = 5
        Point.setN(5);
        allValid(ttt,true,allBumps);
//        allValid(ttt,true,allBumps,pre,suf);
//        allValid(ttt,false);

        i1 = new int[] {1,3,0,2,1,0,4,2,0,1,3,5,0,1,2,0,3,1};
        int[] i2 = new int[] {0,2,4,1,0,3,2,1,0,0,1,2,3,0,1,4,2,0};
//        RhombBoundary RB = createRhombBoundary(Integer.valueOf(args[0]),BoundaryWorkUnit.substitute(i1,allBumps),BoundaryWorkUnit.substitute(i2,allBumps),true);
//        RhombBoundary RB = createSymmetricRhombBoundary(Integer.valueOf(args[0]),BoundaryWorkUnit.substitute(ttt,allBumps));
//        System.out.println(RB.valid());

//        int[] ll = new int[] {1,-1,3,-3,0,1,-1,2,-2,3,-3,1,-1,0,2,-2,0,4,-4,1,-1,2,-2,0};
//        int[] ll = new int[] {1,-1,3,-3,0,2,-2,1,-1,0,1,-1,2,-2,0,3,-3,1,-1,4,-4,2,-2,0};
//        int[] ll = new int[] {1,-1,3,-3,0,1,-1,2,-2,0,1,-1,2,-2,0};
        int[] ll = new int[] {-1,1,0,-1,1,2,-2,0};
        ll = new int[] {3,1,-1,-3,-2,0,2};
        ll = new int[] {-2,0,2,-2,0,2,1,-1,3,1,-1,-3};
        ll = new int[] {-2,0,2,1,-1,1,-1,0,-2,0,2,3,1,-1,-3};
        ll = new int[] {1,-1,0,3,-3,1,-1,2,-2,0,1,-1,2,-2,0};
        ll = new int[] {3,1,-1,-3,-2,0,2,-4,-2,0,2,4,5,3,1,-1,-3,-5,-4,-2,0,2,4,3,1,-1,-3,1,-1,-2,0,2};
        ll = new int[] {-2,0,2,1,-1,3,1,-1,-3,-4,-2,0,2,4,5,3,1,-1,-3,-5,-4,-2,0,2,4,3,1,-1,-3,-2,0,2,0,1,-1};
        ll = new int[] {3,1,-1,-3,-2,0,2,-2,0,2,1,-1,3,1,-1,-3,-4,-2,0,2,4};
        ll = new int[] {-4,-2,0,2,4,-4,-2,0,2,4,3,1,-1,-3,5,3,1,-1,-3,-5};
        ll = new int[] {-4,-2,0,2,4,3,1,-1,-3,3,1,-1,-3,-2,0,2,-4,-2,0,2,4,5,3,1,-1,-3,-5};
        ll = new int[] {-4,-2,0,2,4,3,1,-1,-3,-2,0,2,-2,0,2,1,-1,3,1,-1,-3,-4,-2,0,2,4,5,3,1,-1,-3,-5};
        ll = new int[] {-2,0,2,-4,-2,0,2,4,3,1,-1,-3,3,1,-1,-3};
        ll = new int[] {3,1,-1,-3,-2,0,2,-2,0,2,1,-1,3,1,-1,-3,-4,-2,0,2,4};
        ll = new int[] {3,1,-1,-3,-2,0,2,1,-1,1,-1,0,-2,0,2,3,1,-1,-3,-4,-2,0,2,4};

        ll = new int[] {5,4,4,3,3,3,2,2,2,2,1,1,1,1,1,0,0,0,0,0}; // Pisot, 11, allBumps
        ll = new int[] {1,-1,0,2,-2,3,-3,1,-1,0,4,-4,2,-2,0,1,-1,3,-3,5,-5,2,-2,1,-1,4,-4,0,3,-3,2,-2,1,-1,0};
        ll = new int[] {-4,-2,0,2,4,3,1,-1,-3,5,3,1,-1,-3,-5,-4,-2,0,2,4,-2,0,2,3,1,-1,-3};

//        gapString(ll,args,"eleven-stars.g");
//        gapString(BoundaryWorkUnit.substitute(ll,allBumps),args,"eleven-pisot-special.g");
//        gapString(new int[] {-1,1,0,-1,1,2,-2,0,2,-2,1,-1},args,"seven-medium.g");

//        System.out.println(matrixString(substitutionMatrix(ll)));

        System.exit(0);

    } // end of main method

} // end of class RhombBoundary

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
 * A triple of {@link Rhomb}s, any two of which have a common edge.  
 * This triple can be flipped, which shifts the three Rhombs to opposite sides of the hex.  
 */
public interface Hex {

    /**
     *  Getter method.  
     *  @return A list of the three Rhombs.
     */
    public Rhomb[] getJoins();

    /**
     *  Getter method.  
     *  @return A polygon equal to the union of the three rhombs.  
     */
    public SimplePolygon2D getHex();

    /**
     *  Create a simplified version of this.  
     *  @return A SimpleHex with all the essential information of this.  
     */
    public SimpleHex createSimpleHex(List<Rhomb> allJoins, List<Rhomb> newJoins);

    /**
     *  Determine if this represents a hexagon--i.e., if the three {@link Rhomb}s are still in contact.  
     *  @return true if the three Rhombs are still in contact.  
     */
    public boolean valid();

    /**
     *  Determine if this contains a given Rhomb.  
     *  @param jj The Rhomb we're looking for.  
     *  @return true if this contains jj, false otherwise.  
     */
    public boolean contains(Rhomb jj);

    /**
     *  Flip the {@link Rhomb}s in this Hex.  
     *  This shifts them to opposite sides of the hex.  
     *  @return A list of all Hexes created or destroyed by this flip.  
     */
    public List<Hex> flip();

} // end of interface Hex

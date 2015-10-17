import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import math.geom2d.polygon.SimplePolygon2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.Point2D;
import math.geom2d.AffineTransform2D;
import math.geom2d.line.*;
import math.geom2d.Point2D;
import math.geom2d.Angle2D;
import math.geom2d.Box2D;
import java.util.Collection;

import javax.swing.JPanel;

/**
 *  A class for displaying a collection of {@link SimpleRhombs}.  
 *  It doesn't allow the user to interact with the SimpleRhobms directly, but 
 *  it is typically included in a {@link SubstitutionEditor}, which does have 
 *  interactive elements.  
 */
public class PatchDisplay extends JPanel {
    private final List<SimpleRhomb> SEED; // the starting patch before any substitutions
    private List<List<SimpleRhomb>> poly;
    private List<RhombBoundary> rules;
    private Point[] infl;
    /** edge is for drawing supertile outlines */
    private int[] edge;
    // data for adjusting the size and angle of the image to fit in frame
    private double factor;
    private final double ROTATION_INCREMENT; // how much we increment rotation on each substitution
    private double rotation; // how much we rotate when drawing the current level
    private Point2D centroid;
    private int currentLevel;
    private final int maxSubstitutions;
    private static final Box2D EMPTY_BOX = new Box2D();
    private static final double BUFFER_FACTOR = 1.1;  // scale the window so it doesn't quite hit the edge
    private final double SCALE_DECREMENT;  // for changing scale after a subtitution
    private final double SCALE;  // for scaling tiles
    private final double XTRANS; // for translating graphics
    private final double YTRANS; // for translating graphics
    private int width;
    private int height;
    private static final int BORDER = 125;
    /**
     *  Toggle graphics antialiasing.  
     *  On improves the picture, off is faster.  
     */
    public static boolean antialiasing = false;
    /**
     *  Toggle supertile outlines.  
     */
    public static boolean supertiles = false;

    public PatchDisplay(List<SimpleRhomb> p, List<RhombBoundary> RB, int maxSubstitutions, Point[] infl, int[] edge, int w, int h) {
        this.maxSubstitutions = maxSubstitutions;
        this.currentLevel = 0;
        this.rules = RB;
        this.infl = infl;
        this.edge = edge;
        this.factor = 1.0;
        this.rotation = 0.0;
        this.SEED = p;
        this.poly = new ArrayList<List<SimpleRhomb>>();
        this.poly.add(SEED);
        for (int i = 0; i < this.maxSubstitutions+1; i++) {
            List<SimpleRhomb> nextList = new ArrayList<SimpleRhomb>();
            this.poly.add(nextList);
        }
        this.width  = w;
        this.height = h;
        // assume there's only one SEED polygon
        this.centroid = SEED.get(0).getRhomb().centroid();

        // set SCALE, ROTATION_INCREMENT, XTRANS, YTRANS
        this.ROTATION_INCREMENT = Angle2D.angle(infl[0].getPoint2D(),new Point2D(),new Point2D(1.0,0.0));
        this.SCALE_DECREMENT = infl[0].getPoint2D().distance(0.0,0.0);
        // make a boundary (first substituted image of SEED) withing which the window should fit
        Collection<LineSegment2D> edges = SEED.get(0).outline(infl,edge).transform(AffineTransform2D.createScaling(1/SCALE_DECREMENT,1/SCALE_DECREMENT).concatenate(AffineTransform2D.createRotation(ROTATION_INCREMENT))).edges();
//        Collection<LineSegment2D> edges = SEED.get(0).getRhomb().edges();
        double minParam = 0.0;
        for (int i = -1; i < 3; i+=2) {
            for (int j = -1; j < 3; j+=2) {
                Ray2D testRay = new Ray2D(centroid,i*w,j*h);
                // find the intersection of testRay with the boundary of SEED
                Point2D intersection = null;
                for (LineSegment2D e : edges) {
                    Point2D cross = testRay.intersection(e);
                    if (cross!=null) {
                        intersection = cross;
                        break;
                    }
                }
                // find the parameter t that yields this intersection
                double t = (intersection.x() - centroid.x())/(i*w);
                if ((i==-1&&j==-1)||t<minParam) minParam = t;
            }
        }
        this.SCALE = BUFFER_FACTOR*0.5/minParam;
        this.XTRANS = w/2 - centroid.x()*SCALE;
        this.YTRANS = h/2 - centroid.y()*SCALE;
    }

    /**
     *  Draw the contents of this patch.  
     *  @param g The Graphics on which we draw the patch.  
     */
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g; // cast to Graphics2D
        g2.setColor(Color.WHITE);
        AffineTransform2D scaling = AffineTransform2D.createScaling(SCALE*factor,SCALE*factor);
        AffineTransform2D rot     = AffineTransform2D.createRotation(rotation);
        AffineTransform2D trans   = scaling.concatenate(rot);
        // antialiasing looks better, but the slowdown is significant
        if (antialiasing) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.translate(XTRANS,YTRANS);
        for (SimpleRhomb p : poly.get(currentLevel)) {
            SimplePolygon2D drawMe = p.getRhomb().transform(trans);
            g2.setColor(ColourPalette.colour(p.getType()-1)); 
            drawMe.fill(g2);
            g2.setColor(Color.BLACK); 
            drawMe.draw(g2);
        }
        if (supertiles&&(currentLevel>0)) {
            g2.setColor(Color.BLACK); 
            g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (SimpleRhomb s : poly.get(currentLevel-1)) {
                s.outline(infl,edge).transform(trans).draw(g2);
            }
            g2.setStroke(new BasicStroke());
        }
    }

    /**
     * The default: substitute once.  
     */
    public void subRhomb() {
        factor /= SCALE_DECREMENT;
        rotation += ROTATION_INCREMENT;
        List<SimpleRhomb> nextSubtiles = poly.get(currentLevel+1);
        nextSubtiles.clear();
        for (SimpleRhomb r : poly.get(currentLevel)) {
            Point shift = r.getPoint().multiply(infl);
            int rotation = r.getAngle();
            List<Rhomb> substitutedTile = rules.get(r.getType()-1).getJoins();
            for (Rhomb R : substitutedTile){
                SimpleRhomb nextRhomb = R.createSimpleRhomb().transform(rotation,shift);
                nextSubtiles.add(nextRhomb);
            }
        }
        currentLevel++;
    }

    /**
     * Substitute n times.  
     * @param n The number of times to substitute.  
     */
    public void subRhomb(int n) {
        for (int i = 0; i < n; i++) subRhomb();
    }

    /**
     * Goes back to the beginning and substitutes everything again.  
     * @param substitutions The number of times to substitute, starting from the beginning.  
     */
    public void update(int substitutions) {
        resetRhomb();
        subRhomb(substitutions);
        updateUI();
    }

    /**
     * Resets the patch to its initial state, before any substitutions had 
     * been applied.  
     */
    public void resetRhomb(){
        currentLevel = 0;
        factor = 1.0;
        rotation = 0.0;
    }

    /**
     * Get the rotation that is applied to all tiles to make them fit 
     * in the window.  
     * @return The rotation that is applied to all tiles to make them 
     * fit in the window.  
     */
    public double getRotation() {
        return rotation;
    }

    /**
     * Return a List of SimpleRhombs that appear in this.  
     * @return A List of SimpleRhombs that appear in this.  
     */
    public List<SimpleRhomb> getPatch() {
        return poly.get(currentLevel);
    }

    /**
     * Return a List of SimpleRhombs that are supertiles in this.  
     * They will be at the wrong scale, and so must be rescaled by the 
     * method that calls this one.  
     * @return A List of SimpleRhombs that are supertiles in this.  
     */
    public List<SimpleRhomb> getSupertiles() {
        if (currentLevel>0) {
            return poly.get(currentLevel-1);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * String representation of a supertile for Postscript.  
     * The method inflates the supertile first.  
     * @param tile The tile we want to draw.  
     * @return A String with instructions for how to draw tile in Postscript.  
     */
    public String supertilePostscriptString(SimpleRhomb tile) {
        Point p = tile.getPoint();
        return "gsave " + p.multiply(infl).postscriptString() + Point.order() + "orth translate " + (tile.getAngle()*(180.0/Point.N())) + " rotate supert" + tile.getType() + " grestore";
    }

    /**
     * Produce a String representation of the bounding box of this 
     * display for use in creating Postscript output.  
     * @return The bounding box in the format "xmin ymin xmax ymax".  
     */
    public String boundingBox() {
        double s = poly.get(0).get(0).getScale();
        int w = (int)((width*28.3464)/(s*SCALE*factor));
        int h = (int)((height*28.3464)/(s*SCALE*factor));
        int xmin = (int)(4*28.3464-(XTRANS*28.3464)/(s*SCALE*factor));
        int ymin = (int)(4*28.3464-(YTRANS*28.3464)/(s*SCALE*factor));
        return xmin + " " + ymin + " " + (xmin+w) + " " + (ymin + h);
    }

    /**
     * Produce a String representation of the ith proto-supertile, 
     * for use in a postscript file.  
     * @param i The type of the supertile for which we produce Postscript 
     * instructions.  
     * @return Postscript instructions for drawing the supertile of 
     * type i.  
     */
    public String supertile(int i) {
        List<Point> prototile = RhombBoundary.createPrototile(i).getJoins().get(0).createSimpleRhomb().supertile(infl,edge);
        String output = "/supert" + (i+1) + "{\n   newpath\n";
        output += "     " + prototile.get(0).postscriptString() + Point.order() + "orth moveto\n";
        for (int j = 1; j < prototile.size(); j++) {
            output += "     " + prototile.get(j).postscriptString() + Point.order() + "orth lineto\n";
        }
        output += "   closepath\n   stroke\n}def\n\n";
        return output;
    }

} // end of class PatchDisplay

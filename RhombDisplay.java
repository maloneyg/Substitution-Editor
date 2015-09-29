import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.util.List;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.plaf.basic.*;
import java.io.*;
import math.geom2d.polygon.SimplePolygon2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.Point2D;
import math.geom2d.line.Line2D;
import math.geom2d.AffineTransform2D;
import math.geom2d.Box2D;

// a button located on a hexagon (three rhombs, any pair of which shares an edge)
class HexButton extends JButton {

    private static final Color RED = new Color(1, 0, 0, 0.75f); //Red 
    private static final double scale = 0.6; // shrink the clickable area
    private RhombBoundary r;
    private RhombDisplay RD;
    private static boolean dragging = false;
    private static List<Hex> draggable = null;
    private static Component lastEntered = null;
    private final Hex t;
    private SimplePolygon2D mouseHex;
    private SimplePolygon2D drawHex;
    private static int xmin;
    private static int ymin;
    boolean in = false;

    // private constructor
    private HexButton(Hex tt,RhombDisplay rd) {
        this.t = tt;
        this.RD = rd;
        this.r = RD.getBoundary();
        this.xmin = RD.getXMin();
        this.ymin = RD.getYMin();
        this.setup();
    }

    // private setup method for use in constructor
    private void setup() {
        this.drawHex = t.getHex().transform(AffineTransform2D.createScaling(RD.scale,RD.scale));
        this.mouseHex = drawHex.transform(AffineTransform2D.createTranslation(xmin,ymin));
//        this.mouseHex = drawHex.transform(AffineTransform2D.createTranslation(xmin+RD.WIDTH_SHIFT,ymin+RD.HEIGHT_SHIFT));
        this.mouseHex = mouseHex.transform(AffineTransform2D.createScaling(mouseHex.centroid(),scale,scale));
        this.setEnabled(true);

        this.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e)
            {
                if (mouseHex.contains(e.getX(), e.getY())) {
                    if (dragging&&draggable.contains(t)) {
                        draggable = r.flipTriple(t);
                        RD.click(draggable);
                        draggable.add(t);
                    }
                    in = true;
                    RD.repaint();
                }
            }
            public void mouseExited(MouseEvent e)
            {
                if(!mouseHex.contains(e.getX(), e.getY())) {
                    in = false;
                    RD.repaint();
                }
            }
//            public void mouseClicked(MouseEvent e)
//            {
//                if(mouseHex.contains(e.getX(), e.getY())) {
//                    if (m!=null) {
//                        RD.click(m.addFrame(t));
//                    } else {
//                        RD.click(r.flipTriple(t));
//                    }
//                }
//            }
            public void mousePressed(MouseEvent e)
            {
                if(mouseHex.contains(e.getX(), e.getY())) {
                    dragging = true;
                    draggable = r.flipTriple(t);
                    RD.click(draggable);
                    draggable.add(t);
                }
            }
            public void mouseReleased(MouseEvent e)
            {
                        dragging = false;
                        draggable = null;
//                RD.repaint();
            }
        });

    }

    // public static factory method
    public static HexButton createHexButton(Hex t,RhombDisplay d) {
        return new HexButton(t,d);
    }

    // is this still a hexagon?
    public boolean valid() {
        return t.valid();
    }

    // is tt the Hex of this?
    public boolean hasTriple(Hex tt) {
        return t.equals(tt);
    }

    public boolean contains(int x, int y) {
        if (mouseHex.contains((double)x,(double)y)) {
            return true;
        } else {
            return false;
        }
    }

    public void paintComponent(Graphics g)
    {
        if (in) {
            Graphics2D g2;
            g2 = (Graphics2D)g;
            g2.setColor(RED);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawHex.fill(g2);
        }
    }

} // end of class HexButton

public class RhombDisplay extends JPanel implements ActionListener
{

    public static final int WIDTH_BUFFER = 20;
    public static final int HEIGHT_BUFFER = 20;
    public static final double SCALE = 30.0;
//    public static final int WIDTH_SHIFT = WIDTH_BUFFER/2;
//    public static final int HEIGHT_SHIFT = HEIGHT_BUFFER/2;
    private final RhombBoundary r;
    private List<Rhomb> joins;
    private List<HexButton> buttons;
    private final int xmin;
    private final int ymin;
    private final int width;
    private final int height;
    public final double scale;
    private static SubstitutionEditor editor = null;

    // default constructor
    public RhombDisplay(RhombBoundary r) throws java.awt.HeadlessException
    {
        this(r,RhombDisplay.SCALE);
    }

    public RhombDisplay(RhombBoundary r, double scale, int w, int h) throws java.awt.HeadlessException
    {
        this.r = r;
        this.scale = scale;
        joins = r.getJoins();

        width = w+WIDTH_BUFFER;
        height = h+HEIGHT_BUFFER;

        joins.get(0).setScale(scale);
        Box2D box = joins.get(0).getRhomb().boundingBox();
        for (Rhomb j : joins) {
            j.setScale(scale);
            box = box.union(j.getRhomb().boundingBox());
        }

        ymin = height/2 - (int)((box.getMinY()+box.getMaxY())/2);
        xmin = width/2  - (int)((box.getMinX()+box.getMaxX())/2);
        setup();
    }

    public RhombDisplay(RhombBoundary r, double scale) throws java.awt.HeadlessException
    {
        this.r = r;
        this.scale = scale;
        joins = r.getJoins();

        joins.get(0).setScale(scale);
        Box2D box = joins.get(0).getRhomb().boundingBox();
        for (Rhomb j : joins) {
            j.setScale(scale);
            box = box.union(j.getRhomb().boundingBox());
        }

        width = (int)box.getWidth()+WIDTH_BUFFER;
        height = (int)box.getHeight()+HEIGHT_BUFFER;

        ymin = height/2 - (int)((box.getMinY()+box.getMaxY())/2);
        xmin = width/2  - (int)((box.getMinX()+box.getMaxX())/2);
        this.setPreferredSize(new Dimension(width,height));
        setup();
    }

    // utility method for use in constructors
    private void setup() throws java.awt.HeadlessException
    {
        setLayout(null);

        buttons = new LinkedList<>();
        for (Hex t : r.getTriples()) {
            HexButton b = HexButton.createHexButton(t,this);
            buttons.add(b);
            add(b);
        }

        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());
        content.add(this, BorderLayout.CENTER);
        setBackground(Color.WHITE);
    }

    public void click(List<Hex> l) {
        flushButtons(l);
        repaint();
    }

    public void flushButtons(List<Hex> l) {
        for (Hex t : l) {
            boolean add = true;
            for (HexButton hb : buttons) {
                if (hb.hasTriple(t)) {
                    remove(hb);
                    buttons.remove(hb);
                    add = false;
                    break;
                }
            }
            if (add) {
                HexButton b = HexButton.createHexButton(t,this);
                buttons.add(b);
                add(b);
            }
        }
        if (editor!=null) editor.updatePatch();
    }

    public void resetButtons() {
        for (HexButton hb : buttons) remove(hb);
        buttons.clear();
        for (Hex t : r.getTriples()) {
            HexButton hb = HexButton.createHexButton(t,this);
            add(hb);
            buttons.add(hb);
        }
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getXMin() {
        return xmin;
    }

    public int getYMin() {
        return ymin;
    }

    public RhombBoundary getBoundary() {
        return r;
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2;
        g2 = (Graphics2D)g;
        g2.setColor(Color.BLACK);
        g2.translate(xmin,ymin);
//        g2.translate(xmin+WIDTH_SHIFT,ymin+HEIGHT_SHIFT);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Rhomb j : joins) {
            g2.setColor(ColourPalette.colour(j.getType()-1));
            j.getRhomb().fill(g2);
            g2.setColor(Color.BLACK);
            j.getRhomb().draw(g2);
        }
        for (HexButton b : buttons) b.paintComponent(g2);

    }


    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width,height);
    }

    public void actionPerformed(ActionEvent e) {
        if ("print".equals(e.getActionCommand())) {
            System.out.println(r.gapString());
        }
    }

    public static void setEditor(SubstitutionEditor e) {
        editor = e;
    }

    /*
     * Take a List of RhombBoundaries and return a List of the corresponding RhombDisplays,
     * all given the same scale, which is chosen in such a way as to make each one fit in 
     * a pane of the given dimensions.  
     * @param rules The RhombBoundaries we want to put into RhombDisplays.  
     * @param paneWidth The width of the pane in which we want each one to fit.  
     * @param paneHeight The height of the pane in which we want each one to fit.  
     * @return A List of RhombDisplays generated from rules, each one scaled to fit 
     * in a pane of dimensions paneWidth x paneHeight.  
     */
    public static List<RhombDisplay> displayList(List<RhombBoundary> rules, int paneWidth, int paneHeight) {
        // find out the maximum height and width of all the patches
        double maxHeight = 0.0;
        double maxWidth  = 0.0;
        double prescale = 1.0; // the scale of the given rhombs (hopefully all the same)
        for (RhombBoundary r : rules) {
            List<Rhomb> joins = r.getJoins();
            prescale = joins.get(0).getScale();
            Box2D box = joins.get(0).getRhomb().boundingBox();
            for (Rhomb j : joins) {
                box = box.union(j.getRhomb().boundingBox());
            }
            if (box.getHeight() > maxHeight) maxHeight = box.getHeight();
            if (box.getWidth() > maxWidth) maxWidth = box.getWidth();
        }
        double scale = prescale*Math.min((paneWidth   - RhombDisplay.WIDTH_BUFFER)/maxWidth,
                                         (paneHeight - RhombDisplay.HEIGHT_BUFFER)/maxHeight);

        List<RhombDisplay> RD = new ArrayList<RhombDisplay>();
        for (int i = 0; i < rules.size(); i++) {
            RhombDisplay display = new RhombDisplay(rules.get(i),scale,paneWidth-RhombDisplay.WIDTH_BUFFER,paneHeight-RhombDisplay.HEIGHT_BUFFER);
            RD.add(display);
        }
        return RD;
    }


    public static void main(String[] args) {

        int[] i1 = new int[] {1,-1,2,0,-2};

        RhombBoundary RB = RhombBoundary.createSymmetricRhombBoundary(Integer.valueOf(args[0]),i1);
//        RhombDisplay display = new RhombDisplay(RB,"test");

    }


} // end of class RhombDisplay

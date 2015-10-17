import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.util.List;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.plaf.basic.*;
import java.io.*;
import math.geom2d.polygon.SimplePolygon2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.Point2D;
import math.geom2d.AffineTransform2D;
import math.geom2d.Box2D;
import javax.swing.event.*;

/** 
 * Listen for changes in the RhombColorChooser, then modify ColourPalette.  
 */
class ColourChangeListener implements ChangeListener {

    /**
     * The RhombColourChooser to which this is listening.  
     */
    private final RhombColorChooser chooser;

    public ColourChangeListener(RhombColorChooser rs) {
        this.chooser = rs;
    }

    public void stateChanged(ChangeEvent e) {
        ColourPalette.changeColour(chooser.getSelected(),chooser.getColor());
        chooser.redrawEditor();
    }
}

/** 
 * Listens to the radio buttons. 
 */
class RhombSelectorListener implements ActionListener {

    /**
     * The common RhombSelector that all RhombSelectorListeners modify.  
     */
    public static RhombSelector parent;
    /**
     * The index of the colour to which this Listener corresponds.  
     */
    private final int index;

    /**
     * Constructor.  Set the index.  
     * @param i The index in {@link ColourPalette} of the colour to which this 
     * Listener corresponds.  
     */
    public RhombSelectorListener (int i) {
        this.index = i;
    }

    public void actionPerformed(ActionEvent e) {
        parent.setSelected(index);
    }
} // end of class RhombSelectorListener

/**
 * A panel with one copy of each prototile, with a radio button underneath.  
 * The radio button that is currently selected identifies the prototile, the 
 * colour of which is being changed.  
 */
class RhombSelector extends JPanel {

    // dimensions
    private static final int XBUFFER = 6;
    private static final int YBUFFER = 6;
    private static final int COLOR_CHOOSER_WIDTH = 600; // a guess

    /**
     * The RhombColorChooser that contains this.  
     */
    private RhombColorChooser parent;

    /**
     * pictures of the prototiles.  
     */
    private List<RhombDisplay> RD;

    /**
     * radio buttons for selecting a prototile.  
     */
    private ButtonGroup prototiles;

    /**
     * the index of the colour that is currently selected.  
     */
    private int selected = 0;

    /**
     * Get the index of the colour currently being edited.  
     * @return The index of the colour currently being edited.  
     */
    public int getSelected() {
        return selected;
    }

    /**
     * Set the index of the colour to edit.  
     * @param i The new index of the colour to edit.  
     */
    public void setSelected(int i) {
        selected = i;
        parent.setColor(ColourPalette.colour(i));
    }

    /**
     * Constructor.  
     * @param parent The RhombColorChooser that contains this.  
     */
    public RhombSelector(RhombColorChooser parent) {
        super(new GridLayout(2,Point.N()/2));
        this.parent = parent;
        RhombSelectorListener.parent = this;
        List<RhombBoundary> rules = RhombBoundary.prototileList();
        int paneWidth = 2*COLOR_CHOOSER_WIDTH/(Point.N()-1);
        RD = RhombDisplay.displayList(rules,paneWidth,2*paneWidth/3);

        prototiles = new ButtonGroup();
        for (RhombDisplay rd : RD) {
            // add the pictures
            rd.setBackground(this.getBackground());
            this.add(rd);
            rd.setVisible(true);
        }
        for (int i = 0; i < RD.size(); i++) {
            // add the radio buttons
            JRadioButton b = new JRadioButton("",(i==0));
            b.addActionListener(new RhombSelectorListener(i));
            b.setHorizontalAlignment(SwingConstants.CENTER);
            prototiles.add(b);
            this.add(b);
        }
    }

} // end of class RhombSelector

/**
 * A class for changing the colours of prototiles.  
 */
public class RhombColorChooser extends JColorChooser
{

    private final SubstitutionEditor SE;
    private final RhombSelector selector;

    /**
     * Get the index of the colour currently being edited.  
     * @return The index of the colour currently being edited.  
     */
    public int getSelected() {
        return selector.getSelected();
    }

    /**
     * Redraw the SubstitutionEditor that called this.  
     */
    public void redrawEditor() {
        SE.validate();
        SE.repaint();
    }

    public RhombColorChooser(SubstitutionEditor s) throws java.awt.HeadlessException
    {
        super(ColourPalette.colour(0));
        SE = s;
        this.setPreviewPanel(new JPanel());
        this.setVisible(true);
        this.selector = new RhombSelector(this);
        this.getSelectionModel().addChangeListener(new ColourChangeListener(this));
        this.setPreviewPanel(selector);
        this.validate();
        this.repaint();
    }

} // end of class RhombColorChooser

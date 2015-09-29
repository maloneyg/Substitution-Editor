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
 * A panel with one copy of each prototile, with an unselectable checkbox 
 * button underneath.  The buttons indicate whether or not the prototiles 
 * can be tiled with a given edge sequence.  
 */
class PrototileChecker extends JPanel {

    // dimensions
    private static final int XBUFFER = 6;
    private static final int YBUFFER = 6;
    private final int PANE_HEIGHT;
    private static final int COLOR_CHOOSER_WIDTH = 600; // a guess

    /**
     * The Launcher that contains this.  
     */
    private Launcher parent;

    /**
     * the prototiles.  
     */
    private List<RhombBoundary> RB;

    /**
     * pictures of the prototiles.  
     */
    private List<RhombDisplay> RD;

    /**
     * checkboxes for the prototiles.  
     */
    private List<JCheckBox> CB = new ArrayList<>();

    /**
     * utility field for constructing the layout.  
     */
    private GridBagConstraints c = new GridBagConstraints();

    /**
     * Determine if all prototiles can be tiled with the given edge sequence.  
     * @return true If all prototiles can be tiled.  
     */
    public boolean okay() {
        for (JCheckBox cb : CB) if (!cb.isSelected()) return false;
        return true;
    }

    /**
     * Constructor.  
     * @param parent The Launcher that contains this.  
     */
    public PrototileChecker(Launcher parent) {
        this.parent = parent;
        this.PANE_HEIGHT = (int)parent.getContentPane().getSize().getHeight()-3*Launcher.BUTTON_HEIGHT;
        RD = new ArrayList<RhombDisplay>();
        CB = new ArrayList<JCheckBox>();
        this.setLayout(new GridBagLayout());
        changeN();
    }

    /**
     * Respond to a change in the order of symmetry.  
     * This means changing the prototiles.  
     */
    public void changeN() {
        for (RhombDisplay rd : RD) this.remove(rd);
        for (JCheckBox cb : CB) this.remove(cb);
        RB = RhombBoundary.prototileList();
        int paneWidth = 2*Launcher.WIDTH/(Point.N()-1);
        // change the dimensions of this
        this.setSize(new Dimension(Launcher.WIDTH,PANE_HEIGHT+Launcher.BUTTON_HEIGHT));
        this.setPreferredSize(new Dimension(Launcher.WIDTH,PANE_HEIGHT+Launcher.BUTTON_HEIGHT));
        RD = RhombDisplay.displayList(RB,paneWidth,PANE_HEIGHT);
        for (int i = 0; i < RD.size(); i++) {
            RhombDisplay rd = RD.get(i);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.ipady = (int)rd.getPreferredSize().getHeight()-(int)rd.getMinimumSize().getHeight();
            c.weightx = 0.5;
            c.gridwidth = 1;
            c.gridx = i;
            c.gridy = 0;
            // add the pictures
            rd.setBackground(this.getBackground());
            rd.setVisible(true);
            this.add(rd,c);
        }
        CB.clear();
        Dimension boxSize = new Dimension(paneWidth,PANE_HEIGHT);
        for (int i = 0; i < RD.size(); i++) {
            // add the CheckBoxes
            JCheckBox b = new JCheckBox("",false);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.fill = GridBagConstraints.VERTICAL;
            c.ipady = Launcher.BUTTON_HEIGHT-(int)b.getMinimumSize().getHeight();
            c.weightx = 0.5;
            c.gridwidth = 1;
            c.gridx = i;
            c.gridy = 1;
            b.setSelected(false);
            b.setEnabled(false);
            this.add(b,c);
            CB.add(b);
        }
    }

    /**
     * Respond to a change in the edge sequence.  
     * This means changing the checkboxes to reflect whether or not 
     * the inflated prototiles can be tiled with the given edge 
     * sequence.  
     * @param seq The edge sequence.  
     */
    public void changeEdgeSequence(int[] seq) {
        if (seq==null) {
            for (int i = 0; i < RB.size(); i++) CB.get(i).setSelected(false);
            return;
        }
        for (int i = 0; i < RB.size(); i++) {
            RhombBoundary rb = RhombBoundary.createRhombBoundary(Point.N()-1-2*i,seq);
            CB.get(i).setSelected(rb.valid());
        }
    }

    /**
     * Override.  
     */
    public Dimension getPreferredSize() {
        return new Dimension(Launcher.WIDTH,4*Launcher.WIDTH/(3*(Point.N()-1)));
    }

} // end of class PrototileChecker

/**
 * A class for rendering the entries of a list of edge sequences.  
 */
class EdgeSequenceRenderer<E> extends JLabel implements ListCellRenderer<E> {

    public EdgeSequenceRenderer() {
        setOpaque(true);
        setHorizontalAlignment(LEFT);
        setVerticalAlignment(CENTER);
    }

    /**
     * This method produces a JLabel with text corresponding 
     * to the selected value.  
     * @return A JLabel with text corresponding to the selected value.  
     */
    public Component getListCellRendererComponent(
                                       JList list,
                                       Object value,
                                       int index,
                                       boolean isSelected,
                                       boolean cellHasFocus) {
        //Get the selected index. (The index param isn't
        //always valid, so just use the value.)
        int[] selected = (int[])value;

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        if (selected != null) {
            setText(Arrays.toString(selected));
            setFont(list.getFont());
        } else {
            setText("Please select (or create) an edge sequence.");
            setFont(list.getFont());
        }

        return this;
    }
} // end of class EdgeSequenceRenderer 

/**
 * A class for rendering the entries of a list of Integers.  
 * Renders a String containing the list entry, with appropriate prefix
 * suffix, that describes what the entry represents.  
 */
class IntegerRenderer<E> extends JLabel implements ListCellRenderer<E> {

    /** Prefix and suffix added to the label.  */
    private final String prefix;
    private final String suffix;

    public IntegerRenderer(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        setOpaque(true);
        setHorizontalAlignment(LEFT);
        setVerticalAlignment(CENTER);
    }

    /**
     * This method produces a JLabel with text corresponding 
     * to the selected value, with a prefix and suffix added.  
     * @return A JLabel with text corresponding to the selected value,  
     * containing the String prefix + [Integer] + suffix.  
     */
    public Component getListCellRendererComponent(
                                       JList list,
                                       Object value,
                                       int index,
                                       boolean isSelected,
                                       boolean cellHasFocus) {
        //Get the selected index. (The index param isn't
        //always valid, so just use the value.)
        Integer selected = (Integer)value;

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        if (selected != null) {
            setText(prefix + selected + suffix);
            setFont(list.getFont());
        } else {
            setText("--");
            setFont(list.getFont());
        }

        return this;
    }
} // end of class IntegerRenderer

/**
 * A class for editing the entries of a list of edge sequences.  
 */
class EdgeSequenceEditor extends JTextField implements ComboBoxEditor {

  public EdgeSequenceEditor() {
  }

  public void setItem(Object anObject) {
    if (anObject != null) {
      super.setText(Arrays.toString(((int[])anObject)));
    } else {
      super.setText("Please select (or create) an edge sequence.");
    }
  }

  public Component getEditorComponent() {
    return this;
  }

  public Object getItem() {
    return parseString();
  }

  /** Utility method for recovering the object represented by the String. */
  private int[] parseString() {
    String[] s = getText().replaceAll("[^,-0123456789]","").split(",");
    try {
        int[] output = new int[s.length];
        for (int i = 0; i < s.length; i++) output[i] = Integer.parseInt(s[i]);
        return output;
    } catch (NumberFormatException e) {
        return null;
    }
  }

  public void selectAll() {
    super.selectAll();
  }

  public void addActionListener(ActionListener l) {
    super.addActionListener(l);
  }

  public void removeActionListener(ActionListener l) {
    super.removeActionListener(l);
  }
} // end of class EdgeSequenceEditor

/**
 * THIS IS THE MAIN CLASS.  
 * <br>
 * <br>
 * A window with boxes for selecting the order of symmetry ({@link Point#N()}) and the edge 
 * sequence.  It displays all the prototiles for the given order of symmetry, 
 * each with a checkbox ticked if there exists a tiling of the prototile 
 * with the selected edge sequence.  If all prototiles can be tiled, then 
 * a start button at the bottom is activated, and the user can press it to 
 * open a {@link SubstitutionEditor}.  
 */
public class Launcher extends JFrame {

    /** options for the JComboBoxes. */
    private static final Integer[] SYMMETRIES = new Integer[] {5,7,9,11,13}; // no point in going past 13
    private static final List<int[][]> ALL_EDGE_SEQUENCES;
    private static int[][] test = new int[][] {null,
                                               new int[] {1,-1,0,-2,2,3,-3,0,1,-1,-2,2,0,1,-1,-4,4,3,-3,-2,2,1,-1,0},
                                               new int[] {1,-1,0,3,-3,1,-1,-2,2,0,1,-1,-2,2,0},
                                               new int[] {1,-1,3,-3,0,1,-1,-2,2,0,1,-1,-2,2,0},
                                               new int[] {1,-1,0,1,-1,2,-2,0},
                                               new int[] {-1,1,0},
                                               new int[] {-1,1,0,-1,1,0,2,-2,0},
                                               new int[] {-2,2}
                                              };
    private static final Integer[] MAX_SUBSTITUTIONS = new Integer[] {2,3,4,5};
    public static final int WIDTH = 700;
    public static final int HEIGHT = 300;
    private final PrototileChecker checker;
    private static JComboBox<Integer> N = new JComboBox<Integer>(SYMMETRIES);
    private static final JComboBox<Integer> maxSubstitutions  = new JComboBox<Integer>(MAX_SUBSTITUTIONS);
    public static final int BUTTON_HEIGHT = (int) maxSubstitutions.getPreferredSize().getHeight();
    private JComboBox<int[]> edgeSequence = new JComboBox<int[]>(test);
    private static final EdgeSequenceEditor editor = new EdgeSequenceEditor();
    private JButton go;


    // set the format for the labels in the list of symmetries
    static {
        ALL_EDGE_SEQUENCES      = new ArrayList<int[][]>();
        for (Integer i : SYMMETRIES) {
            int[][] list = FileManager.readEdgeSequenceList(i);
            ALL_EDGE_SEQUENCES.add(list);
        }
        N.setRenderer(new IntegerRenderer<Integer>("","-fold symmetry"));
    }

    public Launcher() throws java.awt.HeadlessException
    {
        // create the layout
        this.setLayout(new GridBagLayout());
        this.setSize(WIDTH, HEIGHT);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
        GridBagConstraints c = new GridBagConstraints();
        // make a final reference to this for passing to actionListeners   
        final Launcher L = this;

        // make and add the symmetry selector
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        N.setSelectedItem((Integer)Point.N());
        N.addActionListener(new AbstractAction("Change N") {
            public void actionPerformed( ActionEvent event ) {
                Point.setN((int)L.N.getSelectedItem());
                L.checker.changeN();
                L.changeComboBox();
                L.checker.changeEdgeSequence((int[])L.edgeSequence.getSelectedItem());
                L.go.setEnabled(L.checker.okay());
                L.validate();
                L.repaint();
            }
        });
        this.add(N, c);

        // make and add the edge sequence selector
        editor.addActionListener(new AbstractAction("Change edge sequence") {
            public void actionPerformed( ActionEvent event ) {
                L.edgeSequence.setSelectedItem(L.editor.getItem());
            }
        });
        edgeSequence = new JComboBox<int[]>(ALL_EDGE_SEQUENCES.get(Arrays.asList(SYMMETRIES).indexOf((Integer)Point.N())));
        edgeSequence.setRenderer(new EdgeSequenceRenderer<int[]>());
        edgeSequence.setEditor(editor);
        edgeSequence.setEditable(true);
        edgeSequence.addActionListener(new AbstractAction("Change edge sequence") {
            public void actionPerformed( ActionEvent event ) {
                L.checker.changeEdgeSequence((int[])L.edgeSequence.getSelectedItem());
                L.go.setEnabled(L.checker.okay());
                L.validate();
                L.repaint();
            }
        });
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.5;
        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 0;
        this.add(edgeSequence, c);

        // make and add the checker
        this.checker = new PrototileChecker(this);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.ipady = checker.getHeight()-(int)checker.getMinimumSize().getHeight();
        c.weightx = 0.0;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy = 1;
        this.add(checker, c);

        // make and add the go button
        this.go = new JButton("Go!");
        go.addActionListener(new AbstractAction("Go!") {
            public void actionPerformed( ActionEvent event ) {
                final int[] path = (int[])L.edgeSequence.getSelectedItem();
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            L.dispose();
                            SubstitutionEditor loaded = SubstitutionEditor.createSubstitutionEditor(path,(int)L.maxSubstitutions.getSelectedItem());
                            RhombDisplay.setEditor(loaded);
                            loaded.setVisible(true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        go.setEnabled(false);
        c.weightx = 0.5;
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 2;
        this.add(go, c);

        // make and add the max substitutions selector
        c.weightx = 0.0;
        c.gridwidth = 1;
        c.gridx = 1;
        c.gridy = 2;
        maxSubstitutions.setSelectedItem((Integer)2);
        maxSubstitutions.setRenderer(new IntegerRenderer<Integer>("Maximum "," substitutions"));
        this.add(maxSubstitutions, c);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.validate();
        this.repaint();
    }

    /**
     *  Change the edge sequence editor/selector that is currently showing 
     *  to reflect the current value of {@link Point#N()}.  
     */
    public void changeComboBox() {
        edgeSequence.removeAllItems();
        for (int[] i : ALL_EDGE_SEQUENCES.get(Arrays.asList(SYMMETRIES).indexOf((Integer)Point.N()))) edgeSequence.addItem(i);
    }


    /**
     *  Start the program!
     */
    public static void main(String[] args) {
        Point.setN(7);
        Launcher L = new Launcher();
    }

} // end of class Launcher

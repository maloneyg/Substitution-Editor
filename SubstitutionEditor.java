import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.AbstractMap;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import math.geom2d.Point2D;
import math.geom2d.polygon.SimplePolygon2D;
import math.geom2d.Box2D;

/**
 * A window containing a number of {@link RhombDisplay}s equal to 
 * ({@link Point#N()} - 1)/2, each of which represents the image under 
 * a substitution rule of one of the rhombic prototiles.  
 * There is a panel underneath containing a {@link PatchDisplay} depicting 
 * a generic patch of a tiling arising from this substitution.  
 * Modifying the rules causes the patch to change in real time.  
 */
public class SubstitutionEditor extends JFrame {

    private JPanel contentPane;
    private List<RhombBoundary> rules;
    private List<SimpleRhomb> rhombs;
    private int substitutions;
    private final int maxSubstitutions;
    private int[] edge;
    private Point[] infl;
    private List<RhombDisplay> RD;
    private PatchDisplay patch;
    /** The width of this window.  */
    public static final int WIDTH = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
    /** The height of this window.  */
    public static final int HEIGHT = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
    /** The horizontal space between the edge of the window and the displays.  */
    public static final int XMARGE = 6;
    /** The horizontal space between adjacent {@link RhombDisplay}s.  */
    public static final int XBUFFER = 10;
    /**
     *  The vertical space between the {@link RhombDisplay}s on top and 
     *  the {@link PatchDisplay} on the bottom.  
     */
    public static final int YBUFFER = 10;
    /** The vertical space between the edge of the window and the displays.  */
    public static final int YMARGE = 6;
    /** stuff associated to the menu */
    private JMenuBar menuBar;
    private JMenu file;
    private JMenu edit;
    private JMenu view;
    private JMenuItem restart;
    private JMenuItem saveImage;
    private JMenuItem save;
    private JMenuItem load;
    private JMenuItem quit;
    private JMenuItem colours;
    private JCheckBox antialiasing;
    private JCheckBox supertiles;
    private ButtonGroup substitutionCount;

    /**
     * Create a new SubstitutionEditor from a saved state.
     * @param fileName The name of the file containing the saved state.  
     * @return The SubstitutionEditor saved in fileName.  
     */
    public static SubstitutionEditor loadSubstitutionEditor(String fileName) {
        SubstitutionEditorSaveState data = FileManager.loadSubstitutionEditor(fileName);
        ColourPalette.setAll(data.colours);
        List<SimpleRhomb> startPatch = standardSeed();
        int maxSubstitutions = (data.maxSubstitutions>0) ? data.maxSubstitutions : 2;
        SubstitutionEditor output = new SubstitutionEditor(startPatch,data.rules,data.edge,maxSubstitutions);
        output.patch.antialiasing = data.antialiasing;
        output.antialiasing.setSelected(output.patch.antialiasing);
        output.patch.supertiles = data.supertiles;
        output.supertiles.setSelected(output.patch.supertiles);
        return output;
    }

    /**
     * Output a standard seed for a substitution: a list consisting of a single big rhomb.  
     * @return A list containing only one SimpleRhomb, which has angle {@link Point#N()}/2 
     * or {@link Point#N()}/2 +1.  
     */
    public static List<SimpleRhomb> standardSeed() {
        Point p = Point.ZERO();
        Point v1 = Point.createPoint(0);
        int even = (Point.N()%4==1) ? Point.N()/2 : Point.N()/2 + 1;
        Point v2 = Point.createPoint(-even);
        int type = (Point.N()-even+1)/2;
        int angle = 0;
        List<SimpleRhomb> output = new ArrayList<SimpleRhomb>();
        output.add(SimpleRhomb.createSimpleRhomb(p, v1, v2, type, angle).rotate(0));
        return output;  
    }

    /**
     * Constructor that takes a list of substitution rules as input.  
     */
    public SubstitutionEditor(List<SimpleRhomb> rhombs, List<RhombBoundary> RB, int[] edge, int maxSubstitutions) { 
        super(Point.N() + "-fold symmetry");
        this.maxSubstitutions = maxSubstitutions;
        this.rhombs = rhombs;
        this.rules = RB;
        this.edge = edge;
        this.infl = Point.inflation(edge);
        setup();
        makeMenu();
    }

    /**
     * Constructor with default maximum of 2 substitutions.  
     */
    public SubstitutionEditor(List<SimpleRhomb> rhombs, List<RhombBoundary> RB, int[] edge) { 
        this(rhombs, RB, edge, 2);
    }

    /**
     * Constructor that takes only the edge sequence and seed as input.  
     */
    public SubstitutionEditor(List<SimpleRhomb> rhombs, int[] edge, int maxSubstitutions) { 
        super(Point.N() + "-fold symmetry");
        this.maxSubstitutions = maxSubstitutions;
        this.rhombs = rhombs;
        this.edge = edge;
        this.infl = Point.inflation(edge);
        rules = new ArrayList<RhombBoundary>();
        for (int i = 0; i < Point.N()/2; i++) {
            RhombBoundary RB = RhombBoundary.createRhombBoundary(Point.N()-2*i-1,edge);
            if (RB.valid()) rules.add(RB);
            else throw new IllegalArgumentException("With the given edge sequence, the rhomb with even angle " + (Point.N()-2*i-1) + " cannot be tiled.");
        }
        setup();
        makeMenu();
    }

    /**
     * Static factory method.  
     */
    public static SubstitutionEditor createSubstitutionEditor(int[] edge, int maxSubstitutions) { 
        return new SubstitutionEditor(standardSeed(),edge,maxSubstitutions);
    }

    /**
     * Static factory method, default maximum number of substitutions (2).  
     */
    public static SubstitutionEditor createSubstitutionEditor(int[] edge) { 
        return new SubstitutionEditor(standardSeed(),edge,2);
    }

    /**
     * Utility function for use in the constructor.  
     */
    private void setup() {

        substitutions = 2;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0, 0, WIDTH, HEIGHT);
        // so that we can get the height of the content pane
        setVisible(true);
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null);

        int paneWidth  = (WIDTH  - 2*XMARGE  - (rules.size()-1)*XBUFFER)/rules.size();
        int paneHeight = 2*(HEIGHT - 2*YBUFFER - YMARGE)/5;

        RD = RhombDisplay.displayList(rules,paneWidth,paneHeight);
        for (int i = 0; i < rules.size(); i++) {
            RD.get(i).setBounds(XMARGE+i*(XBUFFER+paneWidth), YMARGE, paneWidth, paneHeight);
            contentPane.add(RD.get(i));
        }

        int patchHeight = 3*(HEIGHT - 2*YBUFFER - YMARGE)/5;
        patch = new PatchDisplay(rhombs, rules, maxSubstitutions, infl, edge, WIDTH-2*XMARGE, patchHeight-YMARGE);
        patch.setBounds(XMARGE, YMARGE+YBUFFER+paneHeight, WIDTH-2*XMARGE, patchHeight-YMARGE);
        patch.subRhomb(substitutions);
        contentPane.add(patch);

    }

    /**
     * Utility function for use in the constructor.  
     * Creates the menu.  
     */
    private void makeMenu() {
        menuBar = new JMenuBar();
        file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        menuBar.add(file);
        edit = new JMenu("Edit");
        edit.setMnemonic(KeyEvent.VK_E);
        menuBar.add(edit);
        view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        menuBar.add(view);
        substitutionCount = new ButtonGroup();

        // final reference to this for inner functions
        final SubstitutionEditor temp = this;

        restart = new JMenuItem(new AbstractAction("New") {
            public void actionPerformed( ActionEvent event ) {
                switch (savePrompt()) {
                    case    JOptionPane.YES_OPTION:  save(temp);
                                                     break;
                    case     JOptionPane.NO_OPTION:  break;
                    case JOptionPane.CANCEL_OPTION:  return;
                    default:  return;
                }
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            temp.dispose();
                            Launcher L = new Launcher();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        restart.setMnemonic(KeyEvent.VK_N);
        restart.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        file.add(restart);

        saveImage = new JMenuItem(new AbstractAction("Save image") {
            public void actionPerformed( ActionEvent event )
            {
                    FileDialog fDialog = new FileDialog(temp, "Save image", FileDialog.SAVE);
                    fDialog.setDirectory("./images");
                    fDialog.setVisible(true);
                    if (fDialog.getDirectory()==null||fDialog.getFile()==null) return;
                    String path = fDialog.getDirectory() + fDialog.getFile() + ((fDialog.getFile().endsWith(".ps")) ? "" : ".ps");
                    FileManager.postscriptDump(path,temp.patch);
            }
        });
        file.add(saveImage);

        save = new JMenuItem(new AbstractAction("Save") {
            public void actionPerformed( ActionEvent event )
            {
                save(temp);
            }
        });
        save.setMnemonic(KeyEvent.VK_S);
        save.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        file.add(save);

        load = new JMenuItem(new AbstractAction("Open") {
            public void actionPerformed( ActionEvent event ) {
                switch (savePrompt()) {
                    case    JOptionPane.YES_OPTION:  save(temp);
                                                     break;
                    case     JOptionPane.NO_OPTION:  break;
                    case JOptionPane.CANCEL_OPTION:  return;
                    default:  return;
                }
                final SubstitutionEditor loaded = open(temp);
                if (loaded==null) return;
                else {
                    EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            try {
                                temp.dispose();
                                RhombDisplay.setEditor(loaded);
                                loaded.setVisible(true);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
        load.setMnemonic(KeyEvent.VK_O);
        load.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        file.add(load);

        quit = new JMenuItem(new AbstractAction("Quit") {
            public void actionPerformed( ActionEvent event ) {
                switch (savePrompt()) {
                    case    JOptionPane.YES_OPTION:  save(temp);
                                                     break;
                    case     JOptionPane.NO_OPTION:  break;
                    case JOptionPane.CANCEL_OPTION:  return;
                    default:  return;
                }
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        try {
                            temp.dispose();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });
        quit.setMnemonic(KeyEvent.VK_Q);
        quit.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        file.add(quit);

        colours = new JMenuItem(new AbstractAction("Change colours") {
            public void actionPerformed( ActionEvent event )
            {
                final List<Color> oldColours = ColourPalette.copy();
                final JDialog colourDialog = JColorChooser.createDialog(temp,"Colour palette",true,new RhombColorChooser(temp),
                    // the action performed on "OK"
                    new AbstractAction("Accept colours") {
                        public void actionPerformed( ActionEvent event ) {
                        }
                    },
                    // the action performed on "Cancel"
                    new AbstractAction("Cancel colours") {
                        public void actionPerformed( ActionEvent event ) {
                            ColourPalette.set(oldColours);
                            temp.updatePatch();
                            for (RhombDisplay rd : temp.RD) rd.repaint();
                        }
                    }
                );
                colourDialog.setVisible(true);
            }
        });
        edit.add(colours);

        antialiasing = new JCheckBox(new AbstractAction("Antialiasing") {
            public void actionPerformed( ActionEvent event )
            {
                temp.patch.antialiasing = !temp.patch.antialiasing;
                revalidate();
                repaint();
            }
        });
        antialiasing.setSelected(temp.patch.antialiasing);
        view.add(antialiasing);

        supertiles = new JCheckBox(new AbstractAction("Supertiles") {
            public void actionPerformed( ActionEvent event )
            {
                temp.patch.supertiles = !temp.patch.supertiles;
                revalidate();
                repaint();
            }
        });
        supertiles.setSelected(temp.patch.supertiles);
        view.add(supertiles);

        view.addSeparator();

        for (int i = 0; i <= maxSubstitutions; i++) {
            final int I = i;
            JRadioButtonMenuItem button = new JRadioButtonMenuItem(new AbstractAction("Level " + I) {
                public void actionPerformed( ActionEvent event )
                {
                    patch.resetRhomb();
                    substitutions = I;
                    patch.subRhomb(substitutions);
                    contentPane.updateUI();
                }
            });
            button.setSelected(substitutions==I);
            substitutionCount.add(button);
            view.add(button);
        }

        setJMenuBar(menuBar);
        menuBar.setVisible(true);
    }

    /**
     * Utility method for saving.  
     * This dumps the essential data from the editor.  
     * @return {@link SubstitutionEditorSaveState} containing essential fields 
     * needed to reconstruct this.  
     */
    public SubstitutionEditorSaveState dump() {
        return new SubstitutionEditorSaveState(rules,edge,ColourPalette.copy(),patch.antialiasing,patch.supertiles,maxSubstitutions);
    }

    /**
     *  Update the {@link PatchDisplay} in this.  
     *  Calls {@link PatchDisplay#update(int)}.  
     */
    public void updatePatch(){
        patch.update(substitutions);
    }

    /**
     * Return the SimpleRhombs that are showing in the window.  
     */
    public List<SimpleRhomb> getPatch(){
        return patch.getPatch();
    }

    /**
     * Static method for producing a pop-up prompt asking the user 
     * if he or she wants to save before proceeding.  
     * @return One of YES_OPTION, NO_OPTION, or CANCEL_OPTION from 
     * JOptionPane.  
     */
    public static int savePrompt(){
        return JOptionPane.showConfirmDialog(null,"This action will close the current file.  Do you want to save first?","Warning", JOptionPane.YES_NO_CANCEL_OPTION);
    }

    /**
     * Static method for producing a pop-up prompt to save the given 
     * SubstitutionEditor.  
     * @param editor The current editor, which the user might wish 
     * to save.  
     */
    public static void save(SubstitutionEditor editor) {
        FileDialog fDialog = new FileDialog(editor, "Save", FileDialog.SAVE);
        fDialog.setDirectory("./saves");
        fDialog.setVisible(true);
        if (fDialog.getDirectory()==null||fDialog.getFile()==null) return;
        String path = fDialog.getDirectory() + fDialog.getFile() + ((fDialog.getFile().endsWith(".sub")) ? "" : ".sub");
        FileManager.saveSubstitutionEditor(path,editor.dump());
    }

    /**
     * Static method for producing a pop-up prompt to open a saved 
     * SubstitutionEditor.  
     * @param editor The SubstitutionEditor that called this method.  
     * @return The editor that the user chooses to open.  If the user 
     * cancels, this returns null.  
     */
    public static SubstitutionEditor open(SubstitutionEditor editor) {
        FileDialog fDialog = new FileDialog(editor, "Open", FileDialog.LOAD);
        fDialog.setDirectory("./saves");
        fDialog.setVisible(true);
        if (fDialog.getDirectory()==null||fDialog.getFile()==null) return null;
        final String path = fDialog.getDirectory() + fDialog.getFile();
        return loadSubstitutionEditor(path);
    }

    /**
     * Take a screenshot, save it to file.  
     */
    public void SaveAction(){
        try {
            Robot robot = new Robot();
            String format = "jpg";
            String fileName = "PartialScreenshot." + format;

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle captureRect = new Rectangle(106, 485, 1220, 400);
            BufferedImage screenFullImage = robot.createScreenCapture(captureRect);
            ImageIO.write(screenFullImage, format, new File(fileName));

            System.out.println("A partial screenshot saved!");
        } catch (AWTException | IOException ex) {
            System.err.println(ex);
        }
    }
} // end of class SubstitutionEditor

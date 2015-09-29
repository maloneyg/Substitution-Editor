import java.util.Deque;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Map.Entry;
import java.io.*;
import java.lang.Math.*;
import java.util.Map;
import java.util.Stack;
import java.io.PrintWriter;
import java.awt.Color;

import math.geom2d.polygon.SimplePolygon2D;
import math.geom2d.polygon.Polygon2D;
import math.geom2d.Point2D;

import java.util.Arrays;

/*************************************************************************
 * A class for writing, saving, and loading files of various types.  
 *************************************************************************/
public class FileManager {

    private static final String A = "abcdefghijklmnopqrstuvwxyz";

    /**
     * Private constructor.  
     * Don't use this.  
     */
    private FileManager() {
    }

    /**
     * Get the character at position n in the alphabet.  
     * @param n The index in the alphabet at which we want to find a character.  
     * @return The character at index n in the alphabet.  
     */
    public static char getChar(int n) {
        return A.charAt(n);
    }

    /**
     * Write a header for a postscript file to the given file.  
     * @param fileName The name of the file on which to write.  
     * @param box Coordinates of the bounding box for the image, in the 
     * form "xmin ymin xmax ymax"
     */
    public static void postscriptHeader(String fileName,String box) {
        PrintWriter out = null;
        try {
            out = new PrintWriter(fileName);
            // preamble
            out.write("%!PS-Adobe-2.0 EPSF-1.2\n");
            out.write("%%BoundingBox: ");
            out.write(box + "\n");
            out.write("%!PS-Adobe-2.0 EPSF-1.2\n");
            out.write("%! PostScript program\n\n");
            out.write("28.3464 28.3464 scale    % after this coordinates are in cm\n");
            out.write("0.04 setlinewidth\n");
            out.write("1 setlinejoin\n");
            out.write("1 setlinecap\n\n");
            out.write("4 4 translate\n\n");
            out.write("/sc 1 def\n");
            out.write("/unit{sc mul}def\n\n");

            // define sines and cosines
            for (int i = 1; i < Point.N()/2+1; i++) {
                out.write("/c" + i + " " + (180*i) + " " + Point.N() + " div cos def\n");
            }
            out.write("\n");
            for (int i = 1; i < Point.N()/2+1; i++) {
                out.write("/s" + i + " " + (180*i) + " " + Point.N() + " div sin def\n");
            }
            out.write("\n");

            // define linear map down to 2-d space
            out.write("/" + Point.order() + "orth{\n  ");
            char c1 = 'z';
            for (int i = 0; i < Point.N()-1; i++) {
                out.write("/" + c1 + " exch def ");
                c1--;
            }
            out.write("\n  ");
            c1 = getChar(27 - Point.N());
            char c2 = getChar(28 - Point.N());
            out.write(c1 + " " + c2 + " c1 mul add ");
            for (int i = 1; i < Point.N()/2; i++) {
                c1 = getChar(28 - Point.N() + i);
                c2 = getChar(26 - i);
                out.write(c1 + " " + c2 + " sub c" + (i+1) + " mul add ");
            }
            out.write("unit\n  ");
            c2 = getChar(28 - Point.N());
            out.write("  " + c2 + " s1 mul     ");
            for (int i = 1; i < Point.N()/2; i++) {
                c1 = getChar(28 - Point.N() + i);
                c2 = getChar(26 - i);
                out.write(c1 + " " + c2 + " add s" + (i+1) + " mul add ");
            }
            out.write("unit\n}def\n\n");
            out.write("% instructions for drawing tiles\n"); // comment, with metacomment

            // write instructions for drawing each tile
            for (int i = 0; i < Point.N()/2; i++) {
                out.write("/t" + (i+1) + "{\n   ");
                Color col = ColourPalette.colour(i);
                out.write((((float)col.getRed())/255.0) + " ");
                out.write((((float)col.getGreen())/255.0) + " ");
                out.write((((float)col.getBlue())/255.0) + " ");
                out.write("setrgbcolor\n     0 unit 0 unit moveto\n");
                for (int k = 0; k < Point.N()-1; k++) {
                    if (k==0) out.write("     1 ");
                    else if (k==2*i+1) out.write(" 0 ");
                    else out.write("0 ");
                }
                out.write(Point.order() + "orth lineto\n");
                for (int k = 0; k < Point.N()-1; k++) {
                    if (k==0) out.write("     1 ");
                    else if (k==2*i+1) out.write("-1 ");
                    else out.write("0 ");
                }
                out.write(Point.order() + "orth lineto\n");
                for (int k = 0; k < Point.N()-1; k++) {
                    if (k==0) out.write("     0 ");
                    else if (k==2*i+1) out.write("-1 ");
                    else out.write("0 ");
                }
                out.write(Point.order() + "orth lineto\n");
                out.write("     fill\n   0 0 0 setrgbcolor\n   newpath\n");
                out.write("     0 unit 0 unit moveto\n");
                for (int k = 0; k < Point.N()-1; k++) {
                    if (k==0) out.write("     1 ");
                    else if (k==2*i+1) out.write(" 0 ");
                    else out.write("0 ");
                }
                out.write(Point.order() + "orth lineto\n");
                for (int k = 0; k < Point.N()-1; k++) {
                    if (k==0) out.write("     1 ");
                    else if (k==2*i+1) out.write("-1 ");
                    else out.write("0 ");
                }
                out.write(Point.order() + "orth lineto\n");
                for (int k = 0; k < Point.N()-1; k++) {
                    if (k==0) out.write("     0 ");
                    else if (k==2*i+1) out.write("-1 ");
                    else out.write("0 ");
                }
                out.write(Point.order() + "orth lineto\n");
                out.write("     closepath\n   stroke\n}def\n\n");
            }
        } catch ( Exception e ) {
            System.out.println(e);
        } finally {
            try {
                if ( out != null)
                out.close( );
            } catch ( Exception e ) {
            }
        }
    } // end of method postscriptHeader

    /**
     * Write a patch to a postscript file.  
     * @param fileName The name of the file on which to write.  
     * @param patch The PatchDisplay to be depicted in the file.  
     */
    public static void postscriptDump(String fileName, PatchDisplay patch) {
        postscriptHeader(fileName,patch.boundingBox());
        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileWriter(fileName, true));
            if (patch.supertiles) {
                out.write("% instructions for drawing supertiles\n");
                for (int i = 0; i < Point.N()/2; i++) out.write(patch.supertile(i));
            }
            if (Math.abs(patch.getRotation())>0) out.write((patch.getRotation()*180/Math.PI) + " rotate\n\n");
            out.write("% a list of all tiles\n");
            for (SimpleRhomb r : patch.getPatch()) out.write(r.postscriptString()+"\n");
            if (patch.supertiles) {
                out.write("\n% a list of all supertiles\n");
                out.write("0.2 setlinewidth\n");
                for (SimpleRhomb s : patch.getSupertiles()) out.write(patch.supertilePostscriptString(s)+"\n");
            }
            out.write("showpage");
        } catch ( Exception e ) {
            System.out.println(e);
        } finally {
            try {
                if ( out != null)
                out.close( );
            } catch ( Exception e ) {
            }
        }
    } // end of method postscriptDump

    /**
     * Load a RhombBoundary from the file with the given name.  
     * @param filename The name of the file from which to load.  
     */
    public static RhombBoundary loadRhombBoundary(String filename) {
        if ( ! new File(filename).isFile() ) {
            System.out.println(filename + " not found!");
            return null;
        }
        RhombBoundary output = null;
        try {
            FileInputStream fileIn = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            output = ((RhombBoundary)in.readObject());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return output;
    }

    /**
     * Save a RhombBoundary to the file with the given name.  
     * @param path The name of the file to which to save.  
     * @param r The RhombBoundary that we wish to save.  
     */
    public static void saveRhombBoundary(String path, RhombBoundary r) {
        try {
            FileOutputStream fileOut = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(r);
            out.close();
            fileOut.close();
        } catch (Exception e) {
            System.out.println("\nError while saving rhomb boundary.");
            e.printStackTrace();
        }
    }

    /**
     * Load a {@link SubstitutionEditorSaveState} from 
     * the file with the given name.  
     * @param filename The name of the file from which to load.  
     */
    public static SubstitutionEditorSaveState loadSubstitutionEditor(String filename) {
        if ( ! new File(filename).isFile() ) {
            System.out.println(filename + " not found!");
            return null;
        }
        SubstitutionEditorSaveState output = null;
        try {
            FileInputStream fileIn = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            output = (SubstitutionEditorSaveState)in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        return output;
    }

    /**
     * Save a {@link SubstitutionEditorSaveState} to the 
     * file with the given name.  
     * @param filename The name of the file to which to save.  
     * @param state The {@link SubstitutionEditor} to be saved.  
     */
    public static void saveSubstitutionEditor(String filename,SubstitutionEditorSaveState state) {
        try {
            FileOutputStream fileOut = new FileOutputStream(filename);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(state);
            out.close();
            fileOut.close();
        } catch (Exception e) {
            System.out.println("\nError while saving rhomb boundary.");
            e.printStackTrace();
        }
    }

    /**
     *  Read an edge sequence list from a file in ./edges
     *  @param n The value of {@link Point#N()} for which to read 
     *  edge sequences.  
     *  @return The contents of edges$i.txt, formatted and converted 
     *  to int[][].  
     */
    public static int[][] readEdgeSequenceList(int n) {
        String filename = "./edges/edge" + n + ".txt";
        if ( ! new File(filename).isFile() ) {
            System.out.println(filename + " not found!");
            return new int[][] {null};
        }
        List<int[]> output = new ArrayList<int[]>();
        output.add(null);
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                String[] s = line.replaceAll("[^,-0123456789]","").split(",");
                if (s.length > 0) {
                    try {
                        int[] answer = new int[s.length];
                        for (int i = 0; i < s.length; i++) answer[i] = Integer.parseInt(s[i]);
                        output.add(answer);
                    } catch (NumberFormatException e) {
                        // don't add anything to output
                    }
                }
            }
        } catch (Exception e) {
            return new int[][] {null};
        }
        int[][] realOutput = new int[output.size()][];
        for (int i = 0; i < output.size(); i++) realOutput[i] = output.get(i);
        return realOutput;
    }

    /**
     * For testing.  
     */
    public static void main (String[] args) {
    }

} // end of class FileManager

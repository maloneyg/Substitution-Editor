import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/*
 * A class for holding the colour palette currently in use, and the default  
 * colour palettes too.  
 */
public class ColourPalette {

    private static List<Color> colours;
    private static final List<Color> FIVE;
    private static final List<Color> SEVEN;
    private static final List<Color> NINE;
    private static final List<Color> ELEVEN;
    private static final List<Color> ELEVEN2;
    private static final List<Color> PASTELS;
    
    static{
        // initialize colours for fivefold symmetry
        FIVE = new ArrayList<Color>();
        FIVE.add(new Color(200,171,101));
        FIVE.add(new Color(246,154,152));
        // initialize colours for sevenfold symmetry
        SEVEN = new ArrayList<Color>();
        SEVEN.add(new Color(153,0,51));
        SEVEN.add(new Color(255,255,0));
        SEVEN.add(new Color(51,102,153));
        // initialize colours for ninefold symmetry
        NINE = new ArrayList<Color>();
        NINE.add(new Color(239,101,85));
        NINE.add(new Color(37,62,102));
        NINE.add(new Color(194,157,78));
        NINE.add(new Color(227,223,215));
        // initialize colours for elevenfold symmetry
        ELEVEN = new ArrayList<Color>();
        ELEVEN.add(new Color(183,198,139));
        ELEVEN.add(new Color(244,240,203));
        ELEVEN.add(new Color(222,210,158));
        ELEVEN.add(new Color(179,165,128));
        ELEVEN.add(new Color(104,86,66));
        // initialize colours for elevenfold symmetry
        ELEVEN2 = new ArrayList<Color>();
        ELEVEN2.add(new Color(255,114,96));
        ELEVEN2.add(new Color(18,151,147));
        ELEVEN2.add(new Color(80,80,80));
        ELEVEN2.add(new Color(255,245,195));
        ELEVEN2.add(new Color(155,215,213));
        // initialize the pastel colour palette
        PASTELS = new ArrayList<Color>();
        PASTELS.add(new Color(1.0f,0.1f,0.1f,0.25f));
        PASTELS.add(new Color(0.1f,1.0f,0.1f,0.25f));
        PASTELS.add(new Color(0.1f,0.1f,1.0f,0.25f));
        PASTELS.add(new Color(0.1f,1.0f,1.0f,0.25f));
        PASTELS.add(new Color(1.0f,0.1f,1.0f,0.25f));
        PASTELS.add(new Color(1.0f,1.0f,0.1f,0.25f));
        PASTELS.add(new Color(1.0f,0.5f,0.3f,0.25f));
        PASTELS.add(new Color(0.6f,0.2f,0.1f,0.25f));
        colours = SEVEN;
    }

    /**
     * Change to the default colours for N-fold symmetry.  
     * @param N The new order of symmetry.  Should agree with {@link Point#N}.  
     */
    public static void setN(int N) {
        if (N<5) throw new IllegalArgumentException("Trying to set N = " + N + ", which is too low (N >= 5 is required).");
        switch (N) {
            case  5:  colours = FIVE;
                      break;
            case  7:  colours = SEVEN;
                      break;
            case  9:  colours = NINE;
                      break;
            case 11:  colours = ELEVEN;
                      break;
            default:  colours = PASTELS;
                      break;
        }
    }

    /**
     * Get the i-th colour in the list.  
     * @param i The index of the colour we want to get.  
     * @return The colour in position i in the current colour list.  
     */
    public static Color colour(int i) {
        return colours.get(i);
    }

    /**
     * Change the i-th colour in the list.  
     * @param i The index of the colour we want to change.  
     * @param c The new colour that we want to put at index i.  
     */
    public static void changeColour(int i, Color c) {
        colours.set(i,c);
    }

    /**
     * Copy the current colour palette.  
     * @return A copy of the current colour palette.  
     */
    public static List<Color> copy() {
        List<Color> output = new ArrayList<>();
        for (Color c : colours) output.add(c);
        return output;
    }

    /**
     * Set the current colour palette.  
     * @param palette The new colour palette.  
     */
    public static void setAll(List<Color> palette) {
        colours = palette;
    }

    /**
     * Set the colour palette to (a deep copy of) the given one.  
     * @param C The value to which we set the colour palette.  
     */
    public static void set(List<Color> C) {
        colours = new ArrayList<>();
        for (Color c : C) colours.add(c);
    }

} // end of class ColourPalette

import java.awt.event.*;
import java.awt.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.AbstractMap;
import java.awt.Color;
import java.util.List;
import java.io.Serializable;

/**
 * A class containing all of the essential data for saving a copy 
 * of a {@link SubstitutionEditor}.  
 */
public class SubstitutionEditorSaveState implements Serializable {

    /** For serialization. */
    public static final long serialVersionUID = 7532L;

    public final List<RhombBoundary> rules;
    public final int[] edge;
    public final List<Color> colours;
    public final boolean antialiasing;
    public final boolean supertiles;
    public final int maxSubstitutions;

    public SubstitutionEditorSaveState(List<RhombBoundary> rules,int[] edge,List<Color> colours,boolean antialiasing, boolean supertiles, int maxSubstitutions) {
        this.rules = rules;
        this.edge = edge;
        this.colours = colours;
        this.antialiasing = antialiasing;
        this.supertiles = supertiles;
        this.maxSubstitutions = maxSubstitutions;
    }

} // end of class SubstitutionEditorSaveState

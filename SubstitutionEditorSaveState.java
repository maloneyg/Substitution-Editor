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

    /**
     *  A List of patches representing the substituted images of all the 
     *  supertiles of the saved SubstitutionEditor.  
     */
    public final List<RhombBoundary> rules;
    /**
     *  The edge sequence of the substitution.  
     */
    public final int[] edge;
    /**
     *  The colour scheme to which {@link ColourPalette} was set when the 
     *  SubstitutionEditor was saved.  
     */
    public final List<Color> colours;
    /**
     *  A variable telling us if the {@link PatchDisplay} of the SubstitutionEditor 
     *  was set to use antialiasing to draw itself.  
     */
    public final boolean antialiasing;
    /**
     *  A variable telling us if the {@link PatchDisplay} of the SubstitutionEditor 
     *  was set to draw supertile outlines.  
     */
    public final boolean supertiles;
    /**
     *  The maximum number of substitutions that were allowed with this 
     *  SubstitutionEditor.  
     */
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

package jif.types.hierarchy;

import java.util.Map;
import java.util.Set;

import jif.types.Solver;
import jif.types.VarMap;
import jif.types.label.Label;
import jif.types.principal.Principal;

public interface LabelEnv
{
    // adds "p1 acts for p2" into the principal hierarchy
    void addActsFor(Principal p1, Principal p2);
    
    // adds the assertion "L1 <= L2"
    void addAssertionLE(Label L1, Label L2);
    
    // returns true if "L1 <= L2"
    boolean leq(Label L1, Label L2);

    // returns true if "L1 <= L2"
    boolean leq(Label L1, Label L2, SearchState state);

    PrincipalHierarchy ph();

    // make a copy of this environment
    LabelEnv copy(); 

    /**
     * Returns a Map of Strings to List[String]s which is the descriptions of any 
     * components that appear in the environment. This map is used for verbose 
     * output to the user, to help explain the meaning of constraints and 
     * labels.
     * 
     * Seen components is a Set of Labels whose definitions will not be 
     * displayed.
     */
    Map definitions(VarMap bounds, Set seenComponents);

    /**
     * Is this enviornment empty, or does is contain some constraints?
     */
    boolean isEmpty();
    
    /**
     * Do any of the assertions in this label environment contain variables? 
     */
    boolean hasVariables();
    
    /**
     * Set the solver used for this Label Environment. When necessary, the
     * label environment will use the variable bounds of label variables
     * when determining if constraints are satisfied.
     */
    void setSolver(Solver solver);
    
    /**
     * Encapsulates the solvers search state. 
     */
    public interface SearchState { }
}

package jif.types;

import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.types.SemanticException;

/**
 * The class is a simple Label visitor. It can be used to substitute and check
 * labels and principals. All labels and principals implement a method
 * <code>subst(LabelSubstitution)</code>. In addition, dynamic labels and 
 * principals check whether <code>recurseIntoLabelOf()</code> returns
 * <code>true</code> before recursing into the labelOf components. 
 * 
 * @see jif.types.label.Label#subst(LabelSubstitution)
 */
public abstract class LabelSubstitution {
    public Label substLabel(Label L) throws SemanticException {
        return L;
    }
    public Principal substPrincipal(Principal p) throws SemanticException {
        return p;
    }
    
    /**
     * Shoudl dynamic labels and principals recurse into labelOf components?
     */
    public boolean recurseIntoLabelOf() { return true; }
}

package jif.types.label;

import java.util.LinkedHashSet;
import java.util.Set;

import jif.types.LabelSubstitution;
import jif.types.Param;
import jif.types.principal.Principal;
import jif.types.principal.VarPrincipal;
import polyglot.types.SemanticException;

/**
 * This class is used to implement
 * {@link Label#variables() Label.variables()}. It constructs a set of
 * <code>Variable</code>s.
 */
public class VariableGatherer extends LabelSubstitution {
    public final Set<Param> variables = new LinkedHashSet<Param>();

    /**
     * @throws SemanticException  
     */
    @Override
    public Label substLabel(Label L) throws SemanticException {
        if (L instanceof VarLabel) {
            variables.add(L);
        }
        return L;
    }
    
    /**
     * @throws SemanticException  
     */
    @Override
    public Principal substPrincipal(Principal p) throws SemanticException {
        if (p instanceof VarPrincipal) {
            variables.add(p);
        }
        return p;
    }

}
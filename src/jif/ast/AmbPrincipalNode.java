package jif.ast;

import polyglot.ast.*;

/** An ambiguous principal node. 
 */
public interface AmbPrincipalNode extends PrincipalNode, Ambiguous {
    /** Gets the name. */
    String name();
}

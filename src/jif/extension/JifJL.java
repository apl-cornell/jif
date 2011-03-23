package jif.extension;

import java.util.Set;

import polyglot.ast.Node;
import polyglot.types.TypeSystem;
import polyglot.util.SubtypeSet;

public interface JifJL {
    /**
     * The exceptions that will be treated as fatal at this node.
     */
    @SuppressWarnings("unchecked")
    Set fatalExceptions();

    /**
     * Set which exceptions will be treated as fatal at this node.
     */
    void fatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions);
}

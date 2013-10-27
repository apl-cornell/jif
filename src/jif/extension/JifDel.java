package jif.extension;

import java.util.Set;

import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.SubtypeSet;

public interface JifDel {
    /**
     * The exceptions that will be treated as fatal at this node.
     */
    Set<Type> fatalExceptions();

    /**
     * Set which exceptions will be treated as fatal at this node.
     */
    void setFatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions);
}

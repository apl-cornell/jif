package jif.extension;

import java.util.Set;

import polyglot.types.ClassType;
import polyglot.types.TypeSystem;
import polyglot.util.SubtypeSet;

public interface JifJL {
    /**
     * The exceptions that will be treated as fatal at this node.
     */
    @SuppressWarnings("unchecked")
    Set<ClassType> fatalExceptions();

    /**
     * Set which exceptions will be treated as fatal at this node.
     */
    void setFatalExceptions(TypeSystem ts, SubtypeSet fatalExceptions);
}

package jif.types;

import polyglot.ext.jl.types.LazyClassInitializer_c;
import polyglot.types.ParsedClassType;
import polyglot.types.TypeSystem;

/**
 * A LazyClassInitializer is responsible for initializing members of
 * a class after it has been created.  Members are initialized lazily
 * to correctly handle cyclic dependencies between classes.
 */
public class JifLazyClassInitializer_c extends LazyClassInitializer_c
{
    public JifLazyClassInitializer_c(TypeSystem ts) {
        super(ts);
    }

    /** Override to prevent the "class" field from being added to
      * every class.
      */
    public void initFields(ParsedClassType ct) {
    }
}

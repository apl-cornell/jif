package jif.types;

import polyglot.types.*;
import polyglot.ext.jl.types.*;
import polyglot.types.Package;
import polyglot.util.*;
import polyglot.frontend.Job;
import java.io.*;
import java.util.*;

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

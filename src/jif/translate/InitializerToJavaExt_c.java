package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class InitializerToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Initializer n = (Initializer) node();
        n = (Initializer) super.toJava(rw);
        n = n.initializerInstance(null);
        return n;
    }
}

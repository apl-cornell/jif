package jif.translate;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class CannotToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Node n = node();
        throw new InternalCompilerError(n.position(),
                                        "Cannot translate " + n + " to Java.");
    }
}

package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class CanonicalPrincipalNodeToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        CanonicalPrincipalNode n = (CanonicalPrincipalNode) node();
        return rw.principalToJava(n.principal());
    }
}

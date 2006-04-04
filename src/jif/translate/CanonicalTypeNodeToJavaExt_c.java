package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.types.Package;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;
import polyglot.util.*;

public class CanonicalTypeNodeToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        CanonicalTypeNode n = (CanonicalTypeNode) node();
        return rw.typeToJava(n.type(), n.position());
    }
}

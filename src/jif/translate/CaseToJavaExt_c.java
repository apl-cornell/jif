package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class CaseToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Case n = (Case) node();
        long value = n.value();
        n = (Case) super.toJava(rw);
        n = n.value(value);
        return n;
    }
}

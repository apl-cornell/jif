package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;

public class NewLabelToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        NewLabel n = (NewLabel) node();
        return rw.bypass(n.label());
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        NewLabel n = (NewLabel) node();
        return n.visitChild(n.label(), rw);
    }
}

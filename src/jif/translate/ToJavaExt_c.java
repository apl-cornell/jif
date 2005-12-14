package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import jif.visit.*;
import polyglot.visit.*;

public class ToJavaExt_c extends Ext_c implements ToJavaExt {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        return rw;
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Node n = node();
        return n.del(null);
    }
}

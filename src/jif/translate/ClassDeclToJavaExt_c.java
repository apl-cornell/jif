package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;
import java.util.*;

public class ClassDeclToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        // Bypass params and authority.
        JifClassDecl n = (JifClassDecl) node();

        rw.currentClass(n.type());

        return rw.bypass(n.params()).bypass(n.authority());
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        ClassDecl n = (ClassDecl) node();
        n = rw.java_nf().ClassDecl(n.position(), n.flags(), n.name(),
                                   n.superClass(), n.interfaces(), n.body());
        rw.currentClass(null);
        return n;
    }
}

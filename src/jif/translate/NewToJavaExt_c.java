package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;
import polyglot.util.*;

import java.util.*;

public class NewToJavaExt_c extends ExprToJavaExt_c {
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        New n = (New) node();

        if (! rw.jif_ts().isJifClass(n.type())) {
            // Rewrite creation of Jif classes only.
            n = rw.java_nf().New(n.position(), n.qualifier(), n.objectType(),
                                n.arguments(), n.body());
            return n;
        }

        ConstructorInstance ci = n.constructorInstance();
        ClassType ct = ci.container().toClass();
        String name = (ct.fullName() + ".").replace('.', '$');

        return rw.qq().parseExpr("new %T().%s(%LE)",
                                 n.objectType(), name, n.arguments());
    }
}

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

        List paramargs = new ArrayList();
        if (ct instanceof JifSubstType) {
            // add all the actual param expressions to args
            JifSubstType t = (JifSubstType)ct;
            JifSubst subst = (JifSubst)t.subst();
            JifPolyType base = (JifPolyType)t.base();
            for (Iterator iter = base.params().iterator(); iter.hasNext(); ) {
                ParamInstance pi = (ParamInstance)iter.next();
                paramargs.add(rw.paramToJava(subst.get(pi)));
            }
        }

        return rw.qq().parseExpr("new %T(%LE).%s(%LE)",
                                 n.objectType(), paramargs, name, n.arguments());
    }
}

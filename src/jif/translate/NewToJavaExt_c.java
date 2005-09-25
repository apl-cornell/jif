package jif.translate;

import java.util.*;

import jif.types.*;
import polyglot.ast.Expr;
import polyglot.ast.New;
import polyglot.types.*;
import polyglot.visit.NodeVisitor;

public class NewToJavaExt_c extends ExprToJavaExt_c {
    private Type objectType;

    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        New n = (New)this.node();
        this.objectType = n.objectType().type();
        return super.toJavaEnter(rw);
    }

    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        New n = (New) node();
        ConstructorInstance ci = n.constructorInstance();
        ClassType ct = objectType.toClass();

        if (! rw.jif_ts().isParamsRuntimeRep(ct) || (ct instanceof JifSubstType && !rw.jif_ts().isParamsRuntimeRep(((JifSubstType)ct).base()))) {
            // only rewrite creation of classes where params are runtime represented.
            n = rw.java_nf().New(n.position(), n.qualifier(), n.objectType(),
                                n.arguments(), n.body());
            return n;
        }

        String name = (ct.fullName() + ".").replace('.', '$');

        List paramargs = new ArrayList();
        
        if (ct instanceof JifSubstType && rw.jif_ts().isParamsRuntimeRep(((JifSubstType)ct).base())) {
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

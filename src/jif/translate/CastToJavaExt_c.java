package jif.translate;

import java.util.ArrayList;
import java.util.List;

import jif.extension.JifCastDel;
import jif.types.JifPolyType;
import jif.types.JifSubst;
import jif.types.JifSubstType;
import jif.types.ParamInstance;
import polyglot.ast.Cast;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.visit.NodeVisitor;

public class CastToJavaExt_c extends ToJavaExt_c {
    protected Type castType;

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        Cast c = (Cast)this.node();
        this.castType = c.castType().type();
        return super.toJavaEnter(rw);
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Cast c = (Cast)this.node();

        if (!((JifCastDel)c.del()).isToSubstJifClass()) {
            return rw.java_nf().Cast(c.position(), c.castType(), c.expr());

        }

        List<Expr> args = new ArrayList<Expr>();

        // add all the actual param expressions to args
        JifSubstType t = (JifSubstType)this.castType;
        JifSubst subst = (JifSubst)t.subst();
        JifPolyType base = (JifPolyType)t.base();
        for (ParamInstance pi : base.params()) {
            args.add(rw.paramToJava(subst.get(pi)));
        }

        // add the actual expression being cast.
        args.add(c.expr());

        JifSubstType jst = (JifSubstType)castType;
        String jifImplClass = jst.fullName();
        if (jst.flags().isInterface()) {
            jifImplClass = ClassDeclToJavaExt_c.interfaceClassImplName(jifImplClass);
        }
        return rw.qq().parseExpr(jifImplClass + ".%s(%LE)", ClassDeclToJavaExt_c.castMethodName(jst), args);
    }
}

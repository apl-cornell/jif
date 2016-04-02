package jif.translate;

import java.util.ArrayList;
import java.util.List;

import jif.types.JifPolyType;
import jif.types.JifSubst;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import jif.types.ParamInstance;
import polyglot.ast.Expr;
import polyglot.ast.Instanceof;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class InstanceOfToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Type compareType;

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        Instanceof io = (Instanceof) this.node();
        this.compareType = io.compareType().type();
        return super.toJavaEnter(rw);
    }

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Instanceof io = (Instanceof) this.node();
        JifTypeSystem ts = (JifTypeSystem) rw.typeSystem();

        if (!ts.needsDynamicTypeMethods(compareType)) {
            return rw.java_nf().Instanceof(io.position(), io.expr(),
                    io.compareType());
        }

        List<Expr> args = new ArrayList<Expr>();

        // add all the actual param expressions to args
        JifSubstType t = (JifSubstType) compareType;
        JifSubst subst = (JifSubst) t.subst();
        JifPolyType base = (JifPolyType) t.base();
        for (ParamInstance pi : base.params()) {
            args.add(rw.paramToJava(subst.get(pi)));
        }

        // add the actual expression being cast.
        args.add(io.expr());

        String jifImplClass = ((JifSubstType) compareType).fullName();
        if (((JifSubstType) compareType).flags().isInterface()) {
            jifImplClass =
                    ClassDeclToJavaExt_c.interfaceClassImplName(jifImplClass);
        }
        return rw.qq()
                .parseExpr(jifImplClass + "."
                        + ClassDeclToJavaExt_c.INSTANCEOF_METHOD_NAME + "(%LE)",
                (Object) args);
    }
}

package jif.translate;

import java.util.ArrayList;
import java.util.List;

import jif.types.JifPolyType;
import jif.types.JifSubst;
import jif.types.JifSubstType;
import jif.types.ParamInstance;
import jif.types.TypeParam;
import polyglot.ast.Expr;
import polyglot.ast.New;
import polyglot.ast.Special;
import polyglot.ast.TypeNode;
import polyglot.ext.jl5.ast.JL5NodeFactory;
import polyglot.types.ClassType;
import polyglot.types.Named;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.NodeVisitor;

public class NewToJavaExt_c extends ExprToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Type objectType;

    @Override
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw)
            throws SemanticException {
        New n = (New) this.node();
        this.objectType = n.objectType().type();
        return super.toJavaEnter(rw);
    }

    @Override
    public Expr exprToJava(JifToJavaRewriter rw) throws SemanticException {
        New n = (New) node();
        JL5NodeFactory nf = (JL5NodeFactory) rw.java_nf();

        // If the qualifier is "null{amb}.this", replace it with null. Gross.        
        if (n.qualifier() instanceof Special) {
            Special qualifier = (Special) n.qualifier();
            if (qualifier.kind() == Special.THIS
                    && qualifier.qualifier() != null
                    && qualifier.qualifier().name().equals("null")) {
                n = n.qualifier(null);
            }
        }

        ClassType ct = objectType.toClass();

        if (!rw.jif_ts().isParamsRuntimeRep(ct)
                || (ct instanceof JifSubstType && !rw.jif_ts()
                        .isParamsRuntimeRep(((JifSubstType) ct).base()))) {
            // only rewrite creation of classes where params are runtime represented.
            n =
                    nf.New(n.position(), n.qualifier(), n.objectType(),
                            n.arguments(), n.body());
            return n;
        }

        List<Expr> paramargs = new ArrayList<Expr>();

        List<TypeNode> args = new ArrayList<TypeNode>();

        if (ct instanceof JifSubstType
                && rw.jif_ts().isParamsRuntimeRep(((JifSubstType) ct).base())) {
            // add all the actual param expressions to args
            JifSubstType t = (JifSubstType) ct;
            JifSubst subst = (JifSubst) t.subst();
            JifPolyType base = (JifPolyType) t.base();
            for (ParamInstance pi : base.params()) {
                if (!pi.isType()) {
                    paramargs.add(rw.paramToJava(subst.get(pi)));
                } else {
                    TypeParam tp = (TypeParam) subst.get(pi);
                    Named namedType = (Named) tp.type();
                    args.add(nf.AmbTypeNode(pi.position(),
                            nf.Id(pi.position(), namedType.name())));
                }
            }
        }

        TypeNode newType = n.objectType();

        if (args.size() > 0) {
            newType =
                    nf.AmbTypeInstantiation(n.objectType().position(),
                            n.objectType(), args);
        }

        // use the appropriate string for the constructor invocation.
        if (!rw.jif_ts().isSignature(ct)) {
            String name = ClassDeclToJavaExt_c.constructorTranslatedName(ct);
            New newexp =
                    nf.New(n.position(), n.qualifier(), newType, paramargs,
                            n.body());
            return rw.qq().parseExpr("%E.%s(%LE)", newexp, name, n.arguments());
        } else {
            // ct represents params at runtime, but is a Java class with a
            // Jif signature.
            List<Expr> allArgs =
                    new ArrayList<Expr>(paramargs.size() + n.arguments().size());
            allArgs.addAll(paramargs);
            allArgs.addAll(n.arguments());
            return nf.New(Position.compilerGenerated(), n.qualifier(), newType,
                    allArgs, n.body());
        }
    }
}

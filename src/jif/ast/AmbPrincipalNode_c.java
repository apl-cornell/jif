package jif.ast;

import java.util.List;

import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import jif.types.JifVarInstance;
import jif.types.ParamInstance;
import jif.types.PrincipalInstance;
import jif.types.SemanticDetailedException;
import jif.types.label.AccessPath;
import jif.types.principal.ExternalPrincipal;
import jif.types.principal.Principal;
import jif.visit.JifTypeChecker;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.Field;
import polyglot.ast.Id;
import polyglot.ast.Node;
import polyglot.ast.Term;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
import polyglot.types.Context;
import polyglot.types.ParsedClassType;
import polyglot.types.SemanticException;
import polyglot.types.VarInstance;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.CFGBuilder;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>AmbPrincipalNode</code> interface.
 */
public class AmbPrincipalNode_c extends PrincipalNode_c
        implements AmbPrincipalNode {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Expr expr;
    protected Id name;

//    @Deprecated
    public AmbPrincipalNode_c(Position pos, Expr expr) {
        this(pos, expr, null);
    }

    public AmbPrincipalNode_c(Position pos, Expr expr, Ext ext) {
        super(pos, ext);
        this.expr = expr;
        this.name = null;
    }

//    @Deprecated
    public AmbPrincipalNode_c(Position pos, Id name) {
        this(pos, name, null);
    }

    public AmbPrincipalNode_c(Position pos, Id name, Ext ext) {
        super(pos, ext);
        this.expr = null;
        this.name = name;
    }

    protected <N extends AmbPrincipalNode_c> N expr(N n, Expr expr) {
        if (n.expr == expr) return n;
        n = copyIfNeeded(n);
        n.expr = expr;
        return n;
    }

    protected <N extends AmbPrincipalNode_c> N id(N n, Id name) {
        if (n.name == name) return n;
        n = copyIfNeeded(n);
        n.name = name;
        return n;
    }

    @Override
    public boolean isDisambiguated() {
        return false;
    }

    @Override
    public String toString() {
        if (expr != null) return expr + "{amb}";
        return name + "{amb}";
    }

    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (name != null) {
            return disambiguateName(ar, name);
        }

        // must be the case that name == null and expr != null
        if (!ar.isASTDisambiguated(expr)) {
            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();

        // run the typechecker over expr.
        TypeChecker tc = new JifTypeChecker(ar.job(), ts, nf, true);
        tc = (TypeChecker) tc.context(ar.context());
        Expr expr = (Expr) this.expr.visit(tc);

        if (!expr.isTypeChecked()) {
            if (expr instanceof Field) {
                Field f = (Field) expr;
                if (ts.unlabel(f.target().type()) instanceof JifClassType) {
                    // disambiguate the class of the receiver of the field,
                    // so that type checking will eventually go through.
                    JifClassType jct =
                            (JifClassType) ts.unlabel(f.target().type());
                    ParsedClassType pct = null;
                    if (jct instanceof ParsedClassType)
                        pct = (ParsedClassType) jct;
                    //XXX: SubstTypes are not subclasses of ParsedClassTypes!
                    else if (jct instanceof JifSubstType) {
                        JifSubstType jst = (JifSubstType) jct;
                        if (jst.base() instanceof ParsedClassType)
                            pct = (ParsedClassType) jst.base();
                    }
                    if (pct != null) {
                        Scheduler sched = ar.job().extensionInfo().scheduler();
                        Goal g = sched.Disambiguated(pct.job());
                        throw new MissingDependencyException(g);
                    }
                    //XXX: otherwise... hope for the best?
                }
            }

            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return reconstruct(this, expr, name);
        }

        if (expr.type() != null && expr.type().isCanonical()
                && !ts.isFinalAccessExprOrConst(expr, ts.Principal())) {
            // illegal dynamic principal. But try to convert it to an access path
            // to allow a more precise error message.
            AccessPath ap = ts.exprToAccessPath(expr, ts.Principal(),
                    (JifContext) ar.context());
            ap.verify((JifContext) ar.context());

            // previous line should throw an exception, but throw this just to
            // be safe.
            throw new SemanticDetailedException("Illegal dynamic principal.",
                    "Only final access paths or principal expressions can be used as a dynamic principal. "
                            + "A final access path is an expression starting with either \"this\" or a final "
                            + "local variable \"v\", followed by zero or more final field accesses. That is, "
                            + "a final access path is either this.f1.f2....fn, or v.f1.f2.....fn, where v is a "
                            + "final local variables, and each field f1 to fn is a final field. A principal expression "
                            + "is either a principal parameter, or an external principal.",
                    this.position());
        }

        // the expression type may not yet be fully determined, but
        // that's ok, as type checking will ensure that it is
        // a suitable expression.
        return nf.CanonicalPrincipalNode(position(),
                ts.dynamicPrincipal(position(), ts.exprToAccessPath(expr,
                        ts.Principal(), (JifContext) ar.context())));
    }

    protected Node disambiguateName(AmbiguityRemover ar, Id name)
            throws SemanticException {
        if ("_".equals(name.id())) {
            // "_" is the bottom principal
            JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
            JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();
            return nf.CanonicalPrincipalNode(position(),
                    ts.bottomPrincipal(position()));
        }

        Context c = ar.context();
        VarInstance vi = c.findVariable(name.id());

        if (vi instanceof JifVarInstance) {
            return varToPrincipal((JifVarInstance) vi, ar);
        }

        if (vi instanceof PrincipalInstance) {
            return principalToPrincipal((PrincipalInstance) vi, ar);
        }

        if (vi instanceof ParamInstance) {
            return paramToPrincipal((ParamInstance) vi, ar);
        }

        throw new SemanticException(vi + " cannot be used as principal.",
                position());
    }

    protected Node varToPrincipal(JifVarInstance vi, AmbiguityRemover sc)
            throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        if (vi.flags().isFinal()) {
            return nf.CanonicalPrincipalNode(position(),
                    ts.dynamicPrincipal(position(),
                            ts.varInstanceToAccessPath(vi, this.position())));
        }

        throw new SemanticException(
                vi + " is not a final variable " + "of type \"principal\".");
    }

    /**
     * @throws SemanticException
     */
    protected Node principalToPrincipal(PrincipalInstance vi,
            AmbiguityRemover sc) throws SemanticException {
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();
        ExternalPrincipal ep = vi.principal();
        //((JifContext)sc.context()).ph().
        return nf.CanonicalPrincipalNode(position(), ep);
    }

    protected Node paramToPrincipal(ParamInstance pi, AmbiguityRemover sc)
            throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        if (pi.isPrincipal()) {
            // <param principal uid> => <principal-param uid>
            Principal p = ts.principalParam(position(), pi);
            return nf.CanonicalPrincipalNode(position(), p);
        }

        throw new SemanticException(pi + " may not be used as a principal.",
                position());
    }

    /**
     * Visit this term in evaluation order.
     */
    @Override
    public <T> List<T> acceptCFG(CFGBuilder<?> v, List<T> succs) {
        return succs;
    }

    @Override
    public Term firstChild() {
        return null;
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Expr expr = this.expr;
        Id name = this.name;
        if (this.expr != null) {
            expr = visitChild(this.expr, v);
        }
        if (this.name != null) {
            name = visitChild(this.name, v);
        }

        return reconstruct(this, expr, name);
    }

    protected <N extends AmbPrincipalNode_c> N reconstruct(N n, Expr expr,
            Id name) {
        n = expr(n, expr);
        n = id(n, name);
        return n;
    }

}

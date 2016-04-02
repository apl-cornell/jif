package jif.ast;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.Param;
import jif.types.ParamInstance;
import jif.types.SemanticDetailedException;
import jif.visit.JifTypeChecker;
import polyglot.ast.Expr;
import polyglot.ast.Ext;
import polyglot.ast.Field;
import polyglot.ast.Node;
import polyglot.ast.Node_c;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
import polyglot.types.ParsedClassType;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeChecker;

/** An implementation of the <code>AmbParam</code> interface.
 */
public class AmbExprParam_c extends Node_c implements AmbExprParam {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Expr expr;
    protected ParamInstance expectedPI;

//    @Deprecated
    public AmbExprParam_c(Position pos, Expr expr, ParamInstance expectedPI) {
        this(pos, expr, expectedPI, null);
    }

    public AmbExprParam_c(Position pos, Expr expr, ParamInstance expectedPI,
            Ext ext) {
        super(pos, ext);
        this.expr = expr;
        this.expectedPI = expectedPI;
    }

    @Override
    public boolean isDisambiguated() {
        return false;
    }

    public Expr expr() {
        return this.expr;
    }

    public AmbExprParam expr(Expr expr) {
        return expr(this, expr);
    }

    protected <N extends AmbExprParam_c> N expr(N n, Expr expr) {
        if (n.expr == expr) return n;
        n = copyIfNeeded(n);
        n.expr = expr;
        return n;
    }

    @Override
    public AmbExprParam expectedPI(ParamInstance expectedPI) {
        return expectedPI(this, expectedPI);
    }

    protected <N extends AmbExprParam_c> N expectedPI(N n,
            ParamInstance expectedPI) {
        if (n.expectedPI == expectedPI) return n;
        n = copyIfNeeded(n);
        n.expectedPI = expectedPI;
        return n;
    }

    @Override
    public Param parameter() {
        throw new InternalCompilerError("No parameter yet");
    }

    @Override
    public String toString() {
        return expr + "{amb}";
    }

    @Override
    public Node visitChildren(NodeVisitor v) {
        Expr expr = visitChild(this.expr, v);
        return reconstruct(this, expr, expectedPI);
    }

    protected <N extends AmbExprParam_c> N reconstruct(N n, Expr expr,
            ParamInstance expectedPI) {
        n = expr(n, expr);
        n = expectedPI(n, expectedPI);
        return n;
    }

    /**
     * Always return a CanoncialLabelNode, and let the dynamic label be possibly
     * changed to a dynamic principal later.
     */
    @Override
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (!ar.isASTDisambiguated(expr)) {
            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }
        JifContext c = (JifContext) ar.context();
        JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();

        // run the typechecker over expr.
        TypeChecker tc = new JifTypeChecker(ar.job(), ts, nf, true);
        tc = (TypeChecker) tc.context(ar.context());
        expr = (Expr) expr.visit(tc);

        if (!expr.isTypeChecked()) {
            if (expr instanceof Field) {
                Field f = (Field) expr;
                if (ts.unlabel(f.target().type()) instanceof ParsedClassType) {
                    // disambiguate the class of the receiver of the field,
                    // so that type checking will eventually go through.
                    ParsedClassType pct =
                            (ParsedClassType) ts.unlabel(f.target().type());
                    Scheduler sched = ar.job().extensionInfo().scheduler();
                    Goal g = sched.Disambiguated(pct.job());
                    throw new MissingDependencyException(g);
                }
            }
            //            Scheduler sched = ar.job().extensionInfo().scheduler();
            //            Goal g = sched.Disambiguated(ar.job());
            //            throw new MissingDependencyException(g);
            ar.job().extensionInfo().scheduler().currentGoal()
                    .setUnreachableThisRun();
            return this;
        }

        if (expr instanceof PrincipalNode
                || ts.isImplicitCastValid(expr.type(), ts.Principal())
                || (expectedPI != null && expectedPI.isPrincipal())) {
            if (!ts.isFinalAccessExprOrConst(expr, ts.Principal())) {
                throw new SemanticDetailedException(
                        "Illegal principal parameter.",
                        "The expression " + expr + " is not suitable as a "
                                + "principal parameter. Principal parameters can be either "
                                + "dynamic principals, or principal expressions, such as a "
                                + "principal parameter, or an external principal.",
                        this.position());
            }
            return nf.CanonicalPrincipalNode(position(),
                    ts.exprToPrincipal(ts, expr, c));
        }
        if (!ts.isFinalAccessExprOrConst(expr, ts.Label())) {
            throw new SemanticDetailedException("Illegal label parameter.",
                    "The expression " + expr + " is not suitable as a "
                            + "label parameter. Label parameters can be either "
                            + "dynamic labels, or label expressions.",
                    this.position());
        }
        return nf.CanonicalLabelNode(position(), ts.exprToLabel(ts, expr, c));
    }
}

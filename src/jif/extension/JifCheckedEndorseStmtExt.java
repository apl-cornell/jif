package jif.extension;

import jif.ast.CheckedEndorseStmt;
import jif.ast.CheckedEndorseStmt_c;
import jif.translate.ToJavaExt;
import jif.types.ConstraintMessage;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.PathMap;
import jif.types.SemanticDetailedException;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.If;
import polyglot.ast.Local;
import polyglot.ast.Stmt;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>CheckedEndorseStmt</code> node.
 * 
 *  A checked endorse is of the form:
 *  endorse (x, Lfrom to Lto) if (e) S else S'
 * 
 *  and is approximately equivalent to
 * 
 *  T x' = endorse(x, Lfrom to Lto);
 *  if (e[x'/x]) S[x'/x] else S'
 * 
 *  To actually implement this, however, we use a different context
 *  for label checking e and S, that gives the local variable x
 *  the label Lto.
 * 
 *  @see jif.ast.CheckedEndorseStmt
 */
public class JifCheckedEndorseStmtExt extends JifEndorseStmtExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifCheckedEndorseStmtExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    protected Stmt checkBody(LabelChecker lc, JifContext A, Label downgradeFrom,
            Label downgradeTo) throws SemanticException {
        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext Abody = bodyContext(lc, A, downgradeFrom, downgradeTo);

        CheckedEndorseStmt ds = (CheckedEndorseStmt) this.node();
        If body = (If) ds.body();

        // check the conditional and consquent with Abody
        Abody = (JifContext) body.del().enterScope(Abody);

        Expr e = (Expr) lc.context(Abody).labelCheck(body.cond());

        PathMap Xe = getPathMap(e);

        Abody = (JifContext) Abody.pushBlock();
        updateContextForConsequent(lc, Abody, Xe);

        // extend the context with any label tests or actsfor tests
        JifIfExt.extendContext(lc, Abody, e, false);

        Stmt S1 = (Stmt) lc.context(Abody).labelCheck(body.consequent());
        PathMap X1 = getPathMap(S1);

        Stmt S2 = null;
        PathMap X2;

        // check the alternative using the original context
        if (body.alternative() != null) {
            A = (JifContext) A.pushBlock();
            updateContextForConsequent(lc, A, Xe);

            S2 = (Stmt) lc.context(A).labelCheck(body.alternative());
            X2 = getPathMap(S2);
        } else {
            // Simulate the effect of an empty statement.
            // X0[node() := A[pc := Xe[nv][pc]]] == Xe[nv]
            X2 = ts.pathMap().N(Xe.NV());
        }

        PathMap X = Xe.N(ts.notTaken()).join(X1).join(X2);
        X = X.NV(ts.notTaken());
        return (Stmt) updatePathMap(body.cond(e).consequent(S1).alternative(S2),
                X);

    }

    /**
     * Utility method for updating the context for the consequent.
     *
     * Useful for overriding in projects like fabric.
     */
    protected void updateContextForConsequent(LabelChecker lc, JifContext A,
        PathMap Xexpr) {
        A.setPc(Xexpr.NV(), lc);
    }

    @Override
    protected JifContext bodyContext(LabelChecker lc, JifContext A,
            Label downgradeFrom, Label downgradeTo) {
        // the pc of the block is not actually downgraded.
        A = (JifContext) A.pushBlock();
        final CheckedEndorseStmt_c d = (CheckedEndorseStmt_c) this.node();
        Local l = (Local) d.expr();
        A.addCheckedEndorse(l.localInstance(), downgradeTo);
        return A;
    }

    @Override
    protected void checkPCconstraint(LabelChecker lc, JifContext A, Label pc,
            Label downgradeFrom, boolean boundSpecified)
                    throws SemanticException {
        // No constraint on the pc.
    }

    @Override
    protected void checkOneDimenOnly(LabelChecker lc, final JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException {
        JifEndorseExprExt.checkOneDimen(lc, A, labelFrom, labelTo, pos, false,
                false);
    }

    @Override
    protected void checkAuthority(LabelChecker lc, final JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException {
        JifEndorseExprExt.checkAuth(lc, A, labelFrom, labelTo, pos, false,
                false);
    }

    @Override
    protected void checkRobustness(LabelChecker lc, JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException {
        JifEndorseExprExt.checkRobustEndorse(lc, A, labelFrom, labelTo, pos,
                false);
    }

    @Override
    protected void checkAdditionalConstraints(LabelChecker lc, JifContext A,
            Label labelFrom, Label labelTo, Position pos)
                    throws SemanticException {
        final CheckedEndorseStmt_c d = (CheckedEndorseStmt_c) this.node();
        if (d.expr() != null && !(d.expr() instanceof Local)) {
            throw new SemanticDetailedException(
                    "Checked endorse currently only supports locals",
                    "This version of Jif only permits local variables to "
                            + "be used in the expression for a checked endorse statement.",
                    d.expr().position());
        }
        // check that the local can be downgraded appropriately.
        Expr e = (Expr) lc.context(A).labelCheck(d.expr());
        PathMap Xe = getPathMap(e);

        lc.constrain(new NamedLabel("expr.nv", Xe.NV()), LabelConstraint.LEQ,
                new NamedLabel("downgrade_bound", labelFrom), A.labelEnv(),
                d.position(), new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The label of the expression to "
                                + d.downgradeKind() + " is "
                                + "more restrictive than label of data that "
                                + "the " + d.downgradeKind()
                                + " expression is allowed to "
                                + d.downgradeKind() + ".";
                    }

                    @Override
                    public String detailMsg() {
                        return "This " + d.downgradeKind()
                                + " expression is allowed to " + ""
                                + d.downgradeKind()
                                + " information labeled up to " + namedRhs()
                                + ". However, the label of the "
                                + "expression to " + d.downgradeKind() + " is "
                                + namedLhs()
                                + ", which is more restrictive than "
                                + "allowed.";
                    }

                    @Override
                    public String technicalMsg() {
                        return "Invalid " + d.downgradeKind() + ": NV of the "
                                + "expression is out of bound.";
                    }
                });
    }
}

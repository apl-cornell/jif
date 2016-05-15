package jif.extension;

import jif.translate.ToJavaExt;
import jif.types.ConstraintMessage;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.LabelConstraint;
import jif.types.NamedLabel;
import jif.types.PathMap;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Branch;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.While;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** Jif extension of the <code>While</code> node.
 *  
 *  @see polyglot.ast.While
 */
public class JifWhileExt extends JifStmtExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifWhileExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
        While ws = (While) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) ws.del().enterScope(A);

        Label notTaken = ts.notTaken();

        Label L1 = ts.freshLabelVariable(ws.position(), "while",
                "label of PC for the while statement at " + ws.position());
        Label L2 = ts.freshLabelVariable(ws.position(), "while",
                "label of PC for end of the while statement at "
                        + ws.position());
        Label loopEntryPC = A.pc();

        A = (JifContext) A.pushBlock();

        A.setPc(L1, lc);
        A.gotoLabel(Branch.CONTINUE, null, L1);
        A.gotoLabel(Branch.BREAK, null, L2);

        Expr e = (Expr) lc.context(A).labelCheck(ws.cond());
        PathMap Xe = getPathMap(e);

        A = (JifContext) A.pushBlock();

        updateContextForBody(lc, A, Xe);
        Stmt S = (Stmt) lc.context(A).labelCheck(ws.body());
        PathMap Xs = getPathMap(S);

        A = (JifContext) A.pop();
        A = (JifContext) A.pop();

        lc.constrain(
                new NamedLabel("while_body.N",
                        "label of normal termination of the loop body", Xs.N())
                                .join(lc, "loop_entry_pc",
                                        "label of the program counter just before the loop is executed",
                                        loopEntryPC),
                LabelConstraint.LEQ,
                new NamedLabel("loop_pc",
                        "label of the program counter at the top of the loop",
                        L1),
                A.labelEnv(), ws.position(), false, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The information revealed by the normal "
                                + "termination of the body of the while loop "
                                + "may be more restrictive than the "
                                + "information that should be revealed by "
                                + "reaching the top of the loop.";
                    }

                    @Override
                    public String detailMsg() {
                        return "The program counter label at the start of the loop is at least as restrictive "
                                + "as the normal termination label of the loop body, and the entry "
                                + "program counter label (that is, the program counter label just "
                                + "before the loop is executed for the first time).";

                    }

                    @Override
                    public String technicalMsg() {
                        return "X(loopbody).n <= _pc_ of the while statement";
                    }
                });

        PathMap X = Xe.join(Xs);
        X = X.set(ts.gotoPath(Branch.BREAK, null), notTaken);
        X = X.set(ts.gotoPath(Branch.CONTINUE, null), notTaken);
        X = X.N(lc.upperBound(X.N(), L2));

        return updatePathMap(ws.body(S).cond(e), X);
    }

    /**
     * Utility method for updating the context for checking the body.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForBody(LabelChecker lc, JifContext A,
            PathMap Xexpr) {
        A.setPc(Xexpr.NV(), lc);
    }
}

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
import polyglot.ast.Do;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>Do</code> node. 
 * 
 *  @see polyglot.ast.Do
 */
public class JifDoExt extends JifStmtExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifDoExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
        Do ds = (Do) node();

        JifTypeSystem ts = lc.typeSystem();
        JifContext A = lc.context();
        A = (JifContext) ds.del().enterScope(A);
        lc = lc.context(A);

        Label L1 = ts.freshLabelVariable(node().position(), "do",
                "label of PC for the do statement at " + node().position());
        Label L2 = ts.freshLabelVariable(node().position(), "do",
                "label of PC at end of do statement at " + node().position());
        Label loopEntryPC = A.pc();

        A = (JifContext) A.pushBlock();
        A.setPc(L1, lc);
        A.gotoLabel(Branch.CONTINUE, null, L1);
        A.gotoLabel(Branch.BREAK, null, L2);

        Stmt s = (Stmt) lc.context(A).labelCheck(ds.body());
        PathMap Xs = getPathMap(s);

        A = (JifContext) A.pushBlock();
        updateContextForGuard(lc, A, Xs);

        Expr e = (Expr) lc.context(A).labelCheck(ds.cond());
        PathMap Xe = getPathMap(e);

        lc.constrain(
                new NamedLabel("do_while_guard.NV",
                        "label of evaluation of the loop guard", Xe.NV()).join(
                                lc, "loop_entry_pc",
                                "label of the program counter just before the loop is executed",
                                loopEntryPC),
                LabelConstraint.LEQ,
                new NamedLabel("loop_pc",
                        "label of the program counter at the top of the loop",
                        L1),
                lc.context().labelEnv(), ds.position(), false,
                new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The information revealed by the normal "
                                + "termination of the body of the do-while loop "
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
                        return "X(loopbody).n <= _pc_ of the do-while statement";
                    }
                });
        PathMap X = Xe.join(Xs);
        X = X.set(ts.gotoPath(Branch.BREAK, null), ts.notTaken());
        X = X.set(ts.gotoPath(Branch.CONTINUE, null), ts.notTaken());
        X = X.N(lc.upperBound(X.N(), L2));

        return updatePathMap(ds.cond(e).body(s), X);
    }

    /**
     * Utility method for updating the context for checking the test.
     *
     * Useful for overriding in projects like fabric.
     */
    protected void updateContextForGuard(LabelChecker lc, JifContext A,
        PathMap Xbod) {
        A.setPc(Xbod.N(), lc);
    }
}

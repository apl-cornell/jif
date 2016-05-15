package jif.extension;

import java.util.LinkedList;
import java.util.List;

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
import polyglot.ast.Switch;
import polyglot.ast.SwitchElement;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** Jif extension of the <code>Switch</code> node.
 * 
 *  @see polyglot.ast.Switch
 */
public class JifSwitchExt extends JifStmtExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifSwitchExt(ToJavaExt toJava) {
        super(toJava);
    }

    /** Label check the switch statement.
     * 
     *  PC(branch i) = X(branch 0).N + ... + X(branch i-1).N
     */
    @Override
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
        Switch ss = (Switch) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) ss.del().enterScope(A);

        Label notTaken = ts.notTaken();

        Expr e = (Expr) lc.context(A).labelCheck(ss.expr());
        PathMap Xe = getPathMap(e);

        Label L = ts.freshLabelVariable(ss.position(), "switch",
                "label of PC at break target for switch statement at "
                        + node().position());

        A = (JifContext) A.pushBlock();
        updateContextForCases(lc, A, Xe);
        A.gotoLabel(Branch.BREAK, null, L);

        PathMap Xa = Xe.N(notTaken);
        List<SwitchElement> l = new LinkedList<SwitchElement>();

        for (SwitchElement s : ss.elements()) {
            s = (SwitchElement) lc.context(A).labelCheck(s);
            l.add(s);

            PathMap Xs = getPathMap(s);
            updateContextForNextCase(lc, A, Xs);
            Xa = Xa.join(Xs);
        }

        A = (JifContext) A.pop();
        lc.constrain(
                new NamedLabel(
                        "label of normal termination of swtich statement",
                        Xa.N()),
                LabelConstraint.LEQ,
                new NamedLabel("label of break target for the switch stmt", L),
                A.labelEnv(), ss.position(), false, new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The information revealed by the normal "
                                + "termination of the switch statement "
                                + "may be more restrictive than the "
                                + "information that can be revealed by "
                                + "a break statement being executed in the "
                                + "switch statement.";
                    }

                    @Override
                    public String technicalMsg() {
                        return "[join(X(branch_i).n) <= L(break)] is not satisfied.";
                    }

                });

        PathMap X = Xa.set(ts.gotoPath(Branch.BREAK, null), notTaken);
        X = X.NV(ts.notTaken());
        X = X.N(L);

        return updatePathMap(ss.elements(l), X);
    }

    /**
     * Utility method for updating the context for checking the cases of a
     * switch statement.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForCases(LabelChecker lc, JifContext A,
            PathMap Xval) {
        A.setPc(Xval.NV(), lc);
    }

    /**
     * Utility method for updating the context for checking the cases of a
     * switch statement.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForNextCase(LabelChecker lc, JifContext A,
            PathMap Xprev) {
        A.setPc(lc.upperBound(A.pc(), Xprev.N()), lc);
    }
}

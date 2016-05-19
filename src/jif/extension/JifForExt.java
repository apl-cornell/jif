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
import polyglot.ast.For;
import polyglot.ast.ForInit;
import polyglot.ast.ForUpdate;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>For</code> node.
 * 
 *  @see polyglot.ast.For
 */
public class JifForExt extends JifStmtExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifForExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
        For fs = (For) node();

        JifContext A = lc.jifContext();
        A = (JifContext) fs.del().enterScope(A);

        A = (JifContext) A.pushBlock();

        List<ForInit> inits = new LinkedList<>();
        PathMap Xinit = checkInits(lc, A, fs, inits);

        return checkLoop(lc, A, fs, inits, Xinit);
    }

    /**
     * Utility for easier overriding of loop checking.
     */
    public Node checkLoop(LabelChecker lc, JifContext A, For fs,
        List<ForInit> inits, PathMap Xinit) throws SemanticException {

        JifTypeSystem ts = lc.jifTypeSystem();
        Label notTaken = ts.notTaken();

        // Now handle the loop body, condition, and iterators.
        Label L1 = ts.freshLabelVariable(fs.position(), "for",
                "label of PC for the for statement at " + node().position());
        Label L2 = ts.freshLabelVariable(fs.position(), "for",
                "label of PC for end of the for statement at "
                        + node().position());

        A = (JifContext) A.pushBlock();
        Label loopEntryPC = A.pc();

        A.setPc(L1, lc);
        A.gotoLabel(Branch.CONTINUE, null, L1);
        A.gotoLabel(Branch.BREAK, null, L2);

        PathMap Xe;
        Expr cond = fs.cond();
        if (cond != null) {
            cond = (Expr) lc.context(A).labelCheck(fs.cond());
            Xe = getPathMap(cond);
        } else {
            Xe = ts.pathMap().NV(A.pc()).N(A.pc());
        }

        A = (JifContext) A.pushBlock();
        updateContextForBody(lc, A, Xe);
        Stmt body = (Stmt) lc.context(A).labelCheck(fs.body());
        PathMap Xbody = getPathMap(body);

        A = (JifContext) A.pushBlock();
        updateContextForNextIter(lc, A, Xbody);

        List<ForUpdate> iters = new LinkedList<ForUpdate>();

        for (ForUpdate update : fs.iters()) {
            update = (ForUpdate) lc.context(A).labelCheck(update);
            iters.add(update);

            PathMap Xs = getPathMap(update);

            updateContextForNextIter(lc, A, Xs);

            Xbody = Xbody.N(notTaken).join(Xs);
        }

        lc.constrain(
                new NamedLabel("for_body.N",
                        "label of normal termination of the loop body",
                        Xbody.N()).join(lc, "loop_entry_pc",
                                "label of the program counter just before the loop is executed",
                                loopEntryPC),
                LabelConstraint.LEQ,
                new NamedLabel("loop_pc",
                        "label of the program counter at the top of the loop",
                        L1),
                lc.context().labelEnv(), fs.position(), false,
                new ConstraintMessage() {
                    @Override
                    public String msg() {
                        return "The information revealed by the normal "
                                + "termination of the body of the for loop "
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
                        return "X(loopbody).n <= _pc_ of the for statement";
                    }
                });

        // Compute the path map for "loop" == "while (cond) body".
        PathMap Xloop = Xe.join(Xbody);
        Xloop = Xloop.set(ts.gotoPath(Branch.CONTINUE, null), notTaken);
        Xloop = Xloop.set(ts.gotoPath(Branch.BREAK, null), notTaken);
        Xloop = Xloop.N(lc.upperBound(Xloop.N(), L2));

        // Compute the path map for "init ; loop"
        PathMap X = Xinit.N(notTaken).join(Xloop);

        return updatePathMap(fs.iters(iters).cond(cond).inits(inits).body(body),
                X);
    }

    /**
     * Splitting out checking of inits and checking of the loop to make it
     * easier to extend.
     */
    protected PathMap checkInits(LabelChecker lc, JifContext A, For fs,
        List<ForInit> newInits) throws SemanticException {

        JifTypeSystem ts = lc.jifTypeSystem();
        PathMap Xinit = ts.pathMap().N(A.pc());
        Label notTaken = ts.notTaken();

        for (ForInit init : fs.inits()) {
            init = (ForInit) lc.context(A).labelCheck(init);
            newInits.add(init);

            PathMap Xs = getPathMap(init);

            updateContextForNextInit(lc, A, Xs);

            Xinit = Xinit.N(notTaken).join(Xs);
        }

        return Xinit;
    }

    /**
     * Utility method for updating the context for checking the next init
     * statement in the for loop.
     *
     * Useful for overriding in projects like fabric.
     */
    protected void updateContextForNextInit(LabelChecker lc, JifContext A,
        PathMap Xprev) {
        // At this point, the environment A should have been extended
        // to include any declarations of s.  Reset the PC label.
        A.setPc(Xprev.N(), lc);
    }

    /**
     * Utility method for updating the context for checking the body in the for
     * loop.
     *
     * Useful for overriding in projects like fabric.
     */
    protected void updateContextForBody(LabelChecker lc, JifContext A,
        PathMap Xcond) {
        A.setPc(Xcond.NV(), lc);
    }

    /**
     * Utility method for updating the context for checking the next iter
     * statement in the for loop.
     *
     * Useful for overriding in projects like fabric.
     */
    protected void updateContextForNextIter(LabelChecker lc, JifContext A,
        PathMap Xprev) {
        // At this point, the environment A should have been extended
        // to include any declarations of s.  Reset the PC label.
        A.setPc(Xprev.N(), lc);
    }
}

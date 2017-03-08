package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.PathMap;
import jif.visit.LabelChecker;
import polyglot.ast.Block;
import polyglot.ast.LocalClassDecl;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.main.Report;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** The Jif extension of the <code>Block</code> node.
 * 
 *  @see polyglot.ast.Block_c
 */
public class JifBlockExt extends JifStmtExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifBlockExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
        Block bs = (Block) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) bs.del().enterScope(A);

        // A path map incorporating all statements in the block seen so far.
        PathMap Xblock = ts.pathMap();
        Xblock = Xblock.N(A.pc());

        A = (JifContext) A.pushBlock();

        List<Stmt> l = new ArrayList<Stmt>(bs.statements().size());

        for (Stmt s : bs.statements()) {
            s = (Stmt) lc.context(A).labelCheck(s);
            l.add(s);

            if (s instanceof LocalClassDecl)
                // nothing else required
                continue;

            PathMap Xs = getPathMap(s);

            updateContextForNextStmt(lc, A, Xs);

            if (Report.should_report(jif.Topics.pc, 1)) {
                Report.report(1, "pc after statement at " + s.position() + " : "
                        + A.pc().toString());
            }

            Xblock = Xblock.N(ts.notTaken()).join(Xs);
        }

        A = (JifContext) A.pop();

        return updatePathMap(bs.statements(l), Xblock);
    }

    /**
     * Utility method for updating the context for checking the next statement
     * in the block.
     *
     * Useful for overriding in projects like fabric.
     */
    protected void updateContextForNextStmt(LabelChecker lc, JifContext A,
        PathMap Xprev) {
        // At this point, the environment A should have been extended
        // to include any declarations of s.  Reset the PC label.
        A.setPc(Xprev.N(), lc);
    }
}

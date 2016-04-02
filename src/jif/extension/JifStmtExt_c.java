package jif.extension;

import jif.ast.JifExt_c;
import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.Path;
import jif.types.PathMap;
import jif.types.label.NotTaken;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** The root of all kinds of Jif extensions for statements.
 *  It provides a generic <node>labelCheck</code> method, which
 *  will invoke the <ndoe>labelCheckStmt</code> methods provided
 *  by the subclasses of this class.
 */
public abstract class JifStmtExt_c extends JifExt_c implements JifStmtExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected JifStmtExt stmtDel;

    public JifStmtExt_c(JifStmtExt stmtDel, ToJavaExt toJava) {
        super(toJava);
        this.stmtDel = stmtDel;
    }

    public JifStmtExt_c(ToJavaExt toJava) {
        this(null, toJava);
    }

    @Override
    public JifStmtExt stmtDel() {
        return stmtDel != null ? stmtDel : this;
    }

    @Override
    public void init(Node node) {
        super.init(node);
        if (stmtDel != null) {
            stmtDel.init(node);
        }
    }

    @Override
    public JifStmtExt copy() {
        JifStmtExt_c copy = (JifStmtExt_c) super.copy();
        if (stmtDel != null) {
            copy.stmtDel = (JifStmtExt) stmtDel.copy();
        }
        return copy;
    }

    @Override
    public JifStmtExt stmtDel(JifStmtExt stmtDel) {
        if (stmtDel == this.stmtDel) {
            return this;
        }

        JifStmtExt old = this.stmtDel;
        this.stmtDel = null;

        JifStmtExt_c copy = (JifStmtExt_c) copy();
        copy.stmtDel = stmtDel != this ? stmtDel : null;

        // Restore the old pointer.
        this.stmtDel = old;

        return copy;
    }

    @Override
    public abstract Node labelCheckStmt(LabelChecker lc)
            throws SemanticException;

    /** Label check a statement.
     *  After invoking the overrided <code>labelCheckStmt</code> statement,
     *  this method will apply the "single path rule" (Figure 4.15):
     *  If the path map contains only one path and it is "N" or "R",
     *  then the PC label at the end is the same as at the beginning,
     *  so just set the path map to use the PC label.
     */
    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        JifTypeSystem ts = lc.typeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) node().del().enterScope(A);

        // Redispatch in case we're not the first delegate.
        Node n = ((JifStmtExt) JifUtil.jifExt(node())).stmtDel()
                .labelCheckStmt(lc.context(A));

        // Apply the "single path rule"
        PathMap X = getPathMap(n).NV(ts.notTaken());

        Path singlePath = null;

        for (Path p : X.paths()) {
            if (lc.ignoredForSinglePathRule(p)) {
                continue;
            }

            if (X.get(p) instanceof NotTaken) {
                continue;
            }

            if (singlePath != null) {
                // No change.
                return updatePathMap(n, X);
            }

            singlePath = p;
        }

        if (singlePath != null) {
            if (singlePath.equals(Path.N) || singlePath.equals(Path.R)) {
                return updatePathMap(n, X.set(singlePath, A.pc()));
            }
        }

        return updatePathMap(n, X);
    }
}

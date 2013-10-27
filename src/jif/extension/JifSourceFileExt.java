package jif.extension;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import jif.ast.JifExt_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.ast.SourceFile;
import polyglot.ast.TopLevelDecl;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

/** The root of all kinds of Jif extensions for statements.
 *  It provides a generic <node>labelCheck</code> method, which
 *  will invoke the <code>labelCheckStmt</code> methods provided
 *  by the subclasses of this class.
 */
public class JifSourceFileExt extends JifExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifSourceFileExt(ToJavaExt toJava) {
        super(toJava);
    }

    @Override
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        SourceFile n = (SourceFile) node();

        JifTypeSystem ts = lc.typeSystem();
        JifContext A = lc.context();
        A = (JifContext) n.del().enterScope(A);

        A.setAuthority(new LinkedHashSet<Principal>());
        A.setPc(ts.notTaken(), lc);

        lc = lc.context(A);

        LabelChecker orig_lc = lc;

        List<TopLevelDecl> decls = new LinkedList<TopLevelDecl>();
        for (TopLevelDecl d : n.decls()) {
            // push a block to ensure separation of contexts for different
            // declaration within the same source file.
            lc = orig_lc.context((JifContext) orig_lc.context().pushBlock());

            decls.add((TopLevelDecl) lc.labelCheck(d));
        }

        return n.decls(decls);
    }
}

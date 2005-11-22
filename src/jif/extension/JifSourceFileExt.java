package jif.extension;

import java.util.*;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.visit.LabelChecker;
import polyglot.ast.ClassDecl;
import polyglot.ast.Node;
import polyglot.ast.SourceFile;
import polyglot.types.SemanticException;

/** The root of all kinds of Jif extensions for statements. 
 *  It provides a generic <node>labelCheck</code> method, which
 *  will invoke the <ndoe>labelCheckStmt</code> methods provided
 *  by the subclasses of this class. 
 */
public class JifSourceFileExt extends Jif_c
{
    public JifSourceFileExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException {
        SourceFile n = (SourceFile) node();

        JifTypeSystem ts = lc.typeSystem();
	JifContext A = lc.context();
	A = (JifContext) n.enterScope(A);

        A.setAuthority(new LinkedHashSet());
        A.setPc(ts.bottomLabel());

        lc = lc.context(A);

        List decls = new LinkedList();
        for (Iterator i = n.decls().iterator(); i.hasNext(); ) {
            ClassDecl d = (ClassDecl) i.next();
            decls.add(lc.labelCheck(d));
        }

        return n.decls(decls);
    }
}

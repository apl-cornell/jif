package jif.extension;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.visit.LabelChecker;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.types.SemanticException;

/** The Jif extension of the <code>Formal</code> node. 
 * 
 *  @see polyglot.ast.Formal
 */
public class JifFormalExt extends Jif_c
{
    public JifFormalExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException {
	Formal fn = (Formal) node();
        JifContext A = lc.jifContext();
	A = (JifContext) node().enterScope(A);
        return node();
    }
}

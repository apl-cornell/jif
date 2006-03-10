package jif.extension;

import java.util.Iterator;
import java.util.Set;

import jif.ast.DeclassifyStmt;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.types.label.NotTaken;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.types.SemanticException;
import polyglot.util.Position;

/** The Jif extension of the <code>EndorseStmt</code> node. 
 * 
 *  @see jif.ast.EndorseStmt
 */
public class JifEndorseStmtExt extends JifDowngradeStmtExt
{
    public JifEndorseStmtExt(ToJavaExt toJava) {
        super(toJava);
    }

    protected void checkOneDimenOnly(LabelChecker lc, 
            final JifContext A,
            Label labelFrom, 
            Label labelTo, Position pos) 
    throws SemanticException {
        JifTypeSystem jts = lc.jifTypeSystem();
        Label botIntegLabel = jts.pairLabel(pos, 
                                            jts.topConfPolicy(pos),
                                            jts.bottomIntegPolicy(pos));
        
        lc.constrain(new LabelConstraint(new NamedLabel("endorse_from", labelFrom).
                                         meet(lc, "bottom_integ", botIntegLabel), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("endorse_to", labelTo),
                                         A.labelEnv(),       
                                         pos) {
            public String msg() {
                return "Endorse statements cannot downgrade confidentiality.";
            }
            public String detailMsg() { 
                return "The endorse_to label has lower confidentiality than the " +
                "endorse_from label; endorse statements " +
                "cannot downgrade confidentiality.";
            }                     
        }
        );
    }

protected void checkAuthority(LabelChecker lc, 
                                  final JifContext A,
                                  Label labelFrom, 
                                  Label labelTo, Position pos) 
            throws SemanticException {
        JifEndorseExprExt.checkAuth(lc, A, labelFrom, labelTo, pos);
    }

    protected void checkRobustness(LabelChecker lc, 
                                   JifContext A,
                                   Label labelFrom, 
                                   Label labelTo, Position pos) 
    throws SemanticException {
        JifEndorseExprExt.checkRobustEndorse(lc, A, labelFrom, labelTo, pos);
    }
}

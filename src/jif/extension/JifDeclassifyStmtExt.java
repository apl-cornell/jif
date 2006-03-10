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

/** The Jif extension of the <code>DeclassifyStmt</code> node. 
 * 
 *  @see jif.ast.DeclassifyStmt
 */
public class JifDeclassifyStmtExt extends JifDowngradeStmtExt
{
    public JifDeclassifyStmtExt(ToJavaExt toJava) {
        super(toJava);
    }

    protected void checkOneDimenOnly(LabelChecker lc, 
            final JifContext A,
            Label labelFrom, 
            Label labelTo, Position pos) 
    throws SemanticException {
        JifTypeSystem jts = lc.jifTypeSystem();
        Label topConfLabel = jts.pairLabel(pos, 
                                           jts.topConfPolicy(pos),
                                           jts.bottomIntegPolicy(pos));
        
        lc.constrain(new LabelConstraint(new NamedLabel("declass_from", labelFrom), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("declass_to", labelTo).
                                         join(lc, "top_confidentiality", topConfLabel),
                                         A.labelEnv(),       
                                         pos) {
            public String msg() {
                return "Declassify statements cannot downgrade integrity.";
            }
            public String detailMsg() { 
                return "The declass_from label has lower integrity than the " +
                "declass_to label; declassify statements " +
                "cannot downgrade integrity.";
            }                     
        }
        );
    }

    protected void checkAuthority(LabelChecker lc, 
                                  final JifContext A,
                                  Label labelFrom, 
                                  Label labelTo, Position pos) 
            throws SemanticException {
        JifDeclassifyExprExt.checkAuth(lc, A, labelFrom, labelTo, pos);
    }

    protected void checkRobustness(LabelChecker lc, 
                                   JifContext A,
                                   Label labelFrom, 
                                   Label labelTo, Position pos) 
    throws SemanticException {
        JifDeclassifyExprExt.checkRobustDecl(lc, A, labelFrom, labelTo, pos);
    }
}

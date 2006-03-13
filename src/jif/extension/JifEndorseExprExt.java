package jif.extension;

import java.util.Iterator;
import java.util.Set;

import jif.JifOptions;
import jif.ast.EndorseExpr;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.Position;

/** The Jif extension of the <code>EndorseExpr</code> node. 
 * 
 *  @see jif.ast.EndorseExpr
 */
public class JifEndorseExprExt extends JifDowngradeExprExt
{
    public JifEndorseExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    protected void checkOneDimenOnly(LabelChecker lc, 
                                     final JifContext A,
                                     Label labelFrom, 
                                     Label labelTo, Position pos) 
            throws SemanticException {
          checkOneDimen(lc, A, labelFrom, labelTo, pos, true);
      }
      protected static void checkOneDimen(LabelChecker lc, 
                                    final JifContext A,
                                    Label labelFrom, 
                                    Label labelTo, Position pos,
                                    boolean isExpr) 
           throws SemanticException {
          final String exprOrStmt = (isExpr?"expression":"statement");
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
                       return "Endorse " + exprOrStmt + "s cannot downgrade confidentiality.";
                   }
                   public String detailMsg() { 
                       return "The endorse_to label has lower confidentiality than the " +
                                   "endorse_from label; endorse " + exprOrStmt + "s " +
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
        checkAuth(lc, A, labelFrom, labelTo, pos, true);
    }

    protected static void checkAuth(LabelChecker lc, 
                                  final JifContext A,
                                  Label labelFrom, 
                                  Label labelTo, Position pos,
                                  boolean isExpr) 
         throws SemanticException {
        Label authLabel = A.authLabelInteg();
        final String exprOrStmt = (isExpr?"expression":"statement");
        lc.constrain(new LabelConstraint(new NamedLabel("endorse_from", labelFrom).
                                         meet(lc, "auth_label", authLabel),
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("endorse_to", labelTo),
                                         A.labelEnv(),
                                         pos) {
            public String msg() {
                return "The method does not have sufficient " +
                "authority to endorse this " + exprOrStmt + ".";
            }
            public String detailMsg() { 
                StringBuffer sb = new StringBuffer();
                Set authorities = A.authority();
                if (authorities.isEmpty()) {
                    sb.append("no principals");
                }
                else {
                    sb.append("the following principals: ");
                }
                for (Iterator iter = authorities.iterator(); iter.hasNext() ;) {
                    Principal p = (Principal)iter.next();
                    sb.append(p.toString());
                    if (iter.hasNext()) {
                        sb.append(", ");
                    }
                }
                
                
                return "The " + exprOrStmt + " to endorse has label " + 
                namedRhs()+ ", and the " + exprOrStmt + " " +
                "should be downgraded to label " +
                "endorse_to. However, the method has " +
                "the authority of " + sb.toString() + ". " +
                "The authority of other principals is " +
                "required to perform the endorse.";
            }
            public String technicalMsg() {
                return "Invalid endorse: the method does " +
                "not have sufficient authorities.";
            }                     
        }
      );
    }
    
    protected void checkRobustness(LabelChecker lc, 
                                   JifContext A,
                                   Label labelFrom, 
                                   Label labelTo, Position pos) 
                 throws SemanticException {
        checkRobustEndorse(lc, A, labelFrom, labelTo, pos, true);
    }

    protected static void checkRobustEndorse(LabelChecker lc, 
                                          JifContext A,
                                          Label labelFrom, 
                                          Label labelTo, Position pos, 
                                          boolean isExpr) 
        throws SemanticException {
        
        
        JifTypeSystem jts = lc.typeSystem();
        final String exprOrStmt = (isExpr?"expression":"statement");
        Label pcInteg = lc.upperBound(A.pc(),
                                 jts.pairLabel(pos,
                                               jts.topConfPolicy(pos),
                                               jts.bottomIntegPolicy(pos)));        
        
        lc.constrain(new LabelConstraint(new NamedLabel("endorse_from_label", labelFrom).
                                                  meet(lc, "pc_integrity", pcInteg), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("endorse_to_label", labelTo),
                                         A.labelEnv(),
                                         pos) {
                     public String msg() {
                         return "Endorsement not robust: a new reader " +
                                        "may influence the decision to " +
                                        "endorse.";
                     }
                     public String detailMsg() { 
                         return "The endorsement of this " + exprOrStmt + " is " +
                         "not robust; at least one of principals that is " +
                         "allowed to read the information after " +
                         "endorsement may be able to influence the " +
                         "decision to endorse.";
                     }
         }
         );
    }


}

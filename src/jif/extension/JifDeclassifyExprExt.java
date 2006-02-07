package jif.extension;

import java.util.Iterator;
import java.util.Set;

import jif.JifOptions;
import jif.ast.DeclassifyExpr;
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

/** The Jif extension of the <code>DeclassifyExpr</code> node. 
 * 
 *  @see jif.ast.DeclassifyExpr
 */
public class JifDeclassifyExprExt extends JifDowngradeExprExt
{
    public JifDeclassifyExprExt(ToJavaExt toJava) {
        super(toJava);
    }

    protected void checkOneDimenOnly(LabelChecker lc, 
                                  final JifContext A,
                                  Label labelFrom, 
                                  Label labelTo, Position pos) 
         throws SemanticException {
       checkOneDimen(lc, A, labelFrom, labelTo, pos);
   }
   protected static void checkOneDimen(LabelChecker lc, 
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
                    return "Declassify expressions cannot downgrade integrity.";
                }
                public String detailMsg() { 
                    return "The declass_from label has lower integrity than the " +
                                "declass_to label; declassify expressions " +
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
      checkAuth(lc, A, labelFrom, labelTo, pos);
  }
  protected static void checkAuth(LabelChecker lc, 
                                final JifContext A,
                                Label labelFrom, 
                                Label labelTo, Position pos) 
       throws SemanticException {
  
      Label authLabel = A.authLabel();    
  lc.constrain(new LabelConstraint(new NamedLabel("declass_from", labelFrom), 
                                   LabelConstraint.LEQ, 
                                   new NamedLabel("declass_to", labelTo).
                                             join(lc, "auth_label", authLabel),
                                   A.labelEnv(),
                                   pos) {
               public String msg() {
                   return "The method does not have sufficient " +
                          "authority to declassify this expression.";
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
                   
                    
                   return "The expression to declassify has label " + 
                          namedRhs()+ ", and the expression " +
                          "should be downgraded to label " +
                          "declass_to. However, the method has " +
                          "the authority of " + sb.toString() + ". " +
                          "The authority of other principals is " +
                          "required to perform the declassify.";
               }
               public String technicalMsg() {
                   return "Invalid declassify: the method does " +
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
        checkRobustDecl(lc, A, labelFrom, labelTo, pos);
    }

    protected static void checkRobustDecl(LabelChecker lc, 
                                          JifContext A,
                                          Label labelFrom, 
                                          Label labelTo, Position pos) 
        throws SemanticException {
        
        
        JifTypeSystem jts = lc.typeSystem();
        Label pcInteg = jts.writersToReadersLabel(pos, A.pc());
        lc.constrain(new LabelConstraint(new NamedLabel("declass_from", labelFrom), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("declass_to", labelTo).
                                                   join(lc, "pc_integrity", pcInteg),
                                         A.labelEnv(),
                                         pos) {
                     public String msg() {
                         return "Declassification not robust: a new reader " +
                                        "may influence the decision to " +
                                        "declassify.";
                     }
                     public String detailMsg() { 
                         return "The declassification of this expression is " +
                         "not robust; at least one of principals that is " +
                         "allowed to read the information after " +
                         "declassification may be able to influence the " +
                         "decision to declassify.";
                     }
         }
         );

        Label fromInteg = jts.writersToReadersLabel(pos, labelFrom);
        lc.constrain(new LabelConstraint(new NamedLabel("declass_from_label", labelFrom), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("declass_to_label", labelTo).
                                                   join(lc, "from_label_integrity", fromInteg),
                                         A.labelEnv(),
                                         pos) {
                     public String msg() {
                         return "Declassification not robust: a new reader " +
                                        "may influence the data to be " +
                                        "declassified.";
    }
                     public String detailMsg() { 
                         return "The declassification of this expression is " +
                         "not robust; at least one of principals that is " +
                         "allowed to read the information after " +
                         "declassification may be able to influence the " +
                         "data to be declassified.";
}
         }
         );
    }


}

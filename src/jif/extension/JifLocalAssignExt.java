package jif.extension;

import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;

/** The Jif extension of the <code>LocalAssign</code> node. 
 */
public class JifLocalAssignExt extends JifAssignExt
{
    public JifLocalAssignExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheckLHS(LabelChecker lc)
        throws SemanticException
    {
        final Assign assign = (Assign) node();
        Local lve = (Local) assign.left();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) lve.enterScope(A);

        final LocalInstance li = lve.localInstance();

        Label L = ts.labelOfLocal(li, A.pc());

        Expr rhs = (Expr) lc.context(A).labelCheck(assign.right());
        PathMap Xr = X(rhs);
        PathMap X;

        if (assign.operator() != Assign.ASSIGN) {
            PathMap Xv = ts.pathMap();
            Xv = Xv.N(A.pc());
            Xv = Xv.NV(L.join(A.pc()));

            if (assign.throwsArithmeticException()) {
                Type arithExc = ts.ArithmeticException();
                X = Xv.join(Xr).exc(Xr.NV(), arithExc);
            }
            else {
                X = Xv.join(Xr);
            }
        }
        else {
            X = Xr;
        }

        lc.constrain(new LabelConstraint(new NamedLabel("rhs.nv", 
                                                        "label of successful evaluation of right hand of assignment", 
                                                        X.NV()), 
                                         LabelConstraint.LEQ, 
                                         new NamedLabel("label of var " + li.name(), L),
                                         A.labelEnv(),
                                         lve.position()) {
                     public String msg() {
                         return "Label of right hand side not less " + 
                                "restrictive than the label for local variable " + 
                                li.name();
                     }
                     public String detailMsg() { 
                         return "More information is revealed by the successful " +
                                "evaluation of the right hand side of the " +
                                "assignment than is allowed to flow to " +
                                "the local variable " + li.name() + ".";
                     }
                     public String technicalMsg() {
                         return "Invalid assignment: path NV of rhs is " +
                                "more restrictive than the declared label " +
                                "of the local variable <" + li.name() + ">.";
                     }
                     
         }
         );

        Expr lhs = (Expr) X(lve, X);

        //deal with the special case "l = new label(...)" and "L1 = L2"
        if (ts.isLabel(li.type())) {
            JifVarInstance jvi = (JifVarInstance) li;
            DynamicLabel dl = ts.dynamicLabel(lve.position(), jvi.uid(), jvi.name(), jvi.label());
            Label rhs_label = JifUtil.exprToLabel(ts, rhs);
            // the rhs_label may be null, e.g. "l = foo();",
            // since there is no specific label associated with this particular
            // call to the method.
            if (rhs_label != null) {
                lc.bind(dl, rhs_label);
            }   
        }

        return (Assign) X(assign.right(rhs).left(lhs), X);
    }
}

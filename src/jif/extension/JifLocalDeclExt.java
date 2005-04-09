package jif.extension;

import jif.ast.JifUtil;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.*;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.types.Type;

/** The Jif extension of the <code>LocalDecl</code> node. 
 * 
 *  @see polyglot.ast.LocalDecl
 */
public class JifLocalDeclExt extends JifStmtExt_c
{
    public JifLocalDeclExt(ToJavaExt toJava) {
        super(toJava);
    }
    
    SubtypeChecker subtypeChecker = new SubtypeChecker();
    
    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
        LocalDecl decl = (LocalDecl) node();
        
        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
        A = (JifContext) decl.enterScope(A);
        
        PathMap X = ts.pathMap();
        X = X.N(A.pc());
        
        final JifLocalInstance li = (JifLocalInstance) decl.localInstance();
        
        if (polyglot.main.Report.should_report(jif.Topics.jif, 4))
            polyglot.main.Report.report(4, "Processing declaration for " + li);
        
        // Equate the variable label with the declared label.
        VarLabel L = (VarLabel)li.label();
        Type t = decl.declType();
        
        if (ts.isLabeled(t)) {
            Label declaredLabel = ts.labelOfType(t);
            
            lc.constrain(new LabelConstraint(new NamedLabel("local_label", 
                                                            "inferred label of local var " + li.name(), 
                                                            L), 
                                                            LabelConstraint.EQUAL, 
                                                            new NamedLabel("PC", 
                                                                           "Information revealed by program counter being at this program point", 
                                                                           A.pc()).
                                                                           join("declared label of local var " + li.name(), declaredLabel), 
                                                                           A.labelEnv(),
                                                                           decl.position()) {
                public String msg() {
                    return "Declared label of local variable " + li.name() + 
                    " is incompatible with label constraints.";
                }
            }
            );
        }
        else {
            // Label the type, and update the node and local instance to reflect it.
            t = ts.labeledType(t.position(), t, L);
            li.setType(t);
            decl = decl.type(decl.type().type(t));
        }
        
        PathMap Xd;
        Expr init = null;
        
        if (decl.init() != null) {
            init = (Expr) lc.context(A).labelCheck(decl.init());
            
            if (init instanceof ArrayInit) {
                ((JifArrayInitExt)(init.ext())).labelCheckElements(lc, decl.type().type()); 
            }
            
            PathMap Xe = X(init);
            
            lc.constrain(new LabelConstraint(new NamedLabel("init.nv", 
                                                            "label of successful evaluation of initializing expression", 
                                                            Xe.NV()), 
                                                            LabelConstraint.LEQ, 
                                                            new NamedLabel("label of local variable " + li.name(), L),
                                                            A.labelEnv(),
                                                            init.position()) {
                public String msg() {
                    return "Label of local variable initializer not less " + 
                    "restrictive than the label for local variable " + 
                    li.name();
                }
                public String detailMsg() { 
                    return "More information is revealed by the successful " +
                    "evaluation of the intializing expression " +
                    "than is allowed to flow to " +
                    "the local variable " + li.name() + ".";
                }
                public String technicalMsg() {
                    return "Invalid assignment: NV of initializer is " +
                    "more restrictive than the declared label " +
                    "of local variable " + li.name() + ".";
                }                     
            }
            );
            Xd = Xe;
            
            //deal with the special cases "final label l = new label(...)"
            // and "final principal p = ..."
            if (li.flags().isFinal() && 
                    (ts.isLabel(li.type()) || ts.isPrincipal(li.type())) && 
                    JifUtil.isFinalAccessExprOrConst(ts, decl.init())) {
                
                if (ts.isLabel(li.type())) {
                    DynamicLabel dl = ts.dynamicLabel(decl.position(), JifUtil.varInstanceToAccessPath(li));                
                    Label rhs_label = JifUtil.exprToLabel(ts, decl.init(), lc.context().currentClass());
                    lc.context().addAssertionLE(dl, rhs_label);
                    lc.context().addAssertionLE(rhs_label, dl);
                }
                if (ts.isPrincipal(li.type())) {
                    DynamicPrincipal dp = ts.dynamicPrincipal(decl.position(), JifUtil.varInstanceToAccessPath(li));                
                    Principal rhs_principal = JifUtil.exprToPrincipal(ts, decl.init(), lc.context().currentClass());
                    lc.context().addActsFor(dp, rhs_principal);                    
                }
            }                            
            
            // Must check that the expression type is a subtype of the
            // declared type.  Most of this is done in typeCheck, but if
            // they are instantitation types, we must add constraints for
            // the labels.
            subtypeChecker.addSubtypeConstraints(lc, init.position(),
                                                 t, init.type());
        }
        else {
            // There is no PC label at field nodes.
            Xd = ts.pathMap();
            Xd = Xd.N(A.pc());
        }
        
        X = X.N(ts.notTaken()).join(Xd);
        
        decl = (LocalDecl) X(decl.init(init), X);
        //System.out.println("### X(decl): " + Xd + " " + node().position()); 
        return decl;
    }
}

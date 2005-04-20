package jif.extension;

import java.util.Iterator;

import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Cast;
import polyglot.ast.Expr;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.Position;
import polyglot.util.StringUtil;

/** The Jif extension of the <code>Cast</code> node. 
 * 
 *  @see polyglot.ast.Cast
 */
public class JifCastExt extends Jif_c 
{
    public JifCastExt(ToJavaExt toJava) {
        super(toJava);
    }
    
    public Node labelCheck(LabelChecker lc) throws SemanticException {
        Cast c = (Cast) node();
        
        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = (JifContext) lc.context();
        A = (JifContext) c.enterScope(A);
        
        Position pos = c.position();
        
        Expr e = (Expr) lc.context(A).labelCheck(c.expr());
        PathMap Xe = X(e);
        
        if (c.type().isReference()) {
            Xe = Xe.exc(Xe.NV(), ts.ClassCastException());
        }
        
        //Check type parameters
        if (ts.unlabel(c.type()) instanceof JifSubstType) {
            if (! (ts.unlabel(c.expr().type()) instanceof JifSubstType)) {
                throw new SemanticException("Cannot cast non-polymorphic "
                                            + "types to polymorphic types", pos);
            }
            
            // only allow downcasts/upcasts for parameterized types. Otherwise,
            // we have no way to control the instantiation of parameters.
            JifSubstType castType = (JifSubstType) ts.unlabel(c.type());
            JifSubstType exprType = (JifSubstType) ts.unlabel(c.expr().type());
            SubtypeChecker sc = new SubtypeChecker();
            sc.addSubtypeConstraints(lc, c.position(), exprType, castType);

            JifPolyType ctpoly = (JifPolyType) castType.base();
            JifPolyType etpoly = (JifPolyType) exprType.base();
            Iterator cpi = ctpoly.params().iterator();
            Iterator epi = etpoly.params().iterator();
            Iterator cai = castType.actuals().iterator();
            Iterator eai = exprType.actuals().iterator();
            
            int count = 0;
            while (cai.hasNext() && eai.hasNext()) {
                count++;
                ParamInstance cp = (ParamInstance) cpi.next();
                ParamInstance ep = (ParamInstance) epi.next();
                final Param ca = (Param) cai.next();
                final Param ea = (Param) eai.next();
                
                if (cp.isInvariantLabel() && ep.isCovariantLabel()) 
                    throw new SemanticException( 
                                                "Can not cast an invariant label parameter to " +
                                                "a covariant label parameter.", pos);
                
                if (cp.isInvariantLabel() && ep.isInvariantLabel()) { 
                    lc.constrain(new LabelConstraint(
                                                     new NamedLabel("cast_param_"+count, 
                                                                    "instantiation of " + StringUtil.nth(count) + 
                                                                    " param in cast type " + castType, 
                                                                    (Label)ca), 
                                                                    LabelConstraint.EQUAL, 
                                                                    new NamedLabel("expr_param_"+count, 
                                                                                   "instantiation of " + StringUtil.nth(count) + 
                                                                                   " param in expression type " + exprType, 
                                                                                   (Label)ea),
                                                                                   A.labelEnv(),
                                                                                   pos) {
                        public String msg() {
                            return "Cast an invariant label parameter " +
                            "to a different label.";
                        }
                        public String detailMsg() { 
                            return "The label parameter " + ea.toString() +
                            "was cast to the label parameter " + 
                            ca.toString() + ". These label " + 
                            "parameters are meant to be " + 
                            "invariant, and so the cast is not " +
                            "allowed.";
                        }
                        
                    }
                    );
                    
                }
                else if (cp.isPrincipal()) {
                    if (!A.actsFor((Principal)ca, (Principal)ea) ||
                            ! A.actsFor((Principal)ea, (Principal)ca) ) 
                        throw new SemanticException(
                                                    "Principals must be equivalent.", pos);		    
                }
                else {
                    // the two parameters are covariant labels
                    lc.constrain(new LabelConstraint(
                                                     new NamedLabel("expr_param_"+count, 
                                                                    "instantiation of " + StringUtil.nth(count) + 
                                                                    " param in expression type " + exprType, 
                                                                    (Label)ea),
                                                                    LabelConstraint.LEQ, 
                                                                    new NamedLabel("cast_param_"+count, 
                                                                                   "instantiation of " + StringUtil.nth(count) + 
                                                                                   " param in cast type " + castType, 
                                                                                   (Label)ca), 
                                                                                   A.labelEnv(),
                                                                                   pos) {
                        public String msg() {
                            return "Cast a label parameter " +
                            "to a less restrictive label.";
                        }
                        public String detailMsg() { 
                            return "The covariant label parameter " + ea.toString() +
                            "was cast to the less restrictive " +
                            "label parameter " + ca.toString() + 
                            ". This unsafe downcast may leak " +
                            "information, and appropriate " +
                            "declassify expressions should be " +
                            "used instead.";
                        }
                    }
                    );
                }
            }
            
            if (cai.hasNext() || eai.hasNext()) 
                throw new SemanticException(
                                            "The cast type and the expression type have different" +
                                            " number of parameters.", pos);
        }
        
        return X(c.expr(e), Xe);
    }
}

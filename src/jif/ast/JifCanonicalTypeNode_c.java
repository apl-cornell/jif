package jif.ast;

import java.util.*;

import jif.extension.LabelTypeCheckUtil;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.JifTypeChecker;
import polyglot.ast.CanonicalTypeNode_c;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.types.ClassType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.TypeChecker;


/**
 * A <code>JifCanonicalTypeNode</code> is a type node for a canonical type in Polyj.
 */
public class JifCanonicalTypeNode_c extends CanonicalTypeNode_c implements JifCanonicalTypeNode {
    public JifCanonicalTypeNode_c(Position pos, Type type) {
        super(pos, type);
    }

    public boolean isDisambiguated() {
        return true;
    }    
    
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        if (!this.type().isCanonical()) {
            // type should be canonical by the time we start typechecking.
            throw new InternalCompilerError(this.type() + " is not canonical.", this.position);
        }

        TypeNode tn = (TypeNode) super.typeCheck(tc);
        
        JifTypeSystem ts = (JifTypeSystem) tn.type().typeSystem();
        Type t = ts.unlabel(tn.type());
        
        if (t instanceof JifPolyType && !((JifPolyType)t).params().isEmpty()) {
            // the type is missing parameters
            
            JifPolyType jpt = (JifPolyType)t;
            
            JifTypeChecker jtc = (JifTypeChecker)tc;
            boolean inferred = false;
            if (jtc.inferClassParameters()) {
                inferred = true;
                
                // infer the class parameters by parameterizing the type with
                // label variables.
                Map varSubst = new LinkedHashMap();

                for (Iterator iter = jpt.params().iterator(); iter.hasNext();) {
                    ParamInstance pi = (ParamInstance)iter.next();
                    if (pi.isLabel()) {
                        Label l = ts.freshLabelVariable(tn.position(), pi.name()+"_inferred", "Inferred label parameter");
                        varSubst.put(pi, l);
                    }
                    else {
                        // XXX
                        // cannot currently infer principal params
                        inferred = false;
                        break;
                    }
                }                
                t = (ClassType)ts.subst(jpt, varSubst);
                
                // update the typenode.
                if (ts.isLabeled(tn.type())) {
                    tn = tn.type(ts.labeledType(tn.type().position(), 
                                                t,
                                                ts.labelOfType(tn.type())));                    
                }
                else {
                    tn = tn.type(t);
                }
            }
            
            if (!inferred) {
                throw new SemanticDetailedException(
                        "Parameterized type " + t + " is uninstantiated",
                        "The type " + t + " is a parameterized type, " +
                        		"and must be provided with parameters " +
                        		"to instantiate it. Jif prevents the use of " +
                        		"uninstantiated parameterized types.",
                                            position());
            }
        }

        // typecheck the type, make sure principal parameters are instantiated 
        // with principals, label parameters with labels.
        LabelTypeCheckUtil ltcu = ((JifTypeSystem)tc.typeSystem()).labelTypeCheckUtil(); 
        ltcu.typeCheckType(tc, t);
        
        return tn;
        
    }
}
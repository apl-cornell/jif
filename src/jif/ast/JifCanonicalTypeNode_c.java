package jif.ast;

import jif.extension.LabelTypeCheckUtil;
import jif.types.JifPolyType;
import jif.types.JifTypeSystem;
import jif.types.SemanticDetailedException;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl.ast.CanonicalTypeNode_c;
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
            throw new SemanticDetailedException(
                    "Parameterized type " + t + " is uninstantiated",
                    "The type " + t + " is a parameterized type, " +
                    		"and must be provided with parameters " +
                    		"to instantiate it. Jif prevents the use of" +
                    		"uninstantiated parameterized types.",
                                        position());
        }

        // typecheck the type, make sure principal parameters are instantiated 
        // with principals, label parameters with labels.
        LabelTypeCheckUtil.typeCheckType(tc, t);
        
        return tn;
        
    }
}
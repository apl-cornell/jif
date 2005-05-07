package jif.ast;

import jif.extension.LabelTypeCheckUtil;
import jif.types.JifPolyType;
import jif.types.JifTypeSystem;
import polyglot.ast.Node;
import polyglot.ast.TypeNode;
import polyglot.ext.jl.ast.CanonicalTypeNode_c;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.Position;
import polyglot.visit.TypeChecker;


/**
 * A <code>JifCanonicalTypeNode</code> is a type node for a canonical type in Polyj.
 */
public class JifCanonicalTypeNode_c extends CanonicalTypeNode_c implements JifCanonicalTypeNode {
    public JifCanonicalTypeNode_c(Position pos, Type type) {
        super(pos, type);
    }
    
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        TypeNode tn = (TypeNode) super.typeCheck(tc);
        
        JifTypeSystem ts = (JifTypeSystem) tn.type().typeSystem();
        Type t = ts.unlabel(tn.type());
        
        if (t instanceof JifPolyType) {
            throw new SemanticException("Parameterized type is uninstantiated",
                                        position());
        }

        // typecheck the type, make sure principal parameters are instantiated 
        // with principals, label parameters with labels.
        LabelTypeCheckUtil.typeCheckType(tc, t);
        
        return tn;
        
    }
}
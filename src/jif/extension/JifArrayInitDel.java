package jif.extension;

import jif.types.JifTypeSystem;
import polyglot.ast.ArrayInit;
import polyglot.ast.Node;
import polyglot.types.ArrayType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.visit.TypeChecker;


public class JifArrayInitDel extends JifJL_c
{
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        ArrayInit ai = (ArrayInit)super.typeCheck(tc);
        if (ai.type().isArray()) {
            // strip off the label of the base type, and replace them with variables
            JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();            
            ai = (ArrayInit)ai.type(relabelBaseType(ai.type().toArray(), ts));
            
        }
        return ai;
    }

    private ArrayType relabelBaseType(ArrayType type, JifTypeSystem ts) {
        Type base = ts.unlabel(type.base());
        if (base.isArray()) {
            base = relabelBaseType(base.toArray(), ts);
        }
        base = ts.labeledType(base.position(), base, ts.freshLabelVariable(base.position(), "array_base", "label of base type of array"));
        return type.base(base);
    }    
}

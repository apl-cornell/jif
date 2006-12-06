package jif.extension;

import jif.types.JifTypeSystem;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.types.ArrayType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.visit.TypeChecker;


public class JifNewArrayDel extends JifJL_c
{
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        NewArray na = (NewArray)super.typeCheck(tc);
        if (na.type().isArray()) {
            // strip off the label of the base type, and replace them with variables
            JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();            
            na = (NewArray)na.type(relabelBaseType(na.type().toArray(), ts));
            
        }
        return na;
    }

    private ArrayType relabelBaseType(ArrayType type, JifTypeSystem ts) {
        // replace the array type with a const array type.
        type = ts.constArrayOf(type.position(), type.base(), 1, true);
        
        Type base = ts.unlabel(type.base());
        if (base.isArray()) {

            base = relabelBaseType(base.toArray(), ts);
        }
        base = ts.labeledType(base.position(), base, ts.freshLabelVariable(base.position(), "array_base", "label of base type of array"));
        return type.base(base);
    }    
}

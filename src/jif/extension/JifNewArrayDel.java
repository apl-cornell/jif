package jif.extension;

import jif.types.JifTypeSystem;
import polyglot.ast.NewArray;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.visit.TypeChecker;


public class JifNewArrayDel extends JifJL_c
{
    public Node typeCheck(TypeChecker tc) throws SemanticException {
        NewArray na = (NewArray)super.typeCheck(tc);
        if (na.type().isArray()) {
            // strip off the label of the base type, and replace them with variables, and
            // replace the array types with array types that are both const and non-const
            JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();            
            na = (NewArray)na.type(JifArrayInitDel.relabelBaseType(na.type().toArray(), ts));
            
        }
        return na;
    }
}

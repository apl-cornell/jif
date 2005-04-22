package jif.extension;

import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import polyglot.ast.Cast;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>Cast</code> node. 
 * 
 *  @see polyglot.ext.jl.ast.Cast_c
 */
public class JifCastDel extends JifJL_c
{
    public JifCastDel() { }
    

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        // prevent casting to arrays of parameterized types
        Cast c = (Cast)this.node();
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
        Type castType = ts.unlabel(c.castType().type());
        
        if (castType.isArray()) {
            while (castType.isArray()) {
                castType = castType.toArray().base();
            }
            if (castType instanceof JifSubstType) {
                throw new SemanticException("Jif does not currently support casts to an array of a parameterized type.", c.position());
            }
        }
        return super.typeCheck(tc);
    }
}

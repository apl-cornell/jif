package jif.extension;

import java.util.ArrayList;
import java.util.List;

import jif.types.*;
import polyglot.ast.Cast;
import polyglot.ast.Node;
import polyglot.types.*;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>Cast</code> node.
 *
 *  @see polyglot.ext.jl.ast.Cast_c
 */
public class JifCastDel extends JifJL_c
{
    public JifCastDel() { }

    private boolean isToSubstJifClass = false;

    public boolean isToSubstJifClass() { return this.isToSubstJifClass; }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        // prevent casting to arrays of parameterized types
        Cast c = (Cast)this.node();
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
        Type castType = c.castType().type();

        if (ts.isLabeled(castType)) {
            throw new SemanticException("Cannot cast to a labeled type.", c.position());
        }

        if (!ts.isJifClass(castType)) {
            if ((castType instanceof JifSubstType && ((JifSubstType)castType).entries().hasNext()) ||
                (castType instanceof JifPolyType && !((JifPolyType)castType).params().isEmpty()))                    
            throw new SemanticException("Cannot cast to a parameterized " + 
                                        "Java class, since Java classes do " +
                                        "not represent the parameters at runtime.", 
                                        c.position());
        }
        
        if (castType.isArray()) {
            while (castType.isArray()) {
                castType = castType.toArray().base();
            }
            if (castType instanceof JifSubstType && ((JifSubstType)castType).entries().hasNext()) {
                throw new SemanticException("Jif does not currently support casts to an array of a parameterized type.", c.position());
            }
        }

        this.isToSubstJifClass = (castType instanceof JifSubstType && ((JifSubstType)castType).entries().hasNext());

        LabelTypeCheckUtil.typeCheckType(tc, castType);
        return super.typeCheck(tc);
    }
    
    public List throwTypes(TypeSystem ts) {
        List ex = new ArrayList(super.throwTypes(ts));
        Cast c = (Cast)this.node();
        if (c.castType().type() instanceof JifClassType) {
            ex.addAll(LabelTypeCheckUtil.throwTypes((JifClassType)c.castType().type(), 
                                                    (JifTypeSystem)ts));
        }
        return ex;
    }
}

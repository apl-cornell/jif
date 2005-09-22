package jif.extension;

import java.util.*;

import jif.types.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>Cast</code> node.
 *
 *  @see polyglot.ext.jl.ast.Cast_c
 */
public class JifCastDel extends JifJL_c implements JifPreciseClassDel
{
    public JifCastDel() { }

    private Set exprPreciseClasses = null;
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

        if (!ts.isParamsRuntimeRep(castType)) {
            if ((castType instanceof JifSubstType && ((JifSubstType)castType).entries().hasNext()) ||
                (castType instanceof JifPolyType && !((JifPolyType)castType).params().isEmpty()))                    
            throw new SemanticException("Cannot cast to " + castType +
                                        ", since it does " +
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
        Cast c = (Cast)this.node();

        List ex = new ArrayList(super.throwTypes(ts));
        if (!throwsClassCastException()) {
            ex.remove(ts.ClassCastException());            
        }
        if (c.castType().type() instanceof JifClassType) {
            ex.addAll(LabelTypeCheckUtil.throwTypes((JifClassType)c.castType().type(), 
                                                    (JifTypeSystem)ts));
        }
        return ex;
    }
    
    
    public boolean throwsClassCastException() {
        Cast c = (Cast)this.node();
        Type castType = c.castType().type();
        if (exprPreciseClasses != null) {
            for (Iterator iter = exprPreciseClasses.iterator(); iter.hasNext(); ) {
                Type t = (Type)iter.next();
                if (typeCastGuaranteed(castType, t)) {
                    return false;
                }
            }
        }
        return !typeCastGuaranteed(castType, c.expr().type());
    }

    /**
     * Will casting from exprType to castType always succeed?
     */
    private static boolean typeCastGuaranteed(Type castType, Type exprType) {
        if (castType.equals(exprType)) {
            return true;
        }
        if (castType instanceof JifClassType &&
                 SubtypeChecker.polyTypeForClass((JifClassType)castType).params().isEmpty()) {
            // cast type is not parameterized.

            // if the expr is definitely a subtype of the 
            // cast type, no class cast exception will be throw.
            if (castType.typeSystem().isSubtype(exprType, castType)) {
                return true;
            }
        }        
        return false;
    }
    /**
     * 
     */
    public Expr getPreciseClassExpr() {
        return ((Cast)node()).expr();
    }
    /**
     * 
     */
    public void setPreciseClass(Set preciseClasses) {
        this.exprPreciseClasses = preciseClasses;
    }
}

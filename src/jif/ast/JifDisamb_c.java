package jif.ast;

import jif.types.JifParsedPolyType;
import jif.types.JifTypeSystem;
import jif.types.PrincipalInstance;
import polyglot.ast.Node;
import polyglot.ast.Receiver;
import polyglot.ext.jl.ast.Disamb_c;
import polyglot.types.*;

/**
 * Utility class which is used to disambiguate ambiguous
 * AST nodes (Expr, Type, Receiver, Qualifier, Prefix).
 * 
 * We need to override the default functionality of <code>Disamb_c</code>
 * to deal with using the correct instantiations of polymorphic classes.
 */
public class JifDisamb_c extends Disamb_c
{
    protected Node disambiguateVarInstance(VarInstance vi) throws SemanticException {
        Node n = super.disambiguateVarInstance(vi);
        if (n != null) {
            return n;
        }
        if (vi instanceof PrincipalInstance) {
            PrincipalInstance pi = (PrincipalInstance)vi;
            JifNodeFactory jnf = (JifNodeFactory)nf;
            return jnf.CanonicalPrincipalNode(pos, pi.principal());
        }
        return null;
    }

    /**
     * Override superclass functionality to avoid returning an 
     * uninstantiated polymorphic class.
     */
    protected Receiver makeMissingFieldTarget(FieldInstance fi) throws SemanticException {
        if (fi.flags().isStatic()) {
            JifTypeSystem jts = (JifTypeSystem)fi.typeSystem();
            Type container = fi.container(); 
            if (container instanceof JifParsedPolyType) {
                JifParsedPolyType jppt = (JifParsedPolyType)container;
                if (jppt.params().size() > 0) {
                    // return the "null instantiation" of the base type,
                    // to ensure that all TypeNodes contain either
                    // a JifParsedPolyType with zero params, or a 
                    // JifSubstClassType
                    return nf.CanonicalTypeNode(pos, jppt.instantiatedFrom().clazz());
                }
            }
        } 
        
        return super.makeMissingFieldTarget(fi);    
    }

}



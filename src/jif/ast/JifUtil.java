package jif.ast;

import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.ast.Field;
import polyglot.ast.Local;
import polyglot.ast.Node;
import polyglot.util.InternalCompilerError;

/**
 * An implementation of the <code>Jif</code> interface. 
 */
public class JifUtil
{
    // Some utility functions used to avoid casts.
    public static PathMap X(Node n) {
        Jif ext = (Jif) n.ext();
	return ext.del().X();
    }

    public static Node X(Node n, PathMap X) {
        Jif ext = (Jif) n.ext();
        return n.ext(ext.del().X(X));
    }

    /**
     * Return the Label that the expression expr represents. 
     */
    public static Label exprToLabel(JifTypeSystem ts, Expr expr) {
        if (expr instanceof Label) {
            return (Label)expr;
        }
        else if (expr instanceof LabelNode) {
            return ((LabelNode)expr).label();
        }
        return runtimeLabel(ts, expr);
    }
    /**
     * Return the Principal that the expression expr represents. 
     */
    public static Principal exprToPrincipal(JifTypeSystem ts, Expr expr) {
        if (expr instanceof Principal) {
            return (Principal)expr;
        }
        else if (expr instanceof PrincipalNode) {
            return ((PrincipalNode)expr).principal();
        }
        return runtimePrincipal(ts, expr);
    }

    /** Generates a dynamic label from expr. */
    public static Label runtimeLabel(JifTypeSystem ts, Expr expr) {
    if (expr instanceof NewLabel) {
        NewLabel nl = (NewLabel) expr;
        return nl.label().label();
    }
    if (expr instanceof Local) {
        Local local = (Local) expr;
        JifVarInstance jvi = (JifVarInstance) local.localInstance();
        return ts.dynamicLabel(jvi.position(), jvi.uid(), jvi.name(), jvi.label());
    }
    if (expr instanceof Field) {
        Field field = (Field) expr;
        JifVarInstance jvi = (JifVarInstance) field.fieldInstance();
        return ts.dynamicLabel(jvi.position(), jvi.uid(), jvi.name(), jvi.label()); 
    }
    
    return null;
    }
    
    /** Generates a dynamic principal from expr. */
    public static Principal runtimePrincipal(JifTypeSystem ts, Expr expr) {
    if (expr instanceof Local) {
        Local local = (Local) expr;
        JifVarInstance jvi = (JifVarInstance) local.localInstance();
        return ts.dynamicPrincipal(jvi.position(), jvi.uid(), 
            jvi.name(), jvi.label());
    }
    if (expr instanceof Field) {
        Field field = (Field) expr;
        JifVarInstance jvi = (JifVarInstance) field.fieldInstance();
        return ts.dynamicPrincipal(jvi.position(), jvi.uid(), 
            jvi.name(), jvi.label());   
    }
    
    return null;
    }    

}

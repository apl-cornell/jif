package jif.ast;

import jif.types.*;
import jif.types.label.*;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.types.*;
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
    
//    /**
//     * Return the Label that the expression expr represents. 
//     */
//    public static Label exprToLabel(JifTypeSystem ts, Expr expr) {
//        if (expr instanceof Label) {
//            return (Label)expr;
//        }
//        else if (expr instanceof LabelNode) {
//            return ((LabelNode)expr).label();
//        }
//        return runtimeLabel(ts, expr);
//    }
//    /**
//     * Return the Principal that the expression expr represents. 
//     */
//    public static Principal exprToPrincipal(JifTypeSystem ts, Expr expr) {
//        if (expr instanceof Principal) {
//            return (Principal)expr;
//        }
//        else if (expr instanceof PrincipalNode) {
//            return ((PrincipalNode)expr).principal();
//        }
//        return runtimePrincipal(ts, expr);
//    }
    
//    /** Generates a dynamic label from expr. */
//    public static Label runtimeLabel(JifTypeSystem ts, Expr expr) {
//        if (expr instanceof NewLabel) {
//            NewLabel nl = (NewLabel) expr;
//            return nl.label().label();
//        }
//        if (expr instanceof Local) {
//            Local local = (Local) expr;
//            JifLocalInstance jli = (JifLocalInstance) local.localInstance();
//            return ts.dynamicLabel(jli.position(), new AccessPathLocal(jli));
//        }
//        //@@@@Need to deal with final access path expressions generally
////        if (expr instanceof Field) {
////            Field field = (Field) expr;
////            JifVarInstance jvi = (JifVarInstance) field.fieldInstance();
////            return ts.dynamicLabel(jvi.position(), jvi.uid(), jvi.name(), jvi.label()); 
////        }
//        
//        return null;
//    }
//    
//    /** Generates a dynamic principal from expr. */
//    public static Principal runtimePrincipal(JifTypeSystem ts, Expr expr) {
//        if (expr instanceof Local) {
//            Local local = (Local) expr;
//            JifLocalInstance jli = (JifLocalInstance) local.localInstance();
//            return ts.dynamicPrincipal(jli.position(), varInstanceToAccessPath(jli));
//        }
//        //@@@@Need to deal with final access path expressions generally
////        if (expr instanceof Field) {
////            Field field = (Field) expr;
////            JifVarInstance jvi = (JifVarInstance) field.fieldInstance();
////            return ts.dynamicPrincipal(jvi.position(), jvi.uid(), 
////                                       jvi.name(), jvi.label());   
////        }
//        
//        return null;
//    }

    /**
     * @param vi
     * @return
     */
    public static AccessPath varInstanceToAccessPath(JifVarInstance vi) {
        if (vi instanceof LocalInstance) {
            if (((LocalInstance)vi).flags().isFinal()) {
                return new AccessPathLocal((LocalInstance)vi);
            }
        }
        else if (vi instanceof FieldInstance) {
            FieldInstance fi = (FieldInstance)vi;
            JifTypeSystem ts = (JifTypeSystem)fi.typeSystem();
            if (fi.flags().isFinal() && (ts.isLabel(fi.type()) || ts.isPrincipal(fi.type()))) {
                AccessPathRoot root;
                if (fi.flags().isStatic()) {
                    root = new AccessPathClass(fi.container().toClass());                    
                }
                else {
                    root = new AccessPathThis(fi.container().toClass());
                }
                return new AccessPathField(root, fi);
            }            
        }
        throw new InternalCompilerError("Unexpected var instance " + vi.getClass());
    }    

    /**
     * @return
     */
    public static AccessPath exprToAccessPath(Expr e, ReferenceType currentClass) {
        if (e instanceof Local) {
           Local l = (Local)e;
           if (l.localInstance().flags().isFinal()) {
               return new AccessPathLocal(l.localInstance());
           }
        }
        else if (e instanceof Field) {
            Field f = (Field)e;
            if (f.fieldInstance().flags().isFinal()) {
                Receiver target = f.target();
                if (target instanceof Expr) {
                    AccessPath prefix = exprToAccessPath((Expr)f.target(), f.fieldInstance().container());
                    return new AccessPathField(prefix, f.fieldInstance());
                }
                else {
                    throw new InternalCompilerError("Not currently supporting access paths for " + target.getClass().getName());
                }
            }
        }
        else if (e instanceof Special) {
            Special s = (Special)e;
            if (Special.THIS.equals(s.kind())) {
                return new AccessPathThis(currentClass.toClass());
            }
            else {
                throw new InternalCompilerError("Not currently supporting access paths for special of kind " + s.kind());
            }            
        }
        else if (e instanceof NewLabel) {
            NewLabel nl = (NewLabel)e;
            return new AccessPathConstant(nl.label().label());
        }
        else if (e instanceof PrincipalNode) {
            PrincipalNode pn = (PrincipalNode)e;
            return new AccessPathConstant(pn.principal());
        }
        throw new InternalCompilerError("Expression " + e + " not suitable for an access path");
    }        

    private static boolean isFinalAccessExpr(JifTypeSystem ts, Expr e) {
        if (e instanceof Local) {
            Local l = (Local)e;
            return l.localInstance().flags().isFinal();
        }
        if (e instanceof Field) {
            Field f = (Field)e;
            Flags flgs = f.flags();
            return flgs.isFinal() && 
                (flgs.isStatic() || 
                 isFinalAccessExpr(ts, (Expr)f.target()));
        }
        if (e instanceof Special) {
            return ((Special)e).kind() == Special.THIS;          
        }
        return false;
    }
    public static boolean isFinalAccessExprOrConst(JifTypeSystem ts, Expr e) {
        return isFinalAccessExpr(ts, e) || e instanceof NewLabel || e instanceof PrincipalNode;
    }
    public static Label exprToLabel(JifTypeSystem ts, Expr e, ReferenceType currentClass) {
        if (isFinalAccessExpr(ts, e)) {
            return ts.dynamicLabel(e.position(), exprToAccessPath(e, currentClass));
        }
        if (e instanceof NewLabel) {
            return ((NewLabel)e).label().label();
        }
        throw new InternalCompilerError("Expected a final access expression, or constant");
    }
    public static Principal exprToPrincipal(JifTypeSystem ts, Expr e, ReferenceType currentClass) {
        if (isFinalAccessExpr(ts, e)) {
            return ts.dynamicPrincipal(e.position(), exprToAccessPath(e, currentClass));
        }
        if (e instanceof PrincipalNode) {
            return ((PrincipalNode)e).principal();
        }
        throw new InternalCompilerError("Expected a final access expression, or constant");
    }

}

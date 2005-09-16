package jif.ast;

import jif.types.*;
import jif.types.label.*;
import jif.types.principal.Principal;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

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
    
    public static AccessPath varInstanceToAccessPath(VarInstance vi, Position pos) {
        if (vi instanceof LocalInstance) {
            if (((LocalInstance)vi).flags().isFinal()) {
                return new AccessPathLocal((LocalInstance)vi, vi.name(), pos);
            }
        }
        else if (vi instanceof FieldInstance) {
            FieldInstance fi = (FieldInstance)vi;
            JifTypeSystem ts = (JifTypeSystem)fi.typeSystem();
            if (fi.flags().isFinal() && (ts.isLabel(fi.type()) || ts.isPrincipal(fi.type()))) {
                AccessPathRoot root;
                if (fi.flags().isStatic()) {
                    root = new AccessPathClass(fi.container().toClass(), pos);                    
                }
                else {
                    root = new AccessPathThis(fi.container().toClass(), pos);
                }
                return new AccessPathField(root, fi, fi.name(), pos);
            }            
        }
        throw new InternalCompilerError("Unexpected var instance " + vi.getClass());
    }    

    public static AccessPath exprToAccessPath(Expr e, JifContext context) throws SemanticException {
        if (e instanceof Local) {
           Local l = (Local)e;
           return new AccessPathLocal(l.localInstance(), l.name(), e.position());
        }
        else if (e instanceof Field) {
            Field f = (Field)e;
            Receiver target = f.target();
            if (target instanceof Expr) {
  //              ReferenceType container = null;
//                if (f.isTypeChecked()) {
//                    container = f.fieldInstance().container();
                //}
                AccessPath prefix = exprToAccessPath((Expr)f.target(), context);
                return new AccessPathField(prefix, f.fieldInstance(), f.name(), f.position());
            }
            else if (target instanceof TypeNode && ((TypeNode)target).type().isClass()){
                AccessPath prefix = new AccessPathClass(((TypeNode)target).type().toClass(), target.position());
                return new AccessPathField(prefix, f.fieldInstance(), f.name(), f.position());
            }
            else {
                throw new InternalCompilerError("Not currently supporting access paths for targets of " + target.getClass());
            }
        }
        else if (e instanceof Special) {
            Special s = (Special)e;
            if (Special.THIS.equals(s.kind())) {
                if (context.currentClass() == null || context.inStaticContext()) {
                    throw new SemanticException("Cannot use \"this\" in this scope.", e.position());
                }
                return new AccessPathThis(context.currentClass(), s.position());
            }
            else {
                throw new InternalCompilerError("Not currently supporting access paths for special of kind " + s.kind());
            }            
        }
        else if (e instanceof LabelExpr) {
            LabelExpr le = (LabelExpr)e;
            return new AccessPathConstant(le.label().label(), le.position());
        }
        else if (e instanceof PrincipalNode) {
            PrincipalNode pn = (PrincipalNode)e;
            return new AccessPathConstant(pn.principal(), pn.position());
        }
        else if (e instanceof Cast) {
            return exprToAccessPath(((Cast)e).expr(), context);            
        }
        throw new SemanticDetailedException("Expression " + e + " not suitable for an access path.",
                                            "The expression " + e + " is not suitable for a final access " +
                                            "path. A final access path is an expression starting with either " +
                                            "\"this\" or a final local variable \"v\", followed by zero or more final field accesses. That is, " +
                                            "a final access path is either this.f1.f2....fn, or v.f1.f2.....fn, where v is a " +
                                            "final local variables, and each field f1 to fn is a final field.",
                                            e.position());                                        
    }        


    private static boolean isFinalAccessExpr(JifTypeSystem ts, Expr e) {
        if (e instanceof Local) {
            Local l = (Local)e;
            if (l.type() != null && l.type().isCanonical()) {
                return l.localInstance().flags().isFinal();
            }
            else {
                return true;
            }
        }
        if (e instanceof Field) {
            Field f = (Field)e;
            if (f.type() != null && f.type().isCanonical()) {
                Flags flgs = f.flags();
                return flgs.isFinal() && 
                    (flgs.isStatic() || 
                     isFinalAccessExpr(ts, (Expr)f.target()));
            }
            else {
                return true;
            }
        }
        if (e instanceof Special) {
            return ((Special)e).kind() == Special.THIS;          
        }
        return false;
    }
    public static boolean isFinalAccessExprOrConst(JifTypeSystem ts, Expr e) {
        return isFinalAccessExpr(ts, e) || 
            e instanceof LabelExpr || 
            e instanceof PrincipalNode ||
            (e instanceof Cast && isFinalAccessExpr(ts, ((Cast)e).expr()));
    }
    public static Label exprToLabel(JifTypeSystem ts, Expr e, JifContext context) throws SemanticException {
        if (e instanceof LabelExpr) {
            return ((LabelExpr)e).label().label();
        }
        if (isFinalAccessExpr(ts, e)) {
            return ts.dynamicLabel(e.position(), exprToAccessPath(e, context));
        }
        throw new InternalCompilerError("Expected a final access expression, or constant");
    }
    public static Principal exprToPrincipal(JifTypeSystem ts, Expr e, JifContext context) throws SemanticException {
        if (e instanceof PrincipalNode) {
            return ((PrincipalNode)e).principal();
        }
        if (e instanceof Cast) {
            return ts.dynamicPrincipal(e.position(), 
                                       exprToAccessPath(((Cast)e).expr(), context));            
        }
        if (isFinalAccessExpr(ts, e)) {
            return ts.dynamicPrincipal(e.position(), exprToAccessPath(e, context));
        }
        throw new InternalCompilerError("Expected a final access expression, or constant");
    }

    public static String accessPathDescrip(AccessPath path, String kind) {
        if (path.isUninterpreted()) {
            return "an uninterpreted dynamic " + kind;
        }
        return "dynamic " + kind + " represented by the final access path " + path;
    }
}

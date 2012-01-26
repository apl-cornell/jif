package jif.ast;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jif.JifScheduler;
import jif.types.*;
import jif.types.label.*;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.goals.Goal;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * An implementation of the <code>Jif</code> interface. 
 */
public class JifUtil
{
    // Some utility functions used to avoid casts.
    public static PathMap getPathMap(Node n) {
        Jif ext = jifExt(n);
        return ext.X();
    }
    
    public static Jif jifExt(Node n) {
        Ext ext = n.ext();
        while (ext != null && !(ext instanceof Jif)) {
            ext = ext.ext();
        }
        return (Jif)ext;
    }

    public static Node updatePathMap(Node n, PathMap X) {
        Jif ext = jifExt(n);
        return updateJifExt(n, ext.X(X));
    }
    
    private static Node updateJifExt(Node n, Jif jif) {
        return n.ext(updateJifExt(n.ext(), jif));
    }
    private static Ext updateJifExt(Ext e, Jif jif) {
        if (e instanceof Jif) return jif;
        if (e == null) return e;
        return e.ext(updateJifExt(e.ext(), jif));
    }

    public static AccessPath varInstanceToAccessPath(VarInstance vi, Position pos) throws SemanticException {
        return varInstanceToAccessPath(vi, vi.name(), pos);
    }
    public static AccessPath varInstanceToAccessPath(VarInstance vi, String name, Position pos) throws SemanticException {
        if (!vi.flags().isFinal()) {
            throw new SemanticException("Only final fields and final local variables may be used as access paths.", pos);
        }
        if (vi instanceof LocalInstance) {
            return new AccessPathLocal((LocalInstance)vi, name, pos);
        }
        else if (vi instanceof FieldInstance) {
            FieldInstance fi = (FieldInstance)vi;
            AccessPathRoot root;
            if (fi.flags().isStatic()) {
                root = new AccessPathClass(fi.container().toClass(), pos);                    
            }
            else {
                root = new AccessPathThis(fi.container().toClass(), pos);
            }
            return new AccessPathField(root, fi, name, pos);
        }
        throw new InternalCompilerError("Unexpected var instance " + vi.getClass());
    }    

    public static AccessPath exprToAccessPath(Expr e, JifContext context) throws SemanticException {
        Type expectedType = null;
        if (e != null && e.type() != null && !e.type().isNull()) {
            expectedType = e.type();
        }
        return exprToAccessPath(e, expectedType, context);
    }
    public static AccessPath exprToAccessPath(Expr e, Type expectedType, JifContext context) throws SemanticException {
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
                AccessPath prefix = exprToAccessPath((Expr)f.target(), null, context);
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
            } /* 
            else if (Special.SUPER.equals(s.kind())) {
                if(context.currentClass() == null || context.inStaticContext() || !context.inCode()) {
                    throw new SemanticException("Cannot use \"super\" in this scope.", e.position());
                } else {
                    // We are not in a constructor now - using super is safe
                    return new AccessPathThis((ClassType) context.currentClass().superType(), s.position());
                }
            }
              */
            else {
                throw new InternalCompilerError("Not currently supporting access paths for special of kind " + s.kind());
            }            
        }
        else if (e instanceof LabelExpr) {
            LabelExpr le = (LabelExpr)e;
            return new AccessPathConstant(le.label().label(), le.type(), le.position());
        }
        else if (e instanceof PrincipalNode) {
            PrincipalNode pn = (PrincipalNode)e;
            return new AccessPathConstant(pn.principal(), pn.type(), pn.position());
        }
        else if (e instanceof NullLit && expectedType != null && 
                context.typeSystem().isImplicitCastValid(expectedType, 
                                                         ((JifTypeSystem)context.typeSystem()).PrincipalType())) {
            JifTypeSystem ts = (JifTypeSystem)context.typeSystem();
            Principal bot = ts.bottomPrincipal(e.position());
            return new AccessPathConstant(bot, ts.PrincipalType(), e.position());
        }
        else if (e instanceof Cast) {
            return exprToAccessPath(((Cast)e).expr(), expectedType, context);            
        }
        else if (e instanceof DowngradeExpr) {
            return exprToAccessPath(((DowngradeExpr)e).expr(), expectedType, context);            
        }
        throw new SemanticDetailedException("Expression " + e + " not suitable for an access path.",
                                            "The expression " + e + " is not suitable for a final access " +
                                            "path. A final access path is an expression starting with either " +
                                            "\"this\" or a final local variable \"v\", followed by zero or more final field accesses. That is, " +
                                            "a final access path is either this.f1.f2....fn, or v.f1.f2.....fn, where v is a " +
                                            "final local variables, and each field f1 to fn is a final field.",
                                            e.position());                                        
    }        

    // Process Final Access Paths that are reachable from fi
    @SuppressWarnings("unused")
    public static void processFAP(VarInstance fi,
            AccessPath path,
            JifContext A,
            JifTypeSystem ts,
            LabelChecker lc,
            Set<ClassType> visited)
    throws SemanticException {

        // final fields could be the root of a final access path. just check.
        if (fi.flags().isFinal()) {
            ReferenceType rt = fi.type().toReference();
            if (!(rt instanceof ClassType)) return;
            ClassType ct = (ClassType) rt;
            if (visited.contains(ct)) return;
            visited.add(ct);
            if (ct == null || ct.fields() == null) return;
            for (Iterator it = ct.fields().iterator(); it.hasNext();) {
                JifFieldInstance jfi = (JifFieldInstance) it.next();
                if (jfi.flags().isFinal()) {
                    AccessPathField path2 = new AccessPathField(path, jfi, jfi.name(), jfi.position());
                    // if it is static and is the end of a final access path and has an initializer
                    // TODO Could use isFinalAccessExprOrConst instead of restricting to isStatic and hasInitializer
                    Param init2 = jfi.initializer();
                    if (
//                            jfi.flags().isStatic() &&
                            jfi.hasInitializer()) {
                        if (ts.isLabel(jfi.type())) {
                            Label dl = ts.dynamicLabel(jfi.position(), path2);                
                            Label rhs_label = (Label) init2;
                            if (rhs_label == null) {
                                throw new InternalCompilerError("FinalParams has not run yet");
                                // label checking has not been done on ct yet
//                                JifScheduler sched = (JifScheduler) lc.job().extensionInfo().scheduler();
//                                ParsedClassType pct = (ParsedClassType) ct;
//                                Goal g = sched.LabelsChecked(pct.job());
//                                throw new MissingDependencyException(g);
                            }
                            A.addDefinitionalAssertionEquiv(dl, rhs_label, true);
                            continue;
                        }
                        else if (ts.isImplicitCastValid(jfi.type(), ts.PrincipalType())) {
                            DynamicPrincipal dp = ts.dynamicPrincipal(jfi.position(), path2);                
                            Principal rhs_principal = (Principal) init2;
                            if (rhs_principal == null) {
                                throw new InternalCompilerError("FinalParams has not run yet");
                                // label checking has not been done on ct yet
//                                JifScheduler sched = (JifScheduler) lc.job().extensionInfo().scheduler();
//                                ParsedClassType pct = (ParsedClassType) ct;
//                                Goal g = sched.LabelsChecked(pct.job());
//                                throw new MissingDependencyException(g);
                            }
                            A.addDefinitionalEquiv(dp, rhs_principal);
                            continue;
                        } else {
                            // If the field is not a label or a principal, no need to store the initializer
                            jfi.setInitializer(null);
                        }
                       
                    }
                    // this field could be part of a final access path
                    processFAP(jfi, path2, A, ts, lc, visited);

                }
            }
        }

    }
    
    public static void processFAP(VarInstance fi, 
            AccessPath path,
            JifContext A, 
            JifTypeSystem ts,
            LabelChecker lc) throws SemanticException {
        Set<ClassType> visited = new HashSet<ClassType>();
        processFAP(fi, path, A, ts, lc, visited);
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
                     (f.target() instanceof Expr && isFinalAccessExpr(ts, (Expr)f.target())));
            }
            else {
                return true;
            }
        }
        if (e instanceof Special) {
            return ((Special)e).kind() == Special.THIS;
        }
        if (e instanceof Cast) {
            return isFinalAccessExpr(ts, ((Cast)e).expr());        
        }
        if (e instanceof DowngradeExpr) {
            return isFinalAccessExpr(ts, ((DowngradeExpr)e).expr());        
        }
        return false;
    }
    public static boolean isFinalAccessExprOrConst(JifTypeSystem ts, Expr e) {
        Type expectedType = null;
        if (e != null && e.type() != null && !e.type().isNull()) {
            expectedType = e.type();
        }
        return isFinalAccessExprOrConst(ts, e, expectedType);
        
    }
    public static boolean isFinalAccessExprOrConst(JifTypeSystem ts, Expr e, Type expectedType) {
        return isFinalAccessExpr(ts, e) || 
            e instanceof LabelExpr || 
            e instanceof PrincipalNode ||
           (e instanceof Cast && isFinalAccessExprOrConst(ts, ((Cast)e).expr())) ||
           (e instanceof DowngradeExpr && isFinalAccessExprOrConst(ts, ((DowngradeExpr)e).expr())) ||
           (e instanceof NullLit &&
                   expectedType != null && 
                   ts.isImplicitCastValid(expectedType, ts.PrincipalType())) 
                   /*|| (e instanceof Special && ((Special)e).kind() == Special.SUPER)*/ ;
    }
    public static Label exprToLabel(JifTypeSystem ts, Expr e, JifContext context) throws SemanticException {
        if (e instanceof LabelExpr) {
            return ((LabelExpr)e).label().label();
        }
        if (e instanceof DowngradeExpr) {
            return exprToLabel(ts, ((DowngradeExpr)e).expr(), context);            
        }
        if (isFinalAccessExpr(ts, e)) {
            return ts.dynamicLabel(e.position(), exprToAccessPath(e, ts.LabelType(), context));
        }
        throw new InternalCompilerError("Expected a final access expression, or constant");
    }
    public static Principal exprToPrincipal(JifTypeSystem ts, Expr e, JifContext context) throws SemanticException {
        if (e instanceof PrincipalNode) {
            return ((PrincipalNode)e).principal();
        }
        if (e instanceof PrincipalExpr) {
            return ((PrincipalExpr)e).principal().principal();
        }
        if (e instanceof Cast) {
            return exprToPrincipal(ts, ((Cast)e).expr(), context);            
        }
        if (e instanceof DowngradeExpr) {
            return exprToPrincipal(ts, ((DowngradeExpr)e).expr(), context);            
        }
        if (e instanceof NullLit) {
            return ts.bottomPrincipal(e.position());
        }
        if (isFinalAccessExpr(ts, e)) {
            return ts.dynamicPrincipal(e.position(), exprToAccessPath(e, ts.PrincipalType(), context));
        }
        throw new InternalCompilerError("Expected a final access expression, or constant");
    }

    public static String accessPathDescrip(AccessPath path, String kind) {
        if (path.isUninterpreted()) {
            return "an uninterpreted dynamic " + kind;
        }
        return "dynamic " + kind + " represented by the final access path " + path;
    }

    /**
     * Returns the "effective expression" for expr. That is, it strips
     * away casts and downgrade expressions.
     */
    public static Expr effectiveExpr(Expr expr) {
        if (expr instanceof Cast) {
            return effectiveExpr(((Cast)expr).expr());
        }
        if (expr instanceof DowngradeExpr) {
            return effectiveExpr(((DowngradeExpr)expr).expr());            
        }
        return expr;
    }
}

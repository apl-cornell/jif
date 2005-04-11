package jif.ast;

import java.util.*;

import jif.types.*;
import jif.types.label.*;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;

/**
 * TODO: DOCUMENTATION
 */
public class JifInstantiator
{
    public static Label instantiate(Label L, List formalArgLabels, List actualArgLabels, List actualArgExprs, ClassType callerClass) {
        if (formalArgLabels.size() != actualArgLabels.size() || 
                (actualArgExprs != null && formalArgLabels.size() != actualArgExprs.size())) {
            throw new InternalCompilerError("Inconsistent sized lists of args");
        }
        if (L != null) {
            // go through each formal arglabel, and replace it appropriately...
            Iterator iArgLabels = formalArgLabels.iterator();
            Iterator iActualArgLabels = actualArgLabels.iterator();
            Iterator iActualArgExprs = null;
            if (actualArgExprs != null) {
                iActualArgExprs = actualArgExprs.iterator();
            }
            while (iArgLabels.hasNext()) {
                ArgLabel formalArgLbl = (ArgLabel)iArgLabels.next();
                Label actualArgLbl = (Label)iActualArgLabels.next();
                L = L.subst(formalArgLbl.formalInstance(), actualArgLbl);

                if (iActualArgExprs != null) {
                    Expr actualExpr = (Expr)iActualArgExprs.next();
                
                    if (actualExpr != null) {
                        L = L.subst((AccessPathRoot)JifUtil.varInstanceToAccessPath(formalArgLbl.formalInstance()), 
                                    JifUtil.exprToAccessPath(actualExpr, callerClass));
                    }
                }
            }
            if (iArgLabels.hasNext() || 
                    iActualArgLabels.hasNext() || 
                    (iActualArgExprs != null && iActualArgExprs.hasNext())) {
                throw new InternalCompilerError("Inconsistent arg lists");
            }
        }
        return L;
    }
    
    /**
     * replaces any signature ArgLabels in p with the appropriate label, and
     * replaces any signature ArgPrincipal with the appropriate prinicipal. 
     */        
    public static Principal instantiate(Principal p, List formalArgLabels, List actualArgExprs, ClassType callerClass) {
        if (formalArgLabels.size() != actualArgExprs.size()) {
            throw new InternalCompilerError("Inconsistent sized lists of args");
        }
        if (p != null) {
            // go through each formal arglabel, and replace it appropriately...
            Iterator iArgLabels = formalArgLabels.iterator();
            Iterator iActualArgExprs = actualArgExprs.iterator();
            while (iArgLabels.hasNext()) {
                ArgLabel formalArgLbl = (ArgLabel)iArgLabels.next();
                Expr actualExpr = (Expr)iActualArgExprs.next();
                
                if (actualExpr != null) {
                    p = p.subst((AccessPathRoot)JifUtil.varInstanceToAccessPath(formalArgLbl.formalInstance()), 
                            JifUtil.exprToAccessPath(actualExpr, callerClass));
                }
            }
            if (iArgLabels.hasNext() || iActualArgExprs.hasNext()) {
                throw new InternalCompilerError("Inconsistent arg lists");
            }
        }
        
        return p;   
    }

    /**     
     * TODO Documentation
     */
    public static Label subst(Label L) {
        if (L == null) return L;

        LabelInstantiator labelInstantiator = new LabelInstantiator(null, null);
        try {
            return L.subst(labelInstantiator);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException " +
                                            "during label substitution: " + e.getMessage(), L.position());
        }
    }
    
        /**
     * Replace this this access path with an appropraite access path for the
     * receiver expression
     * TODO Documentation
     */
    public static Label instantiate(Label L, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl) {
        if (L == null) return L;
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();

        LabelInstantiator labelInstantiator = new LabelInstantiator(receiverLbl, receiverType);
        try {
            L = L.subst(labelInstantiator);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException " +
                                            "during label substitution: " + e.getMessage(), L.position());
        }
        AccessPath receiverPath;
        if (JifUtil.isFinalAccessExprOrConst(ts, receiverExpr)) {
            receiverPath = JifUtil.exprToAccessPath(receiverExpr, A.currentClass());
        }
        else {
            receiverPath = new AccessPathUninterpreted(); // @@@@@Uninterpreted root?
        }
        
        if (receiverType.isClass()) {
            L = L.subst(new AccessPathThis(receiverType.toClass()), receiverPath);
        }
        return L;
    }

    public static Label instantiate(Label L, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl, List formalArgs, List actualArgLabels, List actualArgExprs) {
        L = instantiate(L, A, receiverExpr, receiverType, receiverLbl);
        return instantiate(L, formalArgs, actualArgLabels, actualArgExprs, A.currentClass());
    }

    /**
     * Replace this this access path with an appropraite access path for the
     * receiver expression
     * TODO Documentation
     */
    public static Principal instantiate(Principal p, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl) {
        if (p == null) return p;
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();

        LabelInstantiator labelInstantiator = new LabelInstantiator(receiverLbl, receiverType);
        try {
            p = p.subst(labelInstantiator);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException " +
                                            "during label substitution: " + e.getMessage(), p.position());
        }
        AccessPath receiverPath;
        if (JifUtil.isFinalAccessExprOrConst(ts, receiverExpr)) {
            receiverPath = JifUtil.exprToAccessPath(receiverExpr, A.currentClass());
        }
        else {
            receiverPath = new AccessPathUninterpreted(); // @@@@@Uninterpreted root?
        }
        
        if (receiverType.isClass()) {
            p = p.subst(new AccessPathThis(receiverType.toClass()), receiverPath);
        }
        return p;
    }
    public static Principal instantiate(Principal p, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl, List formalArgs, List actualArgExprs) {
        p = instantiate(p, A, receiverExpr, receiverType, receiverLbl);
        return instantiate(p, formalArgs, actualArgExprs, A.currentClass());
    }

    public static Type instantiate(Type t, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl, List formalArgs, List actualArgLabels, List actualArgExprs) throws SemanticException {
        t = instantiate(t, A, receiverExpr, receiverType, receiverLbl);
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        if (t instanceof ArrayType) {
            ArrayType at = (ArrayType)t;
            Type baseType = at.base();
            t = at.base(instantiate(baseType, A, receiverExpr, receiverType, receiverLbl, formalArgs, actualArgLabels, actualArgExprs));
        }
        
        if (ts.isLabeled(t)) {
            Label newL = instantiate(ts.labelOfType(t), A, receiverExpr, receiverType, receiverLbl, formalArgs, actualArgLabels, actualArgExprs);
            Type newT = instantiate(ts.unlabel(t), A, receiverExpr, receiverType, receiverLbl, formalArgs, actualArgLabels, actualArgExprs);
            t = ts.labeledType(t.position(), newT, newL);
        }

        if (t instanceof JifSubstType) {
            JifSubstType jit = (JifSubstType)t;
            Map newMap = new HashMap();
            boolean diff = false;
            for (Iterator i = jit.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = instantiate((Label)arg, A, receiverExpr, receiverType, receiverLbl, formalArgs, actualArgLabels, actualArgExprs);
                }
                else if (arg instanceof Principal) {
                    p = instantiate((Principal)arg, A, receiverExpr, receiverType, receiverLbl, formalArgs, actualArgExprs);
                }
                else {
                    throw new InternalCompilerError(
                        "Unexpected type for entry: "
                            + arg.getClass().getName());
                }
                newMap.put(e.getKey(), p);

                if (p != arg)
                    diff = true;
            }
            if (diff) {
                t = ts.subst(jit.base(), newMap);
            }
        }

        return t;
    }
    
    public static Type instantiate(Type t, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        if (t instanceof JifSubstType) {
            JifSubstType jit = (JifSubstType) t;
            Map newMap = new HashMap();
            boolean diff = false;
            for (Iterator i = jit.entries(); i.hasNext(); ) {
                Map.Entry e = (Map.Entry) i.next();
                Object arg = e.getValue();
                Param p;
                if (arg instanceof Label)
                    p = instantiate((Label)arg, A, receiverExpr, receiverType, receiverLbl);
                else {
                    p = instantiate((Principal)arg, A, receiverExpr, receiverType, receiverLbl);
                }
                
                newMap.put(e.getKey(), p);
                
                if (p != arg)
                    diff = true;
            }
            if (diff) {
                return ts.subst(jit.base(), newMap);
            }
        }
        if (t instanceof ArrayType) {
            ArrayType at = (ArrayType) t;
            Type baseType = at.base();
            return at.base(instantiate(baseType, A, receiverExpr, receiverType, receiverLbl));
        }
        if (ts.isLabeled(t)) {
            Label newL = instantiate(ts.labelOfType(t), A, receiverExpr, receiverType, receiverLbl);
            Type newT = instantiate(ts.unlabel(t), A, receiverExpr, receiverType, receiverLbl);
            return ts.labeledType(newT.position(), newT, newL);
        }
        
        return t;
    }    

    private static class LabelInstantiator extends LabelSubstitution {
        private Label thisLabel;
        private Label receiverLabel;
        private Type receiverType;
        protected LabelInstantiator(Label receiverLabel, Type receiverType) {
            if (receiverType != null && receiverType instanceof JifClassType) {
                this.thisLabel = ((JifClassType)receiverType).thisLabel();
                if (!(thisLabel instanceof CovariantThisLabel || thisLabel instanceof ThisLabel)) {
                    // if thisLabel is not a CovariantThisLabel or ThisLabel, then
                    // it is the result of a substitution (see JifSUbstClassType_c.thisLabel), 
                    // i.e., the ThisLabel or CovariantThisLabel has already been
                    // substituted, we don't need to.
                    // We really need to re-examine the param extension, tidy it up, and figure out
                    // what exactly the "this" parameter is.
                    this.thisLabel = null;
                }
            }
            this.receiverLabel = receiverLabel;
            this.receiverType = receiverType;
        }
        
        public Label substLabel(Label L) {
            Label result = L;
            if (this.receiverLabel != null && this.thisLabel != null) {
                if (this.thisLabel.equals(L)) {
                    result = receiverLabel;
                }
            }

            if (receiverType instanceof JifSubstType) {
                JifSubstType t = (JifSubstType)receiverType;
                result = ((JifSubst)t.subst()).substLabel(result);
            }

            return result;
        }

        public Principal substPrincipal(Principal p) {
            if (receiverType instanceof JifSubstType) {
                JifSubst subst = (JifSubst) ((JifSubstType)receiverType).subst();
                return subst.substPrincipal(p);
            }
            return p;
        }

    }
}

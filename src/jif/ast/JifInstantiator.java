package jif.ast;

import java.util.*;

import jif.types.*;
import jif.types.label.*;
import jif.types.principal.DynamicPrincipal;
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
                try {
                    L = L.subst(new LabelInstantiator(formalArgLbl, actualArgLbl));
                }
                catch (SemanticException e) {
                    throw new InternalCompilerError("Unexpected SemanticException " +
                                                    "during label substitution: " + e.getMessage(), L.position());
                }

                if (iActualArgExprs != null) {
                    Expr actualExpr = (Expr)iActualArgExprs.next();
                
                    if (actualExpr != null) {
                        try {
                            AccessPathRoot root = (AccessPathRoot)JifUtil.varInstanceToAccessPath(formalArgLbl.formalInstance(), actualExpr.position());
                            AccessPath target = JifUtil.exprToAccessPath(actualExpr, callerClass); 
                            L = L.subst(new AccessPathInstantiator(root, target));
                        }
                        catch (SemanticException e) {
                            throw new InternalCompilerError("Unexpected SemanticException " +
                                                            "during label substitution: " + e.getMessage(), L.position());
                        }
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
                    try {
                        AccessPathRoot root = (AccessPathRoot)JifUtil.varInstanceToAccessPath(formalArgLbl.formalInstance(), actualExpr.position());
                        AccessPath target = JifUtil.exprToAccessPath(actualExpr, callerClass); 
                        p = p.subst(new AccessPathInstantiator(root, target));
                    }
                    catch (SemanticException e) {
                        throw new InternalCompilerError("Unexpected SemanticException " +
                                                        "during label substitution: " + e.getMessage(), p.position());
                    }
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

        ThisLabelAndParamInstantiator labelInstantiator = new ThisLabelAndParamInstantiator(null, null);
        try {
            return L.subst(labelInstantiator);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException " +
                                            "during label substitution: " + e.getMessage(), L.position());
        }
    }
    
    public static Label instantiate(Label L, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl) {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        AccessPath receiverPath;
        if (JifUtil.isFinalAccessExprOrConst(ts, receiverExpr)) {
            receiverPath = JifUtil.exprToAccessPath(receiverExpr, A.currentClass());
        }
        else {
            receiverPath = new AccessPathUninterpreted(L.position()); 
        }
        return instantiate(L, A, receiverPath, receiverType, receiverLbl);
    }

        /**
     * Replace this this access path with an appropraite access path for the
     * receiver expression
     * TODO Documentation
     */
    public static Label instantiate(Label L, JifContext A, AccessPath receiverPath, ReferenceType receiverType, Label receiverLbl) {
        if (L == null) return L;
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();

        ThisLabelAndParamInstantiator labelInstantiator = new ThisLabelAndParamInstantiator(receiverLbl, receiverType);
        try {
            L = L.subst(labelInstantiator);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected SemanticException " +
                                            "during label substitution: " + e.getMessage(), L.position());
        }
        
        if (receiverType.isClass()) {            
            try {
                AccessPathRoot root = new AccessPathThis(receiverType.toClass(), L.position());
                L = L.subst(new AccessPathInstantiator(root, receiverPath));
            }
            catch (SemanticException e) {
                throw new InternalCompilerError("Unexpected SemanticException " +
                                                "during label substitution: " + e.getMessage(), L.position());
            }
        }
        return L;
    }

    public static Label instantiate(Label L, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl, List formalArgs, List actualArgLabels, List actualArgExprs) {
        L = instantiate(L, A, receiverExpr, receiverType, receiverLbl);
        return instantiate(L, formalArgs, actualArgLabels, actualArgExprs, A.currentClass());
    }
    public static Label instantiate(Label L, JifContext A, AccessPath receiverPath, ReferenceType receiverType, Label receiverLbl, List formalArgs, List actualArgLabels, List actualArgExprs) {
        L = instantiate(L, A, receiverPath, receiverType, receiverLbl);
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

        ThisLabelAndParamInstantiator labelInstantiator = new ThisLabelAndParamInstantiator(receiverLbl, receiverType);
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
            receiverPath = new AccessPathUninterpreted(p.position()); // @@@@@Uninterpreted root?
        }
        
        if (receiverType.isClass()) {
            try {
                AccessPathRoot root = new AccessPathThis(receiverType.toClass(), p.position());
                p = p.subst(new AccessPathInstantiator(root, receiverPath));
            }
            catch (SemanticException e) {
                throw new InternalCompilerError("Unexpected SemanticException " +
                                                "during label substitution: " + e.getMessage(), p.position());
            }
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

    private static class ThisLabelAndParamInstantiator extends LabelSubstitution {
        private Label receiverLabel;
        private Type receiverType;
        protected ThisLabelAndParamInstantiator(Label receiverLabel, Type receiverType) {
            this.receiverLabel = receiverLabel;
            this.receiverType = receiverType;            
        }
        
        public Label substLabel(Label L) {
            Label result = L;
            if (receiverLabel != null && result instanceof ThisLabel) {
                result = receiverLabel;
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
    private static class LabelInstantiator extends LabelSubstitution {
        private Label srcLabel;
        private Label trgLabel;
        protected LabelInstantiator(Label srcLabel, Label trgLabel) {
            this.srcLabel = srcLabel;
            this.trgLabel = trgLabel;
        }
        
        public Label substLabel(Label L) {
            if (srcLabel.equals(L)) {
                return trgLabel;
            }
            return L;
        }
    }

    private static class AccessPathInstantiator extends LabelSubstitution {
        private AccessPathRoot srcRoot;
        private AccessPath trgPath;
        protected AccessPathInstantiator(AccessPathRoot srcRoot, AccessPath trgPath) {
            this.srcRoot = srcRoot;
            this.trgPath = trgPath;
        }
        
        public Label substLabel(Label L) {            
            if (L instanceof DynamicLabel) {
                return ((DynamicLabel)L).subst(srcRoot, trgPath);
            }
            return L;
        }
        public Principal substPrincipal(Principal p) {
            if (p instanceof DynamicPrincipal) {
                return ((DynamicPrincipal)p).subst(srcRoot, trgPath);
            }
            return p;
        }
    }
    
}

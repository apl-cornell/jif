package jif.ast;

import java.util.*;

import jif.types.*;
import jif.types.label.*;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/**
 * TODO: DOCUMENTATION
 */
public class JifInstantiator
{
    private static Label instantiate(Label L, ReferenceType receiverType, List formalArgLabels, List actualArgLabels, List actualArgExprs, List actualParamLabels, JifContext callerContext) {
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
                
                    try {
                        Position pos = Position.COMPILER_GENERATED;
                        if (actualExpr != null) {
                            pos = actualExpr.position();
                        }
                        AccessPathRoot root = (AccessPathRoot)JifUtil.varInstanceToAccessPath(formalArgLbl.formalInstance(), pos);                            
                        AccessPath target = new AccessPathUninterpreted(pos);
                        if (actualExpr != null) {
                            target = JifUtil.exprToAccessPath(actualExpr, callerContext);
                        }
                        L = L.subst(new AccessPathInstantiator(root, target));
                    }
                    catch (SemanticException e) {
                        throw new InternalCompilerError("Unexpected SemanticException " +
                                                        "during label substitution: " + e.getMessage(), L.position());
                    }
                }
            }
            if (iArgLabels.hasNext() || 
                    iActualArgLabels.hasNext() || 
                    (iActualArgExprs != null && iActualArgExprs.hasNext())) {
                throw new InternalCompilerError("Inconsistent arg lists");
            }
            
            // also instantiate the param arg labels. They only occur in static methods
            // of parameterized classes, but no harm in always instantiating them.
            if (!actualParamLabels.isEmpty()) {
                // go through the formal params, and the actual param labels.
                JifTypeSystem ts = (JifTypeSystem)receiverType.typeSystem();
                JifSubstType jst = (JifSubstType)receiverType;
                JifPolyType jpt = (JifPolyType)jst.base();
                Iterator iFormalParams = jpt.params().iterator();
                Iterator iActualParamLabels = actualParamLabels.iterator();
                
                // go through each formal and actual param, and make substitutions.
                while (iActualParamLabels.hasNext()) {
                    Label actualParamLabel = (Label)iActualParamLabels.next();                    
                    ParamInstance pi = (ParamInstance)iFormalParams.next();
                    ArgLabel paramArgLabel = ts.argLabel(pi.position(), pi);
                    paramArgLabel.setUpperBound(ts.topLabel());
                    try {
                        L = L.subst(new LabelInstantiator(paramArgLabel, actualParamLabel));
                    }
                    catch (SemanticException e) {
                        throw new InternalCompilerError("Unexpected SemanticException " +
                                                        "during label substitution: " + e.getMessage(), L.position());
                    }                    
                }
                if (iActualParamLabels.hasNext() || iFormalParams.hasNext()) {
                    throw new InternalCompilerError("Inconsistent param lists");
                }
            }
        }
        return L;
    }
    
    /**
     * replaces any signature ArgLabels in p with the appropriate label, and
     * replaces any signature ArgPrincipal with the appropriate prinicipal. 
     */        
    private static Principal instantiate(Principal p, ReferenceType receiverType, List formalArgLabels, List actualArgExprs, List actualParamLabels, JifContext callerContext) {
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
                
                try {
                    Position pos = Position.COMPILER_GENERATED;
                    if (actualExpr != null) {
                        pos = actualExpr.position();
                    }
                    AccessPathRoot root = (AccessPathRoot)JifUtil.varInstanceToAccessPath(formalArgLbl.formalInstance(), pos);                            
                    AccessPath target = new AccessPathUninterpreted(pos);
                    if (actualExpr != null) {
                        target = JifUtil.exprToAccessPath(actualExpr, callerContext);
                    }
                    p = p.subst(new AccessPathInstantiator(root, target));
                }
                catch (SemanticException e) {
                    throw new InternalCompilerError("Unexpected SemanticException " +
                                                    "during label substitution: " + e.getMessage(), p.position());
                }
            }
            if (iArgLabels.hasNext() || iActualArgExprs.hasNext()) {
                throw new InternalCompilerError("Inconsistent arg lists");
            }
            
            // also instantiate the param arg labels. They only occur in static methods
            // of parameterized classes, but no harm in always instantiating them.
            if (!actualParamLabels.isEmpty()) {
                // go through the formal params, and the actual param labels.
                JifTypeSystem ts = (JifTypeSystem)receiverType.typeSystem();
                JifSubstType jst = (JifSubstType)receiverType;
                JifPolyType jpt = (JifPolyType)jst.base();
                Iterator iFormalParams = jpt.params().iterator();
                Iterator iActualParamLabels = actualParamLabels.iterator();
                
                // go through each formal and actual param, and make substitutions.
                while (iActualParamLabels.hasNext()) {
                    Label actualParamLabel = (Label)iActualParamLabels.next();
                    ParamInstance pi = (ParamInstance)iFormalParams.next();
                    ArgLabel paramArgLabel = ts.argLabel(pi.position(), pi);
                    paramArgLabel.setUpperBound(ts.topLabel());
                    try {
                        p = p.subst(new LabelInstantiator(paramArgLabel, actualParamLabel));
                    }
                    catch (SemanticException e) {
                        throw new InternalCompilerError("Unexpected SemanticException " +
                                                        "during label substitution: " + e.getMessage(), p.position());
                    }                    
                }
                if (iActualParamLabels.hasNext() || iFormalParams.hasNext()) {
                    throw new InternalCompilerError("Inconsistent param lists");
                }
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
    
    public static Label instantiate(Label L, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        AccessPath receiverPath;
        if (JifUtil.isFinalAccessExprOrConst(ts, receiverExpr)) {
            receiverPath = JifUtil.exprToAccessPath(receiverExpr, A);
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

    public static Label instantiate(Label L, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl, List formalArgs, List actualArgLabels, List actualArgExprs, List actualParamLabels) throws SemanticException {
        L = instantiate(L, A, receiverExpr, receiverType, receiverLbl);
        return instantiate(L, receiverType, formalArgs, actualArgLabels, actualArgExprs, actualParamLabels, A);
    }

    /**
     * Replace this this access path with an appropraite access path for the
     * receiver expression
     * TODO Documentation
     * @throws SemanticException
     */
    private static Principal instantiate(Principal p, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl) throws SemanticException {
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
            receiverPath = JifUtil.exprToAccessPath(receiverExpr, A);
        }
        else {
            receiverPath = new AccessPathUninterpreted(p.position());
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
    public static Principal instantiate(Principal p, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl, List formalArgs, List actualArgExprs, List actualParamLabels) throws SemanticException {
        p = instantiate(p, A, receiverExpr, receiverType, receiverLbl);
        return instantiate(p, receiverType, formalArgs, actualArgExprs, actualParamLabels, A);
    }

    public static Type instantiate(Type t, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl, List formalArgs, List actualArgLabels, List actualArgExprs, List actualParamLabels) throws SemanticException {
        t = instantiate(t, A, receiverExpr, receiverType, receiverLbl);
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        if (t instanceof ArrayType) {
            ArrayType at = (ArrayType)t;
            Type baseType = at.base();
            t = at.base(instantiate(baseType, A, receiverExpr, receiverType, receiverLbl, formalArgs, actualArgLabels, actualArgExprs, actualParamLabels));
        }
        
        if (ts.isLabeled(t)) {
            Label newL = instantiate(ts.labelOfType(t), A, receiverExpr, receiverType, receiverLbl, formalArgs, actualArgLabels, actualArgExprs, actualParamLabels);
            Type newT = instantiate(ts.unlabel(t), A, receiverExpr, receiverType, receiverLbl, formalArgs, actualArgLabels, actualArgExprs, actualParamLabels);
            t = ts.labeledType(t.position(), newT, newL);
        }
        Type ut = ts.unlabel(t);
        if (ut instanceof JifSubstType) {
            JifSubstType jit = (JifSubstType)ut;
            Map newMap = new HashMap();
            boolean diff = false;
            for (Iterator i = jit.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = instantiate((Label)arg, A, receiverExpr, receiverType, receiverLbl, formalArgs, actualArgLabels, actualArgExprs, actualParamLabels);
                }
                else if (arg instanceof Principal) {
                    p = instantiate((Principal)arg, A, receiverExpr, receiverType, receiverLbl, formalArgs, actualArgExprs, actualParamLabels);
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
                ut = ts.subst(jit.base(), newMap);
            }
        }
        if (ut != ts.unlabel(t)) {
            if (ts.isLabeled(t)) {
                t = ts.labeledType(t.position(), ut, ts.labelOfType(t));
            }
            else {
                t = ut;
            }
        }

        return t;
    }
    
    public static Type instantiate(Type t, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl) throws SemanticException {
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

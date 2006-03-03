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
 * This class contains a number of static utility methods to help instantiate
 * labels, principals and types. Instantiation includes:
 * <ul>
 * <li>the substitution of actual parameters for formal parameters in
 * parametric types;
 * <li>the substitution of receiver labels for the "this" label;
 * <li>the substitution of actual arg labels for the formal arg labels;
 * <li>the substitution of the receiver expression for dynamic labels and
 * principals mentioning the "this" access path;
 * <li>the substitution of the actual argument expressions for dynamic labels
 * and principals mentioning formal arguments in their access path.
 * </ul>
 */
public class JifInstantiator
{
    private JifInstantiator() {}
    
    /**
     * Perform instantiation on the label L
     *  
     * @param L the Label to substitute
     * @param receiverType
     * @param formalArgLabels List of ArgLabels, the labels of the formal
     *            arguments of the procedure being called
     * @param actualArgLabels List of Labels, the labels of the actuals
     *            arguments of the procedure being called
     * @param actualArgExprs List of Exprs, the actual arguments.
     * @param actualParamLabels List of Labels, the label of the evaluation of
     *            the actual parameters. These are only really needed for static
     *            methods (including constructors), where the actual parameters
     *            need to be evaluated at runtime.
     * @param callerContext the context of the caller of the method
     * @return the instantiated Label
     */
    private static Label instantiate(Label L, 
                                     ReferenceType receiverType, 
                                     List formalArgLabels, 
                                     List actualArgLabels, 
                                     List actualArgExprs, 
                                     List actualParamLabels, 
                                     JifContext callerContext) {
        if (formalArgLabels.size() != actualArgLabels.size() || 
                (actualArgExprs != null && formalArgLabels.size() != actualArgExprs.size())) {
            throw new InternalCompilerError("Inconsistent sized lists of args");
        }
        JifTypeSystem ts = (JifTypeSystem)receiverType.typeSystem();
        if (L != null) {
            // First replace each formalArgLabels with a new label. This
            // is essentially equivalent to renaming, to ensure
            // that if some of the actualArgLabels contains some of the
            // formalArgLabels, we don't get the substitution screwing things
            // up...
            // go through and replace the formal labels with the temp labels
            // QUESTION: Do we also need to do something similar for the 
            // access paths and the this label? i.e., replace the access path
            // roots in dynamic labels and principals for formal args with 
            // some temp var instance. Would need to do this in other methods 
            // too.
            Label[] temp = new Label[formalArgLabels.size()];
            for (int i = 0; i < temp.length; i++) {
                temp[i] = ts.unknownLabel(Position.COMPILER_GENERATED);

                ArgLabel formalArgLbl = (ArgLabel)formalArgLabels.get(i);
                try {
                    L = L.subst(new LabelInstantiator(formalArgLbl, temp[i]));
                }
                catch (SemanticException e) {
                    throw new InternalCompilerError("Unexpected SemanticException " +
                                                    "during label substitution: " + e.getMessage(), L.position());
                }
            }            
            
            // now go through each temp arglabel, and replace it appropriately...
            Iterator iActualArgLabels = actualArgLabels.iterator();
            Iterator iActualArgExprs = null;
            if (actualArgExprs != null) {
                iActualArgExprs = actualArgExprs.iterator();
            }
            for (int i = 0; i < temp.length; i++) {
                Label formalArgTempLbl = temp[i];
                ArgLabel formalArgLbl = (ArgLabel)formalArgLabels.get(i);
                Label actualArgLbl = (Label)iActualArgLabels.next();
                try {
                    L = L.subst(new ExactLabelInstantiator(formalArgTempLbl, actualArgLbl));
                }
                catch (SemanticException e) {
                    throw new InternalCompilerError("Unexpected SemanticException " +
                                                    "during label substitution: " + e.getMessage(), L.position());
                }

                if (iActualArgExprs != null) {
                    Expr actualExpr = (Expr)iActualArgExprs.next();
                
                    try {
                        Position pos = actualExpr.position();
                        AccessPathRoot root = (AccessPathRoot)JifUtil.varInstanceToAccessPath(formalArgLbl.formalInstance(), pos);                            
                        AccessPath target;
                        if (JifUtil.isFinalAccessExprOrConst(ts, actualExpr)) {
                            target = JifUtil.exprToAccessPath(actualExpr, callerContext);
                        }
                        else {
                            target = new AccessPathUninterpreted(actualExpr, pos);                            
                        }
                        L = L.subst(new AccessPathInstantiator(root, target));
                    }
                    catch (SemanticException e) {
                        throw new InternalCompilerError("Unexpected SemanticException " +
                                                        "during label substitution: " + e.getMessage(), L.position());
                    }
                }
            }
            if (iActualArgLabels.hasNext() || 
                    (iActualArgExprs != null && iActualArgExprs.hasNext())) {
                throw new InternalCompilerError("Inconsistent arg lists");
            }
            
            // also instantiate the param arg labels. They only occur in static methods
            // of parameterized classes, but no harm in always instantiating them.
            if (!actualParamLabels.isEmpty()) {
                // go through the formal params, and the actual param labels.
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
     * Perform instantiation on the principal p
     *  
     * @param p the principal to substitute
     * @param receiverType
     * @param formalArgLabels List of ArgLabels, the labels of the formal
     *            arguments of the procedure being called
     * @param actualArgExprs List of Exprs, the actual arguments.
     * @param actualParamLabels List of Labels, the label of the evaluation of
     *            the actual parameters. These are only really needed for static
     *            methods (including constructors), where the actual parameters
     *            need to be evaluated at runtime.
     * @param callerContext the context of the caller of the method
     * @return the instantiated Label
     */
    private static Principal instantiate(Principal p, 
                                         ReferenceType receiverType, 
                                         List formalArgLabels, 
                                         List actualArgExprs, 
                                         List actualParamLabels, 
                                         JifContext callerContext) {
        if (formalArgLabels.size() != actualArgExprs.size()) {
            throw new InternalCompilerError("Inconsistent sized lists of args");
        }
        JifTypeSystem ts = (JifTypeSystem)receiverType.typeSystem();
        if (p != null) {
            // go through each formal arglabel, and replace it appropriately...
            Iterator iArgLabels = formalArgLabels.iterator();
            Iterator iActualArgExprs = actualArgExprs.iterator();
            while (iArgLabels.hasNext()) {
                ArgLabel formalArgLbl = (ArgLabel)iArgLabels.next();
                Expr actualExpr = (Expr)iActualArgExprs.next();
                
                try {
                    Position pos = actualExpr.position();
                    AccessPathRoot root = (AccessPathRoot)JifUtil.varInstanceToAccessPath(formalArgLbl.formalInstance(), pos);                            
                    AccessPath target;
                    if (JifUtil.isFinalAccessExprOrConst(ts, actualExpr)) {
                        target = JifUtil.exprToAccessPath(actualExpr, callerContext);
                    }
                    else {
                        target = new AccessPathUninterpreted(actualExpr, pos);                            
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
     * Perform instantiation on the label L
     *  
     * @param L the Label to substitute
     * @param A the context of the caller of the method
     * @param receiverExpr the receiver expression, e.g., "e" in "e.m()".
     * @param receiverType the type of the receiver
     * @param receiverLbl the label of the receiver expression
     * @return the instantiated Label
     */
    public static Label instantiate(Label L, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        AccessPath receiverPath;
        if (JifUtil.isFinalAccessExprOrConst(ts, receiverExpr)) {
            receiverPath = JifUtil.exprToAccessPath(receiverExpr, A);
        }
        else {
            receiverPath = new AccessPathUninterpreted(receiverExpr, L.position()); 
        }
        return instantiate(L, A, receiverPath, receiverType, receiverLbl);
    }

    /**
     * Perform instantiation on the label L
     *  
     * @param L the Label to substitute
     * @param A the context of the caller of the method
     * @param receiverPath the access path of the receiver expression, e.g., "e" in "e.m()".
     * @param receiverType the type of the receiver
     * @param receiverLbl the label of the receiver expression
     * @return the instantiated Label
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

    /**
     * Perform instantiation on the label L
     *  
     * @param L the Label to substitute
     * @param A the context of the caller of the method
     * @param receiverExpr the receiver expression, e.g., "e" in "e.m()".
     * @param receiverType the type of the receiver
     * @param receiverLbl the label of the receiver expression
     * @param formalArgLabels List of ArgLabels, the labels of the formal
     *            arguments of the procedure being called
     * @param actualArgLabels List of Labels, the labels of the actuals
     *            arguments of the procedure being called
     * @param actualArgExprs List of Exprs, the actual arguments.
     * @param actualParamLabels List of Labels, the label of the evaluation of
     *            the actual parameters. These are only really needed for static
     *            methods (including constructors), where the actual parameters
     *            need to be evaluated at runtime.
     * @return the instantiated Label
     */
    public static Label instantiate(Label L, 
                                    JifContext A, 
                                    Expr receiverExpr, 
                                    ReferenceType receiverType, 
                                    Label receiverLbl, 
                                    List formalArgLabels, 
                                    List actualArgLabels, 
                                    List actualArgExprs, 
                                    List actualParamLabels) throws SemanticException {
        L = instantiate(L, A, receiverExpr, receiverType, receiverLbl);
        return instantiate(L, receiverType, formalArgLabels, actualArgLabels, actualArgExprs, actualParamLabels, A);
    }

    /**
     * Perform instantiation on the principal p
     *  
     * @param p the principal to substitute
     * @param A the context of the caller of the method
     * @param receiverExpr the receiver expression, e.g., "e" in "e.m()".
     * @param receiverType the type of the receiver
     * @param receiverLbl the label of the receiver expression
     * @return the instantiated principal
     * @throws SemanticException
     */
    private static Principal instantiate(Principal p, 
                                         JifContext A, 
                                         Expr receiverExpr, 
                                         ReferenceType receiverType, 
                                         Label receiverLbl) throws SemanticException {
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
            receiverPath = new AccessPathUninterpreted(receiverExpr, p.position());
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

    /**
     * Perform instantiation on the principal p
     *  
     * @param p the principal to instantiate
     * @param A the context of the caller of the method
     * @param receiverExpr the receiver expression, e.g., "e" in "e.m()".
     * @param receiverType the type of the receiver
     * @param receiverLbl the label of the receiver expression
     * @param formalArgLabels List of ArgLabels, the labels of the formal
     *            arguments of the procedure being called
     * @param actualArgExprs List of Exprs, the actual arguments.
     * @param actualParamLabels List of Labels, the label of the evaluation of
     *            the actual parameters. These are only really needed for static
     *            methods (including constructors), where the actual parameters
     *            need to be evaluated at runtime.
     * @return the instantiated principal
     */
    public static Principal instantiate(Principal p, 
                                        JifContext A, 
                                        Expr receiverExpr, 
                                        ReferenceType receiverType, 
                                        Label receiverLbl, 
                                        List formalArgLabels, 
                                        List actualArgExprs, 
                                        List actualParamLabels) throws SemanticException {
        p = instantiate(p, A, receiverExpr, receiverType, receiverLbl);
        return instantiate(p, receiverType, formalArgLabels, actualArgExprs, actualParamLabels, A);
    }

    /**
     * Perform instantiation on the type t
     *  
     * @param t the Type to substitute
     * @param A the context of the caller of the method
     * @param receiverExpr the receiver expression, e.g., "e" in "e.m()".
     * @param receiverType the type of the receiver
     * @param receiverLbl the label of the receiver expression
     * @param formalArgLabels List of ArgLabels, the labels of the formal
     *            arguments of the procedure being called
     * @param actualArgLabels List of Labels, the labels of the actuals
     *            arguments of the procedure being called
     * @param actualArgExprs List of Exprs, the actual arguments.
     * @param actualParamLabels List of Labels, the label of the evaluation of
     *            the actual parameters. These are only really needed for static
     *            methods (including constructors), where the actual parameters
     *            need to be evaluated at runtime.
     * @return the instantiated type
     */
    public static Type instantiate(Type t, JifContext A, Expr receiverExpr, ReferenceType receiverType, Label receiverLbl, List formalArgLabels, List actualArgLabels, List actualArgExprs, List actualParamLabels) throws SemanticException {
        t = instantiate(t, A, receiverExpr, receiverType, receiverLbl);
        JifTypeSystem ts = (JifTypeSystem)A.typeSystem();
        if (t instanceof ArrayType) {
            ArrayType at = (ArrayType)t;
            Type baseType = at.base();
            t = at.base(instantiate(baseType, A, receiverExpr, receiverType, receiverLbl, formalArgLabels, actualArgLabels, actualArgExprs, actualParamLabels));
        }
        
        if (ts.isLabeled(t)) {
            Label newL = instantiate(ts.labelOfType(t), A, receiverExpr, receiverType, receiverLbl, formalArgLabels, actualArgLabels, actualArgExprs, actualParamLabels);
            Type newT = instantiate(ts.unlabel(t), A, receiverExpr, receiverType, receiverLbl, formalArgLabels, actualArgLabels, actualArgExprs, actualParamLabels);
            return ts.labeledType(t.position(), newT, newL);
        }
        
        // t is unlabeled
        if (t instanceof JifSubstType) {
            JifSubstType jit = (JifSubstType)t;
            Map newMap = new HashMap();
            boolean diff = false;
            for (Iterator i = jit.entries(); i.hasNext();) {
                Map.Entry e = (Map.Entry)i.next();
                Object arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = instantiate((Label)arg, A, receiverExpr, receiverType, receiverLbl, formalArgLabels, actualArgLabels, actualArgExprs, actualParamLabels);
                }
                else if (arg instanceof Principal) {
                    p = instantiate((Principal)arg, A, receiverExpr, receiverType, receiverLbl, formalArgLabels, actualArgExprs, actualParamLabels);
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
    
    /**
     * Perform instantiation on the type t
     *  
     * @param t the Type to substitute
     * @param A the context of the caller of the method
     * @param receiverExpr the receiver expression, e.g., "e" in "e.m()".
     * @param receiverType the type of the receiver
     * @param receiverLbl the label of the receiver expression
     * @return the instantiated type
     */
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

    /**
     * Replaces the "this" label with receiverLabel, and uses
     * receiverType to perform substitution of actual parameters for formal 
     * parameters of a parameterized type.
     */
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
    
    /**
     * Replaces L with trgLabel if srcLabel.equals(L) 
     */
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

    /**
     * Replaces L with trgLabel if srcLabel == L 
     */
    private static class ExactLabelInstantiator extends LabelSubstitution {
        private Label srcLabel;
        private Label trgLabel;
        protected ExactLabelInstantiator(Label srcLabel, Label trgLabel) {
            this.srcLabel = srcLabel;
            this.trgLabel = trgLabel;
        }
        
        public Label substLabel(Label L) {
            if (srcLabel == L) {
                return trgLabel;
            }
            return L;
        }
    }

    /**
     * Replaces srcRoot with trgPath in dynamic labels and principals 
     */
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

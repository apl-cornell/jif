package jif.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jif.types.JifContext;
import jif.types.JifPolyType;
import jif.types.JifSubst;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.Param;
import jif.types.ParamInstance;
import jif.types.label.AccessPath;
import jif.types.label.AccessPathRoot;
import jif.types.label.AccessPathThis;
import jif.types.label.AccessPathUninterpreted;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.types.label.ThisLabel;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.types.ArrayType;
import polyglot.types.ReferenceType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
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
public class JifInstantiator {
    protected final JifTypeSystem ts;
    protected final ReferenceType receiverType;
    protected final Label receiverLbl;
    protected final AccessPath receiverPath;
    protected final List<ArgLabel> formalArgLabels;
    protected final List<? extends Type> formalArgTypes;
    protected final List<? extends Label> actualArgLabels;
    protected final List<Expr> actualArgExprs;
    protected final List<? extends Label> actualParamLabels;
    protected final JifContext callerContext;

    // temp labels and paths
    protected final List<Label> formalTempLabels;
    protected final List<AccessPathRoot> formalTempAccessPathRoots;
    protected final AccessPathRoot tempThisRoot;
    protected final Label tempThisLbl;

    protected JifInstantiator(ReferenceType receiverType, Label receiverLbl,
            AccessPath receiverPath, List<ArgLabel> formalArgLabels,
            List<? extends Type> formalArgTypes,
            List<? extends Label> actualArgLabels, List<Expr> actualArgExprs,
            List<? extends Label> actualParamLabels, JifContext callerContext) {
        this.callerContext = callerContext;
        this.receiverType = receiverType;
        this.receiverLbl = receiverLbl;
        this.receiverPath = receiverPath;
        this.formalArgLabels = formalArgLabels;
        this.formalArgTypes = formalArgTypes;
        this.actualArgLabels = actualArgLabels;
        this.actualArgExprs = actualArgExprs;
        this.actualParamLabels = actualParamLabels;

        this.ts = (JifTypeSystem) callerContext.typeSystem();

        if (formalArgLabels != null) {
            this.formalTempAccessPathRoots =
                    new ArrayList<AccessPathRoot>(formalArgLabels.size());
            this.formalTempLabels =
                    new ArrayList<Label>(formalArgLabels.size());
            for (int i = 0; i < formalArgLabels.size(); i++) {
                Label t = ts.unknownLabel(Position.compilerGenerated());
                t.setDescription("temp formal arg " + i);
                formalTempLabels.add(t);
                formalTempAccessPathRoots.add(new AccessPathUninterpreted(
                        "temp arg path", Position.compilerGenerated(), true));
            }
        } else {
            this.formalTempAccessPathRoots = null;
            this.formalTempLabels = null;
        }
        this.tempThisLbl = ts.unknownLabel(Position.compilerGenerated());
        this.tempThisLbl.setDescription("temp this");
        this.tempThisRoot = new AccessPathUninterpreted("temp this",
                Position.compilerGenerated(), true);

    }

    // replace the formal argLabels, formal arg
    // AccessPathRoots, the "this" label, and the "this"
    // access path root with appropriate temporary values.
    private Object substTempsForFormals(Object L, Position pos) {
        if (L == null) return null;

        // formal argLabels to formalTempLabels
        for (int i = 0; formalArgLabels != null
                && i < formalArgLabels.size(); i++) {
            Label temp = formalTempLabels.get(i);
            ArgLabel formalArgLbl = formalArgLabels.get(i);
            try {
                L = substImpl(L,
                        new LabelInstantiator(formalArgLbl, temp, false));
            } catch (SemanticException e) {
                throw new InternalCompilerError("Unexpected SemanticException "
                        + "during label substitution: " + e.getMessage(), pos);
            }
        }

        // formal this label to temp this label
        try {
            L = substImpl(L, new ThisLabelInstantiator(tempThisLbl));
        } catch (SemanticException e) {
            throw new InternalCompilerError(
                    "Unexpected SemanticException "
                            + "during label substitution: " + e.getMessage(),
                    pos);
        }

        // formal arg access paths to temp access paths
        for (int i = 0; formalArgLabels != null
                && i < formalArgLabels.size(); i++) {
            try {
                ArgLabel formalArgLbl = formalArgLabels.get(i);
                if (formalArgLbl.formalInstance().flags().isFinal()) {
                    AccessPathRoot formalRoot =
                            (AccessPathRoot) ts.varInstanceToAccessPath(
                                    formalArgLbl.formalInstance(),
                                    formalArgLbl.name(),
                                    formalArgLbl.position());
                    AccessPathRoot tempRoot = formalTempAccessPathRoots.get(i);

                    L = substImpl(L,
                            new AccessPathInstantiator(formalRoot, tempRoot));
                }
            } catch (SemanticException e) {
                throw new InternalCompilerError(
                        "Unexpected SemanticException " + e.getMessage(), pos);
            }
        }

        // formal this access path to temp this access path
        if (receiverType != null && receiverType.isClass()) {
            AccessPathRoot formalThisRoot = new AccessPathThis(
                    receiverType.toClass(), receiverType.position());
            try {
                L = substImpl(L, new AccessPathInstantiator(formalThisRoot,
                        tempThisRoot));
            } catch (SemanticException e) {
                throw new InternalCompilerError(
                        "Unexpected SemanticException " + e.getMessage(), pos);
            }
        }

        return L;

    }

    protected Object instantiateImpl(Object L, Position pos) {
        if (L == null) return L;

        // now go through and substitute things...

        // this label and params
        ThisLabelAndParamInstantiator labelInstantiator =
                new ThisLabelAndParamInstantiator();
        try {
            L = substImpl(L, labelInstantiator);
        } catch (SemanticException e) {
            throw new InternalCompilerError(
                    "Unexpected SemanticException "
                            + "during label substitution: " + e.getMessage(),
                    pos);
        }

        // this access path
        try {
            L = substImpl(L,
                    new AccessPathInstantiator(tempThisRoot, receiverPath));
        } catch (SemanticException e) {
            throw new InternalCompilerError(
                    "Unexpected SemanticException "
                            + "during label substitution: " + e.getMessage(),
                    pos);
        }

        // replace arg labels
        for (int i = 0; formalTempLabels != null
                && i < formalTempLabels.size(); i++) {
            Label formalArgTempLbl = formalTempLabels.get(i);
            if (actualArgLabels != null) {
                Label actualArgLbl = actualArgLabels.get(i);
                try {
                    L = substImpl(L, new ExactLabelInstantiator(
                            formalArgTempLbl, actualArgLbl));
                } catch (SemanticException e) {
                    throw new InternalCompilerError(
                            "Unexpected SemanticException "
                                    + "during label substitution: "
                                    + e.getMessage(),
                            pos);
                }
            }

            // arg access paths
            if (actualArgExprs != null) {
                try {
                    Expr actualExpr = actualArgExprs.get(i);
                    Type formalArgType = formalArgTypes.get(i);
                    AccessPath target;
                    if (ts.isFinalAccessExprOrConst(actualExpr,
                            formalArgType)) {
                        target = ts.exprToAccessPath(actualExpr, formalArgType,
                                callerContext);
                    } else {
                        target = new AccessPathUninterpreted(actualExpr,
                                actualExpr.position());
                    }

                    AccessPathRoot formalTempRoot =
                            formalTempAccessPathRoots.get(i);

                    L = substImpl(L,
                            new AccessPathInstantiator(formalTempRoot, target));
                } catch (SemanticException e) {
                    throw new InternalCompilerError(
                            "Unexpected SemanticException "
                                    + "during label substitution: "
                                    + e.getMessage(),
                            pos);
                }
            }
        }

        // param arg labels
        // they only occur in static methods
        // of parameterized classes, but no harm in always instantiating them.
        if (actualParamLabels != null && !actualParamLabels.isEmpty()
                && receiverType != null) {
            // go through the formal params, and the actual param labels.
            JifSubstType jst = (JifSubstType) receiverType;
            JifPolyType jpt = (JifPolyType) jst.base();
            Iterator<ParamInstance> iFormalParams = jpt.params().iterator();
            Iterator<? extends Label> iActualParamLabels =
                    actualParamLabels.iterator();

            // go through each formal and actual param, and make substitutions.
            if (jpt.params().size() != actualParamLabels.size()) {
                throw new InternalCompilerError(
                        "Inconsistent sizes for params. Error, please contact a Jif developer");
            }
            while (iActualParamLabels.hasNext()) {
                Label actualParamLabel = iActualParamLabels.next();
                ParamInstance pi = iFormalParams.next();
                ArgLabel paramArgLabel = ts.argLabel(pi.position(), pi);
                paramArgLabel.setUpperBound(ts.topLabel());
                try {
                    L = substImpl(L, new LabelInstantiator(paramArgLabel,
                            actualParamLabel));
                } catch (SemanticException e) {
                    throw new InternalCompilerError(
                            "Unexpected SemanticException "
                                    + "during label substitution: "
                                    + e.getMessage(),
                            pos);
                }
            }
            if (iActualParamLabels.hasNext() || iFormalParams.hasNext()) {
                throw new InternalCompilerError("Inconsistent param lists");
            }
        }

        // check if L is ill-formed
        try {
            substImpl(L, new CheckLeftOvers());
        } catch (SemanticException e) {
            throw new InternalCompilerError(
                    "Unexpected SemanticException "
                            + "during label substitution: " + e.getMessage(),
                    pos);
        }

        return L;
    }

    protected Object substImpl(Object o, LabelSubstitution lblsubst)
            throws SemanticException {
        if (o instanceof Principal) {
            return ((Principal) o).subst(lblsubst);
        }
        return ((Label) o).subst(lblsubst);
    }

    public Principal instantiate(Principal p) {
        p = (Principal) substTempsForFormals(p, p.position());
        return (Principal) instantiateImpl(p, p.position());
    }

    public Label instantiate(Label L) {
        L = (Label) substTempsForFormals(L, L.position());
        return (Label) instantiateImpl(L, L.position());
    }

    public Type instantiate(Type t) {
        if (t instanceof ArrayType) {
            ArrayType at = (ArrayType) t;
            Type baseType = at.base();
            t = at.base(instantiate(baseType));
        }

        if (ts.isLabeled(t)) {
            Label newL = instantiate(ts.labelOfType(t));
            Type newT = instantiate(ts.unlabel(t));
            return ts.labeledType(t.position(), newT, newL);
        }

        // t is unlabeled
        if (t instanceof JifSubstType) {
            JifSubstType jit = (JifSubstType) t;
            Map<ParamInstance, Param> newMap =
                    new HashMap<ParamInstance, Param>();
            boolean diff = false;
            for (Iterator<Map.Entry<ParamInstance, Param>> i = jit.entries(); i
                    .hasNext();) {
                Map.Entry<ParamInstance, Param> e = i.next();
                Param arg = e.getValue();
                Param p;
                if (arg instanceof Label) {
                    p = instantiate((Label) arg);
                } else if (arg instanceof Principal) {
                    p = instantiate((Principal) arg);
                } else {
                    throw new InternalCompilerError(
                            "Unexpected type for entry: "
                                    + arg.getClass().getName());
                }
                newMap.put(e.getKey(), p);

                if (p != arg) diff = true;
            }
            if (diff) {
                t = ts.subst(jit.base(), newMap);
            }
        }

        return t;
    }

    /**
     * Replaces the temp "this" label with receiverLabel, and uses
     * receiverType to perform substitution of actual parameters for formal
     * parameters of a parameterized type.
     */
    private class ThisLabelAndParamInstantiator extends LabelSubstitution {
        @Override
        public Label substLabel(Label L) {
            Label result = L;
            if (receiverLbl != null && result == tempThisLbl) {
                result = receiverLbl;
            }

            if (receiverType instanceof JifSubstType) {
                JifSubstType t = (JifSubstType) receiverType;
                result = ((JifSubst) t.subst()).substLabel(result);
            }

            return result;
        }

        @Override
        public Principal substPrincipal(Principal p) {
            if (receiverType instanceof JifSubstType) {
                JifSubst subst =
                        (JifSubst) ((JifSubstType) receiverType).subst();
                return subst.substPrincipal(p);
            }
            return p;
        }

    }

    /**
     * Check there are no temp labels or access paths still hanging around
     */
    private class CheckLeftOvers extends LabelSubstitution {
        Set<ReferenceType> thisClasses = new HashSet<ReferenceType>();

        @Override
        public Label substLabel(Label L) {
            if (L instanceof ThisLabel) {
                ThisLabel tl = (ThisLabel) L;
                if (!thisClasses.contains(tl.classType())
                        && !thisClasses.isEmpty()) {
                    throw new InternalCompilerError(
                            "multiple this classes: " + L);
                }
                thisClasses.add(tl.classType());

            }

            if (formalTempLabels != null && formalTempLabels.contains(L)) {
                throw new InternalCompilerError("Left over: " + L);
            }
            return L;
        }

        @Override
        public AccessPath substAccessPath(AccessPath ap) {
            AccessPathRoot root = ap.root();
            if (tempThisRoot == root) {
                throw new InternalCompilerError("Left over: " + ap);
            }
            if (formalTempAccessPathRoots != null
                    && formalTempAccessPathRoots.contains(root)) {
                throw new InternalCompilerError("Left over: " + ap);
            }
            return ap;
        }

    }

    /**
     * Replaces L with trgLabel if srcLabel.equals(L)
     */
    private static class LabelInstantiator extends LabelSubstitution {
        private Label srcLabel;
        private Label trgLabel;
        private boolean recurseArgLabelBounds;

        protected LabelInstantiator(Label srcLabel, Label trgLabel) {
            this(srcLabel, trgLabel, true);
        }

        protected LabelInstantiator(Label srcLabel, Label trgLabel,
                boolean recurseArgLabelBounds) {
            this.srcLabel = srcLabel;
            this.trgLabel = trgLabel;
            this.recurseArgLabelBounds = recurseArgLabelBounds;
        }

        @Override
        public Label substLabel(Label L) {
            if (srcLabel.equals(L)) {
                return trgLabel;
            }
            return L;
        }

        @Override
        public boolean recurseIntoChildren(Label L) {
            return recurseArgLabelBounds || !(L instanceof ArgLabel);
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

        @Override
        public Label substLabel(Label L) {
            if (srcLabel == L) {
                return trgLabel;
            }
            return L;
        }
    }

    /**
     * Replaces all ThisLabels with trgLabel
     */
    private static class ThisLabelInstantiator extends LabelSubstitution {
        private Label trgLabel;

        protected ThisLabelInstantiator(Label trgLabel) {
            this.trgLabel = trgLabel;
        }

        @Override
        public Label substLabel(Label L) {
            if (L instanceof ThisLabel) {
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

        protected AccessPathInstantiator(AccessPathRoot srcRoot,
                AccessPath trgPath) {
            this.srcRoot = srcRoot;
            this.trgPath = trgPath;
        }

        @Override
        public AccessPath substAccessPath(AccessPath ap) {
            if (ap.root().equals(srcRoot)) return ap.subst(srcRoot, trgPath);
            return ap;
        }
    }

    public static Label instantiate(Label L, JifContext callerContext,
            Expr receiverExpr, ReferenceType receiverType, Label receiverLabel,
            List<ArgLabel> formalArgLabels, List<? extends Type> formalArgTypes,
            List<Label> actualArgLabels, List<Expr> actualArgExprs,
            List<Label> actualParamLabels) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) callerContext.typeSystem();
        AccessPath receiverPath;
        if (ts.isFinalAccessExprOrConst(receiverExpr, receiverType)) {
            receiverPath = ts.exprToAccessPath(receiverExpr, receiverType,
                    callerContext);
        } else {
            receiverPath =
                    new AccessPathUninterpreted(receiverExpr, L.position());
        }
        JifInstantiator inst = new JifInstantiator(receiverType, receiverLabel,
                receiverPath, formalArgLabels, formalArgTypes, actualArgLabels,
                actualArgExprs, actualParamLabels, callerContext);
        return inst.instantiate(L);
    }

    /**
     * Instantiate a label into a new context where some of the names it
     * mentions may have a different meaning. Instantiation is needed because
     * labels may be used in a context different from the one in which they are
     * defined.  For example, in the following code:
     * 
     * <pre>
     * class C [label A] {
     *   final label x;
     * 
     *   f {this; x} () {
     *   }
     * }
     * 
     * class D[principal A] {
     *   void g() { o.f(); }
     * }
     * </pre>
     * 
     * The begin label of f is defined in a context containing:
     * <ul>
     *  <li>x:    label</li>
     *  <li>A:    label</li>
     *  <li>this: C    </li> </ul>
     *
     * In the context of g(), the begin label of f ({this; x}) should be
     * interpreted as {o; o.x}.
     * 
     * @param L
     *          the label to be instantiated ({this;x} in the example)
     * 
     * @param callerContext
     *          the context in which result label will be used (g in the example)
     * 
     * @param receiverExpr
     *          the expression to be used for interpreting dynamic labels (o in the example)
     * 
     * @param receiverType
     *          the type in which L is defined (C in this example)
     * 
     * @param receiverLbl
     *          the label to be substituted for {this} ({o} in the example)
     * 
     * @return
     *          the instantiated label ({o; o.x} in the example)
     *
     * @throws SemanticException
     *          TODO
     */
    public static Label instantiate(Label L, JifContext callerContext,
            Expr receiverExpr, ReferenceType receiverType, Label receiverLbl)
                    throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) callerContext.typeSystem();
        AccessPath receiverPath;
        if (ts.isFinalAccessExprOrConst(receiverExpr, receiverType)) {
            receiverPath = ts.exprToAccessPath(receiverExpr, receiverType,
                    callerContext);
        } else {
            receiverPath =
                    new AccessPathUninterpreted(receiverExpr, L.position());
        }
        return instantiate(L, callerContext, receiverPath, receiverType,
                receiverLbl);
    }

    public static Label instantiate(Label L, JifContext callerContext,
            AccessPath receiverPath, ReferenceType receiverType,
            Label receiverLbl) {
        JifInstantiator inst = new JifInstantiator(receiverType, receiverLbl,
                receiverPath, null, null, null, null, null, callerContext);
        return inst.instantiate(L);

    }

    public static Type instantiate(Type t, JifContext callerContext,
            AccessPath receiverPath, ReferenceType receiverType,
            Label receiverLbl) {
        JifInstantiator inst = new JifInstantiator(receiverType, receiverLbl,
                receiverPath, null, null, null, null, null, callerContext);
        return inst.instantiate(t);
    }

    public static Principal instantiate(Principal p, JifContext callerContext,
            Expr receiverExpr, ReferenceType receiverType, Label receiverLabel,
            List<ArgLabel> formalArgLabels, List<? extends Type> formalArgTypes,
            List<Expr> actualArgExprs, List<Label> actualParamLabels)
                    throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) callerContext.typeSystem();
        AccessPath receiverPath;
        if (ts.isFinalAccessExprOrConst(receiverExpr, receiverType)) {
            receiverPath = ts.exprToAccessPath(receiverExpr, receiverType,
                    callerContext);
        } else {
            receiverPath =
                    new AccessPathUninterpreted(receiverExpr, p.position());
        }
        JifInstantiator inst = new JifInstantiator(receiverType, receiverLabel,
                receiverPath, formalArgLabels, formalArgTypes, null,
                actualArgExprs, actualParamLabels, callerContext);
        return inst.instantiate(p);
    }

    public static Type instantiate(Type t, JifContext callerContext,
            Expr receiverExpr, ReferenceType receiverType, Label receiverLabel,
            List<ArgLabel> formalArgLabels, List<? extends Type> formalArgTypes,
            List<? extends Label> actualArgLabels, List<Expr> actualArgExprs,
            List<? extends Label> actualParamLabels) throws SemanticException {
        JifTypeSystem ts = (JifTypeSystem) callerContext.typeSystem();
        AccessPath receiverPath;
        if (ts.isFinalAccessExprOrConst(receiverExpr, receiverType)) {
            receiverPath = ts.exprToAccessPath(receiverExpr, receiverType,
                    callerContext);
        } else {
            receiverPath =
                    new AccessPathUninterpreted(receiverExpr, t.position());
        }
        JifInstantiator inst = new JifInstantiator(receiverType, receiverLabel,
                receiverPath, formalArgLabels, formalArgTypes, actualArgLabels,
                actualArgExprs, actualParamLabels, callerContext);
        return inst.instantiate(t);
    }

    public static Type instantiate(Type t, JifContext callerContext,
            Expr receiverExpr, ReferenceType receiverType, Label receiverLbl)
                    throws SemanticException {

        JifTypeSystem ts = (JifTypeSystem) callerContext.typeSystem();
        AccessPath receiverPath;
        if (ts.isFinalAccessExprOrConst(receiverExpr, receiverType)) {
            receiverPath = ts.exprToAccessPath(receiverExpr, receiverType,
                    callerContext);
        } else {
            receiverPath =
                    new AccessPathUninterpreted(receiverExpr, t.position());
        }
        JifInstantiator inst = new JifInstantiator(receiverType, receiverLbl,
                receiverPath, null, null, null, null, null, callerContext);
        return inst.instantiate(t);

    }
}

package jif.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import jif.types.LabelSubstitution;
import jif.types.Param;
import jif.types.PathMap;
import jif.types.SemanticDetailedException;
import jif.types.label.AccessPath;
import jif.types.label.AccessPathField;
import jif.types.label.AccessPathLocal;
import jif.types.label.ConfProjectionPolicy_c;
import jif.types.label.DynamicLabel;
import jif.types.label.IntegProjectionPolicy_c;
import jif.types.label.JoinLabel;
import jif.types.label.JoinPolicy_c;
import jif.types.label.Label;
import jif.types.label.MeetLabel;
import jif.types.label.MeetPolicy_c;
import jif.types.label.PairLabel;
import jif.types.label.Policy;
import jif.types.label.ReaderPolicy;
import jif.types.label.VarLabel;
import jif.types.label.WriterPolicy;
import jif.types.principal.ConjunctivePrincipal;
import jif.types.principal.DisjunctivePrincipal;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import jif.types.principal.VarPrincipal;
import jif.visit.LabelChecker;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.visit.TypeChecker;

/**
 * Contains some common utility code to type check dynamic labels and principals
 */
public class LabelTypeCheckUtil {
    protected final JifTypeSystem ts;

    public LabelTypeCheckUtil(JifTypeSystem ts) {
        this.ts = ts;
    }

    /**
     * Check the type of any access path contained in a dynamic principal. All such access paths should have type
     * Principal.
     * @param tc
     * @param principal
     * @throws SemanticException
     */
    public void typeCheckPrincipal(TypeChecker tc, Principal principal)
            throws SemanticException {
        if (principal instanceof DynamicPrincipal) {
            DynamicPrincipal dp = (DynamicPrincipal) principal;

            // Make sure that the access path is set correctly
            // check also that all field accesses are final, and that
            // the type of the expression is principal
            AccessPath path = dp.path();
            try {
                path.verify((JifContext) tc.context());
            } catch (SemanticException e) {
                throw new SemanticException(e.getMessage(),
                        principal.position());
            }

            if (!ts.isImplicitCastValid(dp.path().type(), ts.Principal())) {
                throw new SemanticDetailedException(
                        "The type of a dynamic principal must be \"principal\".",
                        "The type of a dynamic principal must be "
                                + "\"principal\". The type of the expression "
                                + dp.path().exprString() + " is "
                                + dp.path().type() + ".",
                        principal.position());
            }
        }
        if (principal instanceof ConjunctivePrincipal) {
            ConjunctivePrincipal p = (ConjunctivePrincipal) principal;
            for (Principal q : p.conjuncts()) {
                typeCheckPrincipal(tc, q);
            }
        }
        if (principal instanceof DisjunctivePrincipal) {
            DisjunctivePrincipal p = (DisjunctivePrincipal) principal;
            for (Principal q : p.disjuncts()) {
                typeCheckPrincipal(tc, q);
            }
        }
    }

    /**
     * Check that all access paths occurring in label Lbl have the appropriate type.
     * @param tc
     * @param Lbl
     * @throws SemanticException
     */
    public void typeCheckLabel(final TypeChecker tc, Label Lbl)
            throws SemanticException {
        Lbl.subst(new LabelSubstitution() {
            @Override
            public Label substLabel(Label l) throws SemanticException {

                if (l instanceof DynamicLabel) {
                    DynamicLabel dl = (DynamicLabel) l;

                    // Make sure that the access path is set correctly
                    // check also that all field accesses are final, and that
                    // the type of the expression is label
                    AccessPath path = dl.path();
                    try {
                        path.verify((JifContext) tc.context());
                    } catch (SemanticException e) {
                        throw new SemanticException(e.getMessage(),
                                dl.position());
                    }

                    if (!ts.isLabel(dl.path().type())) {
                        throw new SemanticDetailedException(
                                "The type of a dynamic label must be \"label\".",
                                "The type of a dynamic label must be "
                                        + "\"label\". The type of the expression "
                                        + dl.path().exprString() + " is "
                                        + dl.path().type() + ".",
                                dl.position());
                    }
                } else if (l instanceof PairLabel) {
                    PairLabel pl = (PairLabel) l;
                    typeCheckPolicy(tc, pl.confPolicy());
                    typeCheckPolicy(tc, pl.integPolicy());
                }
                return l;
            }
        });

    }

    public Collection<Label> labelComponents(Label L) {
        if (L instanceof JoinLabel) {
            return ((JoinLabel) L).joinComponents();
        } else if (L instanceof MeetLabel) {
            return ((MeetLabel) L).meetComponents();
        } else {
            return Collections.singleton(L);
        }
    }

    public void typeCheckPolicy(TypeChecker tc, Policy p)
            throws SemanticException {
        if (p instanceof ConfProjectionPolicy_c) {
            ConfProjectionPolicy_c cpp = (ConfProjectionPolicy_c) p;
            typeCheckLabel(tc, cpp.label());
        } else if (p instanceof IntegProjectionPolicy_c) {
            IntegProjectionPolicy_c ipp = (IntegProjectionPolicy_c) p;
            typeCheckLabel(tc, ipp.label());
        } else if (p instanceof JoinPolicy_c) {
            @SuppressWarnings("unchecked")
            JoinPolicy_c<Policy> jp = (JoinPolicy_c<Policy>) p;
            Collection<Policy> joinComponents = jp.joinComponents();
            for (Policy pol : joinComponents) {
                typeCheckPolicy(tc, pol);
            }
        } else if (p instanceof MeetPolicy_c) {
            @SuppressWarnings("unchecked")
            MeetPolicy_c<Policy> mp = (MeetPolicy_c<Policy>) p;
            Collection<Policy> meetComponents = mp.meetComponents();
            for (Policy pol : meetComponents) {
                typeCheckPolicy(tc, pol);
            }
        } else if (p instanceof ReaderPolicy) {
            ReaderPolicy pol = (ReaderPolicy) p;
            typeCheckPrincipal(tc, pol.owner());
            typeCheckPrincipal(tc, pol.reader());
        } else if (p instanceof WriterPolicy) {
            WriterPolicy pol = (WriterPolicy) p;
            typeCheckPrincipal(tc, pol.owner());
            typeCheckPrincipal(tc, pol.writer());
        } else {
            throw new InternalCompilerError("Unexpected policy " + p);
        }
    }

    public void typeCheckType(TypeChecker tc, Type t) throws SemanticException {
        t = ts.unlabel(t);

        if (t instanceof JifSubstType) {
            JifClassType jct = (JifSubstType) t;

            for (Param arg : jct.actuals()) {
                if (arg instanceof Label) {
                    Label L = (Label) arg;
                    typeCheckLabel(tc, L);
                } else if (arg instanceof Principal) {
                    Principal p = (Principal) arg;
                    typeCheckPrincipal(tc, p);
                } else {
                    throw new InternalCompilerError(
                            "Unexpected type for entry: "
                                    + arg.getClass().getName());
                }
            }
        }
    }

    public PathMap labelCheckType(Type t, LabelChecker lc,
            List<Type> throwTypes, Position pos) throws SemanticException {
        JifContext A = lc.context();
        PathMap X = ts.pathMap().N(A.pc());

        List<PathMap> Xparams = labelCheckTypeParams(t, lc, throwTypes, pos);

        for (PathMap Xj : Xparams) {
            X = X.join(Xj);
        }
        return X;
    }

    /**
     * 
     * @param t
     * @param lc
     * @return List of <code>PathMap</code>s, one for each parameter of the subst type.
     * @throws SemanticException
     */
    public List<PathMap> labelCheckTypeParams(Type t, LabelChecker lc,
            List<Type> throwTypes, Position pos) throws SemanticException {
        t = ts.unlabel(t);
        List<PathMap> Xparams;

        if (t instanceof JifSubstType) {
            JifContext A = lc.context();
            PathMap X = ts.pathMap().N(A.pc());
            JifSubstType jst = (JifSubstType) t;
            Xparams =
                    new ArrayList<PathMap>(jst.subst().substitutions().size());

            JifClassType jct = jst;
            for (Param arg : jct.actuals()) {
                if (arg instanceof Label) {
                    Label L = (Label) arg;
                    A = (JifContext) A.pushBlock();

                    if (ts.isParamsRuntimeRep(t)) {
                        // make sure the label is runtime representable
                        if (!L.isRuntimeRepresentable()) {
                            if (L instanceof VarLabel && ((VarLabel) L)
                                    .mustRuntimeRepresentable()) {
                                // the var label has already been marked as needing to be
                                // runtime representable, and so the solver will
                                // make sure it is indeed runtime representable.
                            } else {
                                throw new SemanticDetailedException(
                                        "A label used in a type examined at runtime must be representable at runtime.",
                                        "If a type is used in an instanceof, "
                                                + "cast, constructor call, or static method call, "
                                                + "all parameters of the type must be runtime "
                                                + "representable. Arg labels are not represented at runtime.",
                                        pos);
                            }
                        }
                    }

                    updateContextForParam(lc, A, X);
                    PathMap Xj = L.labelCheck(A, lc);
                    throwTypes.removeAll(L.throwTypes(ts));
                    Xparams.add(Xj);
                    X = X.join(Xj);
                    A = (JifContext) A.pop();
                } else if (arg instanceof Principal) {
                    Principal p = (Principal) arg;
                    A = (JifContext) A.pushBlock();
                    if (ts.isParamsRuntimeRep(t)
                            && !p.isRuntimeRepresentable()) {
                        if (p instanceof VarPrincipal && ((VarPrincipal) p)
                                .mustRuntimeRepresentable()) {
                            // the var principal has already been marked as needing to be
                            // runtime representable, and so the solver will
                            // make sure it is indeed runtime representable.
                        } else {
                            throw new SemanticDetailedException(
                                    "A principal used in a "
                                            + "type examined at runtime must be "
                                            + "representable at runtime.",
                                    "If a type is used in an instanceof, "
                                            + "cast, constructor call, or static method call, "
                                            + "all parameters of the type must be runtime "
                                            + "representable. The principal "
                                            + p + " is not "
                                            + "represented at runtime.",
                                    pos);
                        }
                    }

                    updateContextForParam(lc, A, X);
                    PathMap Xj = p.labelCheck(A, lc);
                    throwTypes.removeAll(p.throwTypes(ts));
                    Xparams.add(Xj);
                    X = X.join(Xj);
                    A = (JifContext) A.pop();
                } else {
                    throw new InternalCompilerError(
                            "Unexpected type for entry: "
                                    + arg.getClass().getName());
                }
            }
        } else {
            Xparams = Collections.emptyList();
        }
        return Xparams;
    }

    /**
     * Utility method for updating the context for checking a parameter for the
     * type.
     *
     * Useful for overriding in projects like Fabric.
     */
    protected void updateContextForParam(LabelChecker lc, JifContext A,
            PathMap Xprev) {
        A.setPc(Xprev.N(), lc);
    }

    /**
     * Return the types that may be thrown by a runtime evalution
     * of the type <code>type</code>.
     * 
     * @param type
     * @return the types that may be thrown by a runtime evalution
     * of the type <code>type</code>.
     */
    public List<Type> throwTypes(JifClassType type) {
        Type t = ts.unlabel(type);
        if (t instanceof JifSubstType && ts.isParamsRuntimeRep(t)) {
            JifClassType jct = (JifSubstType) t;
            List<Type> exc = new ArrayList<Type>();
            for (Param arg : jct.actuals()) {
                if (arg instanceof Label) {
                    exc.addAll(((Label) arg).throwTypes(ts));
                } else if (arg instanceof Principal) {
                    exc.addAll(((Principal) arg).throwTypes(ts));
                } else {
                    throw new InternalCompilerError(
                            "Unexpected type for entry: "
                                    + arg.getClass().getName());
                }
            }
            return exc;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns a set of local instances that are used in the type.
     */
    public Set<LocalInstance> localInstancesUsed(JifClassType type) {
        Type t = ts.unlabel(type);
        if (t instanceof JifSubstType) {
            Set<LocalInstance> lis = new LinkedHashSet<LocalInstance>();
            for (Param arg : type.actuals()) {
                AccessPath p = null;
                if (arg instanceof DynamicLabel) {
                    p = ((DynamicLabel) arg).path();
                } else if (arg instanceof DynamicPrincipal) {
                    p = ((DynamicPrincipal) arg).path();
                }
                while (p != null && p instanceof AccessPathField) {
                    p = ((AccessPathField) p).path();
                }
                if (p instanceof AccessPathLocal) {
                    lis.add(((AccessPathLocal) p).localInstance());
                }
            }
            return lis;
        } else {
            return Collections.emptySet();
        }
    }

}

package jif.types;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jif.types.label.AccessPath;
import jif.types.label.AccessPathField;
import jif.types.label.CovariantParamLabel;
import jif.types.label.Label;
import jif.types.label.ParamLabel;
import jif.types.principal.ParamPrincipal;
import jif.types.principal.Principal;
import polyglot.ext.param.types.Subst_c;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.FieldInstance;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.util.CachingTransformingList;
import polyglot.util.InternalCompilerError;
import polyglot.util.SerialVersionUID;
import polyglot.util.Transformation;

public class JifSubst_c extends Subst_c<ParamInstance, Param>
        implements JifSubst {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifSubst_c(JifTypeSystem ts,
            Map<ParamInstance, ? extends Param> subst) {
        super(ts, subst);
    }

    @Override
    public Iterator<Map.Entry<ParamInstance, Param>> entries() {
        return super.entries();
    }

    @Override
    public Param get(ParamInstance pi) {
        return subst.get(pi);
    }

    ////////////////////////////////////////////////////////////////
    // Override substitution methods to handle Jif constructs

    @Override
    protected boolean cacheTypeEquality(Type t1, Type t2) {
        // don't strip away the instantiation info. At worst, we'll return
        // false more often than we need to, resulting in more instantiations.
        // But at least it'll be correct, otherwise we end up with, say,
        // C[L1] and C[L2] being regarded as equal, and thus having the same
        // substitution.
        return ((JifTypeSystem) ts).equalsNoStrip(t1, t2);
    }

    @Override
    public Type uncachedSubstType(Type t) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;

        if (ts.isLabeled(t)) {
            return ts.labeledType(t.position(), substType(ts.unlabel(t)),
                    substLabel(ts.labelOfType(t)));
        }

        return super.uncachedSubstType(t);
    }

    @Override
    protected ClassType substClassTypeImpl(ClassType t) {
        // Don't bother trying to substitute into a non-Jif class.
        if (!(t instanceof JifClassType)) {
            return t;
        }

        return new JifSubstClassType_c((JifTypeSystem) ts, t.position(),
                (JifClassType) t, this);
    }

    @Override
    public <MI extends MethodInstance> MI substMethod(MI mi) {
        mi = super.substMethod(mi);

        if (mi instanceof JifProcedureInstance) {
            JifProcedureInstance jmi = (JifProcedureInstance) mi;

            jmi.setPCBound(substLabel(jmi.pcBound()), jmi.isDefaultPCBound());
            jmi.setReturnLabel(substLabel(jmi.returnLabel()),
                    jmi.isDefaultReturnLabel());
            jmi.setConstraints(
                    new CachingTransformingList<Assertion, Assertion>(
                            jmi.constraints(), new ConstraintXform()));

            @SuppressWarnings("unchecked")
            MI tmpMi = (MI) jmi;
            mi = tmpMi;
        }

        return mi;
    }

    @Override
    public <CI extends ConstructorInstance> CI substConstructor(CI ci) {
        ci = super.substConstructor(ci);

        if (ci instanceof JifProcedureInstance) {
            JifProcedureInstance jci = (JifProcedureInstance) ci;

            jci.setPCBound(substLabel(jci.pcBound()), jci.isDefaultPCBound());
            jci.setReturnLabel(substLabel(jci.returnLabel()),
                    jci.isDefaultReturnLabel());
            jci.setConstraints(
                    new CachingTransformingList<Assertion, Assertion>(
                            jci.constraints(), new ConstraintXform()));

            @SuppressWarnings("unchecked")
            CI tmpCi = (CI) jci;
            ci = tmpCi;
        }

        return ci;
    }

    /** Perform substititions on a field. */
    @Override
    public <FI extends FieldInstance> FI substField(FI fi) {
        fi = super.substField(fi);
        if (fi instanceof JifFieldInstance) {
            JifFieldInstance jfi = (JifFieldInstance) fi;
            jfi.setLabel(substLabel(jfi.label()));

            @SuppressWarnings("unchecked")
            FI tmpFi = (FI) jfi;
            fi = tmpFi;
        }
        return fi;
    }

    ////////////////////////////////////////////////////////////////
    // Substitution methods for Jif constructs

    @Override
    public List<Assertion> substConstraintList(List<Assertion> constraints) {
        return new CachingTransformingList<Assertion, Assertion>(constraints,
                new ConstraintXform());
    }

    @Override
    public List<Label> substLabelList(List<Label> labels) {
        return new CachingTransformingList<Label, Label>(labels,
                new LabelXform());
    }

    @Override
    public List<Principal> substPrincipalList(List<Principal> principals) {
        return new CachingTransformingList<Principal, Principal>(principals,
                new PrincipalXform());
    }

    @Override
    public <Actor extends ActsForParam, Granter extends ActsForParam> Assertion substConstraint(
            Assertion constraint) {
        if (constraint == null) {
            return null;
        }

        if (constraint instanceof ActsForConstraint) {
            @SuppressWarnings("unchecked")
            ActsForConstraint<Actor, Granter> c =
                    (ActsForConstraint<Actor, Granter>) constraint;
            c = c.actor(substActsForParam(c.actor()));
            c = c.granter(substActsForParam(c.granter()));
            return c;
        } else if (constraint instanceof LabelLeAssertion) {
            LabelLeAssertion c = (LabelLeAssertion) constraint;
            c = c.lhs(substLabel(c.lhs()));
            c = c.rhs(substLabel(c.rhs()));
            return c;
        } else if (constraint instanceof CallerConstraint) {
            CallerConstraint c = (CallerConstraint) constraint;
            List<Principal> l =
                    new CachingTransformingList<Principal, Principal>(
                            c.principals(), new PrincipalXform());
            return c.principals(l);
        } else if (constraint instanceof AuthConstraint) {
            AuthConstraint c = (AuthConstraint) constraint;
            List<Principal> l =
                    new CachingTransformingList<Principal, Principal>(
                            c.principals(), new PrincipalXform());
            return c.principals(l);
        } else if (constraint instanceof AutoEndorseConstraint) {
            AutoEndorseConstraint c = (AutoEndorseConstraint) constraint;
            c = c.endorseTo(substLabel(c.endorseTo()));
            return c;
        }

        return constraint;
    }

    public <P extends ActsForParam> P substActsForParam(P param) {
        if (param == null) return null;

        try {
            @SuppressWarnings("unchecked")
            P result = (P) param.subst(substLabelSubst);
            return result;
        } catch (SemanticException e) {
            throw new InternalCompilerError("Unexpected semantic exception", e);
        }
    }

    @Override
    public Label substLabel(Label label) {
        return substActsForParam(label);
    }

    @Override
    public Principal substPrincipal(Principal principal) {
        return substActsForParam(principal);
    }

    /**
     * An instance of the nested class <code>SubstLabelSubst</code>, to be
     * used by <code>substLabel(Label)</code> and
     * <code>substPrincipal(Principal)</code>.
     */
    protected SubstLabelSubst substLabelSubst = new SubstLabelSubst();

    /**
     * This class is a <code>LabelSubstitution</code> that performs
     * substitutions on <code>Label</code>s and <code>Principal</code>s.
     *
     */
    @SuppressWarnings("serial")
    protected class SubstLabelSubst extends LabelSubstitution
            implements Serializable {
        /**
         * @throws SemanticException
         */
        @Override
        public Label substLabel(Label L) throws SemanticException {
            if (L instanceof ParamLabel) {
                ParamLabel c = (ParamLabel) L;
                return subLabel(c, c.paramInstance());
            } else if (L instanceof CovariantParamLabel) {
                CovariantParamLabel c = (CovariantParamLabel) L;
                return subLabel(c, c.paramInstance());
            }
            return L;
        }

        /**
         * @throws SemanticException
         */
        @Override
        public Principal substPrincipal(Principal p) throws SemanticException {
            if (p instanceof ParamPrincipal) {
                ParamPrincipal pp = (ParamPrincipal) p;
                return subPrincipal(pp, pp.paramInstance());
            }

            return p;
        }

        @Override
        public AccessPath substAccessPath(AccessPath ap)
                throws SemanticException {
            ap = super.substAccessPath(ap);
            if (ap instanceof AccessPathField) {
                // Also perform substitution within the access path's field
                // instance.
                AccessPathField apf = (AccessPathField) ap;
                FieldInstance substFI = substField(apf.fieldInstance());
                return apf.fieldInstance(substFI);
            }

            return ap;
        }
    }

    /** Return the substitution of uid, or label if not found. */
    protected Label subLabel(Label label, ParamInstance pi) {
        Param sub = subst.get(pi);
        JifTypeSystem ts = (JifTypeSystem) typeSystem();

        if (sub instanceof UnknownParam) {
            return ts.unknownLabel(sub.position());
        } else if (sub instanceof Label) {
            return (Label) sub;
        } else if (sub == null) {
            return label;
        } else {
            throw new InternalCompilerError("Cannot substitute " + label
                    + " for " + sub + " with param instance " + pi,
                    label.position());
        }
    }

    /** Return the substitution of uid, or principal if not found. */
    protected Principal subPrincipal(Principal principal, ParamInstance pi) {
        Param sub = subst.get(pi);
        JifTypeSystem ts = (JifTypeSystem) typeSystem();

        if (sub instanceof UnknownParam) {
            return ts.unknownPrincipal(sub.position());
        } else if (sub instanceof Principal) {
            return (Principal) sub;
        } else if (sub == null) {
            return principal;
        } else {
            throw new InternalCompilerError(
                    "Cannot substitute " + principal + " for " + sub
                            + " with param instance " + pi,
                    principal.position());
        }
    }

    ////////////////////////////////////////////////////////////////
    // Substitution machinery

    @Override
    protected Param substSubstValue(Param value) {
        if (value instanceof Label) {
            return substLabel((Label) value);
        } else if (value instanceof Principal) {
            return substPrincipal((Principal) value);
        }
        return super.substSubstValue(value);
    }

    public class ConstraintXform
            implements Transformation<Assertion, Assertion> {
        @Override
        public Assertion transform(Assertion a) {
            return substConstraint(a);
        }
    }

    public class LabelXform implements Transformation<Label, Label> {
        @Override
        public Label transform(Label lbl) {
            return substLabel(lbl);
        }
    }

    public class PrincipalXform
            implements Transformation<Principal, Principal> {
        @Override
        public Principal transform(Principal p) {
            return substPrincipal(p);
        }
    }
}

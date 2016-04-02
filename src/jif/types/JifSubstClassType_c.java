package jif.types;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jif.types.label.ProviderLabel;
import jif.types.label.ThisLabel;
import jif.types.principal.Principal;
import polyglot.ext.param.types.PClass;
import polyglot.ext.param.types.SubstClassType_c;
import polyglot.main.Report;
import polyglot.types.ClassType;
import polyglot.types.FieldInstance;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class JifSubstClassType_c extends SubstClassType_c<ParamInstance, Param>
        implements JifSubstType {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public JifSubstClassType_c(JifTypeSystem ts, Position pos, ClassType base,
            JifSubst subst) {
        super(ts, pos, base, subst);

        if (!(base instanceof JifPolyType)) {
            throw new InternalCompilerError(
                    "Cannot perform subst on \"" + base + "\".");
        }
    }

    @Override
    public List<? extends FieldInstance> fields() {
        // XXX Kludge. JifFieldInstance.equalsImpl() doesn't test for equality
        // on its label, but should. Adding this equality test to
        // JifFieldInstance, however, exposes scheduling bugs are complicated to
        // fix. (Method instances with field final access paths in their
        // constraints end up with unsubstituted VarLabels in the paths' field
        // instances, which should have been substituted out by the
        // FieldLabelResolver. The pthScript tests involving
        // Regression0[12]?.jif exercise this issue.) Therefore, we override
        // the super class's behaviour here to ensure label equality is captured
        // because it is needed for correct substitution behaviour.
        List<? extends FieldInstance> fields = base.fields();

        // See if fields.equals(this.fields), but also take into account field
        // instance labels.
        boolean equals;
        if (this.fields == null)
            equals = false;
        else {
            equals = true;
            for (Iterator<? extends FieldInstance> it1 =
                    fields.iterator(), it2 = this.fields.iterator(); it1
                            .hasNext() || it2.hasNext();) {
                if (!it1.hasNext() || !it2.hasNext()) {
                    equals = false;
                    break;
                }

                FieldInstance fi1 = it1.next();
                FieldInstance fi2 = it2.next();
                if (!ts.equals(fi1, fi2)) {
                    equals = false;
                    break;
                }

                if (fi1 instanceof JifFieldInstance) {
                    JifFieldInstance jfi1 = (JifFieldInstance) fi1;
                    JifFieldInstance jfi2 = (JifFieldInstance) fi2;
                    if (!ts.equals(jfi1.label(), jfi2.label())) {
                        equals = false;
                        break;
                    }
                }
            }
        }

        if (!equals) {
            this.fields = deepCopy(fields);
            this.substFields = subst.substFieldList(fields);
        }
        return super.fields();
    }

    ////////////////////////////////////////////////////////////////
    // Implement methods of JifSubstType

    @Override
    public PClass<ParamInstance, Param> instantiatedFrom() {
        return ((JifPolyType) base).instantiatedFrom();
    }

    @Override
    public List<Param> actuals() {
        JifPolyType pt = (JifPolyType) base;
        JifSubst subst = (JifSubst) this.subst;

        List<Param> actuals = new ArrayList<Param>(pt.params().size());

        for (ParamInstance pi : pt.params()) {
            Param p = subst.get(pi);
            actuals.add(p);
        }

        return actuals;
    }

    ////////////////////////////////////////////////////////////////
    // Implement methods of JifClassType

    @Override
    public JifTypeSystem typeSystem() {
        return (JifTypeSystem) super.typeSystem();
    }

    @Override
    public List<Principal> authority() {
        JifClassType base = (JifClassType) this.base;
        JifSubst subst = (JifSubst) this.subst;
        return subst.substPrincipalList(base.authority());
    }

    @Override
    public List<Assertion> constraints() {
        JifClassType base = (JifClassType) this.base;
        JifSubst subst = (JifSubst) this.subst;
        return subst.substConstraintList(base.constraints());
    }

    @Override
    public List<Principal> constructorCallAuthority() {
        JifClassType base = (JifClassType) this.base;
        JifSubst subst = (JifSubst) this.subst;
        return subst.substPrincipalList(base.constructorCallAuthority());
    }

    @Override
    public ThisLabel thisLabel() {
        return ((JifTypeSystem) ts).thisLabel(this);
    }

    @Override
    public ThisLabel thisLabel(Position p) {
        return ((JifTypeSystem) ts).thisLabel(p, this);
    }

    @Override
    public String toString() {
        if (Report.should_report(Report.debug, 1)) {
            return super.toString();
        }

        // do something a little more readable
        JifPolyType jpt = (JifPolyType) base;
        String s = "";

        if (jpt.params() != null) {
            for (Iterator<ParamInstance> i = jpt.params().iterator(); i
                    .hasNext();) {
                ParamInstance pi = i.next();
                s += subst.substitutions().get(pi);

                if (i.hasNext()) {
                    s += ", ";
                }
            }
        }

        if (s.length() > 0) {
            s = "[" + s + "]";
        }

        return jpt.name() + s;
    }

    @Override
    public ProviderLabel provider() {
        JifClassType jpt = (JifClassType) base;
        return jpt.provider();
    }

    @Override
    public boolean isUnsafe() {
        JifClassType jpt = (JifClassType) base;
        return jpt.isUnsafe();
    }

}

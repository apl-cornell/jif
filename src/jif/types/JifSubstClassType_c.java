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
            throw new InternalCompilerError("Cannot perform subst on \"" + base
                    + "\".");
        }
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

}

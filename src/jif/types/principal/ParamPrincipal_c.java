package jif.types.principal;

import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.ParamInstance;
import jif.types.PathMap;
import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>ParamPrincipal</code> interface.
 */
public class ParamPrincipal_c extends Principal_c implements ParamPrincipal {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final ParamInstance paramInstance;

    public ParamPrincipal_c(ParamInstance paramInstance, JifTypeSystem ts,
            Position pos) {
        super(ts, pos, ts.paramPrincipalTranslator());
        this.paramInstance = paramInstance;
    }

    @Override
    public ParamInstance paramInstance() {
        return paramInstance;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return ((JifTypeSystem) ts)
                .isParamsRuntimeRep(paramInstance.container());
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

    @Override
    public String toString() {
        if (Report.should_report(Report.debug, 1)) {
            return "<param-" + paramInstance + ">";
        }
        return paramInstance.name();
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (!(o instanceof ParamPrincipal)) {
            return false;
        }

        ParamPrincipal that = (ParamPrincipal) o;
        return this.paramInstance.equals(that.paramInstance());
    }

    @Override
    public int hashCode() {
        return paramInstance.hashCode();
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        JifTypeSystem ts = (JifTypeSystem) A.typeSystem();
        Label l;
        if (A.inStaticContext()) {
            // return a special arg label
            ArgLabel al = ts.argLabel(this.position, paramInstance);
            if (A.inConstructorCall()) {
                al.setUpperBound(ts.thisLabel(this.position(),
                        (JifClassType) A.currentClass()));
            } else {
                al.setUpperBound(ts.topLabel());
            }
            l = al;
        } else {
            l = ts.thisLabel(this.position(), (JifClassType) A.currentClass());
        }
        return ts.pathMap().N(l).NV(l);
    }

}

package jif.types.label;

import java.util.Collections;
import java.util.Set;

import jif.types.JifClassType;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.ParamInstance;
import jif.types.PathMap;
import jif.types.hierarchy.LabelEnv;
import jif.visit.LabelChecker;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>CovariantLabel</code> interface.
 */
public class CovariantParamLabel_c extends Label_c
        implements CovariantParamLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final ParamInstance paramInstance;

    public CovariantParamLabel_c(ParamInstance paramInstance, JifTypeSystem ts,
            Position pos) {
        super(ts, pos, ts.paramLabelTranslator());
        this.paramInstance = paramInstance;
        String className = null;
        if (paramInstance != null && paramInstance.container() != null) {
            className = paramInstance.container().fullName();
        }
        if (className != null && paramInstance.name() != null) {
            setDescription("covariant label parameter " + paramInstance.name()
                    + " of class " + className);
        }
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
    public boolean isCovariant() {
        return true;
    }

    @Override
    public boolean isComparable() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return paramInstance.isCanonical();
    }

    @Override
    protected boolean isDisambiguatedImpl() {
        return isCanonical();
    }

    @Override
    public boolean isEnumerable() {
        return true;
    }

    @Override
    public int hashCode() {
        return paramInstance.hashCode();
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (!(o instanceof CovariantParamLabel)) {
            return false;
        }
        CovariantParamLabel that = (CovariantParamLabel) o;
        return (this.paramInstance == that.paramInstance());
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        return componentString(printedLabels);
    }

    @Override
    public String componentString(Set<Label> printedLabels) {
        if (Report.should_report(Report.debug, 1)) {
            return "<covariant-param-label " + this.paramInstance + ">";
        }
        return this.paramInstance.name();
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
        return ts.pathMap().N(A.pc()).NV(l);
    }

    @Override
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        if (L instanceof PairLabel) {
            PairLabel that = (PairLabel) L;
            return env.leq(this.confProjection(), that.confPolicy(), state)
                    && env.leq(this.integProjection(), that.integPolicy(),
                            state);
        }

        // only leq if equal to this parameter, which is checked before
        // this method is called.
        return false;
    }

    @Override
    public Set<Variable> variables() {
        return Collections.emptySet();
    }

}

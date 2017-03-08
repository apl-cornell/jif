package jif.types;

import jif.types.label.Label;
import jif.types.label.ProviderLabel;
import polyglot.types.FieldInstance;
import polyglot.types.FieldInstance_c;
import polyglot.types.Flags;
import polyglot.types.ParsedClassType;
import polyglot.types.ReferenceType;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>JifFieldInstance</code> interface.
 */
public class JifFieldInstance_c extends FieldInstance_c
        implements JifFieldInstance {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Label label;
    protected boolean hasInitializer;
    protected Param initializer;

    public JifFieldInstance_c(JifTypeSystem ts, Position pos,
            ReferenceType container, Flags flags, Type type, String name) {

        super(ts, pos, container, flags, type, name);
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (!(o instanceof JifFieldInstance)) return false;

        JifFieldInstance jfi = (JifFieldInstance) o;
        // XXX This doesn't test for label equality, but should. Adding the
        // label-equality test, however, exposes scheduling bugs are complicated
        // to fix. (Procedure instances with field final access paths in their
        // constraints end up with unsubstituted VarLabels in the paths' field
        // instances, which should have been substituted out by the
        // FieldLabelResolver. The pthScript tests involving
        // Regression0[12]?.jif exercise this issue.) Once this scheduling issue
        // is fixed, the line below can be uncommented, and the kludge in
        // JifSubstClassType_c.fields() can be removed.
        return super.equalsImpl(jfi)
//                && ts.equals(label, jfi.label())
                && hasInitializer == jfi.hasInitializer() && (hasInitializer
                        ? ts.equals(initializer, jfi.initializer()) : true);
    }

    @Override
    public void subst(VarMap bounds) {
        this.setLabel(bounds.applyTo(label));
        this.setType(bounds.applyTo(type));
    }

    @Override
    public Label label() {
        return label;
    }

    @Override
    public void setLabel(Label label) {
        this.label = label;
    }

    @Override
    public ProviderLabel provider() {
        return ((JifClassType) container).provider();
    }

    @Override
    public boolean hasInitializer() {
        return hasInitializer;
    }

    @Override
    public void setHasInitializer(boolean hasInitializer) {
        this.hasInitializer = hasInitializer;
    }

    @Override
    public Param initializer() {
        return initializer;
    }

    @Override
    public void setInitializer(Param init) {
        this.initializer = init;
    }

    private FieldInstance findOrigFieldInstance() {
        if (this.container() instanceof JifSubstType) {
            JifSubstType jst = (JifSubstType) this.container();
            if (jst.base() instanceof ParsedClassType) {
                return ((ParsedClassType) jst.base()).fieldNamed(this.name());
            } else {
                throw new InternalCompilerError("Unexpected base type");
            }
        }
        return this;
    }

    @Override
    public boolean isConstant() {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            return orig.isConstant();
        }
        return super.isConstant();
    }

    @Override
    public Object constantValue() {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            return orig.constantValue();
        }
        return super.constantValue();
    }

    @Override
    public boolean constantValueSet() {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            return orig.constantValueSet();
        }
        return super.constantValueSet();
    }

    @Override
    public FieldInstance constantValue(Object constantValue) {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            throw new InternalCompilerError(
                    "Cant modify constant value on a copy");
        }
        return super.constantValue(constantValue);
    }

    @Override
    public FieldInstance notConstant() {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            throw new InternalCompilerError(
                    "Cant modify constant value on a copy");
        }
        return super.notConstant();
    }

    @Override
    public void setConstantValue(Object constantValue) {
        FieldInstance orig = findOrigFieldInstance();
        if (this != orig) {
            throw new InternalCompilerError(
                    "Cant modify constant value on a copy");
        }
        super.setConstantValue(constantValue);
    }

    @Override
    public String toString() {
        return super.toString() + " label = " + label;
    }

}

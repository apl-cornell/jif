package jif.types;

import java.util.Collections;
import java.util.List;

import polyglot.types.FieldInstance;
import polyglot.types.MethodInstance;
import polyglot.types.Named;
import polyglot.types.ReferenceType;
import polyglot.types.ReferenceType_c;
import polyglot.types.Resolver;
import polyglot.types.Type;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.SerialVersionUID;

public class UninstTypeParam_c extends ReferenceType_c implements
        UninstTypeParam, Named {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private ParamInstance pi;

    public UninstTypeParam_c(TypeSystem ts, ParamInstance pi) {
        super(ts);
        this.pi = pi;
    }

    @Override
    public String name() {
        return pi.name();
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

    @Override
    public List<? extends MethodInstance> methods() {
        return Collections.emptyList();
    }

    @Override
    public List<? extends FieldInstance> fields() {
        return Collections.emptyList();
    }

    @Override
    public FieldInstance fieldNamed(String name) {
        for (FieldInstance fi : fields()) {
            if (fi.name().equals(name)) {
                return fi;
            }
        }
        return null;
    }

    @Override
    public List<? extends ReferenceType> interfaces() {
        return Collections.emptyList();
    }

    @Override
    public Type superType() {
        return ts.Object();
    }

    @Override
    public String translate(Resolver c) {
        return this.name();
    }

    @Override
    public String toString() {
        return this.name();
    }

    @Override
    public boolean isCastValidImpl(Type toType) {
        if (super.isCastValidImpl(toType)) {
            return true;
        }

        return ts.isCastValid(ts.Object(), toType);
    }

    @Override
    public boolean descendsFromImpl(Type ancestor) {
        if (super.descendsFromImpl(ancestor)) {
            return true;
        }

        return ts.isSubtype(ts.Object(), ancestor);
    }

    @Override
    public boolean isImplicitCastValidImpl(Type toType) {
        if (super.isImplicitCastValidImpl(toType)) {
            return true;
        }

        return ts.isImplicitCastValid(ts.Object(), toType);
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        return o instanceof UninstTypeParam
                && pi.equals(((UninstTypeParam) o).paramInstance());
    }

    @Override
    public boolean typeEqualsImpl(Type t) {
        return equalsImpl(t);
    }

    @Override
    public ParamInstance paramInstance() {
        return pi;
    }

    @Override
    public String fullName() {
        return name();
    }

}

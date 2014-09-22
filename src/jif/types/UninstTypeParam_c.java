package jif.types;

import java.util.ArrayList;
import java.util.List;

import polyglot.types.ClassType;
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
    private ReferenceType upperBound;

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
        return upperBound == null || upperBound.isCanonical();
    }

    @Override
    public ReferenceType upperBound() {
        return upperBound;
    }

    @Override
    public UninstTypeParam upperBound(ReferenceType upperBound) {
        UninstTypeParam_c n = (UninstTypeParam_c) copy();
        n.upperBound = upperBound;
        return n;
    }

    @Override
    public List<? extends MethodInstance> methods() {
        return new ArrayList<MethodInstance>();
    }

    @Override
    public List<? extends FieldInstance> fields() {
        return new ArrayList<FieldInstance>();
    }

    @Override
    public FieldInstance fieldNamed(String name) {
        return null;
    }

    @Override
    public List<? extends ReferenceType> interfaces() {
        return new ArrayList<ReferenceType>();
    }

    @Override
    public Type superType() {
        return upperBound == null ? ts.Object() : upperBound;
    }

    @Override
    public ClassType toClass() {
        return upperBound == null ? ts.Object() : upperBound.toClass();
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

        return ts.isCastValid(upperBound == null ? ts.Object() : upperBound,
                toType);
    }

    @Override
    public boolean descendsFromImpl(Type ancestor) {
        if (super.descendsFromImpl(ancestor)) {
            return true;
        }

        return ts.isSubtype(upperBound == null ? ts.Object() : upperBound,
                ancestor);
    }

    @Override
    public boolean isImplicitCastValidImpl(Type toType) {
        if (super.isImplicitCastValidImpl(toType)) {
            return true;
        }

        return ts.isImplicitCastValid(upperBound == null ? ts.Object()
                : upperBound, toType);
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        // TODO: name equality of type parameters may not be sound.
        return o instanceof UninstTypeParam
                && pi.name().equals(
                        ((UninstTypeParam) o).paramInstance().name());
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

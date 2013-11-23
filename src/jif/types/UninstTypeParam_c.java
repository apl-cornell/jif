package jif.types;

import polyglot.types.Named;
import polyglot.types.Resolver;
import polyglot.types.TypeSystem;
import polyglot.types.Type_c;
import polyglot.util.SerialVersionUID;

public class UninstTypeParam_c extends Type_c implements UninstTypeParam, Named {
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
    public String translate(Resolver c) {
        return null;
    }

    @Override
    public String toString() {
        return "type " + pi.name();
    }

    @Override
    public String fullName() {
        return pi.name();
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

    @Override
    public ParamInstance paramInstance() {
        return pi;
    }

}

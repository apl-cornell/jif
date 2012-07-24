package jif.types.principal;

import java.util.Collections;
import java.util.Set;

import jif.types.JifTypeSystem;
import jif.types.Param;
import polyglot.types.TypeObject;
import polyglot.util.Position;

/** An implementation of the <code>VarPrincipal</code> interface.
 */
public class VarPrincipal_c extends Principal_c implements VarPrincipal {
    private final transient int uid = ++counter;
    private static int counter = 0;
    private String name;

    /**
     * Does whatever this variable resolves to need to be runtime representable?
     */
    private boolean mustRuntimeRepresentable = false;

    protected String description;

    public VarPrincipal_c(String name, String description, JifTypeSystem ts, Position pos) {
        super(ts, pos);
        this.name = name;
        setDescription(description);
    }

    protected void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isCanonical() { return true; }

    @Override
    public void setMustRuntimeRepresentable() {
        this.mustRuntimeRepresentable = true;
    }
    @Override
    public boolean mustRuntimeRepresentable() {
        return this.mustRuntimeRepresentable;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        return this == o;
    }
    @Override
    public int hashCode() { return -88393 + uid; }

    @Override
    public Set<Param> variables() {
        return Collections.<Param> singleton(this);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name();
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return false;
    }
}

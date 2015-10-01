package jif.types.principal;

import jif.translate.ExternalPrincipalToJavaExpr_c;
import jif.translate.PrincipalToJavaExpr;
import jif.types.JifTypeSystem;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

/** An implementation of the <code>ExternalPrincipal</code> interface. 
 */
public class ExternalPrincipal_c extends Principal_c
        implements ExternalPrincipal {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private final String name;

    public ExternalPrincipal_c(String name, JifTypeSystem ts, Position pos) {
        super(ts, pos, new ExternalPrincipalToJavaExpr_c());
        this.name = name;
    }

    public ExternalPrincipal_c(String name, JifTypeSystem ts,
            PrincipalToJavaExpr toJava, Position pos) {
        super(ts, pos, toJava);
        this.name = name;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return true;
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

    @Override
    public String toString() {
        if (Report.should_report(Report.debug, 1)) {
            return "<pr-external " + name + ">";
        }
        return name();
    }

    /** Compares the specified object with this principal for equality. 
     *  Return true if and only the specific object is an <code>ExternalPrincipal</code>
     *  and both principals have the same name. 
     */
    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (!(o instanceof ExternalPrincipal)) {
            return false;
        }

        ExternalPrincipal that = (ExternalPrincipal) o;
        return this.name.equals(that.name());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}

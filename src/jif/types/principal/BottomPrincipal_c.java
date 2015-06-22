package jif.types.principal;

import jif.translate.BottomPrincipalToJavaExpr_c;
import jif.types.JifTypeSystem;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class BottomPrincipal_c extends Principal_c implements BottomPrincipal {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public BottomPrincipal_c(JifTypeSystem ts, Position pos) {
        super(ts, pos, new BottomPrincipalToJavaExpr_c());
    }

    @Override
    public boolean isBottomPrincipal() {
        return true;
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
        if (Report.should_report(Report.debug, 2)) {
            return "<bottom principal>";
        }
        return "⊥";
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        return o instanceof BottomPrincipal_c;
    }

    @Override
    public int hashCode() {
        return 4212;
    }
}

package jif.types.principal;

import jif.translate.TopPrincipalToJavaExpr_c;
import jif.types.JifTypeSystem;
import polyglot.main.Report;
import polyglot.types.TypeObject;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class TopPrincipal_c extends Principal_c implements TopPrincipal {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public TopPrincipal_c(JifTypeSystem ts, Position pos) {
        super(ts, pos, new TopPrincipalToJavaExpr_c());
    }

    @Override
    public boolean isTopPrincipal() {
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
            return "<top principal>";
        }
        return "⊤";
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        return o instanceof TopPrincipal_c;
    }

    @Override
    public int hashCode() {
        return 451212;
    }
}

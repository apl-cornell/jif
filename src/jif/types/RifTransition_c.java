package jif.types;

import java.util.List;
import java.util.Set;

import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.Id;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.SerialVersionUID;

public class RifTransition_c implements RifTransition {
    private static final long serialVersionUID = SerialVersionUID.generate();

    protected Id name;
    protected Id lstate;
    protected Id rstate;

    public RifTransition_c(Id name, Id lstate, Id rstate) {
        this.name = name;
        this.lstate = lstate;
        this.rstate = rstate;
    }

    @Override
    public Id name() {
        return this.name;
    }

    @Override
    public Id lstate() {
        return this.lstate;
    }

    @Override
    public Id rstate() {
        return this.rstate;
    }

    @Override
    public boolean isCanonical() {
        return true;
    }

    @Override
    public boolean isRuntimeRepresentable() {
        return true;
    }

    @Override
    public String toString(Set<Label> printedLabels) {
        StringBuffer sb = new StringBuffer(name.toString());
        sb.append(":");
        sb.append(this.lstate.toString());
        sb.append("->");
        sb.append(this.rstate.toString());
        return sb.toString();
    }

    @Override
    public List<Type> throwTypes(TypeSystem ts) {
        return null;
    }

    @Override
    public RifComponent subst(LabelSubstitution substitution)
            throws SemanticException {
        return this;
    }

    @Override
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        return null;
    }

}

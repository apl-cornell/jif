package jif.types.label;

import jif.types.JifTypeSystem;
import polyglot.ast.Id;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class RifVarLabel_c extends VarLabel_c implements RifVarLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private Id transition;

    public RifVarLabel_c(String name, String description, JifTypeSystem ts,
            Position pos) {
        super(name, description, ts, pos);
        this.transition = null;
    }

    @Override
    public void takeTransition(Id transition) {
        this.transition = transition;
    }

    @Override
    public Id transition() {
        return this.transition;
    }

}

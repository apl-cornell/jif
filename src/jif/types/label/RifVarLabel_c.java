package jif.types.label;

import jif.types.JifTypeSystem;
import polyglot.ast.Id;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class RifVarLabel_c extends VarLabel_c implements RifVarLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    public RifVarLabel_c(String name, String description, JifTypeSystem ts,
            Position pos) {
        super(name, description, ts, pos);
    }

    @Override
    public RifVarLabel takeTransition(Id transition) {
        return new TransitionVarLabel_c(transition.id() + "(" + this.name()
                + ")", "Apply transition " + transition.id() + " to "
                + this.name(), (JifTypeSystem) ts, this.position(), this,
                transition);
    }

}

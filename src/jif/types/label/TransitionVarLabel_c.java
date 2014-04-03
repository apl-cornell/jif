package jif.types.label;

import jif.types.JifTypeSystem;
import polyglot.ast.Id;
import polyglot.types.TypeObject;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class TransitionVarLabel_c extends RifVarLabel_c implements
        TransitionVarLabel {
    private static final long serialVersionUID = SerialVersionUID.generate();

    private Id transition;
    private RifVarLabel innerRifLabel;

    public TransitionVarLabel_c(String name, String description,
            JifTypeSystem ts, Position pos, RifVarLabel rifl, Id transition) {
        super(name, description, ts, pos);
        this.innerRifLabel = rifl;
        this.transition = transition;
    }

    @Override
    public Id transition() {
        return transition;
    }

    @Override
    public RifVarLabel innerRifLabel() {
        return innerRifLabel;
    }

    @Override
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof TransitionVarLabel) {
            TransitionVarLabel newlbl = (TransitionVarLabel) o;
            return this.transition.equals(newlbl.transition())
                    && this.innerRifLabel.equals(newlbl.innerRifLabel());

        }
        return false;
    }

    @Override
    public int hashCode() {
        return transition.hashCode() ^ innerRifLabel.hashCode();
    }

}

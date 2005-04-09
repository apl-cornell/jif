package jif.types;

import jif.types.label.Label;
import polyglot.ext.jl.types.*;
import polyglot.types.*;
import polyglot.util.*;

/** An implementation of the <code>JifFieldInstance</code> interface.
 */
public class JifFieldInstance_c extends FieldInstance_c
                               implements JifFieldInstance
{
    protected Label label;
    protected boolean hasInitializer;

    public JifFieldInstance_c(JifTypeSystem ts, Position pos,
	ReferenceType container, Flags flags,
	Type type, String name) {

	super(ts, pos, container, flags, type, name);
    }

    public void subst(VarMap bounds) {
        this.setLabel(bounds.applyTo(label));
        this.setType(bounds.applyTo(type));
    }

    public Label label() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public boolean hasInitializer() {
        return hasInitializer;
    }

    public void setHasInitializer(boolean hasInitializer) {
        this.hasInitializer = hasInitializer;        
    }
}

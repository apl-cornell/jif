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
    UID uid;
    protected Label label;
    protected boolean hasInitializer;

    public JifFieldInstance_c(JifTypeSystem ts, Position pos,
	ReferenceType container, Flags flags,
	Type type, String name, UID uid, Label label) {

	super(ts, pos, container, flags, type, name);
	this.uid = uid;
        this.label = label;
    }

    public UID uid() {
	return uid;
    }

    public JifFieldInstance uid(UID uid) {
	JifFieldInstance_c n = (JifFieldInstance_c) copy();
	n.uid = uid;
	return n;
    }

    public Label label() {
        return label;
    }

    public JifFieldInstance label(Label label) {
	JifFieldInstance_c n = (JifFieldInstance_c) copy();
	n.label = label;
	return n;
    }

    public void setLabel(Label L) {
	JifTypeSystem jts = (JifTypeSystem) ts;
	Type t = this.type;
	if (jts.isLabeled(t)) {
	    t = jts.unlabel(t);
	}
	
	this.type = jts.labeledType(t.position(), t, L);
	this.label = L;
    }

    public boolean hasInitializer() {
        return hasInitializer;
    }

    public void setHasInitializer(boolean hasInitializer) {
        this.hasInitializer = hasInitializer;        
    }
}

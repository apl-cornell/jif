package jif.types;

import jif.types.label.Label;
import polyglot.ext.jl.types.*;
import polyglot.types.*;
import polyglot.util.*;

/** An implementation of the <code>JifLocalInstance</code> interface. 
 */
public class JifLocalInstance_c extends LocalInstance_c
                               implements JifLocalInstance
{
    UID uid;
    protected Label label;
    public JifLocalInstance_c(JifTypeSystem ts, Position pos, Flags flags,
	Type type, String name, UID uid) {

	super(ts, pos, flags, type, name);
	this.uid = uid;
    }

    public boolean isCanonical() {
        return label != null && label.isCanonical() && super.isCanonical();
    }
    
    public UID uid() {
	return uid;
    }

    public void setUid(UID uid) {
	this.uid = uid;
    }

    public Label label() {
	return label;
    }

    public void setLabel(Label L) {
	this.label = L;
    }
}

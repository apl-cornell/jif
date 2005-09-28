package jif.lang;

import java.util.*;

/**
 * A Label is the runtime representation of a Jif label. A Label consists of a
 * set of components, each of which is a {@link jif.lang.Policy Policy}.
 *  
 */
public class JoinLabel extends AbstractLabel implements Label
{
    private Set components;
    JoinLabel() {
	components = Collections.EMPTY_SET;
    }

    JoinLabel(Set labels) {
	components = Collections.unmodifiableSet(labels);
    }

    public Set components() {
	return components;
    }

    public boolean relabelsTo(Label l) {
	// Check if this <= l

        // If this = { .. Ci .. }, check that for all i, Ci <= l
	for (Iterator i = components.iterator(); i.hasNext(); ) {
	    Label Ci = (Label) i.next();
	    if (!Ci.relabelsTo(l)) {
	        return false;
	    }
	}
	return true;
    }

    public boolean equals(Object o) {
        if (o instanceof JoinLabel) {
            JoinLabel that = (JoinLabel)o;
            return this.components().equals(that.components());
        }        
        if (this.components().size() == 1) {
            return this.components().iterator().next().equals(o);
        }
        return false;
    }
    public String componentString() {
	String str = "";
	for (Iterator iter = components.iterator(); iter.hasNext(); ) {
	    str += iter.next();
	    if (iter.hasNext()) str += "; ";
	}
	return str;
    }
}

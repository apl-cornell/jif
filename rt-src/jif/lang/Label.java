package jif.lang;

import java.util.*;

/**
 * A Label is the runtime representation of a Jif label. A Label consists of a
 * set of components, each of which is a {@link jif.lang.Policy Policy}.
 *  
 */
public class Label
{
    private static final Label BOTTOM = new Label();
    private static final Map intern = new HashMap();

    static {
	intern.put(BOTTOM, BOTTOM);
    }

    private Set components;
    private Map relabels;

    public Label() {
	components = new HashSet();
	relabels = new HashMap();
    }

    public Label(Policy p) {
	components = new HashSet();
	components.add(p);
	relabels = new HashMap();
    }

    public Label(List policies) {
	components = new HashSet();
	components.addAll(policies);
	relabels = new HashMap();
    }

    public static Label bottom() {
	return BOTTOM;
    }

    public static Label policy(Principal owner, Collection readers) {
	Label newLabel = new Label();
	newLabel.components.add(new PrivacyPolicy(owner, readers));

	Label label = (Label) intern.get(newLabel);

	if (label == null) {
	    intern.put(newLabel, newLabel);
	    label = newLabel;
	}

	return label;
    }

    public static Label policy(Principal owner, PrincipalSet readers) {
	return policy(owner, readers.getSet());
    }

    public Set components() {
	return components;
    }

    public Label join(Label l) {
	return join(this, l);
    }

    public static Label join(Label l1, Label l2) {
	Label newLabel = new Label();
	newLabel.components.addAll(l1.components);
	newLabel.components.addAll(l2.components);

	Label label = (Label) intern.get(newLabel);

	if (label == null) {
	    intern.put(newLabel, newLabel);
	    label = newLabel;
	}

	return newLabel;
    }

    public Policy policy() {
	//TODO: extract a policy using PH.
	return (Policy) components.iterator().next();
    }

    public boolean equivalentTo(Label l) {
        return this.relabelsTo(l) && l.relabelsTo(this);
    }

    public boolean relabelsTo(Label l) {
	// Check if this <= l
	Boolean b = (Boolean) relabels.get(l);

	if (b != null) {
	    return b.booleanValue();
	}

	// If this = { .. Pi .. } and l = { .. Pj' .. }, check if for all i,
	// there exists a j such that Pi <= Pj'
	for (Iterator i = components.iterator(); i.hasNext(); ) {
	    Policy pi = (Policy) i.next();

	    boolean sat = false;

	    for (Iterator j = l.components.iterator(); j.hasNext(); ) {
		Policy pj = (Policy) j.next();
		if (pi.relabelsTo(pj)) {
		    sat = true;
		    break;
		}
	    }

	    if (! sat) {
		relabels.put(l, new Boolean(false));
		return false;
	    }
	}

	relabels.put(l, new Boolean(true));
	return true;
    }

    public int hashCode() {
	int h = 0;

	for (Iterator iter = components.iterator(); iter.hasNext(); ) {
	    Policy p = (Policy) iter.next();
	    h += p.hashCode();
	}

	return h;
    }

    public boolean equals(Object o) {
	if (! (o instanceof Label)) {
	    return false;
	}

	Label label = (Label) o;

	if (! components.containsAll(label.components)) {
	    return false;
	}

	if (! label.components.containsAll(components)) {
	    return false;
	}

	return true;
    }

    public String toString() {
	String str = "{";
	for (Iterator iter = components.iterator(); iter.hasNext(); ) {
	    str += iter.next();
	    if (iter.hasNext()) str += ", ";
	}
	str += "}";
	return str;
    }
}

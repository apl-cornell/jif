package jif.types;

import jif.types.principal.Principal;
import polyglot.ext.jl.types.*;
import polyglot.util.*;

/** An implementation of the <code>ActsForConstraint</code> interface.
 */
public class ActsForConstraint_c extends TypeObject_c
				implements ActsForConstraint
{
    protected Principal granter;
    protected Principal actor;

    public ActsForConstraint_c(JifTypeSystem ts, Position pos,
	                         Principal actor, Principal granter) {
	super(ts, pos);
	this.actor = actor;
	this.granter = granter;
    }

    public ActsForConstraint actor(Principal actor) {
	ActsForConstraint_c n = (ActsForConstraint_c) copy();
	n.actor = actor;
	return n;
    }

    public ActsForConstraint granter(Principal granter) {
	ActsForConstraint_c n = (ActsForConstraint_c) copy();
	n.granter = granter;
	return n;
    }

    public Principal actor() {
	return actor;
    }

    public Principal granter() {
	return granter;
    }

    public String toString() {
	return "actsFor(" + actor + ", " + granter + ")";
    }

    public boolean isCanonical() {
	return true;
    }
}

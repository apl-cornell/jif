package jif.types;

import jif.types.principal.Principal;
import polyglot.types.TypeObject_c;
import polyglot.util.Position;

/** An implementation of the <code>ActsForConstraint</code> interface.
 */
public class ActsForConstraint_c extends TypeObject_c
				implements ActsForConstraint
{
    protected Principal granter;
    protected Principal actor;
    protected final boolean isEquiv;

    public ActsForConstraint_c(JifTypeSystem ts, Position pos,
            Principal actor, Principal granter, boolean isEquiv) {
        super(ts, pos);
        this.actor = actor;
        this.granter = granter;
        this.isEquiv = isEquiv;
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

    public boolean isEquiv() {
        return isEquiv;
    }
    
    public String toString() {
        
	return actor + " " + (isEquiv?"equiv":"actsfor") + " " + granter;
    }

    public boolean isCanonical() {
	return true;
    }
}

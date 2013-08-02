package jif.lang;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import jif.lang.PrincipalUtil.DelegationPair;

/**
 * Abstract class representing the join of policies. All the policies should be
 * of the same kind, either all IntegPolicies or all ConfPolicies.
 */
public abstract class JoinPolicy extends AbstractPolicy implements Policy {
    private Set<Policy> components; // Set of Policies

    JoinPolicy(LabelUtil labelUtil, Set<Policy> policies) {
        super(labelUtil);
        components = Collections.unmodifiableSet(policies);
    }

    public Set<Policy> joinComponents() {
        return components;
    }

    @Override
    public boolean relabelsTo(Policy pol, Set<DelegationPair> s) {
        if (this == pol || this.equals(pol)) return true;

        Set<DelegationPair> temp = new HashSet<DelegationPair>();
        // this == c1 join ... join cn
        // this <= pol if for all Ci, Ci <= pol
        boolean sat = true;
        for (Policy Ci : components) {
            if (!labelUtil.relabelsTo(Ci, pol, temp)) {
                sat = false;
                break;
            }
        }
        if (sat) {
            s.addAll(temp);
            return true;
        }

        temp.clear();

        // failed so far, try taking advantage of structure on the RHS
        if (pol instanceof MeetPolicy) {
            // this <= d1 meet ... meet dn if for all di
            // we have this <= di
            MeetPolicy mp = (MeetPolicy) pol;
            sat = true;
            for (Policy Di : mp.meetComponents()) {
                if (!labelUtil.relabelsTo(this, Di, temp)) {
                    sat = false;
                    break;
                }
            }
            if (sat) {
                s.addAll(temp);
                return true;
            }
        }
        if (pol instanceof JoinPolicy) {
            // this <= d1 join ... join dn if there is some di
            // such that this <= di
            JoinPolicy jp = (JoinPolicy) pol;
            for (Policy Di : jp.joinComponents()) {
                temp.clear();
                if (labelUtil.relabelsTo(this, Di, temp)) {
                    s.addAll(temp);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JoinPolicy) {
            JoinPolicy that = (JoinPolicy) o;
            return this == that
                    || this.joinComponents().equals(that.joinComponents());
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return components.hashCode();
    }

    @Override
    public final String toString() {
        String str = "";
        for (Iterator<Policy> iter = components.iterator(); iter.hasNext();) {
            str += iter.next().toString();
            if (iter.hasNext()) str += "; ";
        }
        return str;
    }
}

package jif.types;
import java.util.Random;

import polyglot.main.Report;

/** A unique ID. Used to create fresh objects. 
 */
public class UID implements java.io.Serializable 
{
    final public String name;   // for debugging only
    final int index; // for debugging only
    final long nonce, salt; //<nonce,salt> would make a virtually unique Id.
    UID equiv;
    private static int next;
    private static Random random = new Random(System.currentTimeMillis());

    public UID(String name) {
	this.name = name;
	this.index = next++;
	this.equiv = this;
	this.nonce = System.currentTimeMillis();
	this.salt = random.nextLong();
    }
    
    public String name() {
	return name;
    }

    public boolean equals(Object o) {
	if (o instanceof UID) {
	    UID u = (UID) o;
	    UID uu = find(u);
	    UID ii = find(this);
	    return uu.nonce == ii.nonce &&
		   uu.salt == ii.salt;
	}

	return false;
    }

    /** Equate two uids. */
    public void equate(UID uid) {
	this.equiv = find(uid);
    }

    /** Find the representative for the equivalence class of this uid.
     * This is just the classic union-find with path compression. */
    private UID find(UID u) {
	if (u.equiv != u) {
	    u.equiv = find(u.equiv);
	}
	return u.equiv;
    }

    public String toString() {
	UID u = find(this);

        if (Report.should_report(Report.debug, 2)) { 
            if (u != this) {
                return name + index + "=" + u;
            }
            else
                return name + index + " ("+nonce+")";
        }
        else if (Report.should_report(Report.debug, 1)) {
            if (u != this) {
                return name + index + "=" + u;
            }
            else
                return name + index;
        }
        
        if (u != this) {
            return u.toString();
        }
        else
            return name + index;
    }

    public int hashCode() {
	UID u = find(this);

	if (u != this) {
	    return u.hashCode();
	}

	return name.hashCode() + index;

    }
}

package jif.types.label;

import java.util.*;

import jif.translate.PolicyLabelToJavaExpr_c;
import jif.types.*;
import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.PrincipalHierarchy;
import jif.types.principal.Principal;
import jif.types.principal.PrincipalImpl;
import polyglot.types.*;
import polyglot.types.Resolver;
import polyglot.types.TypeObject;
import polyglot.util.*;

/** An implementation of the <code>PolicyLabel</code> interface. 
 */
public class PolicyLabel_c extends Label_c implements PolicyLabel {
    private final Principal owner;
    private final Collection readers;
    
    public PolicyLabel_c(Principal owner, Collection readers, JifTypeSystem ts, Position pos) {
        super(ts, pos, new PolicyLabelToJavaExpr_c()); 
        if (owner == null) throw new InternalCompilerError("null owner");
        this.owner = owner;
        this.readers = new HashSet(TypedList.copyAndCheck(new ArrayList(readers), Principal.class, true));
    }
    
    public Principal owner() {
        return this.owner;
    }
    public Collection readers() {
        return readers;
    }
    public boolean isComparable() { return true; }
    
    public boolean isCovariant() { return false; }
    public boolean isEnumerable() { return true; }
    public boolean isCanonical() {
        if (! owner.isCanonical()) {
            return false;
        }        
        for (Iterator i = readers.iterator(); i.hasNext(); ) {
            Principal r = (Principal) i.next();            
            if (! r.isCanonical()) {
                return false;
            }
        }        
        return true;
    }
    public boolean isDisambiguated() { return isCanonical(); }
    public boolean isRuntimeRepresentable() {
        if (! owner.isRuntimeRepresentable()) {
            return false;
        }
        
        for (Iterator i = readers.iterator(); i.hasNext(); ) {
            Principal r = (Principal) i.next();
            
            if (! r.isRuntimeRepresentable()) {
                return false;
            }
        }
        
        return true;
    }
    public boolean equalsImpl(TypeObject o) {
        return this == o;
    }
    
    public int hashCode() {
        return owner.hashCode() + readers.hashCode();
    }
    
    public boolean leq_(Label L, LabelEnv env) {
        if (! L.isSingleton() || ! L.isComparable() || ! L.isEnumerable()) {
            throw new InternalCompilerError("Cannot compare " + L);
        }

        L = L.singletonComponent();
        
        PrincipalHierarchy ph = env.ph();
        if (L instanceof PolicyLabel) {
            PolicyLabel that = (PolicyLabel) L;            
            // this = { o  : .. ri .. }
            // that = { o' : .. rj' .. }
            
            // o' >= o
            if (!ph.actsFor(that.owner(), this.owner)) {
                return false;
            }
            
            // for all j . rj' >= o || exists i . rj' >= ri
            for (Iterator j = that.readers().iterator(); j.hasNext(); ) {
                Principal rj = (Principal) j.next();                
                boolean sat = false;                
                if (ph.actsFor(rj, this.owner)) {
                    sat = true;
                }
                else {
                    for (Iterator i = this.readers.iterator(); i.hasNext(); ) {
                        Principal ri = (Principal) i.next();                        
                        if (ph.actsFor(rj, ri)) {
                            sat = true;
                            break;
                        }
                    }
                }
                
                if (!sat) {
                    return false;
                }
            }            
            return true;
        }        
        return false;
    }

    public String componentString() {
        StringBuffer sb = new StringBuffer(owner.toString());
        sb.append(": ");        
        for (Iterator i = readers.iterator(); i.hasNext(); ) {
            Principal p = (Principal) i.next();
            sb.append(p);        
            if (i.hasNext()) {
                sb.append(", ");        
            }
        }        
        return sb.toString();
    }
    
    public void translate(Resolver c, CodeWriter w) {
        w.write("jif.lang.Label.policy(");
        w.write(((PrincipalImpl)owner).translate(c) + ", ");
        w.write("new jif.lang.PrincipalSet()");
        
        for (Iterator i = readers.iterator(); i.hasNext(); ) {
            PrincipalImpl p = (PrincipalImpl) i.next();
            w.write(".add(" + p.translate(c) + ")");
        }
        
        w.write(")");
    }
    public Label subst(LocalInstance arg, Label l) {
        return this;
    }
    public Label subst(AccessPathRoot r, AccessPath e) {
        boolean changed = false;

        Principal newOwner = owner.subst(r, e);
        if (newOwner != owner) changed = true;
        Set newReaders = new HashSet(readers.size());
        
        
        for (Iterator i = readers.iterator(); i.hasNext(); ) {
            Principal rd = (Principal) i.next();
            Principal newRd = rd.subst(r, e);
            if (newRd != rd) changed = true;
            newReaders.add(newRd);
        }
        
        if (!changed) return this;

        return ((JifTypeSystem)typeSystem()).policyLabel(this.position(), newOwner, newReaders);        
    }
    public Label subst(LabelSubstitution substitution) throws SemanticException {
        boolean changed = false;

        Principal newOwner = owner.subst(substitution);
        if (newOwner != owner) changed = true;
        Set newReaders = new HashSet(readers.size());
        
        
        for (Iterator i = readers.iterator(); i.hasNext(); ) {
            Principal rd = (Principal) i.next();
            Principal newRd = rd.subst(substitution);
            if (newRd != rd) changed = true;
            newReaders.add(newRd);
        }
        
        if (!changed) return this;

        JifTypeSystem ts = (JifTypeSystem)typeSystem();
        PolicyLabel newLabel = ts.policyLabel(this.position(), newOwner, newReaders);
        return substitution.substLabel(newLabel);
    }
    
}

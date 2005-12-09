package jif.types.label;

import java.util.*;

import jif.translate.JifToJavaRewriter;
import jif.translate.LabelJToJavaExpr_c;
import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.PrincipalHierarchy;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.types.SemanticException;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.TypedList;

/** An implementation of the <code>PolicyLabel</code> interface. 
 */
public class ReaderPolicy_c extends LabelJ_c implements ReaderPolicy {
    private final Principal owner;
    private final Collection readers;
    
    public ReaderPolicy_c(Principal owner, Collection readers, JifTypeSystem ts, Position pos) {
        super(ts, pos); 
        if (owner == null) throw new InternalCompilerError("null owner");
        this.owner = owner;
        this.readers = new LinkedHashSet(TypedList.copyAndCheck(new ArrayList(readers), Principal.class, true));
    }
    
    public Principal owner() {
        return this.owner;
    }
    public Collection readers() {
        return readers;
    }
    public boolean isComparable() { return true; }
    
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
    public boolean isDisambiguatedImpl() { return isCanonical(); }
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
        if (this == o) return true;
        if (o instanceof ReaderPolicy_c) {
            ReaderPolicy_c that = (ReaderPolicy_c)o;
            if (this.owner == that.owner || (this.owner != null && this.owner.equals(that.owner))) {
                return this.readers.equals(that.readers);
            }
        }
        return false;
    }
    
    public int hashCode() {
        return (owner==null?0:owner.hashCode()) ^ readers.hashCode();
    }
    
    public boolean leq_(LabelJ L, LabelEnv env) {
        PrincipalHierarchy ph = env.ph();
        if (L instanceof ReaderPolicy) {
            ReaderPolicy that = (ReaderPolicy) L;            
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

    public String componentString(Set printedLabels) {
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
    
    public List throwTypes(TypeSystem ts) {
        List throwTypes = new ArrayList();
        throwTypes.addAll(owner.throwTypes(ts));
        for (Iterator i = readers.iterator(); i.hasNext(); ) {
            Principal r = (Principal) i.next();
            throwTypes.addAll(r.throwTypes(ts));
        }
        return throwTypes; 
    }

    public LabelJ subst(LabelSubstitution substitution) throws SemanticException {
        boolean changed = false;

        Principal newOwner = owner.subst(substitution);
        if (newOwner != owner) changed = true;
        Set newReaders = new LinkedHashSet(readers.size());
        
        
        for (Iterator i = readers.iterator(); i.hasNext(); ) {
            Principal rd = (Principal) i.next();
            Principal newRd = rd.subst(substitution);
            if (newRd != rd) changed = true;
            newReaders.add(newRd);
        }
        
        if (!changed) return substitution.substLabelJ(this);

        JifTypeSystem ts = (JifTypeSystem)typeSystem();
        ReaderPolicy newLabel = ts.readerPolicy(this.position(), newOwner, newReaders);
        return substitution.substLabelJ(newLabel);
    }
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        // check each principal in turn.
        PathMap X = owner.labelCheck(A, lc);
        for (Iterator i = readers.iterator(); i.hasNext(); ) {
            A.setPc(X.N());
            Principal r = (Principal) i.next();
            PathMap Xr = r.labelCheck(A, lc);
            X = X.join(Xr);            
        }
        return X;
    }

    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava((ReaderPolicy)this, rw);
    }    
}

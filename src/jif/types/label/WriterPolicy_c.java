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
public class WriterPolicy_c extends LabelM_c implements WriterPolicy {
    private final Principal owner;
    private final Collection writers;
    
    public WriterPolicy_c(Principal owner, Collection writers, JifTypeSystem ts, Position pos) {
        super(ts, pos); 
        if (owner == null) throw new InternalCompilerError("null owner");
        this.owner = owner;
        this.writers = new LinkedHashSet(TypedList.copyAndCheck(new ArrayList(writers), Principal.class, true));
    }
    
    public Principal owner() {
        return this.owner;
    }
    public Collection writers() {
        return writers;
    }
    public boolean isComparable() { return true; }
    
    public boolean isEnumerable() { return true; }
    public boolean isCanonical() {
        if (! owner.isCanonical()) {
            return false;
        }        
        for (Iterator i = writers.iterator(); i.hasNext(); ) {
            Principal w = (Principal) i.next();            
            if (! w.isCanonical()) {
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
        
        for (Iterator i = writers.iterator(); i.hasNext(); ) {
            Principal w = (Principal) i.next();
            
            if (! w.isRuntimeRepresentable()) {
                return false;
            }
        }
        
        return true;
    }
        
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof WriterPolicy_c) {
            WriterPolicy_c that = (WriterPolicy_c)o;
            if (this.owner == that.owner || (this.owner != null && this.owner.equals(that.owner))) {
                return this.writers.equals(that.writers);
            }
        }
        return false;
    }
    
    public int hashCode() {
        return (owner==null?0:owner.hashCode()) ^ writers.hashCode();
    }
    
    public boolean leq_(LabelM L, LabelEnv env) {
        PrincipalHierarchy ph = env.ph();
        if (L instanceof WriterPolicy) {
            WriterPolicy that = (WriterPolicy) L;            
            // this = { o  !: .. wi .. }
            // that = { o' !: .. wj' .. }
            
            // o >= o'
            if (!ph.actsFor(this.owner, that.owner())) {
                return false;
            }
            
            // for all i . wi >= o || exists j . wi >= wj'
            for (Iterator i = this.writers().iterator(); i.hasNext(); ) {
                Principal wi = (Principal) i.next();                
                boolean sat = false;                
                if (ph.actsFor(that.owner(), wi)) {
                    sat = true;
                }
                else {
                    for (Iterator j = that.writers().iterator(); j.hasNext(); ) {
                        Principal wj = (Principal) j.next();                        
                        if (ph.actsFor(wi, wj)) {
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
        sb.append("!: ");        
        for (Iterator i = writers.iterator(); i.hasNext(); ) {
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
        for (Iterator i = writers.iterator(); i.hasNext(); ) {
            Principal r = (Principal) i.next();
            throwTypes.addAll(r.throwTypes(ts));
        }
        return throwTypes; 
    }

    public LabelM subst(LabelSubstitution substitution) throws SemanticException {
        boolean changed = false;

        Principal newOwner = owner.subst(substitution);
        if (newOwner != owner) changed = true;
        Set newWriters = new LinkedHashSet(writers.size());
        
        
        for (Iterator i = writers.iterator(); i.hasNext(); ) {
            Principal w = (Principal) i.next();
            Principal newW = w.subst(substitution);
            if (newW != w) changed = true;
            newWriters.add(newW);
        }
        
        if (!changed) return substitution.substLabelM(this);

        JifTypeSystem ts = (JifTypeSystem)typeSystem();
        WriterPolicy newLabel = ts.writerPolicy(this.position(), newOwner, newWriters);
        return substitution.substLabelM(newLabel);
    }
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        // check each principal in turn.
        PathMap X = owner.labelCheck(A, lc);
        for (Iterator i = writers.iterator(); i.hasNext(); ) {
            A.setPc(X.N());
            Principal r = (Principal) i.next();
            PathMap Xr = r.labelCheck(A, lc);
            X = X.join(Xr);            
        }
        return X;
    }
    
    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava((WriterPolicy)this, rw);               
    }    
    
}

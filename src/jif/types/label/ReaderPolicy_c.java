package jif.types.label;

import java.util.*;

import jif.translate.JifToJavaRewriter;
import jif.translate.ReaderPolicyToJavaExpr_c;
import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import jif.types.hierarchy.PrincipalHierarchy;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.ast.Expr;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>PolicyLabel</code> interface. 
 */
public class ReaderPolicy_c extends Label_c implements ReaderPolicy {
    private final Principal owner;
    private final Principal reader;
    
    public ReaderPolicy_c(Principal owner, Principal reader, JifTypeSystem ts, Position pos) {
        super(ts, pos, new ReaderPolicyToJavaExpr_c()); 
        if (owner == null) throw new InternalCompilerError("null owner");
        this.owner = owner;
        this.reader = reader;
    }    
    
    public Principal owner() {
        return this.owner;
    }
    public Principal reader() {
        return reader;
    }
        
    public boolean isComparable() { return true; }    
    public boolean isCovariant() { return false; }
    public boolean isEnumerable() { return true; }
    public boolean isCanonical() {
        return owner.isCanonical() && reader.isCanonical();
    }
    public boolean isDisambiguatedImpl() { return isCanonical(); }
    public boolean isRuntimeRepresentable() {
        return owner.isRuntimeRepresentable() && reader.isRuntimeRepresentable();
    }
        
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof ReaderPolicy_c) {
            ReaderPolicy_c that = (ReaderPolicy_c)o;
            if (this.owner == that.owner || (this.owner != null && this.owner.equals(that.owner))) {
                return this.reader.equals(that.reader);
            }
        }
        return false;
    }
    
    public int hashCode() {
        return (owner==null?0:owner.hashCode()) ^ reader.hashCode() ^ 948234;
    }
    
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        PrincipalHierarchy ph = env.ph();
        if (L instanceof ReaderPolicy) {
            ReaderPolicy that = (ReaderPolicy) L;            
            // this = { o  : .. ri .. }
            // that = { o' : .. rj' .. }
            
            // o' >= o
            if (!ph.actsFor(that.owner(), this.owner)) {
                return false;
            }
            
            return ph.actsFor(that.reader(), this.owner()) ||
                    ph.actsFor(that.reader(), this.reader());
        }        
        return false;
    }

    public String componentString(Set printedLabels) {
        StringBuffer sb = new StringBuffer(owner.toString());
        sb.append(": ");        
        if (!reader.isTopPrincipal()) sb.append(reader.toString());        
        return sb.toString();
    }
    
    public List throwTypes(TypeSystem ts) {
        List throwTypes = new ArrayList();
        throwTypes.addAll(owner.throwTypes(ts));
        throwTypes.addAll(reader.throwTypes(ts));
        return throwTypes; 
    }

    public Label subst(LabelSubstitution substitution) throws SemanticException {
        boolean changed = false;

        Principal newOwner = owner.subst(substitution);
        if (newOwner != owner) changed = true;
        Principal newReader = reader.subst(substitution);
        if (newReader != reader) changed = true;
                
        if (!changed) return substitution.substLabel(this);

        JifTypeSystem ts = (JifTypeSystem)typeSystem();
        ReaderPolicy newLabel = ts.readerPolicy(this.position(), newOwner, newReader);
        return substitution.substLabel(newLabel);
    }
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        // check each principal in turn.
        PathMap X = owner.labelCheck(A, lc);
        A.setPc(X.N());
        PathMap Xr = reader.labelCheck(A, lc);
        X = X.join(Xr);            
        return X;
    }

    public boolean isBottomConfidentiality() {
        return owner.isBottomPrincipal() && reader.isBottomPrincipal();
    }
    public boolean isTopConfidentiality() {
        return owner.isTopPrincipal() && reader.isTopPrincipal();
    }
    public boolean isBottomIntegrity() { return false; }
    public boolean isTopIntegrity() { return false; }    
}

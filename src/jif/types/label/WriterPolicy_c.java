package jif.types.label;

import java.util.*;

import jif.translate.*;
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
public class WriterPolicy_c extends Label_c implements WriterPolicy {
    private final Principal owner;
    private final Principal writer;
    
    public WriterPolicy_c(Principal owner, Principal writer, JifTypeSystem ts, Position pos) {
        super(ts, pos, new WriterPolicyToJavaExpr_c()); 
        if (owner == null) throw new InternalCompilerError("null owner");
        this.owner = owner;
        this.writer = writer;
    }
    
    public Principal owner() {
        return this.owner;
    }
    public Principal writer() {
        return writer;
    }
    public boolean isComparable() { return true; }
    public boolean isCovariant() { return false; }
    
    public boolean isEnumerable() { return true; }
    public boolean isCanonical() {
        return owner.isCanonical() && writer.isCanonical();
    }
    public boolean isDisambiguatedImpl() { return isCanonical(); }
    public boolean isRuntimeRepresentable() {
        return owner.isRuntimeRepresentable() && writer.isRuntimeRepresentable();
    }
        
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (o instanceof WriterPolicy_c) {
            WriterPolicy_c that = (WriterPolicy_c)o;
            if (this.owner == that.owner || (this.owner != null && this.owner.equals(that.owner))) {
                return this.writer.equals(that.writer);
            }
        }
        return false;
    }
    
    public int hashCode() {
        return (owner==null?0:owner.hashCode()) ^ writer.hashCode()  ^ 1234352;
    }
    
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
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
            return ph.actsFor(this.writer(), that.owner()) ||
                ph.actsFor(this.writer(), that.writer());
        }        
        return false;
    }

    public String componentString(Set printedLabels) {
        StringBuffer sb = new StringBuffer(owner.toString());
        sb.append("!: ");        
        if (!writer.isTopPrincipal()) sb.append(writer.toString());        
        return sb.toString();
    }
    
    public List throwTypes(TypeSystem ts) {
        List throwTypes = new ArrayList();
        throwTypes.addAll(owner.throwTypes(ts));
        throwTypes.addAll(writer.throwTypes(ts));
        return throwTypes; 
    }

    public Label subst(LabelSubstitution substitution) throws SemanticException {
        boolean changed = false;

        Principal newOwner = owner.subst(substitution);
        if (newOwner != owner) changed = true;
        Principal newWriter = writer.subst(substitution);
        if (newWriter != writer) changed = true;

        if (!changed) return substitution.substLabel(this);

        JifTypeSystem ts = (JifTypeSystem)typeSystem();
        WriterPolicy newLabel = ts.writerPolicy(this.position(), newOwner, newWriter);
        return substitution.substLabel(newLabel);
    }
    public PathMap labelCheck(JifContext A, LabelChecker lc) {
        // check each principal in turn.
        PathMap X = owner.labelCheck(A, lc);
        A.setPc(X.N());
        PathMap Xr = writer.labelCheck(A, lc);
        X = X.join(Xr);            
        return X;
    }
    
    public Expr toJava(JifToJavaRewriter rw) throws SemanticException {
        return toJava.toJava((WriterPolicy)this, rw);               
    }

    public boolean isBottomConfidentiality() { return false; }
    public boolean isTopConfidentiality() { return false; }
    public boolean isBottomIntegrity() {
        return owner.isBottomPrincipal() && writer.isTopPrincipal();
    }
    public boolean isTopIntegrity() {
        return owner.isTopPrincipal() && writer.isBottomPrincipal();
    }        
}

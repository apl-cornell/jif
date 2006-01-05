package jif.types.label;

import java.util.*;

import jif.translate.CannotLabelToJavaExpr_c;
import jif.types.*;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

/** An implementation of the <code>DynamicLabel</code> interface. 
 */
public class WritersToReadersLabel_c extends Label_c implements WritersToReadersLabel {
    final Label label;
    public WritersToReadersLabel_c(Label label, JifTypeSystem ts, Position pos) {
        super(ts, pos, new CannotLabelToJavaExpr_c()); 
        this.label = label;
        setDescription("converts the writers of " + label
                       + " into readers");
    }
    public Label label() {
        return label;
    }
    public boolean isRuntimeRepresentable() {
        return false;
    }
    public boolean isCovariant() {
        return false;
    }
    public boolean isComparable() {
        return true;
    }
    public boolean isCanonical() { return true; }
    protected boolean isDisambiguatedImpl() { return label.isCanonical(); }
    public boolean isEnumerable() {
        return true;
    }
    public boolean equalsImpl(TypeObject o) {
        if (this == o) return true;
        if (! (o instanceof WritersToReadersLabel)) {
            return false;
        }           
        WritersToReadersLabel that = (WritersToReadersLabel) o;
        return (this.label.equals(that.label()));
    }
    public int hashCode() {
        return label.hashCode() ^ 597829;
    }
    
    public String componentString(Set printedLabels) {
        if (Report.should_report(Report.debug, 1)) { 
            return "<writersToReaders " + label + ">";
        }
        return "writersToReaders("+label()+")";
    }
    
    public boolean leq_(Label L, LabelEnv env, LabelEnv.SearchState state) {
        return false;
    }
    
    public List throwTypes(TypeSystem ts) {
        return Collections.EMPTY_LIST;
    }
    public Label subst(LabelSubstitution substitution) throws SemanticException {
        WritersToReadersLabel lbl = this;
        Label newLabel = lbl.label().subst(substitution);
        
        if (newLabel != lbl.label()) {
            JifTypeSystem ts = (JifTypeSystem)typeSystem();
            lbl = ts.writersToReadersLabel(lbl.position(), newLabel);
        }
        return substitution.substLabel(lbl);
    }
    
    
    public ReaderPolicy transform(LabelEnv env) {        
        ReaderPolicy rp = transform(env, label());
//        System.out.println(this.toString() + " becomes " + rp);
        return rp;
    }
    
    protected static ReaderPolicy transform(LabelEnv env, Label label) {
        // We are implementing the following transformation:
        // writersToReaders( {o1:!w1; ...; ok:!wk; ok1:r1; ...; okn:rn})
        //         = { o1 and ... and ok : w1 or ... wk or o1 or ... or ok }
        
        // If the label contains something other than reader and writer policies, then
        // we find an upper bound of the label with reader and writer policies, and use 
        // that instead.
        
        JifTypeSystem ts = (JifTypeSystem)label.typeSystem();
        
        
        Set owners = new HashSet();
        Set writers = new HashSet();
        for (Iterator iter = label.components().iterator(); iter.hasNext();) {
            Label c = (Label)iter.next();
            if (c instanceof ReaderPolicy) {
                // ignore reader policies
                continue;
            }
            else if (c instanceof WriterPolicy) {
                WriterPolicy wp = (WriterPolicy)c;
                owners.add(wp.owner());
                writers.add(wp.writer());
            }
            else if (c instanceof ArgLabel) {
                Label L = findPolicyUpperBound(c, env, new HashSet());
                processPolicies(L,owners,writers);
            }
            else if (c instanceof ParamLabel) {
                Label L = findPolicyUpperBound(c, env, new HashSet());
                processPolicies(L,owners,writers);
            }
            else if (c instanceof CovariantParamLabel) {
                Label L = findPolicyUpperBound(c, env, new HashSet());
                processPolicies(L,owners,writers);
            }    
            else if (c instanceof DynamicLabel) {
                Label L = findPolicyUpperBound(c, env, new HashSet());
                processPolicies(L,owners,writers);
            }    
            else if (c instanceof ThisLabel) {
                Label L = findPolicyUpperBound(c, env, new HashSet());
                processPolicies(L,owners,writers);
            }    
            else {
                throw new InternalCompilerError("WritersToReaders undefined " +
                                                "for " + label);
            }            
        }
        Position pos = label.position();
        
        // all owners are implicitly writers
        writers.addAll(owners);
        if (owners.isEmpty()) {
            // no labels at all. We can be a bit more precise and return a label that permits no readers
            return ts.readerPolicy(pos,
                                   ts.topPrincipal(pos),
                                   ts.topPrincipal(pos));
        }
        return ts.readerPolicy(pos,
                               ts.conjunctivePrincipal(pos, owners),
                               ts.disjunctivePrincipal(pos, writers));
    }
    
    private static Label findPolicyUpperBound(Label label, LabelEnv env, Set visited) {
        // Find a label consisting only of reader and writer 
        // policies that is an upper bound for the label L
        
        JifTypeSystem ts = (JifTypeSystem)label.typeSystem();
        if (visited.contains(label)) {
            // recursively defined upper bound
            // return a conservative answer            
            return ts.topLabel(label.position());
        }
        visited.add(label);

        Set newPolicies = new HashSet();
        for (Iterator iter = label.components().iterator(); iter.hasNext();) {
            Label c = (Label)iter.next();
            if (c instanceof Policy) {
                newPolicies.add(c);
                continue;
            }
            Label ub;
            if (c instanceof ArgLabel) {
                ArgLabel L = (ArgLabel)c;
                ub = findPolicyUpperBound(L.upperBound(), env, visited);
            }
            else if (c instanceof ParamLabel) {
                ParamLabel L = (ParamLabel)c;
                ub = findPolicyUpperBound(env.findUpperBound(L), env, visited);
            }
            else if (c instanceof CovariantParamLabel) {
                CovariantParamLabel L = (CovariantParamLabel)c;
                ub = findPolicyUpperBound(env.findUpperBound(L), env, visited);
            }    
            else if (c instanceof DynamicLabel) {
                DynamicLabel L = (DynamicLabel)c;
                ub = findPolicyUpperBound(env.findUpperBound(L), env, visited);
            }    
            else if (c instanceof ThisLabel) {
                ThisLabel L = (ThisLabel)c;
                ub = findPolicyUpperBound(env.findUpperBound(L), env, visited);
            }
            else {
                throw new InternalCompilerError("Unexpected label type");
            }
            newPolicies.addAll(ub.components());
        }
        return ts.joinLabel(label.position(), newPolicies);
    }
    
    /**
     * Add the writers and writers from label l to the sets owners and writers
     */
    private static void processPolicies(Label l, Set owners, Set writers) {
        if (l instanceof ReaderPolicy) {
            // ignore reader policies
            return;
        }
        else if (l instanceof WriterPolicy) {
            WriterPolicy wp = (WriterPolicy)l;
            owners.add(wp.owner());
            writers.add(wp.writer());
        }
        else if (l.components().isEmpty()) {
            // ignore empty labels
        }
        else if (!l.isSingleton()) {
            for (Iterator iter = l.components().iterator(); iter.hasNext();) {
                Label c = (Label)iter.next();
                processPolicies(c, owners, writers);
            }
        }
        else {
            throw new InternalCompilerError("Can't process non policy: " + l);
        }
        
    }
}

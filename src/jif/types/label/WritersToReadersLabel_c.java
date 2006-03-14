package jif.types.label;

import java.util.*;

import jif.ExtensionInfo;
import jif.Topics;
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

    public IntegPolicy integProjection() {
        return ((JifTypeSystem)ts).bottomIntegPolicy(position());
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
        if (substitution.recurseIntoChildren(lbl)) {
            Label newLabel = lbl.label().subst(substitution);
                
            if (newLabel != lbl.label()) {
                JifTypeSystem ts = (JifTypeSystem)typeSystem();
                lbl = ts.writersToReadersLabel(lbl.position(), newLabel);
            }
        }
        return substitution.substLabel(lbl);
    }
    
    public boolean hasWritersToReaders() {
        return true;        
    }
    
    
    public Label transform(LabelEnv env) {
        return transform(env, label(), new LinkedList());
    }
    
    protected static Label transform(LabelEnv env, Label label, List visited) {
        JifTypeSystem ts = (JifTypeSystem)label.typeSystem();
        if (visited.contains(label)) return ts.bottomLabel();

        Label result = null;
        if (label instanceof PairLabel) {
            PairLabel pl = (PairLabel)label;   
            ConfPolicy newCP = transformIntegToCong(pl.integPolicy());
            result = ts.pairLabel(pl.position(), newCP, ts.bottomIntegPolicy(pl.position()));
        }
        else if (label instanceof JoinLabel) {
            JoinLabel L = (JoinLabel)label;

            Set comps = new LinkedHashSet();
            for (Iterator iter = L.joinComponents().iterator(); iter.hasNext();) {
                Label c = (Label)iter.next();
                comps.add(transform(env, c, visited));
            }
            result = ts.meetLabel(label.position(), comps);
        }
        else if (label instanceof MeetLabel) {
            MeetLabel L = (MeetLabel)label;

            Set comps = new LinkedHashSet();
            for (Iterator iter = L.meetComponents().iterator(); iter.hasNext();) {
                Label c = (Label)iter.next();
                comps.add(transform(env, c, visited));
            }
            result = ts.joinLabel(label.position(), comps);
        }
        else if (label instanceof ArgLabel) {
            ArgLabel L = (ArgLabel)label;
            visited.add(0, L);
            result = transform(env, env.findUpperBound(L), visited);
            if (L != visited.remove(0)) {
                throw new InternalCompilerError("Stack discipline broken");
            }
        }
        else if (label instanceof ParamLabel) {
            ParamLabel L = (ParamLabel)label;
            result = transform(env, env.findUpperBound(L), visited);
        }
        else if (label instanceof CovariantParamLabel) {
            CovariantParamLabel L = (CovariantParamLabel)label;
            result = transform(env, env.findUpperBound(L), visited);            
        }    
        else if (label instanceof DynamicLabel) {
            DynamicLabel L = (DynamicLabel)label;
            result = transform(env, env.findUpperBound(L), visited);
        }    
        else if (label instanceof ThisLabel) {
            ThisLabel L = (ThisLabel)label;
//            Label thisUpperBound = A.entryPC();
//            return transform(env, thisUpperBound, visited, true);
            Label thisUpperBound = env.findUpperBound(L);
            result = transform(env, thisUpperBound, visited);  
            if (Report.should_report(Topics.jif, 3)) { 
                Report.report(3, "Transforming " + label + " with ub " + 
                              thisUpperBound + " in env " + env + 
                              " to " + result);
            }
        }  
        else if (label instanceof VarLabel_c) {
            // cant do anything.
            result = ts.writersToReadersLabel(label.position(), label);
        }
        else {
            throw new InternalCompilerError("WritersToReaders undefined " +
                        "for " + label);
        }
        if (Report.should_report(Topics.jif, 3)) { 
            Report.report(3, "Transformed " + label + " to " + result);
        }
//        System.err.println("Transformed " + label + " to " + result);
        return result;        
    }
    
    protected static ConfPolicy transformIntegToCong(IntegPolicy pol) {
        JifTypeSystem ts = (JifTypeSystem)pol.typeSystem();
        if (pol instanceof WriterPolicy) {
            WriterPolicy wp = (WriterPolicy)pol;
            return ts.readerPolicy(wp.position(), wp.owner(), wp.writer());
        }
        if (pol instanceof JoinIntegPolicy_c) {
            JoinPolicy_c jp = (JoinPolicy_c)pol;
            Collection newPols = new ArrayList(jp.joinComponents().size());
            for (Iterator iter = jp.joinComponents().iterator(); iter.hasNext();) {
                IntegPolicy ip = (IntegPolicy)iter.next();
                ConfPolicy cp = transformIntegToCong(ip);
                newPols.add(cp);
            }
            return ts.meetConfPolicy(jp.position(), newPols);
        }
        if (pol instanceof MeetIntegPolicy_c) {
            MeetPolicy_c mp = (MeetPolicy_c)pol;
            Collection newPols = new ArrayList(mp.meetComponents().size());
            for (Iterator iter = mp.meetComponents().iterator(); iter.hasNext();) {
                IntegPolicy ip = (IntegPolicy)iter.next();
                ConfPolicy cp = transformIntegToCong(ip);
                newPols.add(cp);
            }
            return ts.joinConfPolicy(mp.position(), newPols);
        }
        throw new InternalCompilerError("Unexpected integ policy: " + pol);
    }
    
}

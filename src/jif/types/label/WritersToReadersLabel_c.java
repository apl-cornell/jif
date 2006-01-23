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
    
    
    public Label transform(LabelEnv env) {
        return transform(env, label(), new HashSet());
    }
    
    protected static Label transform(LabelEnv env, Label label, Set visited) {
        JifTypeSystem ts = (JifTypeSystem)label.typeSystem();
        if (visited.contains(label)) return ts.bottomLabel();

        if (label instanceof PairLabel) {
            PairLabel pl = (PairLabel)label;   
            ConfPolicy newCP = transformIntegToCong(pl.integPolicy());
            return ts.pairLabel(pl.position(), newCP, ts.bottomIntegPolicy(pl.position()));
        }
        else if (label instanceof JoinLabel) {
            JoinLabel L = (JoinLabel)label;

            Label result = ts.topLabel();
            for (Iterator iter = L.joinComponents().iterator(); iter.hasNext();) {
                Label c = (Label)iter.next();
                result = ts.meet(result, transform(env, c, visited));
            }
            return result;
        }
        else if (label instanceof MeetLabel) {
            MeetLabel L = (MeetLabel)label;

            Label result = ts.bottomLabel();
            for (Iterator iter = L.meetComponents().iterator(); iter.hasNext();) {
                Label c = (Label)iter.next();
                result = ts.join(result, transform(env, c, visited));
            }
            return result;
        }
        else if (label instanceof ArgLabel) {
            ArgLabel L = (ArgLabel)label;
            visited.add(L);
            return transform(env, L.upperBound(), visited);
        }
        else if (label instanceof ParamLabel) {
            ParamLabel L = (ParamLabel)label;
            return transform(env, env.findUpperBound(L), visited);            
        }
        else if (label instanceof CovariantParamLabel) {
            CovariantParamLabel L = (CovariantParamLabel)label;
            return transform(env, env.findUpperBound(L), visited);            
        }    
        else if (label instanceof DynamicLabel) {
            DynamicLabel L = (DynamicLabel)label;
            return transform(env, env.findUpperBound(L), visited);            
        }    
        else if (label instanceof ThisLabel) {
            ThisLabel L = (ThisLabel)label;
//            Label thisUpperBound = A.entryPC();
//            return transform(env, thisUpperBound, visited, true);
            return transform(env, env.findUpperBound(L), visited);            
        }    
        else {
            throw new InternalCompilerError("WritersToReaders undefined " +
                        "for " + label);
        }
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

package jif.types.label;

import java.util.*;

import jif.translate.CannotLabelToJavaExpr_c;
import jif.types.*;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.hierarchy.LabelEnv;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.types.TypeObject;
import polyglot.types.TypeSystem;
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
        return transform(env, label(), new HashSet(), true);
    }
    
    protected static Label transform(LabelEnv env, Label label, Set visited, boolean topLevel) {
        JifTypeSystem ts = (JifTypeSystem)label.typeSystem();
        if (visited.contains(label)) return ts.bottomLabel();

        if (label.isBottom()) 
            return ts.readerPolicy(label.position(), 
                                   ts.topPrincipal(label.position()), 
                                   ts.topPrincipal(label.position()));
        
        if (label instanceof WriterPolicy) {
            WriterPolicy L = (WriterPolicy)label;
            return ts.readerPolicy(L.position(), L.owner(), L.writer());
        }
        else if (label instanceof ReaderPolicy) {
            ReaderPolicy L = (ReaderPolicy)label;
            if (topLevel) {
                // this label only has a reader policy, so the writer policy
                // is effectively {*!:*}
                return ts.readerPolicy(label.position(), 
                                       ts.topPrincipal(label.position()), 
                                       ts.topPrincipal(label.position()));
            }
            return ts.bottomLabel();
        }
        else if (label instanceof JoinLabel) {
            JoinLabel L = (JoinLabel)label;

            Label result = ts.bottomLabel();
            for (Iterator iter = L.components().iterator(); iter.hasNext();) {
                Label c = (Label)iter.next();
                result = ts.join(result, transform(env, c, visited, false));
            }
            return result;
        }
        else if (label instanceof ArgLabel) {
            ArgLabel L = (ArgLabel)label;
            visited.add(L);
            return transform(env, L.upperBound(), visited, true);
        }
        else if (label instanceof ParamLabel) {
            ParamLabel L = (ParamLabel)label;
            return ts.bottomLabel();            
        }
        else if (label instanceof CovariantParamLabel) {
            CovariantParamLabel L = (CovariantParamLabel)label;
            return ts.bottomLabel();            
        }    
        else if (label instanceof DynamicLabel) {
            DynamicLabel L = (DynamicLabel)label;
            return ts.bottomLabel();            
        }    
        else if (label instanceof ThisLabel) {
            ThisLabel L = (ThisLabel)label;
//            Label thisUpperBound = A.entryPC();
//            return transform(env, thisUpperBound, visited, true);
            return ts.bottomLabel();            
        }    
        else {
            throw new InternalCompilerError("WritersToReaders undefined " +
                        "for " + label);
        }
    }
}

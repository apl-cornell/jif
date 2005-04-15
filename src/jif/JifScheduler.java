package jif;

import java.util.Iterator;

import jif.ast.JifNodeFactory;
import jif.translate.JifToJavaRewriter;
import jif.types.JifSubstClassType_c;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import polyglot.ext.jl.JLScheduler;
import polyglot.frontend.Job;
import polyglot.frontend.goals.FieldConstantsChecked;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.goals.VisitorGoal;
import polyglot.types.FieldInstance;
import polyglot.types.ParsedClassType;

public class JifScheduler extends JLScheduler {
   OutputExtensionInfo jlext;

    public JifScheduler(jif.ExtensionInfo extInfo, OutputExtensionInfo jlext) {
        super(extInfo);
        this.jlext = jlext;
    }

    public LabelCheckGoal LabelsChecked(Job job) {
        return (LabelCheckGoal)internGoal(new LabelCheckGoal(job));
    }

    public FieldLabelInferenceGoal FieldLabelInference(Job job) {
        return (FieldLabelInferenceGoal)internGoal(new FieldLabelInferenceGoal(job));
    }

    public Goal JifToJavaRewritten(Job job) {
        JifTypeSystem ts = (JifTypeSystem) extInfo.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) extInfo.nodeFactory();
        return internGoal(new VisitorGoal(job, new JifToJavaRewriter(job, ts, nf, jlext)));        
    }
    public Goal FieldConstantsChecked(FieldInstance fi) {
        return internGoal(new JifFieldConstantsChecked(fi));
    }
    public boolean runToCompletion() {
        if (super.runToCompletion()) {
            // Create a goal to compile every source file.
            for (Iterator i = jlext.scheduler().jobs().iterator(); i.hasNext(); ) {
                Job job = (Job) i.next();
                jlext.scheduler().addGoal(jlext.getCompileGoal(job));
            }
            return jlext.scheduler().runToCompletion();
        }
        return false;
    }
    
    private static class JifFieldConstantsChecked extends FieldConstantsChecked {
        public JifFieldConstantsChecked(FieldInstance fi) {
            super(fi);
        }
        protected ParsedClassType findContainer() {
            if (var().container() instanceof JifSubstType) {
                JifSubstType jst = (JifSubstType)var().container();
                if (jst.base() instanceof ParsedClassType) {
                    return (ParsedClassType) jst.base();
                }
            }
            return super.findContainer();
        }        
    }

}
package jif;

import java.util.Iterator;

import jif.ast.JifNodeFactory;
import jif.translate.JifToJavaRewriter;
import jif.types.JifTypeSystem;
import polyglot.ext.jl.JLScheduler;
import polyglot.frontend.Job;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.goals.VisitorGoal;

public class JifScheduler extends JLScheduler {
   OutputExtensionInfo jlext;

    public JifScheduler(jif.ExtensionInfo extInfo, OutputExtensionInfo jlext) {
        super(extInfo);
        this.jlext = jlext;
    }

    public LabelCheckGoal LabelsChecked(Job job) {
        return (LabelCheckGoal)internGoal(new LabelCheckGoal(job));
    }

//    public Goal PostJavaRewriteBarrier() {
//        return internGoal(new Barrier(this) {
//            public Goal goalForJob(Job j) {
//                return JifScheduler.this.JifToJavaRewritten(j);
//            }
//        });
//    }

    public Goal JifToJavaRewritten(Job job) {
        JifTypeSystem ts = (JifTypeSystem) extInfo.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) extInfo.nodeFactory();
        return internGoal(new VisitorGoal(job, new JifToJavaRewriter(job, ts, nf, jlext)));        
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
}

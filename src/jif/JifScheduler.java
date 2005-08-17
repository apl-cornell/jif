package jif;

import java.util.Iterator;

import jif.ast.JifNodeFactory;
import jif.translate.JifToJavaRewriter;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import jif.visit.JifInitChecker;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ext.jl.JLScheduler;
import polyglot.frontend.*;
import polyglot.frontend.goals.*;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;

public class JifScheduler extends JLScheduler {
   OutputExtensionInfo jlext;
   /**
    * Hack to ensure that we track the job for java.lang.Object specially.
    * In particular, ensure that it is submitted for re-writing before
    * any other job.
    */
   private Job objectJob = null;

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
        Goal g = internGoal(new VisitorGoal(job, new JifToJavaRewriter(job, ts, nf, jlext)));     
        
        try {
            // make sure that if Object.jif is being compiled, it is always
            // written to Java before any other job.
            if (objectJob != null && job != objectJob)
                addPrerequisiteDependency(g, JifToJavaRewritten(objectJob));
        }
        catch (CyclicDependencyException e) {
            // Cannot happen
            throw new InternalCompilerError(e);
        }
        return g;
    }
    public Goal FieldConstantsChecked(FieldInstance fi) {
        Goal g = internGoal(new JifFieldConstantsChecked(fi));
        try {
            if (fi.container() instanceof ParsedTypeObject) {
                ParsedTypeObject t = (ParsedTypeObject) fi.container();
                if (t.job() != null) {
                    addConcurrentDependency(g, ConstantsChecked(t.job()));
                }
                if (t instanceof ParsedClassType) {
                    ParsedClassType ct = (ParsedClassType) t;
                    addPrerequisiteDependency(g, SignaturesResolved(ct));
                }
            }
        }
        catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
    }
    public Goal InitializationsChecked(Job job) {
        TypeSystem ts = extInfo.typeSystem();
        NodeFactory nf = extInfo.nodeFactory();
        Goal g = internGoal(new VisitorGoal(job, new JifInitChecker(job, ts, nf)));
        try {
            addPrerequisiteDependency(g, ReachabilityChecked(job));
        }
        catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
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

    /**
     * 
     */
    public Job addJob(Source source, Node ast) {
        Job j = super.addJob(source, ast);
        if ("Object.jif".equals(source.name())) {
            this.objectJob = j;
        }
        return j;
    }
    /**
     * 
     */
    public Job addJob(Source source) {
        Job j = super.addJob(source);
        if ("Object.jif".equals(source.name())) {
            this.objectJob = j;
        }
        return j;
    }
}

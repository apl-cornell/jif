package jif;

import java.util.Iterator;

import jif.ast.JifNodeFactory;
import jif.translate.JifToJavaRewriter;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import jif.visit.JifInitChecker;
import jif.visit.NotNullChecker;
import jif.visit.PreciseClassChecker;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.*;
import polyglot.frontend.goals.FieldConstantsChecked;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.goals.VisitorGoal;
import polyglot.types.*;
import polyglot.util.InternalCompilerError;

public class JifScheduler extends JLScheduler {
    protected OutputExtensionInfo jlext;
    /**
     * Hack to ensure that we track the job for java.lang.Object specially.
     * In particular, ensure that it is submitted for re-writing before
     * any other job.
     */
    protected Job objectJob = null;

    public JifScheduler(jif.ExtensionInfo extInfo, OutputExtensionInfo jlext) {
        super(extInfo);
        this.jlext = jlext;
    }

    public LabelCheckGoal LabelsChecked(Job job) {
        LabelCheckGoal g = (LabelCheckGoal)internGoal(new LabelCheckGoal(job));

        try {
            addPrerequisiteDependency(g, this.FieldLabelInference(job));
        }
        catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;

    }

    public FieldLabelInferenceGoal FieldLabelInference(Job job) {
        FieldLabelInferenceGoal g = (FieldLabelInferenceGoal)internGoal(new FieldLabelInferenceGoal(job));

        try {
            addPrerequisiteDependency(g, this.ExceptionsChecked(job));
        }
        catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;

    }

    public Goal PreciseClassChecker(Job job) {
        Goal g = internGoal(new VisitorGoal(job, new PreciseClassChecker(job)));

        try {
            addPrerequisiteDependency(g, this.ReachabilityChecked(job));
        }
        catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;

    }

    public Goal NotNullChecker(Job job) {
        Goal g = internGoal(new VisitorGoal(job, new NotNullChecker(job)));

        try {
            addPrerequisiteDependency(g, this.ReachabilityChecked(job));
        }
        catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;

    }


    public Goal ExceptionsChecked(Job job) {
        Goal g = super.ExceptionsChecked(job);
        try {
            addPrerequisiteDependency(g, this.NotNullChecker(job));
            addPrerequisiteDependency(g, this.PreciseClassChecker(job));
        }
        catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
    }

    public Goal JifToJavaRewritten(Job job) {
        JifTypeSystem ts = (JifTypeSystem) extInfo.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) extInfo.nodeFactory();
        Goal g = internGoal(new VisitorGoal(job, new JifToJavaRewriter(job, ts, nf, jlext)));     

        try {
            addPrerequisiteDependency(g, this.Serialized(job));
            
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

    public Goal Serialized(Job job) {
        Goal g = super.Serialized(job);
        try {
            addPrerequisiteDependency(g, this.LabelsChecked(job));
        }
        catch (CyclicDependencyException e) {
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
                    addCorequisiteDependency(g, ConstantsChecked(t.job()));
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

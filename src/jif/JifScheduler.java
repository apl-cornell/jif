package jif;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jif.ast.JifNodeFactory;
import jif.translate.JifToJavaRewriter;
import jif.types.JifSubstType;
import jif.types.JifTypeSystem;
import jif.visit.FinalParams;
import jif.visit.IntegerBoundsChecker;
import jif.visit.JifInitChecker;
import jif.visit.JifTypeChecker;
import jif.visit.NativeConstructorAdder;
import jif.visit.NotNullChecker;
import jif.visit.PreciseClassChecker;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.frontend.CyclicDependencyException;
import polyglot.frontend.JLExtensionInfo;
import polyglot.frontend.JLScheduler;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.frontend.Source;
import polyglot.frontend.goals.Barrier;
import polyglot.frontend.goals.EmptyGoal;
import polyglot.frontend.goals.FieldConstantsChecked;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.goals.VisitorGoal;
import polyglot.types.FieldInstance;
import polyglot.types.ParsedClassType;
import polyglot.types.ParsedTypeObject;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;

public class JifScheduler extends JLScheduler {
    protected JLExtensionInfo jlext;
    /**
     * Hack to ensure that we track the job for java.lang.Object specially.
     * In particular, ensure that it is submitted for re-writing before
     * any other job.
     */
    protected Job objectJob = null;

    public JifScheduler(jif.ExtensionInfo extInfo, JLExtensionInfo jlext) {
        super(extInfo);
        this.jlext = jlext;
    }

    public Goal LabelsChecked(Job job) {
        JifOptions opts = (JifOptions) job.extensionInfo().getOptions();
        Goal ig;
        if (opts.skipLabelChecking) {
            ig = new EmptyGoal(job, "LabelsChecked");
        } else {
            ig = new LabelCheckGoal(job, true);
        }
        Goal g = internGoal(ig);
        try {
            addPrerequisiteDependency(g, this.FinalParamsBarrier());
            addPrerequisiteDependency(g, this.FieldLabelInference(job));
            addPrerequisiteDependency(g, this.IntegerBoundsChecker(job));
            addPrerequisiteDependency(g, this.ExceptionsChecked(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
    }

    /**
     * Creates a goal for the label double-checking pass. Labels need to be
     * double checked because the labels inferred during the first
     * label-checking pass may be unsound.
     */
    public Goal LabelsDoubleChecked(Job job) {
        JifOptions opts = (JifOptions) job.extensionInfo().getOptions();
        Goal ig;
        if (opts.skipLabelChecking) {
            ig = new EmptyGoal(job, "LabelsDoubleChecked");
        } else {
            ig = new LabelCheckGoal(job, false) {
                // Create an anonymous subclass to avoid conflating with the
                // goal for LabelsChecked.
            };
        }
        Goal g = internGoal(ig);
        try {
            addPrerequisiteDependency(g, this.LabelsChecked(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
    }

    public Goal FinalParamsBarrier() {
        Goal g = internGoal(new Barrier("FinalParamsBarrier", this) {
            @Override
            public Goal goalForJob(Job job) {
                return FinalParams(job);
            }
        });
        return g;
    }

    public Goal FinalParams(Job job) {
        JifTypeSystem ts = (JifTypeSystem) extInfo.typeSystem();
        NodeFactory nf = extInfo.nodeFactory();
        Goal g = internGoal(new VisitorGoal(job, new FinalParams(job, ts, nf)));

        try {
            addPrerequisiteDependency(g, this.TypeChecked(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
    }

    public FieldLabelInferenceGoal FieldLabelInference(Job job) {
        FieldLabelInferenceGoal g = (FieldLabelInferenceGoal) internGoal(
                new FieldLabelInferenceGoal(job));

// Jif Dependency bugfix
        try {
//          addPrerequisiteDependency(g, this.ExceptionsChecked(job));
            addPrerequisiteDependency(g, this.Disambiguated(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;

    }

    public Goal IntegerBoundsChecker(Job job) {
        Goal g = internGoal(
                new VisitorGoal(job, new IntegerBoundsChecker(job)));

        try {
            addPrerequisiteDependency(g, this.ReachabilityChecked(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;

    }

    @Override
    public Goal TypeChecked(Job job) {
        TypeSystem ts = extInfo.typeSystem();
        NodeFactory nf = extInfo.nodeFactory();
        Goal g = TypeChecked.create(this, job, ts, nf);
        return g;
    }

    public Goal PreciseClassChecker(Job job) {
        Goal g = internGoal(new VisitorGoal(job, new PreciseClassChecker(job)));

        try {
            addPrerequisiteDependency(g, this.ReachabilityChecked(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;

    }

    public Goal NotNullChecker(Job job) {
        Goal g = internGoal(new VisitorGoal(job, new NotNullChecker(job)));

        try {
            addPrerequisiteDependency(g, this.ReachabilityChecked(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;

    }

    @Override
    public Goal ExceptionsChecked(Job job) {
        TypeSystem ts = extInfo.typeSystem();
        NodeFactory nf = extInfo.nodeFactory();
        Goal g = JifExceptionsChecked.create(this, job, ts, nf);

        try {
            addPrerequisiteDependency(g, this.NotNullChecker(job));
            addPrerequisiteDependency(g, this.PreciseClassChecker(job));
            addPrerequisiteDependency(g, this.IntegerBoundsChecker(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
    }

    public Goal JifToJavaRewritten(Job job) {
        JifTypeSystem ts = (JifTypeSystem) extInfo.typeSystem();
        JifNodeFactory nf = (JifNodeFactory) extInfo.nodeFactory();
        Goal g = internGoal(new VisitorGoal(job,
                new JifToJavaRewriter(job, ts, nf, jlext)));

        try {
            addPrerequisiteDependency(g, this.Serialized(job));

            // make sure that if Object.jif is being compiled, it is always
            // written to Java before any other job.
            if (objectJob != null && job != objectJob)
                addPrerequisiteDependency(g, JifToJavaRewritten(objectJob));
        } catch (CyclicDependencyException e) {
            // Cannot happen
            throw new InternalCompilerError(e);
        }
        return g;
    }

    @Override
    public Goal Validated(Job job) {
        Goal g = super.Validated(job);
        try {
            addPrerequisiteDependency(g, this.LabelsDoubleChecked(job));
            addPrerequisiteDependency(g, this.NativeConstructorsAdded(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
    }

    public Goal NativeConstructorsAdded(Job job) {
        NodeFactory nf = extInfo.nodeFactory();
        Goal g = internGoal(
                new VisitorGoal(job, new NativeConstructorAdder(nf)));

        try {
            addPrerequisiteDependency(g, this.TypeChecked(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
    }

    @Override
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
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
    }

    @Override
    public Goal InitializationsChecked(Job job) {
        TypeSystem ts = extInfo.typeSystem();
        NodeFactory nf = extInfo.nodeFactory();
        Goal g = internGoal(
                new VisitorGoal(job, new JifInitChecker(job, ts, nf)));
        try {
            addPrerequisiteDependency(g, ReachabilityChecked(job));
        } catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }
        return g;
    }

    @Override
    public boolean runToCompletion() {
        if (super.runToCompletion()) {
            // Create a goal to compile every source file.
            for (Job job : jlext.scheduler().jobs()) {
                jlext.scheduler().addGoal(jlext.getCompileGoal(job));
            }
            return jlext.scheduler().runToCompletion();
        }
        return false;
    }

    private static class JifFieldConstantsChecked
            extends FieldConstantsChecked {
        public JifFieldConstantsChecked(FieldInstance fi) {
            super(fi);
        }

        @Override
        protected ParsedClassType findContainer() {
            if (var().container() instanceof JifSubstType) {
                JifSubstType jst = (JifSubstType) var().container();
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
    @Override
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
    @Override
    public Job addJob(Source source) {
        Job j = super.addJob(source);
        if ("Object.jif".equals(source.name())) {
            this.objectJob = j;
        }
        return j;
    }

}

class TypeChecked extends VisitorGoal {
    public static Goal create(Scheduler scheduler, Job job, TypeSystem ts,
            NodeFactory nf) {
        return scheduler.internGoal(new TypeChecked(job, ts, nf));
    }

    protected TypeChecked(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, new JifTypeChecker(job, ts, nf));
    }

    @Override
    public Collection<Goal> prerequisiteGoals(Scheduler scheduler) {
        List<Goal> l = new ArrayList<Goal>();
        l.add(scheduler.Disambiguated(job));
        l.addAll(super.prerequisiteGoals(scheduler));
        return l;
    }

    @Override
    public Collection<Goal> corequisiteGoals(Scheduler scheduler) {
        List<Goal> l = new ArrayList<Goal>();
//        l.add(((JifScheduler)scheduler).FinalParams(job));
        // Should this line be here, since FieldLabelResolver is added as a missing dependency during runtime?
        l.add(((JifScheduler) scheduler).FieldLabelInference(job));
        l.add(scheduler.ConstantsChecked(job));
        l.addAll(super.corequisiteGoals(scheduler));
        return l;
    }
}

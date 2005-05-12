package jif;

import java.util.ArrayList;
import java.util.List;

import jif.visit.JifTranslator;
import polyglot.ext.jl.JLScheduler;
import polyglot.frontend.*;
import polyglot.frontend.goals.CodeGenerated;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.goals.Parsed;
import polyglot.main.Options;
import polyglot.types.LoadedClassResolver;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

/**
 * This is an extension to jl that is the target language of
 * the jif extension.  The only differences between this language
 * and jl is in the translation pass.  The input language is
 * identical.  This extension cannot live on its own: it only provides
 * the passes after EXC_CHECK_ALL: that is those which deal with
 * translation.
 */
public class OutputExtensionInfo extends polyglot.ext.jl.ExtensionInfo {
    ExtensionInfo jifExtInfo;
    Job objectJob = null;
    
    public OutputExtensionInfo(ExtensionInfo jifExtInfo) {
        this.jifExtInfo = jifExtInfo;        
    }
    
    public Options getOptions() {
        return jifExtInfo.getOptions();
    }
    
    void setObjectJob(Job objectJob) {
        this.objectJob = objectJob;
    }
    
    public Scheduler createScheduler() {
        return new OutputScheduler(this);
    }
    
    protected List compileGoalList(final Job job) {
        List l = new ArrayList(super.compileGoalList(job));
        Goal last = (Goal) l.get(l.size()-1);
        if (last instanceof CodeGenerated) {
            l.remove(l.size()-1);
        }                                                                   

        CodeGenerated output = new CodeGenerated(job) {
            public Pass createPass(ExtensionInfo extInfo) {
                return new OutputPass(this, new JifTranslator(job, typeSystem(),
                                                              nodeFactory(), targetFactory()));
            }            
        };

        l.add(scheduler.internGoal(output));
        
        if ("Object.jif".equals(job.source().name())) {
            this.setObjectJob(job);
        }

        try {
            if (this.objectJob != null && job != this.objectJob)
            scheduler().addPrerequisiteDependency(scheduler().TypesInitialized(job),
                                                  scheduler().TypesInitialized(objectJob));
        }
        catch (CyclicDependencyException e) {
            // Cannot happen
            throw new InternalCompilerError(e);
        }
           
        
        return l;
    }
    
    static class OutputScheduler extends JLScheduler {
        OutputScheduler(OutputExtensionInfo extInfo) {
            super(extInfo);
        }
    
        public Goal Parsed(Job job) {
            return internGoal(new Parsed(job) {
                public Pass createPass(ExtensionInfo extInfo) {
                    return new EmptyPass(this) {
                        public boolean run() {
                            markRun();
                            return true;
                        }
                    };              
                }
            });
        }
    }
    
    protected void initTypeSystem() {
        try {
            LoadedClassResolver lr;
            lr = new LoadedClassResolver(typeSystem(), 
                    jifExtInfo.getJifOptions().constructOutputExtClasspath(),
                    compiler.loader(), 
                    version(), 
                    true);
            ts.initialize(lr, this);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(
                "Unable to initialize type system.", e);
        }
    }
}

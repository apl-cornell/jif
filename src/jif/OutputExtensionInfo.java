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
    
    public OutputExtensionInfo(ExtensionInfo jifExtInfo) {
        this.jifExtInfo = jifExtInfo;        
    }
    
    public Options getOptions() {
        return jifExtInfo.getOptions();
    }
    
    public Scheduler createScheduler() {
        return new OutputScheduler(this);
    }

    public Goal getCompileGoal(final Job job) {
        CodeGenerated output = new CodeGenerated(job) {
            public Pass createPass(ExtensionInfo extInfo) {
                return new OutputPass(this, new JifTranslator(job, typeSystem(),
                                                              nodeFactory(), targetFactory()));
            }            
        };

        output = (CodeGenerated) scheduler.internGoal(output);

        try {
            scheduler().addPrerequisiteDependency(output,
                                                  scheduler().Serialized(job));
        }
        catch (CyclicDependencyException e) {
            // Cannot happen
            throw new InternalCompilerError(e);
        }

        return output;
    }
        
    static class OutputScheduler extends JLScheduler {
        Job objectJob = null;
    
        OutputScheduler(OutputExtensionInfo extInfo) {
            super(extInfo);
        }

        void setObjectJob(Job objectJob) {
            this.objectJob = objectJob;
        }

        public Goal TypesInitialized(Job job) {
            if ("Object.jif".equals(job.source().name())) {
                this.setObjectJob(job);
            }

            Goal g = super.TypesInitialized(job);
            try {
                if (this.objectJob != null && job != this.objectJob)
                    addPrerequisiteDependency(g, TypesInitialized(objectJob));
            }
            catch (CyclicDependencyException e) {
                // Cannot happen
                throw new InternalCompilerError(e);
            }
            return g;
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

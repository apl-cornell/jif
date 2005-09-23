package jif;

import java.io.Reader;
import java.util.*;

import jif.ast.JifNodeFactory;
import jif.ast.JifNodeFactory_c;
import jif.types.JifTypeSystem;
import jif.types.JifTypeSystem_c;
import jif.visit.*;
import jif.visit.JifLabelSubst;
import jif.visit.NotNullChecker;
import polyglot.ast.NodeFactory;
import polyglot.frontend.*;
import polyglot.frontend.Compiler;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.goals.VisitorGoal;
import polyglot.main.Options;
import polyglot.types.*;
import polyglot.util.ErrorQueue;
import polyglot.util.InternalCompilerError;

/** The configuration information of the Jif extension.
 *
 *  Compiling passes and corresponding visitors:
 *  <ul>
 *	<li> ... clean super barrier, inherited from the jl extension </li>
 *	<li> rewrite argument labels, <code>RewriteArgsVisitor</code> </li>
 *	<li> add dummy fields, <code>AddDummyFieldsVisitor</code> </li>
 *	<li> clean signatures, inherited from the jl extension </li>
 *      <li> resolve field labels, <code>FieldLabelResolver </li>
 *	<li> ... check exceptions, inherited from the jl extension </li>
 *	<li> (barrier) check labels, <code>LabelChecker</code> </li>
 *	<li> serialization </li>
 *	<li> translation, <code>JifTranslator</code> </li>
 *  </ul>
 */
public class ExtensionInfo extends polyglot.ext.jl.ExtensionInfo
{
//    protected boolean doInfer = false;
    protected OutputExtensionInfo jlext = new OutputExtensionInfo(this);
    protected OutputExtensionInfo jlrtext = new OutputExtensionInfo(this);

    public String defaultFileExtension() {
	return "jif";
    }

    public String compilerName() {
        return "jifc";
    }

    protected Options createOptions() {
        return new JifOptions(this);
    }

    public JifOptions getJifOptions() {
        return (JifOptions)this.getOptions();
    }
    
    static public Set topics = new HashSet();
    static { topics.add("jif"); }

    private TypeSystem jlTypeSystem() {
        // Use a JL type system for looking up principals.
        return jlrtext.typeSystem();
    }

    protected TypeSystem createTypeSystem() {
        // For looking up Java code during rewriting.
	return new JifTypeSystem_c(jlTypeSystem());
    }

    public void initCompiler(Compiler compiler) {
        jlext.initCompiler(compiler);
        jlrtext.initCompiler(compiler);
        super.initCompiler(compiler);
    }

    protected void initTypeSystem() {
        try {
            LoadedClassResolver lr;
            lr = new SourceClassResolver(compiler, this, 
                    getJifOptions().constructJifClasspath(),
                    compiler.loader(), false,
		    getOptions().compile_command_line_only);
            ts.initialize(lr, this);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(
                "Unable to initialize type system: ", e);
        }
    }

    protected NodeFactory createNodeFactory() {
	return new JifNodeFactory_c();
    }

    public polyglot.main.Version version() {
	return new Version();
    }

    public Parser parser(Reader reader, FileSource source, ErrorQueue eq) {

      polyglot.lex.Lexer lexer =
          new jif.parse.Lexer_c(reader, source, eq);
      polyglot.parse.BaseParser grm =
          new jif.parse.Grm(lexer, (JifTypeSystem)ts,
					 (JifNodeFactory)nf, eq);

      return new CupParser(grm, source, eq);
    }

    public static class JifJobExt implements JobExt {
      public JifJobExt(JifTypeSystem ts) {     }
    }

    public JobExt jobExt() {
      return new JifJobExt((JifTypeSystem) typeSystem());
    }

    protected Scheduler createScheduler() {
        return new JifScheduler(this, jlext);
    }

    public Goal getCompileGoal(Job job) {
        JifScheduler jifScheduler = (JifScheduler)scheduler();
        
        List l = new ArrayList();

        // add not null check and precise classes check before exception checking
        l.add(jifScheduler.ReachabilityChecked(job));

        l.add(jifScheduler.internGoal(new VisitorGoal(job, new NotNullChecker(job, ts, nf))));
        l.add(jifScheduler.internGoal(new VisitorGoal(job, new PreciseClassChecker(job, ts, nf))));

        l.add(jifScheduler.ExceptionsChecked(job));

        // add field label inference after exception checking.
        FieldLabelInferenceGoal fliGoal = jifScheduler.FieldLabelInference(job);
        l.add(fliGoal);

        // add label checking after field label inference.
        LabelCheckGoal labelCheckGoal = jifScheduler.LabelsChecked(job);
        l.add(labelCheckGoal);

        l.add(jifScheduler.Serialized(job));

        // add the jif to java rewrite at the end of the list.
        Goal g = jifScheduler.internGoal(jifScheduler.JifToJavaRewritten(job));
        l.add(g);

        try {
            jifScheduler.addPrerequisiteDependencyChain(l);
        }
        catch (CyclicDependencyException e) {
            throw new InternalCompilerError(e);
        }

        return g;
    }
    static {
        // touch Topics to force the static initializer to be loaded.
        String s = Topics.jif;
    }
}

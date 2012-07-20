package jif;

import static java.io.File.pathSeparator;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.tools.FileObject;

import jif.ast.JifNodeFactory;
import jif.ast.JifNodeFactory_c;
import jif.types.JifTypeSystem;
import jif.types.JifTypeSystem_c;
import jif.visit.LabelChecker;
import polyglot.ast.NodeFactory;
import polyglot.filemanager.FileManager;
import polyglot.frontend.*;
import polyglot.frontend.Compiler;
import polyglot.frontend.goals.Goal;
import polyglot.main.Options;
import polyglot.types.LoadedClassResolver;
import polyglot.types.SemanticException;
import polyglot.types.SourceClassResolver;
import polyglot.types.TypeSystem;
import polyglot.types.reflect.ClassFileLoader;
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
public class ExtensionInfo extends JLExtensionInfo
{
//  protected boolean doInfer = false;
    protected OutputExtensionInfo jlext = new OutputExtensionInfo(this);

    @Override
    public String defaultFileExtension() {
        return "jif";
    }

    @Override
    public String compilerName() {
        return "jifc";
    }
    
    @Override
    protected Options createOptions() {
        return new JifOptions(this);
    }

    public JifOptions getJifOptions() {
        return (JifOptions)this.getOptions();
    }

    static public Set<String> topics = new LinkedHashSet<String>();
    static { topics.add("jif"); }

    protected TypeSystem jlTypeSystem() {
        // Use a JL type system for looking up principals.
        return jlext.typeSystem();
    }

    @Override
    protected TypeSystem createTypeSystem() {
        // Need to pass it the jlTypeSystem() so that
        // it can look up jif.lang.Principal.
        return new JifTypeSystem_c(jlTypeSystem());
    }
    
    @Override
    public void initCompiler(Compiler compiler) {
        jlext.initCompiler(compiler);
        super.initCompiler(compiler);
    }

    @Override
    protected void initTypeSystem() {
        try {
            LoadedClassResolver lr;
            lr = new SourceClassResolver(compiler, this, false,
                    getOptions().compile_command_line_only,
                    getOptions().ignore_mod_times);
            ts.initialize(lr, this);
        }
        catch (SemanticException e) {
            throw new InternalCompilerError(
                                            "Unable to initialize type system: ", e);
        }
    }
    
    @Override
    protected FileManager createFileManager() {
        JifOptions options = getJifOptions();
        FileManager fm = super.createFileManager();
        // use the signature classpath if it exists for compiling Jif classes
        List<File> path = new ArrayList<File>();
        path.addAll(options.sigcp);
        path.addAll(options.classpath_directories);
        try {
            fm.setLocation(options.signature_path, path);
        } catch (IOException e) {
            throw new InternalCompilerError(e);
        }
        return fm;
    }

    @Override
    protected NodeFactory createNodeFactory() {
        return new JifNodeFactory_c();
    }

    @Override
    public polyglot.main.Version version() {
        return new Version();
    }

    @Override
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

    @Override
    public JobExt jobExt() {
        return new JifJobExt((JifTypeSystem) typeSystem());
    }

    @Override
    protected Scheduler createScheduler() {
        return new JifScheduler(this, jlext);
    }

    public LabelChecker createLabelChecker(Job job, boolean solvePerClassBody, boolean solvePerMethod, boolean doLabelSubst) {
        return new LabelChecker(job, typeSystem(), nodeFactory(), solvePerClassBody, solvePerMethod, doLabelSubst);
    }
    
    @Override
    public Goal getCompileGoal(Job job) {
        JifScheduler jifScheduler = (JifScheduler)scheduler();
        return jifScheduler.JifToJavaRewritten(job);
    }
    static {
        // touch Topics to force the static initializer to be loaded.
        Topics.jif.toLowerCase();
    }

    @Override
    public FileSource createFileSource(FileObject f, boolean user)
            throws IOException {
        return new jif.parse.UTF8FileSource(f, user);
    }
    
    @Override
    public ClassFileLoader classFileLoader() {
        if (classFileLoader == null) {
            super.classFileLoader();
            classFileLoader.addLocation(getJifOptions().signature_path);
        }
        return classFileLoader;
    }
}

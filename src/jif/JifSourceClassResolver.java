package jif;

import java.io.File;
import java.util.Map;

import jif.types.JifClassType;
import jif.types.JifTypeSystem;
import jif.types.SemanticDetailedException;
import jif.types.principal.Principal;
import polyglot.frontend.Compiler;
import polyglot.frontend.ExtensionInfo;
import polyglot.main.Version;
import polyglot.types.ClassType;
import polyglot.types.LoadedClassResolver;
import polyglot.types.Named;
import polyglot.types.SemanticException;
import polyglot.types.SourceClassResolver;
import polyglot.types.TypeSystem;
import polyglot.types.reflect.ClassFile;
import polyglot.types.reflect.ClassFileLoader;
import polyglot.util.InternalCompilerError;

public class JifSourceClassResolver extends SourceClassResolver {

	public JifSourceClassResolver(Compiler compiler, ExtensionInfo ext,
			String classpath, ClassFileLoader loader, boolean allowRawClasses,
			boolean compileCommandLineOnly, boolean ignoreModTimes) {
		super(compiler, ext, classpath, loader, allowRawClasses,
				compileCommandLineOnly, ignoreModTimes);
	}

	protected ClassType getEncodedType(ClassFile clazz, String name) throws SemanticException {
		ClassType ct = super.getEncodedType(clazz, name);
		
		// Check that the provider of the class file matches the provider of the class type
		if(ct instanceof JifClassType) {
			JifTypeSystem jifts = (JifTypeSystem) ext.typeSystem();
			File classFile = new File(clazz.getClassFileLocation());
			if(jifts.isInitialized()) {
				JifClassType jct = (JifClassType) ct;
				Principal expectedProvider = jifts.providerForFile(classFile);
				if(expectedProvider.equals(jct.provider()))
					return ct;
				else 
					//This results in an ugly stacktrace, but throwing a semantic 
					// error results in a "class not found" error.
					throw new InternalCompilerError(classFile
							+ " has provider " + jct.provider()
							+ " but expected " + expectedProvider);
			}
			else {
	    		Map<File, String> pp = ((JifOptions)ext.getOptions()).providerPaths;
	    		if(pp != null) {
		    		File s = classFile;
	        		while(s != null && !pp.containsKey(s)) {
	        			s = s.getParentFile();
	        		}
	    			//TODO: Allow top to be specified on the cmdline
	        		if(s!=null)
	        			throw new InternalCompilerError(classFile + 
	        					" is required to initialize the type system" +
	        					" and so must have provider TOP.");
	    		}
	        	return ct;
			}
		}
		return ct;
	}
}

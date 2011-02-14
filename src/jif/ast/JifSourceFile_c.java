package jif.ast;

import java.io.File;
import java.util.List;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.principal.Principal;

import polyglot.ast.PackageNode;
import polyglot.ast.SourceFile;
import polyglot.ast.SourceFile_c;
import polyglot.frontend.Source;
import polyglot.types.Context;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class JifSourceFile_c extends SourceFile_c {

	protected Principal provider;
	public JifSourceFile_c(Position pos, PackageNode package1, List imports,
			List decls) {
		super(pos, package1, imports, decls);
	}

	@Override
	public Context enterScope(Context c) {
		JifContext A =  (JifContext) super.enterScope(c);
		if(provider == null) {
			JifTypeSystem jifts = (JifTypeSystem) A.typeSystem();
			File f = new File(source.path());
			provider = jifts.providerForFile(f);
			if(provider == null)
				throw new InternalCompilerError("Source file has no provider: " + this);
		}			
		A.setProvider(provider);
		return A;
	}
	
	public Principal provider() {
		return provider;
	}
}

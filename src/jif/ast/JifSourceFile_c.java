package jif.ast;

import java.io.File;
import java.util.List;

import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import polyglot.ast.Import;
import polyglot.ast.PackageNode;
import polyglot.ast.SourceFile_c;
import polyglot.ast.TopLevelDecl;
import polyglot.types.Context;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;

public class JifSourceFile_c extends SourceFile_c {

    protected Label provider;

    public JifSourceFile_c(Position pos, PackageNode package1,
            List<Import> imports, List<TopLevelDecl> decls) {
        super(pos, package1, imports, decls);
    }

    @Override
    public Context enterScope(Context c) {
        JifContext A = (JifContext) super.enterScope(c);
        if (provider == null) {
            JifTypeSystem jifts = (JifTypeSystem) A.typeSystem();
            File f = new File(source.path());
            provider = jifts.providerForFile(f);
            if (provider == null)
                throw new InternalCompilerError("Source file has no provider: "
                        + this);
        }
        A.setProvider(provider);
        return A;
    }

    public Label provider() {
        return provider;
    }
}

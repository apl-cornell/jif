package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.SourceFile;
import polyglot.frontend.Source;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class SourceFileToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        SourceFile n = (SourceFile) node();
        Source source = n.source();
        n = rw.java_nf().SourceFile(n.position(), n.package_(), n.imports(),
                n.decls());
        //n = (SourceFile)n.del(null);
        n = n.source(source);
        return rw.leavingSourceFile(n);
    }
}

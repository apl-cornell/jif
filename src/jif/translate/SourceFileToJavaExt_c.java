package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.SourceFile;
import polyglot.frontend.Source;
import polyglot.types.SemanticException;

public class SourceFileToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        SourceFile n = (SourceFile) node();
        Source source = n.source();
        n = (SourceFile) rw.java_nf().SourceFile(n.position(), n.decls());
        n = n.source(source);
        return rw.leavingSourceFile(n);
    }
}

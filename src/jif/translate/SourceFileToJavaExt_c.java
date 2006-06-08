package jif.translate;

import polyglot.ast.Node;
import polyglot.ast.SourceFile;
import polyglot.frontend.Source;
import polyglot.types.SemanticException;

public class SourceFileToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        SourceFile n = (SourceFile) node();
        Source source = n.source();
        n = (SourceFile) super.toJava(rw);
        n = n.source(source);
        return rw.leavingSourceFile(n);
    }
}

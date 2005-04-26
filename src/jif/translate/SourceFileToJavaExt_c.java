package jif.translate;

import polyglot.ast.*;
import polyglot.ext.jl.ast.*;
import jif.ast.*;
import polyglot.types.*;
import polyglot.ext.jl.types.*;
import jif.types.*;
import polyglot.visit.*;
import polyglot.frontend.Source;

public class SourceFileToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        SourceFile n = (SourceFile) node();
        Source source = n.source();
        n = (SourceFile) super.toJava(rw);
        n = n.source(source);
        return rw.leavingSourceFile(n);
    }
}

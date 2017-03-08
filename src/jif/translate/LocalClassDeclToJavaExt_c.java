package jif.translate;

import polyglot.ast.LocalClassDecl;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class LocalClassDeclToJavaExt_c extends ToJavaExt_c
        implements ToJavaExt {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        LocalClassDecl lcd = (LocalClassDecl) node();
        return rw.java_nf().LocalClassDecl(lcd.position(), lcd.decl());
    }
}

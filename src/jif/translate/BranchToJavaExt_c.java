package jif.translate;

import polyglot.ast.Branch;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.SerialVersionUID;

public class BranchToJavaExt_c extends ToJavaExt_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        Branch n = (Branch) node();
        return rw.java_nf().Branch(n.position(), n.kind(), n.labelNode());
    }
}

package jif.translate;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import polyglot.ast.ClassBody;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.types.ClassType;
import polyglot.types.Flags;
import polyglot.types.SemanticException;

public class ClassBodyToJavaExt_c extends ToJavaExt_c {
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        ClassBody body = (ClassBody) node();
        body = (ClassBody) super.toJava(rw);

        ClassType ct = rw.currentClass();

        if (ct.flags().isInterface() || !rw.jif_ts().isJifClass(ct)) {
            return body;
        }
        
        // All constructors have been rewritten--add a default.
        NodeFactory nf = rw.java_nf();

        ConstructorDecl d = nf.ConstructorDecl(ct.position(),
                Flags.PUBLIC, ct.name(), Collections.EMPTY_LIST,
                Collections.EMPTY_LIST, 
                nf.Block(ct.position(),
                         nf.ConstructorCall(ct.position(), 
                                            ConstructorCall.SUPER, 
                                            Collections.EMPTY_LIST)));
        
        List members = new LinkedList();
        members.add(d);
        members.addAll(body.members());
        
        return body.members(members);

    }
}

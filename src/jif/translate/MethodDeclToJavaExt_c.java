package jif.translate;

import java.util.List;

import jif.ast.JifMethodDecl;
import polyglot.ast.Block;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.types.Flags;
import polyglot.types.SemanticException;
import polyglot.util.CollectionUtil;
import polyglot.visit.NodeVisitor;

public class MethodDeclToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        // Bypass labels and constraints
        JifMethodDecl n = (JifMethodDecl) node();

        // Bypass startLabel, returnLabel and constraints.
        return rw.bypass(n.startLabel()).bypass(n.returnLabel()).bypass(n.constraints());
    }

    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        MethodDecl n = (MethodDecl) node();
        if ("main".equals(n.name()) && 
            n.flags().isStatic() && 
            n.formals().size() == 2) {
            // the method is static main(principal p, String[] args). We
            // need to translate this specially.
            // (The typechecking for JifMethodDecl ensures that the formals
            // are of the correct type.)
            return staticMainToJava(rw, n); 
        }
            
        n = rw.java_nf().MethodDecl(n.position(), n.flags(), n.returnType(),
                                    n.name(), n.formals(), n.throwTypes(),
                                    n.body());
        n = n.methodInstance(null);
        return n;
    }
    
    /** Rewrite static main(principal p, String[] args) {...} to 
     * static main(String[] args) {Principal p = Runtime.getUser(); {...} }; 
     */
    public Node staticMainToJava(JifToJavaRewriter rw, MethodDecl n) {
        Formal formal0 = (Formal)n.formals().get(0); // the principal
        Formal formal1 = (Formal)n.formals().get(1); // the string array
        List formalList = CollectionUtil.list(formal1);
        
        Block origBody = n.body();

        TypeNode type = rw.qq().parseType("jif.lang.Principal");
        Expr init = rw.qq().parseExpr("jif.runtime.Runtime.user()");
        
        Stmt declPrincipal = 
            rw.java_nf().LocalDecl(origBody.position(), 
                               Flags.FINAL, 
                               type,
                               formal0.name(),
                               init);
        Block newBody = rw.java_nf().Block(origBody.position(), 
                                           declPrincipal, 
                                           origBody);
                                             
        n = rw.java_nf().MethodDecl(n.position(), 
                                    n.flags(), 
                                    n.returnType(),
                                    n.name(), 
                                    formalList, 
                                    n.throwTypes(),
                                    newBody);
        n = n.methodInstance(null);
        return n;
    }

}

package jif.translate;

import java.util.LinkedList;
import java.util.List;

import jif.ast.JifConstructorDecl;
import polyglot.ast.Block;
import polyglot.ast.ConstructorCall;
import polyglot.ast.ConstructorDecl;
import polyglot.ast.Empty;
import polyglot.ast.If;
import polyglot.ast.MethodDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Return;
import polyglot.ast.Stmt;
import polyglot.ast.TypeNode;
import polyglot.types.ClassType;
import polyglot.types.ConstructorInstance;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;
import polyglot.visit.NodeVisitor;

public class ConstructorDeclToJavaExt_c extends ToJavaExt_c {
    public NodeVisitor toJavaEnter(JifToJavaRewriter rw) throws SemanticException {
        JifConstructorDecl n = (JifConstructorDecl) node();

        rw.inConstructor(true);

        // Bypass startLabel, returnLabel and constraints.
        return rw.bypass(n.startLabel()).bypass(n.returnLabel()).bypass(n.constraints());
    }

    /** Rewrite constructor C(a) to method C C$(a) */
    public Node toJava(JifToJavaRewriter rw) throws SemanticException {
        ConstructorDecl n = (ConstructorDecl) node();
        
        ConstructorInstance ci = n.constructorInstance();
        ClassType ct = ci.container().toClass();

        Node retVal;        
        // only translate jif constructors
        if (! rw.jif_ts().isJifClass(ct)) {
            NodeFactory nf = rw.java_nf();
            retVal = nf.ConstructorDecl(n.position(),
                                      n.flags(),
                                      n.name(),
                                      n.formals(),
                                      n.throwTypes(),
                                      n.body());            
        }
        else {
            retVal = jifClassConstructorDecl(rw, n);
        }
        
        rw.inConstructor(false);
        return retVal;
    }

    private Node jifClassConstructorDecl(JifToJavaRewriter rw, ConstructorDecl n) {
        NodeFactory nf = rw.java_nf();
        ConstructorInstance ci = n.constructorInstance();
        ClassType ct = ci.container().toClass();

        // Add an explicit return to the body.
        Block body = n.body();
        Return return_this = nf.Return(n.position(), nf.This(n.position()));

        if (body.statements().isEmpty() ||
            (body.statements().size() == 1 &&
             body.statements().get(0) instanceof Empty)) {

          body = nf.Block(n.position(), return_this);
        }
        else {
          
          // If this is a Jif class but the superclass is not a Jif class, then
          // we need to remove any calls to super constructors from body.
          // Previous checks should have ensured that it is the first statement
          // that is a constructor call, and that it is the default constructor
          if (rw.jif_ts.isJifClass(ct) && !rw.jif_ts.isJifClass(ct.superType())) {
              // check that the first statement of the body is a constructor call
              Stmt s = (Stmt)body.statements().get(0);
              if (!(s instanceof ConstructorCall)) {
                  throw new InternalCompilerError(body.position(), 
                               "Expected first statement of constructor of a " +
                               "Jif class with a non-Jif superclass to be a " +
                               "constructor call.");
              }
              ConstructorCall cc = (ConstructorCall)s;
              if (cc.kind() == ConstructorCall.SUPER) {
                  // it's a super call.
                  // check that it's the default constructor
                  if (cc.arguments().size() > 0) {
                      throw new InternalCompilerError(body.position(), 
                                   "Expected super constructor call to be the" +                                   "default constructor as we have a " +
                                   "Jif class with a non-Jif superclass.");
                  }
                  
                  // remove the default constructor.
                  List stmtList = new LinkedList(body.statements());
                  stmtList.remove(0);
                  body = body.statements(stmtList);                
              }
          }
          
          // if (true) body
          If if_true = nf.If(n.position(),
                              nf.BooleanLit(n.position(), true), body);

          // We need the if (true) to avoid problems with flow-checking.
          // { if (true) body; return this; }
          body = nf.Block(n.position(), if_true, return_this);
        }

        String name = (ct.fullName() + ".").replace('.', '$');
        
        TypeNode tn = rw.jif_nf().CanonicalTypeNode(n.position(), ct);
        tn = (TypeNode) tn.visit(rw);
        
        MethodDecl m = nf.MethodDecl(n.position(), n.flags(), tn, name,
                                     n.formals(), n.throwTypes(), body);
        m = m.methodInstance(null);
        
        return m;
    }
}

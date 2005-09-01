package jif.translate;

import java.util.*;

import jif.ast.JifConstructorDecl;
import polyglot.ast.*;
import polyglot.types.*;
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
        if (! rw.jif_ts().isParamsRuntimeRep(ct)) {
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

    private Node jifClassConstructorDecl(JifToJavaRewriter rw, ConstructorDecl n) throws SemanticException {
        NodeFactory nf = rw.java_nf();
        ConstructorInstance ci = n.constructorInstance();
        ClassType ct = ci.container().toClass();

        Block body = n.body();
        List inits = new ArrayList(3);

        // add a call to the initializer.
        inits.add(rw.qq().parseStmt("this." + 
                  ClassDeclToJavaExt_c.INITIALIZATIONS_METHOD_NAME + "();"));

        if (body.statements().isEmpty() ||
            (body.statements().size() == 1 &&
             body.statements().get(0) instanceof Empty)) {
            // no body to add...
        }
        else {

          // If this is a Jif class but the superclass is not a Jif class, then
          // we need to remove any calls to super constructors from body.
          // Previous checks should have ensured that the first statement
          // is either a this(...) call (permitted if the java superclass is
          // trusted) or the default super call, super().
          if (rw.jif_ts().isJifClass(ct) && !rw.jif_ts().isJifClass(ct.superType())) {
              // check that the first statement of the body is a constructor call
              Stmt s = (Stmt)body.statements().get(0);
              if (s instanceof ConstructorCall) {
                  ConstructorCall cc = (ConstructorCall)s;
                  if (cc.kind() == ConstructorCall.SUPER) {
                      // it's a super call.
                      // check that it's the default constructor
                      if (cc.arguments().size() > 0) {
                          throw new InternalCompilerError(body.position(),
                                       "Expected super constructor call to be the" +                                       "default constructor as we have a " +
                                       "Jif class with a non-Jif superclass.");
                      }
    
                      // remove the default constructor.
                      List stmtList = new LinkedList(body.statements());
                      stmtList.remove(0);
                      body = body.statements(stmtList);
                  }
              }
          }
          
          inits.add(body);
        }
        
        // Add an explicit return to the body.
        inits.add(nf.Return(n.position(), nf.This(n.position())));

        body = nf.Block(n.position(), inits);

        String name = (ct.fullName() + ".").replace('.', '$');

        TypeNode tn = rw.jif_nf().CanonicalTypeNode(n.position(), ct);
        tn = (TypeNode) tn.visit(rw);

        MethodDecl m = nf.MethodDecl(n.position(), n.flags(), tn, name,
                                     n.formals(), n.throwTypes(), body);
        m = m.methodInstance(null);

        return m;
    }
}

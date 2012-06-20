package jif.translate;

import jif.types.label.AccessPath;
import jif.types.label.AccessPathField;
import jif.types.label.AccessPathLocal;
import jif.types.label.AccessPathThis;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;
import polyglot.ast.Expr;
import polyglot.ast.NodeFactory;
import polyglot.types.FieldInstance;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;

public class DynamicPrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    @SuppressWarnings("unused")
    @Override
    public Expr toJava(Principal principal, JifToJavaRewriter rw) throws SemanticException {
        DynamicPrincipal p = (DynamicPrincipal) principal;
        if (p.path() instanceof AccessPathThis) {
          if (rw.context().inStaticContext()) {
            if (rw.staticThisPrincipal() == null)
              throw new Error("Cannot translate \"this\" principal in a static context!");
            else 
              return rw.staticThisPrincipal();
          }
        }
        return accessPathToExpr(rw, p.path());
    }
    
    protected Expr accessPathToExpr(JifToJavaRewriter rw, AccessPath ap) {
      NodeFactory nf = rw.java_nf();

      if (ap instanceof AccessPathThis) {
        return nf.This(ap.position());
      }
      else if (ap instanceof AccessPathLocal) {
        LocalInstance li = ((AccessPathLocal) ap).localInstance();
        return nf.Local(li.position(), nf.Id(li.position(), li.name()));
      }
      else if (ap instanceof AccessPathField) {
        AccessPathField apf = (AccessPathField) ap;
        FieldInstance fi = apf.fieldInstance();
        
        return nf.Field(ap.position(), accessPathToExpr(rw, apf.path()),
            nf.Id(fi.position(), fi.name()));
      }
      else {
          throw new Error("Don't know how to translate " + ap);
         // return rw.qq().parseExpr(ap.exprString());
      }
   }

}

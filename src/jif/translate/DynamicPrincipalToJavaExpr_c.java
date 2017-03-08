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
import polyglot.util.SerialVersionUID;

public class DynamicPrincipalToJavaExpr_c extends PrincipalToJavaExpr_c {
    private static final long serialVersionUID = SerialVersionUID.generate();

    @Override
    public Expr toJava(Principal principal, JifToJavaRewriter rw,
            Expr thisQualifier) throws SemanticException {
        DynamicPrincipal p = (DynamicPrincipal) principal;
        return accessPathToExpr(rw, p.path(), thisQualifier);
    }

    protected Expr accessPathToExpr(JifToJavaRewriter rw, AccessPath ap,
            Expr thisQualifier) {
        NodeFactory nf = rw.java_nf();

        if (ap instanceof AccessPathThis) {
            return thisQualifier;
        } else if (ap instanceof AccessPathLocal) {
            LocalInstance li = ((AccessPathLocal) ap).localInstance();
            return nf.Local(li.position(), nf.Id(li.position(), li.name()));
        } else if (ap instanceof AccessPathField) {
            AccessPathField apf = (AccessPathField) ap;
            FieldInstance fi = apf.fieldInstance();

            return nf.Field(ap.position(),
                    accessPathToExpr(rw, apf.path(), thisQualifier),
                    nf.Id(fi.position(), fi.name()));
        } else {
            throw new Error("Don't know how to translate " + ap);
        }
    }

}

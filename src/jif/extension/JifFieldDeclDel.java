package jif.extension;

import jif.types.JifFieldInstance;
import jif.visit.ConstChecker;
import polyglot.ast.Expr;
import polyglot.ast.FieldDecl;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>FieldDecl</code> node. 
 * 
 *  @see polyglot.ast.FieldDecl
 */
public class JifFieldDeclDel extends JifJL_c
{
    public JifFieldDeclDel() {
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        // Set the flag JifFieldInstance.hasInitializer correctly.
        FieldDecl fd = (FieldDecl)super.disambiguate(ar);
        JifFieldInstance jfi = (JifFieldInstance)fd.fieldInstance();
        jfi.setHasInitializer(fd.init() != null);               
        return fd;
    }


    public Node typeCheck(TypeChecker tc) throws SemanticException {
	FieldDecl fd = (FieldDecl) node();

	if (fd.fieldInstance().flags().isStatic()) {
	    Expr init = fd.init();
	   
	    if (init != null) {
                // if the static field has an initializer, then the 
                // initialization expression must be constant.
		ConstChecker cc = new ConstChecker();
		init.visit(cc);
		if (!cc.isConst()) { 
		    throw new SemanticException("Jif does not support " +
                        "static fields without constant initializers.",
			    fd.position());
		}
	    }
	}

	return super.typeCheck(tc);
    }
}

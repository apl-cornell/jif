package jif.extension;

import jif.types.JifFieldInstance;
import jif.types.JifTypeSystem;
import jif.visit.ConstChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.visit.TypeBuilder;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>FieldDecl</code> node. 
 * 
 *  @see polyglot.ast.FieldDecl
 */
public class JifFieldDeclDel extends JifJL_c
{
    public JifFieldDeclDel() {
    }

    public Node buildTypes(TypeBuilder tb) throws SemanticException {
        // Set the flag JifFieldInstance.hasInitializer correctly.
        FieldDecl fd = (FieldDecl)super.buildTypes(tb);
        JifFieldInstance jfi = (JifFieldInstance)fd.fieldInstance();
        jfi.setHasInitializer(fd.init() != null);
        
        JifTypeSystem ts = (JifTypeSystem)tb.typeSystem();
        jfi.setLabel(ts.freshLabelVariable(fd.position(), fd.name(), 
                                           "label of the field " + tb.currentClass().name() + "." + fd.name()));
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

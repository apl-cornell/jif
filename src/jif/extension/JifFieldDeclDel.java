package jif.extension;

import jif.types.JifFieldInstance;
import jif.types.JifTypeSystem;
import jif.types.label.Label;
import jif.visit.ConstChecker;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.Position;
import polyglot.visit.*;

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

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        JifTypeSystem jts = (JifTypeSystem)ar.typeSystem();

        FieldDecl n = (FieldDecl)node();
        FieldInstance fi = n.fieldInstance();

        if (n.declType().isCanonical()) {
            if (!jts.isLabeled(n.declType())) {
                Type lblType = n.declType();
                Position pos = lblType.position();
                Label defaultLbl = jts.defaultSignature().defaultFieldLabel(n);
                
                lblType = jts.labeledType(pos, lblType, defaultLbl);
                n = n.type(n.type().type(lblType));
            }

            fi.setType(n.declType());
        }
                
        return n;
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

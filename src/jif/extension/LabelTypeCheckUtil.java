package jif.extension;

import java.util.Iterator;

import polyglot.types.SemanticException;
import polyglot.visit.TypeChecker;
import jif.types.JifContext;
import jif.types.JifTypeSystem;
import jif.types.label.*;
import jif.types.label.AccessPath;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;
import jif.types.principal.DynamicPrincipal;
import jif.types.principal.Principal;

/**
 * Contains some common utility code to type check dynamic labels and principals
 */
public class LabelTypeCheckUtil {

    public static void typeCheckPrincipal(TypeChecker tc, Principal principal) throws SemanticException {
        if (principal instanceof DynamicPrincipal) {
            JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
            DynamicPrincipal dp = (DynamicPrincipal)principal;

            // Make sure that the access path is set correctly
            // check also that all field accesses are final, and that
            // the type of the expression is principal
            AccessPath path = dp.path();
            try {
                path.verify((JifContext)tc.context());                
            }
            catch (SemanticException e) {
                throw new SemanticException(e.getMessage(), principal.position());
            }
            
            if (!ts.isPrincipal(dp.path().type())) {
                throw new SemanticException("The type of a dynamic label must be \"principal\"", principal.position());
            }
        }        
    }

    public static void typeCheckLabel(TypeChecker tc, Label Lbl) throws SemanticException {
        for (Iterator comps = Lbl.components().iterator(); comps.hasNext(); ) {
            Label l = (Label)comps.next();
            if (l instanceof DynamicLabel) {
                JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
                DynamicLabel dl = (DynamicLabel)l;
                
                // Make sure that the access path is set correctly
                // check also that all field accesses are final, and that
                // the type of the expression is label
                AccessPath path = dl.path();
                try {
                    path.verify((JifContext)tc.context());                
                }
                catch (SemanticException e) {
                    throw new SemanticException(e.getMessage(), dl.position());
                }
                
                if (!ts.isLabel(dl.path().type())) {
                    throw new SemanticException("The type of a dynamic label must be \"label\"", dl.position());
                }
            }        
            else if (l instanceof PolicyLabel) {
                JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
                PolicyLabel pl = (PolicyLabel)l;
                typeCheckPrincipal(tc, pl.owner());
                for (Iterator i = pl.readers().iterator(); i.hasNext(); ) {
                    Principal r = (Principal)i.next();
                    typeCheckPrincipal(tc, r);                
                }
            }
        }

    }


}

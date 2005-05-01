package jif.extension;

import jif.types.*;
import polyglot.ast.Instanceof;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>Cast</code> node.
 *
 *  @see polyglot.ext.jl.ast.Cast_c
 */
public class JifInstanceOfDel extends JifJL_c
{
    public JifInstanceOfDel() { }

    private boolean isToSubstJifClass = false;

    public boolean isToSubstJifClass() { return this.isToSubstJifClass; }


    public Node typeCheck(TypeChecker tc) throws SemanticException {
        // prevent instanceof to arrays of parameterized types
        Instanceof io = (Instanceof)this.node();
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
        Type compareType = io.compareType().type();
        if (ts.isLabeled(compareType)) {
            throw new SemanticException("Cannot perform instanceof on a labeled type.", io.position());
        }

        if (!ts.isJifClass(compareType)) {
            if ((compareType instanceof JifSubstType && ((JifSubstType)compareType).entries().hasNext()) ||
                (compareType instanceof JifPolyType && !((JifPolyType)compareType).params().isEmpty()))                    
            throw new SemanticException("Cannot perform instanceof on a parameterized " + 
                                        "Java class, since Java classes do " +
                                        "not represent the parameters at runtime.", 
                                        io.position());
        }

        if (compareType.isArray()) {
            while (compareType.isArray()) {
                compareType = compareType.toArray().base();
            }
            if (compareType instanceof JifSubstType && ((JifSubstType)compareType).entries().hasNext()) {
                throw new SemanticException("Jif does not currently support instanceof to an array of a parameterized type.", io.position());
            }
        }
        this.isToSubstJifClass = (compareType instanceof JifSubstType && ((JifSubstType)compareType).entries().hasNext());

        return super.typeCheck(tc);
    }
}

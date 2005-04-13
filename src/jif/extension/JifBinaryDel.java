package jif.extension;

import jif.types.JifTypeSystem;
import polyglot.ast.Binary;
import polyglot.ast.Node;
import polyglot.ast.Precedence;
import polyglot.ast.Binary.Operator;
import polyglot.types.SemanticException;
import polyglot.visit.TypeChecker;

/** The Jif extension of the <code>Call</code> node. 
 * 
 *  @see polyglot.ext.jl.ast.Call_c
 */
public class JifBinaryDel extends JifJL_c
{
    public static final Binary.Operator ACTSFOR  = new Operator("actsfor", Precedence.RELATIONAL);

    public JifBinaryDel() { }

    public Node typeCheck(TypeChecker tc) throws SemanticException {
        Binary b = (Binary)node();
        if (b.operator() == ACTSFOR) {
            throw new SemanticException("The actsfor binary operator can only be used in an if statement, for example \"if (" + b + ") { ... }\"", b.position());
        }
        
        JifTypeSystem ts = (JifTypeSystem)tc.typeSystem();
        if (b.operator() == Binary.LE && 
                (ts.isLabel(b.left().type()) || ts.isLabel(b.right().type()))) {
            // looks like we may have an if label. Currently, we'll just ignore type
            // checking for this node.
            return b.type(ts.Boolean());
        }
        return super.typeCheck(tc);
    }
}

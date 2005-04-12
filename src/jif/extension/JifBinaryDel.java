package jif.extension;

import java.util.LinkedList;
import java.util.List;

import polyglot.ast.*;
import polyglot.ast.Call;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Receiver;
import polyglot.ast.Special;
import polyglot.ast.Binary.Operator;
import polyglot.types.MethodInstance;
import polyglot.types.SemanticException;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;
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
            throw new SemanticException("The actsfor binary operator can only be used in an if statement, for example \"if (" + b + ") { ... }\"");
        }
        return super.typeCheck(tc);
    }
}

package jif.extension;

import java.util.LinkedList;
import java.util.List;

import polyglot.ast.Call;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Receiver;
import polyglot.ast.Special;
import polyglot.types.MethodInstance;
import polyglot.types.TypeSystem;
import polyglot.util.InternalCompilerError;

/** The Jif extension of the <code>Call</code> node. 
 * 
 *  @see polyglot.ext.jl.ast.Call_c
 */
public class JifCallDel extends JifJL_c
{
    public JifCallDel() { }

    /**
     * This flag records whether the target of a method call is never
     * null. This flag is by default false, but may be set to true by the
     * dataflow analysis performed by jif.visit.NotNullChecker
     */
    private boolean isTargetNeverNull = false;
    
    public void setTargetIsNeverNull() {
        isTargetNeverNull = true;
    }

    public boolean targetIsNeverNull() {
        Receiver r = ((Call)node()).target();
        return (r instanceof Special 
                || isTargetNeverNull 
                || r instanceof CanonicalTypeNode);
    }

    /** 
     *  List of Types of exceptions that might get thrown.
     * 
     * This differs from the method defined in Call_c in that it does not
     * throw a null pointer exception if the receiver is guaranteed to be 
     * non-null
     */
    public List throwTypes(TypeSystem ts) {
        MethodInstance mi = ((Call)node()).methodInstance();
        if (mi == null) {
            throw new InternalCompilerError(
                node().position(),
                "Null method instance after type " + "check.");
        }

        List l = new LinkedList();

        l.addAll(mi.throwTypes());

        // We may throw a null pointer exception except when the target
        // is "this" or "super", or the receiver is guaranteed to be non-null
        if (!targetIsNeverNull()) {
            l.add(ts.NullPointerException());
        }

        return l;
    }
}

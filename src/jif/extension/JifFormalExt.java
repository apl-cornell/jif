package jif.extension;

import jif.ast.JifUtil;
import jif.ast.Jif_c;
import jif.translate.ToJavaExt;
import jif.types.JifContext;
import jif.types.JifLocalInstance;
import jif.types.JifTypeSystem;
import jif.types.label.ArgLabel;
import jif.visit.LabelChecker;
import polyglot.ast.Formal;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

/** The Jif extension of the <code>Formal</code> node. 
 * 
 *  @see polyglot.ast.Formal
 */
public class JifFormalExt extends Jif_c
{
    public JifFormalExt(ToJavaExt toJava) {
        super(toJava);
    }

    public Node labelCheck(LabelChecker lc) throws SemanticException {
        JifContext A = lc.jifContext();
	A = (JifContext) node().del().enterScope(A);
        
        // let's check some invariants
        Formal f = (Formal)node();
        JifFormalDel jfd = (JifFormalDel)f.del();
        JifTypeSystem ts = lc.jifTypeSystem();
        JifLocalInstance li = (JifLocalInstance)f.localInstance();
        

        if (!jfd.isCatchFormal()) {
//            System.err.println("Formal " + f.name() + " of " + A.currentCode());        
//            System.err.println("   type of node is " + f.declType()); 
//            System.err.println("   type of local instance is " + li.type()); 
//            System.err.println("   label of local instance is " + li.label()); 

            // the label of the type of the node should be an ArgLabel
            if (!(ts.labelOfType(f.declType()) instanceof ArgLabel)) {
                throw new InternalCompilerError("Invariant broken: " +
                                                "after disambiguation we expect the label of a " +
                                                "Formal's declared type to be an ArgLabel"); 
            }
            if (!(li.label() instanceof ArgLabel)) {
                throw new InternalCompilerError("Invariant broken: " +
                                                "after disambiguation we expect the label of a " +
                                                "Formal's local instance to be an ArgLabel"); 
            }
            ArgLabel al = (ArgLabel)li.label();        
            if (ts.isLabeled(li.type()) && !(ts.labelOfType(li.type()).equals(al.upperBound()))) {
                throw new InternalCompilerError("Invariant broken: " +
                                                "after disambiguation we expect the label of a " +
                                                "Formal's local instance's type to be the upper " +
                                                "bound of the ArgLabel for the formal."); 
            }
        }
        
        return node();
    }
}

package jif.ast;

import jif.types.JifClassType;
import jif.types.JifContext;
import polyglot.ast.Node;
import polyglot.frontend.MissingDependencyException;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
import polyglot.types.SemanticException;
import polyglot.util.CodeWriter;
import polyglot.util.Position;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>AmbThisLabelNode</code> interface. 
 */
public class AmbThisLabelNode_c extends AmbLabelNode_c
                               implements AmbThisLabelNode
{
    public AmbThisLabelNode_c(Position pos) {
	super(pos);
    }

    public String toString() {
	return "this{amb}";
    }

    /** Disambiguates the type of this node by finding the correct label for
     * "this". 
     */
    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
	JifContext c = (JifContext)sc.context();
    
        if (c.inStaticContext() && !c.inConstructorCall()) {
            throw new SemanticException("The label \"this\" cannot be used " +
                "in a static context.", position());
        }
        
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

	JifClassType ct = (JifClassType) c.currentClass();
    
	if (!ct.thisLabel().isCanonical()) {
	    Scheduler sched = sc.job().extensionInfo().scheduler();
	    Goal g = sched.Disambiguated(sc.job());
	    throw new MissingDependencyException(g);
	}

        return nf.CanonicalLabelNode(position(), ct.thisLabel());
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("this");
    }
}

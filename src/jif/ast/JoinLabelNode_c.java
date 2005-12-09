package jif.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.label.LabelJ;
import jif.types.label.LabelM;
import jif.types.label.PairLabel;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>JoinLabel</code> interface.
 */
public class JoinLabelNode_c extends AmbLabelNode_c implements JoinLabelNode
{
    protected List components;

    public JoinLabelNode_c(Position pos, List components) {
	super(pos);
	this.components = TypedList.copyAndCheck(components, LabelNode.class, true);
    }

    public List components() {
	return this.components;
    }

    public JoinLabelNode components(List components) {
	JoinLabelNode_c n = (JoinLabelNode_c) copy();
	n.components = TypedList.copyAndCheck(components, LabelNode.class, true);
	return n;
    }

    protected JoinLabelNode_c reconstruct(List components) {
	if (! CollectionUtil.equals(components, this.components)) {
	    JoinLabelNode_c n = (JoinLabelNode_c) copy();
	    n.components = TypedList.copyAndCheck(components, LabelNode.class, true);
	    return n;
	}

	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	List components = visitList(this.components, v);
	return reconstruct(components);
    }

    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

        List l = new LinkedList();
        List pairLabels = new LinkedList();

	for (Iterator i = this.components.iterator(); i.hasNext(); ) {
	    LabelNode n = (LabelNode) i.next();
            if (!n.isDisambiguated()) {
                sc.job().extensionInfo().scheduler().currentGoal().setUnreachableThisRun();
                return this;
            }
            if (n.label() instanceof PairLabel) {
                pairLabels.add(n.label());
            }
            else {
                l.add(n.label());
            }
	}
        
        // the pair labels are all the explicitly specified 
        // reader and writer policies. We need to combine these into
        // exactly one pair label, such that the LabelJ part of
        // the new pair is the join of all the reader policies, and
        // the LabelM part of the new pair is the meet of all the writer
        // policies.
        if (!pairLabels.isEmpty()) {
            LabelJ lj = ts.bottomLabelJ(position());
            LabelM lm = ts.topLabelM(position());
            for (Iterator iter = pairLabels.iterator(); iter.hasNext(); ) {
                PairLabel pl = (PairLabel)iter.next();
                lj = ts.join(lj, pl.labelJ());
                lm = ts.meet(lm, pl.labelM());
            }
            l.add(ts.pairLabel(position(), lj, lm));
        }

	return nf.CanonicalLabelNode(position(), ts.joinLabel(position(), l));
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
	for (Iterator i = this.components.iterator(); i.hasNext(); ) {
	    LabelNode n = (LabelNode) i.next();
            print(n, w, tr);
            if (i.hasNext()) {
                w.write(";");
                w.allowBreak(0, " ");
            }
        }
    }
}

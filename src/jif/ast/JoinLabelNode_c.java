package jif.ast;

import polyglot.ext.jl.ast.*;
import jif.types.*;
import jif.visit.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;
import java.util.*;

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

	for (Iterator i = this.components.iterator(); i.hasNext(); ) {
	    LabelNode n = (LabelNode) i.next();
	    l.add(n.label());
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

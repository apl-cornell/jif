package jif.ast;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.NodeVisitor;
import polyglot.visit.PrettyPrinter;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public abstract class PolicyLabelNode_c extends AmbLabelNode_c implements PolicyLabelNode
{
    protected PrincipalNode owner;
    protected List principals;

    public PolicyLabelNode_c(Position pos, PrincipalNode owner, List principals) {
	super(pos);
        if (owner == null) throw new InternalCompilerError("null owner");
	this.owner = owner;
	this.principals = TypedList.copyAndCheck(principals, PrincipalNode.class, true);
    }

    public PrincipalNode owner() {
	return this.owner;
    }

    public PolicyLabelNode owner(PrincipalNode owner) {
	PolicyLabelNode_c n = (PolicyLabelNode_c) copy();
	n.owner = owner;
	return n;
    }

    public List principals() {
	return this.principals;
    }

    public PolicyLabelNode principals(List principals) {
	PolicyLabelNode_c n = (PolicyLabelNode_c) copy();
	n.principals = TypedList.copyAndCheck(principals, PrincipalNode.class, true);
	return n;
    }

    protected PolicyLabelNode_c reconstruct(PrincipalNode owner, List principals) {
	if (owner != this.owner || ! CollectionUtil.equals(principals, this.principals)) {
	    PolicyLabelNode_c n = (PolicyLabelNode_c) copy();
	    n.owner = owner;
	    n.principals = TypedList.copyAndCheck(principals, PrincipalNode.class, true);
	    return n;
	}

	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	PrincipalNode owner = (PrincipalNode) visitChild(this.owner, v);
	List readers = visitList(this.principals, v);
	return reconstruct(owner, readers);
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) ar.nodeFactory();

        if (!owner.isDisambiguated()) {
            ar.job().extensionInfo().scheduler().currentGoal().setUnreachableThisRun();
            return this;
        }

        Principal o = owner.principal();
        if (o == null) throw new InternalCompilerError("null owner " + owner.getClass().getName() + " " + owner.position());

	List l = new LinkedList();

	for (Iterator i = this.principals.iterator(); i.hasNext(); ) {
	    PrincipalNode r = (PrincipalNode) i.next();
            if (!r.isDisambiguated()) {
                ar.job().extensionInfo().scheduler().currentGoal().setUnreachableThisRun();
                return this;
            }
	    l.add(r.principal());
	}
	return nf.CanonicalLabelNode(position(), produceLabel(ts, o, l));
    }
    
    protected abstract Label produceLabel(JifTypeSystem ts, Principal owner, List principals);

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(owner, w, tr);

        w.write(": ");

	for (Iterator i = this.principals.iterator(); i.hasNext(); ) {
	    PrincipalNode n = (PrincipalNode) i.next();
            print(n, w, tr);
            if (i.hasNext()) {
                w.write(";");
                w.allowBreak(0, " ");
            }
        }
    }
}

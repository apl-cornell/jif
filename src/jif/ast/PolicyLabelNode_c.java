package jif.ast;

import polyglot.ext.jl.ast.*;
import jif.types.*;
import jif.types.principal.Principal;
import jif.visit.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.visit.*;
import polyglot.util.*;
import java.util.*;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public class PolicyLabelNode_c extends AmbLabelNode_c implements PolicyLabelNode
{
    protected PrincipalNode owner;
    protected List readers;

    public PolicyLabelNode_c(Position pos, PrincipalNode owner, List readers) {
	super(pos);
        if (owner == null) throw new InternalCompilerError("null owner");
	this.owner = owner;
	this.readers = TypedList.copyAndCheck(readers, PrincipalNode.class, true);
    }

    public PrincipalNode owner() {
	return this.owner;
    }

    public PolicyLabelNode owner(PrincipalNode owner) {
	PolicyLabelNode_c n = (PolicyLabelNode_c) copy();
	n.owner = owner;
	return n;
    }

    public List readers() {
	return this.readers;
    }

    public PolicyLabelNode readers(List readers) {
	PolicyLabelNode_c n = (PolicyLabelNode_c) copy();
	n.readers = TypedList.copyAndCheck(readers, PrincipalNode.class, true);
	return n;
    }

    protected PolicyLabelNode_c reconstruct(PrincipalNode owner, List readers) {
	if (owner != this.owner || ! CollectionUtil.equals(readers, this.readers)) {
	    PolicyLabelNode_c n = (PolicyLabelNode_c) copy();
	    n.owner = owner;
	    n.readers = TypedList.copyAndCheck(readers, PrincipalNode.class, true);
	    return n;
	}

	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	PrincipalNode owner = (PrincipalNode) visitChild(this.owner, v);
	List readers = visitList(this.readers, v);
	return reconstruct(owner, readers);
    }

    public Node disambiguate(AmbiguityRemover sc) throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) sc.typeSystem();
	JifNodeFactory nf = (JifNodeFactory) sc.nodeFactory();

	Principal o = owner.principal();
        if (o == null) throw new InternalCompilerError("null owner " + owner.getClass().getName() + " " + owner.position());

	List l = new LinkedList();

	for (Iterator i = this.readers.iterator(); i.hasNext(); ) {
	    PrincipalNode r = (PrincipalNode) i.next();
	    l.add(r.principal());
	}

	return nf.CanonicalLabelNode(position(),
		                     ts.policyLabel(position(), o, l));
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        print(owner, w, tr);

        w.write(": ");

	for (Iterator i = this.readers.iterator(); i.hasNext(); ) {
	    PrincipalNode n = (PrincipalNode) i.next();
            print(n, w, tr);
            if (i.hasNext()) {
                w.write(";");
                w.allowBreak(0, " ");
            }
        }
    }
}

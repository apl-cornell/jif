package jif.ast;

import java.util.*;

import jif.types.JifTypeSystem;
import jif.types.label.Label;
import jif.types.label.Policy;
import jif.types.principal.Principal;
import polyglot.ast.Ambiguous;
import polyglot.ast.Node;
import polyglot.ext.jl.ast.Node_c;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>PolicyLabel</code> interface.
 */
public class PolicyNode_c extends Node_c implements PolicyNode
{
    protected PrincipalNode owner;
    protected List principals;
    protected Policy policy = null;

    public PolicyNode_c(Position pos, Policy policy) {
        super(pos);
        this.policy = policy;
        this.owner = null; 
        this.principals = null;
    }
    public PolicyNode_c(Position pos, PrincipalNode owner, List principals) {
	super(pos);
        if (owner == null) throw new InternalCompilerError("null owner");
	this.owner = owner;
	this.principals = TypedList.copyAndCheck(principals, PrincipalNode.class, true);
    }

    public Policy policy() {
        return this.policy;
    }

    public PrincipalNode owner() {
	return this.owner;
    }
   
    public PolicyNode owner(PrincipalNode owner) {
	PolicyNode_c n = (PolicyNode_c) copy();
	n.owner = owner;
	return n;
    }

    public List principals() {
	return this.principals;
    }

    public PolicyNode principals(List principals) {
	PolicyNode_c n = (PolicyNode_c) copy();
	n.principals = TypedList.copyAndCheck(principals, PrincipalNode.class, true);
	return n;
    }

    protected PolicyNode_c reconstruct(PrincipalNode owner, List principals) {
	if (owner != this.owner || ! CollectionUtil.equals(principals, this.principals)) {
	    PolicyNode_c n = (PolicyNode_c) copy();
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

    public boolean isDisambiguated() {
        return policy != null;
    }
    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
	JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();
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
        this.policy = producePolicy(ts, o, l);
        return this;
    }
    
    protected Policy producePolicy(JifTypeSystem ts, Principal owner, List principals) {
        return null;
    }

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

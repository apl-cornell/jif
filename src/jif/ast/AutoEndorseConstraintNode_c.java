package jif.ast;

import java.util.*;

import jif.types.*;
import polyglot.ast.Node;
import polyglot.types.SemanticException;
import polyglot.util.*;
import polyglot.visit.*;

public class AutoEndorseConstraintNode_c extends ConstraintNode_c implements AutoEndorseConstraintNode
{
    protected List principals;

    public AutoEndorseConstraintNode_c(Position pos, List principals) {
	super(pos);
	this.principals = TypedList.copyAndCheck(principals, PrincipalNode.class, true);
    }

    public List principals() {
	return this.principals;
    }

    public AutoEndorseConstraintNode principals(List principals) {
	AutoEndorseConstraintNode_c n = (AutoEndorseConstraintNode_c) copy();
	n.principals = TypedList.copyAndCheck(principals, PrincipalNode.class, true);
        if (constraint()!=null) {
            List l = new LinkedList();
            for (Iterator i = principals.iterator(); i.hasNext(); ) {
                PrincipalNode p = (PrincipalNode) i.next();
                l.add(p.principal());
            }
            n.constraint = ((AutoEndorseConstraint_c)constraint()).principals(l);
        }
	return n;
    }

    protected AutoEndorseConstraintNode_c reconstruct(List principals) {
	if (! CollectionUtil.equals(principals, this.principals)) {
            List newPrincipals = TypedList.copyAndCheck(principals, PrincipalNode.class, true);
            return (AutoEndorseConstraintNode_c)this.principals(newPrincipals);
	}

	return this;
    }

    public Node visitChildren(NodeVisitor v) {
	List principals = visitList(this.principals, v);
	return reconstruct(principals);
    }

    public Node disambiguate(AmbiguityRemover ar) throws SemanticException {
        if (constraint() == null) {
            JifTypeSystem ts = (JifTypeSystem) ar.typeSystem();

            List l = new LinkedList();

            for (Iterator i = this.principals.iterator(); i.hasNext(); ) {
                PrincipalNode p = (PrincipalNode) i.next();
                l.add(p.principal());
            }

            return constraint(ts.autoEndorseConstraint(position(), l));
        }

        return this;
    }

    public void prettyPrint(CodeWriter w, PrettyPrinter tr) {
        w.write("autoendorse(");

        for (Iterator i = principals.iterator(); i.hasNext(); ) {
            PrincipalNode p = (PrincipalNode) i.next();
            print(p, w, tr);
            w.write(",");
            w.allowBreak(0, " ");
        }

        w.write(")");
    }

    public void translate(CodeWriter w, Translator tr) {
        throw new InternalCompilerError("Cannot translate " + this);
    }
}

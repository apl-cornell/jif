package jif.parse;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jif.ast.AmbNewArray;
import polyglot.ast.Expr;
import polyglot.ast.Labeled;
import polyglot.ast.NewArray;
import polyglot.ast.Prefix;
import polyglot.ast.Receiver;
import polyglot.ast.TypeNode;
import polyglot.util.Position;

/**
 * An <code>InstOrAccess</code> represents a <code>Amb</code> of the form
 * "P[e]" or "P[p]".
 */
public class InstOrAccess extends Amb {
    // prefix[e] or prefix[p]
    Amb prefix;
    Name param;

    InstOrAccess(Grm parser, Position pos, Amb prefix, Name param) throws Exception {
	super(parser, pos);
	this.prefix = prefix;
	this.param = param;

	if (prefix instanceof Labeled) parser.die(pos);
	if (prefix instanceof Array) parser.die(pos);
    }

    public TypeNode toType() throws Exception {
	LinkedList l = new LinkedList();
	l.add(parser.nf.AmbParam(param.pos, param.toIdentifier()));
	return parser.nf.InstTypeNode(pos, prefix.toUnlabeledType(), l);
    }

    public TypeNode toUnlabeledType() throws Exception { return toType(); }

    public Prefix toPrefix() throws Exception { return toReceiver(); }

    public Receiver toReceiver() throws Exception {
	// The prefix must be either a type or an expression
	// (i.e., a receiver).
	return parser.nf.AmbParamTypeOrAccess(pos, prefix.toReceiver(),
					    param.toIdentifier());
    }

    public Expr toExpr() throws Exception {
	return parser.nf.ArrayAccess(pos, prefix.toExpr(),
				    parser.nf.AmbExpr(param.pos,
				    param.toIdentifier()));
    }

    public Expr toNewArray(Position p) throws Exception {
	Access a = new Access(parser, pos, prefix,
			    parser.nf.AmbExpr(param.pos,
				param.toIdentifier()));
	return a.toNewArray(p);
    }

    public Expr toNewArrayPrefix(Position p) throws Exception {
	// Only the first dimension of "new T[p][q][r]" is ambiguous;
	// the rest must be expressions.

	if (prefix instanceof Name) {
	    // "new T.a[n]".  "name" may be either an expr or a param.
	    List params = new LinkedList();
		return parser.nf.AmbNewArray(p, prefix.toType(),
					    param.toIdentifier(),
					    Collections.EMPTY_LIST);
	}
	else if (prefix instanceof Inst) {
	    // "new T[L,M][n]".  "name" must be an expr.
	    List dims = new LinkedList();
	    dims.add(parser.nf.AmbExpr(param.pos, param.toIdentifier()));
	    return parser.nf.NewArray(p, prefix.toType(), dims);
	}
	else if (prefix instanceof Access ||
		prefix instanceof InstOrAccess) {
	    // The prefix is "S[n]".  Recurse as if we were doing
	    // "new S[n]".  "name" must be an expression.
	    Expr e = prefix.toNewArrayPrefix(p);

	    // Take the expression we built and add a new dimension.
	    if (e instanceof NewArray) {
		NewArray a = (NewArray) e;
		List dims = new LinkedList(a.dims());
		dims.add(parser.nf.AmbExpr(param.pos,
					param.toIdentifier()));
		return a.dims(dims);
	    }
	    else if (e instanceof AmbNewArray) {
		AmbNewArray a = (AmbNewArray) e;
		List dims = new LinkedList(a.dims());
		dims.add(parser.nf.AmbExpr(param.pos,
					param.toIdentifier()));
		return a.dims(dims);
	    }
	}

	parser.die(this.pos);
	return null;
    }
}



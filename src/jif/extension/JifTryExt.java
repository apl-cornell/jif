package jif.extension;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.translate.ToJavaExt;
import jif.types.*;
import jif.types.label.Label;
import jif.visit.LabelChecker;
import polyglot.ast.*;
import polyglot.types.SemanticException;
import polyglot.types.Type;

/** Jif extension of the <code>Try</code> node.
 *  
 *  @see polyglot.ast.Try
 */
public class JifTryExt extends JifStmtExt_c
{
    public JifTryExt(ToJavaExt toJava) {
        super(toJava);
    }

    // SubtypeChecker subtypeChecker = new SubtypeChecker();

    public Node labelCheckStmt(LabelChecker lc) throws SemanticException {
	Try trs = (Try) node();

        JifTypeSystem ts = lc.jifTypeSystem();
        JifContext A = lc.jifContext();
	A = (JifContext) trs.enterScope(A);

	Block t = (Block) lc.context(A).labelCheck(trs.tryBlock());
	PathMap Xs = X(t);

	PathMap Xall = ts.pathMap();

	List catches = new LinkedList();

	for (Iterator i = trs.catchBlocks().iterator(); i.hasNext(); ) {
	    Catch cb = (Catch) i.next();

	    Label pc_i = excLabel(Xs, cb.catchType(), ts);

	    A = (JifContext) A.pushBlock();
	    A.setPc(pc_i);

	    // This adds the formal to the environment.
	    Formal f = (Formal) lc.context(A).labelCheck(cb.formal());
	    JifLocalInstance vi = (JifLocalInstance) f.localInstance();
	    Label Li = vi.label();

	    // Constrain the variable label to be equivalent to the exc-label.
	    // This differs from the thesis.
            // error messages for equality constraints aren't displayed, so no
            // need top define error messages.  
            lc.constrain(new LabelConstraint(
                     new NamedLabel("label_exc_i", 
                                    "label of variable " + vi.name(), 
                                    Li), 
                     LabelConstraint.EQUAL, 
                     new NamedLabel("join(pc|where exc_i coulb be thrown)", 
                                    "the information that could be revealed " +
                                    "by the exception " + cb.catchType() + " " +
                                    "being thrown", 
                                    pc_i), 
                     A.labelEnv(),
                     f.position()));


	    Block si = (Block) lc.context(A).labelCheck(cb.body());
	    PathMap Xi = X(si);

	    Xall = Xall.join(Xi);

            A = (JifContext) A.pop();

	    catches.add(X(cb.formal(f).body(si), Xi));
	}
	PathMap Xunc = uncaught(Xs, trs, ts);
	Xall = Xall.join(Xunc);
	
	PathMap X;

	Block f = (Block) trs.finallyBlock();

	if (f != null) {
	    f = (Block) lc.context(A).labelCheck(f);

	    PathMap X2 = X(f);
	    //X = Xall.N(ts.notTaken()).join(X2);
	    Label finalPath = ts.bottomLabel();
	    for (Iterator iter = X2.paths().iterator(); iter.hasNext(); ) {
		Path p = (Path) iter.next();
		finalPath = finalPath.join(X2.get(p));
	    }
	    for (Iterator iter = Xall.paths().iterator(); iter.hasNext(); ) {
		Path p = (Path) iter.next();
		if (p instanceof ExceptionPath) {
		    Xall = Xall.set(p, Xall.get(p).join(finalPath));
		}
	    }
	    X = Xall.join(X2);
	}
	else {
	    X = Xall;
	}

	trs = trs.tryBlock(t).catchBlocks(catches).finallyBlock(f);

	return X(trs, X);
    }

    private PathMap uncaught(PathMap X, Try trs, JifTypeSystem ts)
	throws SemanticException {

	PathMap Xp = X;

	for (Iterator iter = X.paths().iterator(); iter.hasNext(); ) {
	    Path p = (Path) iter.next();

	    if (p instanceof ExceptionPath) {
		ExceptionPath jep = (ExceptionPath) p;

		boolean sat = false;

		for (Iterator i = trs.catchBlocks().iterator(); i.hasNext(); ) {
		    Catch cb = (Catch) i.next();

		    if (ts.isImplicitCastValid(jep.exception(), cb.catchType()) ||
		        ts.equals(jep.exception(), cb.catchType())) {

			// FIXME:
			// subtypeChecker.addSubtypeConstraints(lc,
			//      trs.position(),	cb.catchType(),
			//      jep.exception());

			sat = true;
			break;
		    }
		}

		if (sat) {
		    Xp = Xp.set(jep, ts.notTaken());
		}
	    }
	}

	return Xp;
    }

    private Label excLabel(PathMap X, Type ct, JifTypeSystem ts)
	throws SemanticException {

	Label L = ts.bottomLabel(ct.position());

	for (Iterator iter = X.paths().iterator(); iter.hasNext(); ) {
	    Path p = (Path) iter.next();

	    if (p instanceof ExceptionPath) {
		ExceptionPath ep = (ExceptionPath) p;

		if (ts.isSubtype(ct, ep.exception()) ||
		    ts.isSubtype(ep.exception(), ct)) {
		    L = L.join(X.get(ep));
		}
	    }
	}

	return L;
    }
}

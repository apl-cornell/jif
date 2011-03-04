package jif.visit;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.ast.JifNew_c;
import jif.extension.JifNewExt;
import jif.extension.JifThrowDel;
import jif.types.JifContext;
import jif.types.JifContext_c;
import jif.types.JifLocalInstance;
import jif.types.JifTypeSystem;
import jif.types.LabeledType;

import polyglot.ast.Block;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Catch;
import polyglot.ast.CodeDecl;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Local;
import polyglot.ast.New;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.Throw;
import polyglot.ast.Try;
import polyglot.ast.TypeNode;
import polyglot.frontend.Job;
import polyglot.qq.QQ;
import polyglot.types.ConstructorInstance;
import polyglot.types.Context;
import polyglot.types.Flags;
import polyglot.types.ParsedClassType;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SubtypeSet;
import polyglot.util.UniqueID;
import polyglot.visit.AmbiguityRemover;
import polyglot.visit.ContextVisitor;
import polyglot.visit.ExceptionChecker;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeBuilder;

public class JifExceptionChecker extends ExceptionChecker {

	public JifExceptionChecker(Job job, TypeSystem ts, NodeFactory nf) {
		super(job, ts, nf);
	}
	/**
     * Call exceptionCheck(ExceptionChecker) on the node.
     *
     * @param old The original state of root of the current subtree.
     * @param n The current state of the root of the current subtree.
     * @param v The <code>NodeVisitor</code> object used to visit the children.
     * @return The final result of the traversal of the tree rooted at 
     *  <code>n</code>.
     */
    protected Node leaveCall(Node parent, Node old, Node n, NodeVisitor v)
	throws SemanticException {
        //when parent is a CodeDecl, v should be the correct EC???

    	JifExceptionChecker inner = (JifExceptionChecker) v;
        {
        	// code in this block checks the invariant that
            // this ExceptionChecker must be an ancestor of inner, i.e.,
            // inner must be the result of zero or more pushes.
            boolean isAncestor = false;
            JifExceptionChecker ec = inner;
            while (!isAncestor && ec != null) {
                isAncestor = isAncestor || (ec == this);
                ec = (JifExceptionChecker) ec.outer;
            }

            if (!isAncestor) {
                throw new InternalCompilerError("oops!");
            }
        }

        if(parent instanceof CodeDecl && throwsSet.size() > 0) {
        	//XXX: this is probably redundant.  It seems like we should be 
        	// able to use throwsSet directly, but I must not 
        	// understand how things are added in relation to catchable
        	// types. --owen
    		JifTypeSystem jifts = ((JifTypeSystem) ts);
        	SubtypeSet fatalExcs = new SubtypeSet(ts.Throwable());

        	for(Iterator tsIter = throwsSet.iterator(); tsIter.hasNext();) {
        		Type uncaughtExc = (Type)tsIter.next();
        		boolean declared = false;
        		for (Iterator cIter = catchable.iterator(); cIter.hasNext(); ) {
                    Type declaredExc = (Type)cIter.next();
                    if (ts.isSubtype(uncaughtExc, declaredExc)) {
                    	declared=true;
                        break;
                    }
                }
        		if(!declared && jifts.promoteToFatal(uncaughtExc)) 
        			fatalExcs.add(uncaughtExc);
	        }

        	if(fatalExcs.size() > 0) {
				errorQueue().enqueue(
						ErrorInfo.WARNING,
						"Uncaught exceptions in " + parent + " at " + parent.position()
								+ " will be treated as fatal errors: "
								+ fatalExcs);
	        	Position pos = Position.compilerGenerated();
	        	String s = UniqueID.newID("exc");
	        	List catchBlocks = new LinkedList();
	        	for(Iterator it = fatalExcs.iterator();it.hasNext();) {
	        		Type exType = (Type) it.next();
	        		
	        		if(exType instanceof LabeledType)
	        			exType = ((LabeledType) exType).labelPart(jifts.topLabel());
	        		else 
	        			exType = jifts.labeledType(pos, exType, jifts.topLabel());
		        	
	        		CanonicalTypeNode exTypeNode = nf.CanonicalTypeNode(pos, exType);
		        	Formal exc = nf.Formal(pos, Flags.NONE, exTypeNode, nf.Id(pos, s));		        	
		        	JifLocalInstance fli = (JifLocalInstance) jifts.localInstance(pos, Flags.NONE, exType, s);
		        	fli.setLabel(jifts.topLabel());
		        	exc = exc.localInstance(fli);

		        	Local loc = nf.Local(pos, nf.Id(pos, s));
		        	JifLocalInstance lli = (JifLocalInstance) jifts.localInstance(pos, Flags.NONE, exType, s);
		        	lli.setLabel(jifts.topLabel());
		        	loc = loc.localInstance(lli);
		        	loc = (Local) loc.type(exType);

		        	List args = new LinkedList<Expr>();
		        	args.add(loc);
		        	
		        	New newExc = nf.New(pos, nf.CanonicalTypeNode(pos, ts.Error()), args);
		        	ConstructorInstance ci = ts.findConstructor(ts.Error(), 
		        			Collections.singletonList(ts.Throwable()), ts.Error());
		        	newExc = newExc.constructorInstance(ci);
		        	Throw thrw = nf.Throw(pos, newExc.type(ts.Error()));		        	
		        	((JifThrowDel)thrw.del()).setThrownIsNeverNull();
		        	Block body = nf.Block(pos, thrw);
		        	
		        	Catch c = nf.Catch(pos, exc, body);
		        	catchBlocks.add(c);
	        	}
	        	return nf.Block(pos, nf.Try(pos, (Block)n, catchBlocks));
        	}
        }
        // gather exceptions from this node.
        return n.del().exceptionCheck(inner);        
    }

	/**
     * The ast nodes will use this callback to notify us that they throw an
     * exception of type t. This method will throw a SemanticException if the
     * type t is not allowed to be thrown at this point; the exception t will be
     * added to the throwsSet of all exception checkers in the stack, up to (and
     * not including) the exception checker that catches the exception.
     * 
     * @param t The type of exception that the node throws.
     * @throws SemanticException
     */
	@Override
    public void throwsException(Type t, Position pos) throws SemanticException {
		
        if (! t.isUncheckedException()) {            
            // go through the stack of catches and see if the exception
            // is caught.
            boolean exceptionCaught = false;
            JifExceptionChecker ec = this;
            while (!exceptionCaught && ec != null) {
                if (ec.catchable != null) {
                    for (Iterator iter = ec.catchable.iterator(); iter.hasNext(); ) {
                        Type catchType = (Type)iter.next();
                        if (ts.isSubtype(t, catchType)) {
                            exceptionCaught = true;
                            break;
                        }
                    }
                }           
                if (!exceptionCaught && ec.throwsSet != null) {
                    // add t to ec's throwsSet.
                    ec.throwsSet.add(t); 
                }
                if (ec.catchAllThrowable) {
                    // stop the propagation
                    exceptionCaught = true;
                }
                ec = (JifExceptionChecker) ec.pop();
           }
            if (! exceptionCaught && !((JifTypeSystem) ts).promoteToFatal(t)) {
                reportUncaughtException(t, pos);
            }
        }
    }

}

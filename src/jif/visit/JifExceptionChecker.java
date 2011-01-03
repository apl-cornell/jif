package jif.visit;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jif.types.JifTypeSystem;
import jif.types.LabeledType;

import polyglot.ast.CodeDecl;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.Stmt;
import polyglot.ast.Try;
import polyglot.frontend.Job;
import polyglot.qq.QQ;
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
import polyglot.visit.ExceptionChecker;
import polyglot.visit.NodeVisitor;
import polyglot.visit.TypeBuilder;

public class JifExceptionChecker extends ExceptionChecker {
    public QQ qq;
    protected SubtypeSet uncaught;
	protected boolean recheck;
    
	public JifExceptionChecker(Job job, TypeSystem ts, NodeFactory nf) {
		super(job, ts, nf);
		qq = new QQ(ts.extensionInfo());
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
        Node nNew = n.del().exceptionCheck(inner);

        if(nNew instanceof CodeDecl && inner.recheck) {
			ParsedClassType ct = (ParsedClassType) ((CodeDecl)nNew).memberInstance().container();

			//XXX: calling all of these visitors explicitly seems like a hack.
			// Is there a better option?
        	//Initialize types
        	TypeBuilder tb = new TypeBuilder(job, ts, nf);
        	tb = (TypeBuilder) tb.begin();
           	nNew = nNew.visit(tb);
           	tb.finish(nNew);
           	
        	//Disambiguate 
        	AmbiguityRemover ar = new AmbiguityRemover(job, ts, nf, true, true);
        	ar = (AmbiguityRemover) ar.begin();
        	ar = (AmbiguityRemover) ar.context(ar.context().pushClass(ct, ct));
        	nNew = nNew.visit(ar);
        	ar.finish(nNew);
        	
        	//TypeCheck 
        	JifTypeChecker tc = new JifTypeChecker(job, ts, nf);
			tc = (JifTypeChecker) tc.begin();
        	//set the scope correctly
			tc = (JifTypeChecker) tc.context(tc.context().pushClass(ct,ct));
			nNew = nNew.visit(tc);
        	tc.finish(nNew);
        	
        	//NotNullChecker 
        	NotNullChecker nnc = new NotNullChecker(job, ts, nf);
        	nnc = (NotNullChecker) nnc.begin();
        	nNew = nNew.visit(nnc);
        	nnc.finish(nNew);
        }
        else if(parent instanceof CodeDecl && throwsSet.size() > 0) {
        	//XXX: this is probably redundant.  It seems like we should be 
        	// able to use throwsSet directly, but I must not 
        	// understand how things are added in relation to catchable
        	// types. --owen
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
        		if(!declared) 
        			fatalExcs.add(uncaughtExc);
	        }
        	if(fatalExcs.size() > 0) {
				errorQueue().enqueue(
						ErrorInfo.WARNING,
						"Uncaught exceptions in " + parent + " at " + parent.position()
								+ " will be treated as fatal errors: "
								+ fatalExcs);
	        	String qqStr = "try { %S } ";
	        	List qqSubs = new LinkedList();
	        	qqSubs.add(nNew);
	        	Position pos = Position.compilerGenerated();
	        	String s = UniqueID.newID("exc");
	        	for(Iterator it = fatalExcs.iterator();it.hasNext();) {
	        		qqStr += "catch (%T %s) { throw new %T(%s); }";
	        		JifTypeSystem jifts = ((JifTypeSystem) ts);
	        		Type exType = (Type) it.next();
	        		
	        		if(exType instanceof LabeledType)
	        			exType = ((LabeledType) exType).labelPart(jifts.topLabel());
	        		else 
	        			exType = jifts.labeledType(pos, exType, jifts.topLabel());
		        	qqSubs.add(exType);
		        	qqSubs.add(s);
		        	qqSubs.add(ts.Error());
		        	qqSubs.add(s);
	        	}
	        	Stmt stmt = qq.parseStmt("{"+qqStr+"}", qqSubs);
	        	recheck = true;
	        	return stmt;
        	}
        }
        // gather exceptions from this node.
        return nNew;
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

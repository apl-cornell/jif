package jif.visit;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import jif.ExtensionInfo;
import jif.extension.JifDel;
import jif.extension.JifThrowDel;
import jif.types.JifLocalInstance;
import jif.types.JifTypeSystem;
import jif.types.LabeledType;
import polyglot.ast.Block;
import polyglot.ast.CanonicalTypeNode;
import polyglot.ast.Catch;
import polyglot.ast.Expr;
import polyglot.ast.Formal;
import polyglot.ast.Local;
import polyglot.ast.New;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.ProcedureDecl;
import polyglot.ast.Stmt;
import polyglot.ast.Throw;
import polyglot.ast.Try;
import polyglot.frontend.Job;
import polyglot.types.ConstructorInstance;
import polyglot.types.Flags;
import polyglot.types.ProcedureInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.util.ErrorInfo;
import polyglot.util.InternalCompilerError;
import polyglot.util.Position;
import polyglot.util.SubtypeSet;
import polyglot.util.UniqueID;
import polyglot.visit.ErrorHandlingVisitor;
import polyglot.visit.ExceptionChecker;
import polyglot.visit.NodeVisitor;

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
    @Override
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

        if (parent instanceof ProcedureDecl && throwsSet.size() > 0) {
            JifTypeSystem jifts = ((JifTypeSystem) ts);
            ExtensionInfo extInfo = (ExtensionInfo) ts.extensionInfo();
            SubtypeSet fatalExcs = new SubtypeSet(ts.Throwable());
            ProcedureDecl pd = (ProcedureDecl) parent;
            ProcedureInstance pi = pd.procedureInstance();

            for (Type uncaughtExc : throwsSet) {
                boolean declared = false;
                for (Type declaredExc : catchable) {
                    if (ts.isSubtype(uncaughtExc, declaredExc)) {
                        declared = true;
                        break;
                    }
                }
                if (!declared && jifts.promoteToFatal(uncaughtExc))
                    fatalExcs.add(uncaughtExc);
            }

            if (fatalExcs.size() > 0) {
                if (pi instanceof ConstructorInstance) {
                    throw new SemanticException(
                            "Fail on exception not yet supported in constructors. "
                                    + "The following exceptions must be declared or caught: "
                                    + fatalExcs);
                }
                if (!extInfo.getJifOptions().noWarnings())
                    errorQueue().enqueue(ErrorInfo.WARNING,
                            "Uncaught exceptions in " + parent + " at "
                                    + parent.position()
                                    + " will be treated as fatal errors: "
                                    + fatalExcs);

                Position pos = Position.compilerGenerated();
                String s = UniqueID.newID("exc");
                List<Catch> catchBlocks = new LinkedList<Catch>();
                for (Type exType : fatalExcs) {
                    if (exType instanceof LabeledType)
                        exType = ((LabeledType) exType)
                                .labelPart(jifts.topLabel());
                    else exType =
                            jifts.labeledType(pos, exType, jifts.topLabel());

                    CanonicalTypeNode exTypeNode =
                            nf.CanonicalTypeNode(pos, exType);
                    Formal exc = nf.Formal(pos, Flags.NONE, exTypeNode,
                            nf.Id(pos, s));
                    JifLocalInstance fli = (JifLocalInstance) jifts
                            .localInstance(pos, Flags.NONE, exType, s);
                    fli.setLabel(jifts.topLabel());
                    exc = exc.localInstance(fli);

                    Local loc = nf.Local(pos, nf.Id(pos, s));
                    JifLocalInstance lli = (JifLocalInstance) jifts
                            .localInstance(pos, Flags.NONE, exType, s);
                    lli.setLabel(jifts.topLabel());
                    loc = loc.localInstance(lli);
                    loc = (Local) loc.type(exType);

                    List<Expr> args = new LinkedList<Expr>();
                    args.add(loc);

                    New newExc = nf.New(pos,
                            nf.CanonicalTypeNode(pos, jifts.fatalException()),
                            args);
                    ConstructorInstance ci =
                            ts.findConstructor(jifts.fatalException(),
                                    Collections.singletonList(
                                            (Type) ts.Throwable()),
                            jifts.fatalException());
                    newExc = newExc.constructorInstance(ci);
                    Throw thrw =
                            nf.Throw(pos, newExc.type(jifts.fatalException()));
                    ((JifThrowDel) thrw.del()).setThrownIsNeverNull();
                    Block body = nf.Block(pos, thrw);

                    Catch c = nf.Catch(pos, exc, body);
                    catchBlocks.add(c);
                }
                //remove fatal exceptions from throw types of children
                Block newBlock = (Block) n.visit(
                        new FatalExceptionSetter(job, ts, nf, fatalExcs));

                List<Stmt> stmts = newBlock.statements();
                Try t = nf.Try(pos, nf.Block(pos, stmts), catchBlocks);
                List<Stmt> newStmts = Collections.singletonList((Stmt) t);
                return newBlock.statements(newStmts);
            }
        }
        // gather exceptions from this node.
        return n.del().exceptionCheck(inner);
    }

    public static class FatalExceptionSetter extends ErrorHandlingVisitor {

        public FatalExceptionSetter(Job job, TypeSystem ts, NodeFactory nf,
                SubtypeSet toRemove) {
            super(job, ts, nf);
            this.toRemove = toRemove;
        }

        protected SubtypeSet toRemove;

        @Override
        public Node leaveCall(Node old, Node n, NodeVisitor v)
                throws SemanticException {
            if (n instanceof Throw) {
                Throw th = (Throw) n;
                if (toRemove.contains(th.expr().type()))
                    throw new SemanticException("Explicitly thrown exception "
                            + th.expr().type()
                            + " must either be caught or declared to be thrown."
                            + " : " + toRemove + ":: " + th.expr().type(),
                            th.position());
            }
            //sigh... Not all nodes have JifJL delegates.
            if (n.del() instanceof JifDel)
                ((JifDel) n.del()).setFatalExceptions(ts, toRemove);

            return n;
        }
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

        if (!t.isUncheckedException()) {
            // go through the stack of catches and see if the exception
            // is caught.
            boolean exceptionCaught = false;
            JifExceptionChecker ec = this;
            while (!exceptionCaught && ec != null) {
                if (ec.catchable != null) {
                    for (Type catchType : ec.catchable) {
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

            if (!exceptionCaught && !((JifTypeSystem) ts).promoteToFatal(t)) {
                reportUncaughtException(t, pos);
            }
        }
    }

}

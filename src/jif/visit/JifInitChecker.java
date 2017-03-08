package jif.visit;

import java.util.Map.Entry;
import java.util.Set;

import jif.types.JifClassType;
import jif.types.JifTypeSystem;
import jif.types.SemanticDetailedException;
import polyglot.ast.ConstructorCall;
import polyglot.ast.Node;
import polyglot.ast.NodeFactory;
import polyglot.ast.TypeNode;
import polyglot.frontend.Job;
import polyglot.types.ClassType;
import polyglot.types.FieldInstance;
import polyglot.types.LocalInstance;
import polyglot.types.SemanticException;
import polyglot.types.Type;
import polyglot.types.TypeSystem;
import polyglot.types.VarInstance;
import polyglot.visit.DefiniteAssignmentChecker;
import polyglot.visit.FlowGraph;

/**
 * Override the init checker, since type nodes may now mention local variables.
 */
public class JifInitChecker extends DefiniteAssignmentChecker {
    public JifInitChecker(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, ts, nf);
    }

    @Override
    protected void checkOther(FlowGraph<FlowItem> graph, Node n, FlowItem dfIn)
            throws SemanticException {
        if (n instanceof TypeNode) {
            // need to check type nodes for uses of locals.
            TypeNode tn = (TypeNode) n;
            Type t = tn.type();
            if (t instanceof JifClassType) {
                JifTypeSystem ts = (JifTypeSystem) t.typeSystem();
                Set<LocalInstance> lis = ts.labelTypeCheckUtil()
                        .localInstancesUsed((JifClassType) t);
                for (LocalInstance li : lis) {
                    checkLocalInstanceInit(li, dfIn, tn.position());
                }
            }
        } else if (n instanceof ConstructorCall) {
            ConstructorCall cc = (ConstructorCall) n;
            if (cc.kind() == ConstructorCall.SUPER
                    && superClassCouldAccessFinals(currCBI.currClass)) {
                // go through every final non-static field in dfIn.assignmentStatus
                for (Entry<VarInstance, AssignmentStatus> e : dfIn.assignmentStatus
                        .entrySet()) {
                    if (e.getKey() instanceof FieldInstance
                            && ((FieldInstance) e.getKey()).flags().isFinal()
                            && !((FieldInstance) e.getKey()).flags()
                                    .isStatic()) {
                        // we have a final non-static field                           
                        FieldInstance fi = (FieldInstance) e.getKey();
                        AssignmentStatus initCount = e.getValue();
                        if (!initCount.definitelyAssigned) {
                            throw new SemanticDetailedException(
                                    "Final field \"" + fi.name()
                                            + "\" must be initialized before "
                                            + "calling the superclass constructor.",
                                    "All final fields of a class must "
                                            + "be initialized before the superclass "
                                            + "constructor is called, to prevent "
                                            + "ancestor classes from reading "
                                            + "uninitialized final fields. The "
                                            + "final field \"" + fi.name()
                                            + "\" needs to "
                                            + "be initialized before the superclass "
                                            + "constructor call.",
                                    cc.position());
                        }
                    }
                }
            }
        }
    }

    protected boolean superClassCouldAccessFinals(ClassType ct) {
        JifTypeSystem ts = (JifTypeSystem) this.ts;
        if (ts.isSignature(ct))
            // contructor in a signature -- ignore.
            return false;
        else if (!ts.isSignature(ct) && ts.isSignature(ct.superType())
                && ts.hasUntrustedAncestor(ct) == null) {
            // extends a java class with a signature, and all ancestors 
            // of that java class have signatures (are trusted).
            return false;
        } else {
            return true;
        }
    }
//    /**
//     * Check that the local variable <code>l</code> is used correctly.
//     */
//    protected void checkField(FlowGraph<FlowItem> graph, Field l, FlowItem dfIn)
//            throws SemanticException {
//        AssignmentStatus initCount =
//                dfIn.assignmentStatus.get(l.fieldInstance().orig());
//        if (initCount == null || !initCount.definitelyAssigned) {
//            // the local variable may not have been initialized. 
//            // However, we only want to complain if the local is reachable
//            if (l.reachable()) {
//                throw new SemanticException("Local variable \"" + l.name()
//                        + "\" may not have been initialized", l.position());
//            }
//        }
//    }
}

package jif.visit;

import jif.JifScheduler;
import polyglot.ast.Node;
import polyglot.frontend.AbstractPass;
import polyglot.frontend.Job;
import polyglot.frontend.goals.Goal;
import polyglot.types.SemanticException;
import polyglot.util.*;

/** A pass which runs a visitor. */
public class LabelCheckPass extends AbstractPass
{
    private final Job job;
    private final LabelChecker lc;

    public LabelCheckPass(Goal goal, Job job, LabelChecker lc) {
	super(goal);
        this.job = job;
        this.lc = lc;
    }

    public boolean run() {
	Node ast = job.ast();

	if (ast == null) {
	    throw new InternalCompilerError("Null AST: did the parser run?");
	}

        ErrorQueue q = job.compiler().errorQueue();
        int nErrsBefore = q.errorCount();

        try {
            ast = lc.labelCheck(ast);
            ast = lc.finishedLabelCheckPass(ast);
        }
        catch (SemanticException e) {
            Position pos = e.position() != null
                         ? e.position() : ast.position();
            q.enqueue(ErrorInfo.SEMANTIC_ERROR, e.getMessage(), pos);
        }

        int nErrsAfter = q.errorCount();

        job.ast(ast);
        
        return (nErrsBefore == nErrsAfter);
    }
}

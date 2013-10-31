package jif;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jif.visit.JifExceptionChecker;
import polyglot.ast.NodeFactory;
import polyglot.frontend.Job;
import polyglot.frontend.Scheduler;
import polyglot.frontend.goals.Goal;
import polyglot.frontend.goals.VisitorGoal;
import polyglot.types.TypeSystem;

public class JifExceptionsChecked extends VisitorGoal {
    public static Goal create(Scheduler scheduler, Job job, TypeSystem ts,
            NodeFactory nf) {
        return scheduler.internGoal(new JifExceptionsChecked(job, ts, nf));
    }

    protected JifExceptionsChecked(Job job, TypeSystem ts, NodeFactory nf) {
        super(job, new JifExceptionChecker(job, ts, nf));
    }

    @Override
    public Collection<Goal> prerequisiteGoals(Scheduler scheduler) {
        List<Goal> l = new ArrayList<Goal>();
        l.add(scheduler.TypeChecked(job));
        l.add(scheduler.ReachabilityChecked(job));
        l.addAll(super.prerequisiteGoals(scheduler));
        return l;
    }

}

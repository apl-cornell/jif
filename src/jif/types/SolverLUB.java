package jif.types;

import java.util.Iterator;

import jif.types.hierarchy.PrincipalHierarchy;
import jif.types.label.*;
import jif.types.label.DynamicLabel;
import jif.types.label.Label;

import polyglot.types.SemanticException;
import polyglot.util.InternalCompilerError;

/** 
 * A solver of Jif constraints. Finds solution to constraints essentially
 * by propogating upper bounds backwards. 
 */
public class SolverLUB extends Solver
{
    /**
     * Constructor
     */
    public SolverLUB(JifTypeSystem ts) {
        super(ts);
    }

    /**
     * Constructor
     */
    protected SolverLUB(SolverLUB js) {
        super(js);
    }

    /**
     * This method adds the correct dependencies from Equation
     * eqn to varaiables occuring in eqn, and dependencies in the other 
     * direction (that is, from variables occuring in eqn to eqn).
     * 
     * There is a dependency from Equation eqn to all variables that occur on
     * the LHS of eqn, as the bounds on these variables may be modified 
     * (downwards) as a result of solving eqn. 
     * 
     * There is a dependency from all variables on the RHS of eqn to eqn, 
     * because modifying (downwards) the bounds on these variables may cause 
     * eqn to no longer be satisfied. 
     */
    protected void addDependencies(Equation eqn) {
        if (shouldReport(4)) {
            report(4, "Equation " + eqn + " depends on variables: " +
                   eqn.rhs().variables());
        }

        // Build dependency maps for this equation.
        for (Iterator j = eqn.lhs().variables().iterator(); j.hasNext(); ) {
            VarLabel v = (VarLabel) j.next();

            // If this equation is examined, then the bound for v may be changed
            addDependency(eqn, v);
        }    
        for (Iterator j = eqn.rhs().variables().iterator(); j.hasNext(); ) {
            VarLabel v = (VarLabel) j.next();

            // If the bound for v is changed (downward), then we may need to 
            // reexamine this equation.
            addDependency(v, eqn);
        }    
    }

    /**
     * The default bound of variables in this solver is top
     */
    protected Label getDefaultBound() {
        return ts.topLabel();
    }

    /**
     * This method changes the bounds of variables in the LHS of Equation eqn, 
     * to make the equation satisfied. If the LHS is a LabelOfVar, then it 
     * will postpone solving this equation until the only equations left to be
     * solved are dynamic equations.
     */
    protected void solve_eqn(Equation eqn) throws SemanticException {
        if (eqn.lhs() instanceof VarLabel) {
            // v <= L
            refineVariableEquation(
                    (VarLabel) eqn.lhs(), eqn, eqn.env().ph());
        }
        else if (eqn.lhs() instanceof LabelOfVar) {
            // label(v) <= L
            
            if (allActivesAreDynamic()) {
                refine_LoV(eqn);
                if (!eqn.env().leq(bounds().applyTo(eqn.lhs()), 
			    bounds().applyTo(eqn.rhs()))) {
                    throw new InternalCompilerError(
                        "Dynamic constraint refinement " +
                        "did not satisfy constraint " + eqn + ":" +
                        "LHS = " + bounds().applyTo(eqn.lhs()) + ", RHS = " +
                        bounds().applyTo(eqn.rhs()));
                }
            } else {
                // defer working on dynamic constraints as long as
                // possible: push back on the end of the worklist
                if (shouldReport(3))
                    report(3, "Deferring dynamic constraint");
                addEquationToQueue(eqn);
            }
        }
        else if (eqn.lhs() instanceof DynamicLabel) {
            // <dynamic L> <= L', where L may contain variables.
            // May need to lower the variables in LHS contained label.
            handleDynamicLHS(eqn);
        }
        else {
            // L <= L', where L cannot contain variables.
            checkEquation(eqn);
        }                
    }
    
    /**
     * Lower the upper bound on the label variable v, which is the LHS of the
     * equation eqn.
     */
    protected void refineVariableEquation(VarLabel v,
	                                Equation eqn, 
                                        PrincipalHierarchy ph) throws SemanticException {
	Label lhsBound = bounds().boundOf(v);
	Label rhs = eqn.rhs();
	if (shouldReport(4)) report(4, "BOUND of " + v + " = " + lhsBound);

	Label rhsBound = bounds().applyTo(rhs);
	if (shouldReport(4)) report(4, "APP = " + rhsBound);

	Label meet = solverMeet(ts, lhsBound, rhsBound, ph);
        
	if (meet.isBottom()) {
            meet = dynMeet(lhsBound, rhsBound, ph);
	}
	// XXX Previous line is highly suspect.

	if (shouldReport(3))
	    report(3, "   MEET: " + v + " := " + meet);
        
        addTrace(v, eqn, meet);
	setBound(v, meet, eqn.constraint()); 
        wakeUp(v);
    }
    
    /**
     * Make the constraint eqn, of the form label(V) <= L satisfied,
     * by lowering the upperbound of label(V).
     */
    protected void refine_LoV(Equation eqn) throws SemanticException {
	// We have a constraint of the form label(V) <= L.
	//
	// The thesis (Section 5.1.5) says:
	//
	// If this constraint is _not_ satisfied, then there is a
	// component P in U(V) such that P' in label(P) is not
	// covered by U(L).
	//
	// We can refine the upper bound of V in two ways to ensure
	// P' is not part of label(U(V)):
	//
	// 1. Drop P from U(V).
	// 2. If P = <dynamic L'>, and if L' contains variable V',
	//    such that P' in U(V'), then we can drop P' from U(V').
	//
	// Actually there may be multiple P's causing trouble, and multiple
	// P''s within a given P. So several separate refinements may be
	// needed, in general, and for each refinement the choice between
	// (1) and (2) exists. When it discovers a refinement that might
	// help, refine_LoV applies just that refinement and then starts
	// a recursive search to finish solving the current equation
	// and the rest of the constraint system. Any successful recursive
	// search will have made sure that all the constraints are satisfied,
	// so refine_LoV returns immediately in that case.

	// v is label(V)

	if (shouldReport(3))
	    report(3, "Refining a dynamic constraint");
	LabelOfVar v = (LabelOfVar)eqn.lhs();

	// varBound = U(V)
	Label varBound = bounds().boundOf(v.var());
	if (shouldReport(4))
	    report(4, "BOUND U(V) of " + v.var() + " := " + varBound);

	// lhsBound = label(U(V)) = U(label(V))
	Label lhsBound = varBound.labelOf();
	if (shouldReport(4))
	    report(4, "BOUND U(label(V)) of " + v + " = " + lhsBound);

	// U(L)
	Label rhsBound = bounds().applyTo(eqn.rhs());
	if (shouldReport(4))
	    report(4, "RHS L = " + rhsBound);

	// some subset of the components in U(V) are dynamic components
	// P such that label(P) is not <= U(L)
        if (varBound instanceof MeetLabel) {
            // U(V) is a meet of components, which is not desirable.
            // How do we deal with this situation? ###
            throw new InternalCompilerError("Meetlabel where I was hoping there wasn't one");
        }
	for (Iterator i = varBound.components().iterator(); i.hasNext(); ) {
	    // P in U(V)
	    Label P = (Label) i.next();
	    Label labelP = P.labelOf();

/*
	    if (P instanceof RuntimeLabel) {
		// Current upper bound of variable is TOP. Since there are
		// no other constraints on the variable (we've processed all
		// other equations that might lower it), we can assume its
		// label is entirely dynamic. Therefore can start with an
		// upper bound that includes all dynamic labels in the
		// equation system.
		labelP = maximumLabelOfLabel();
		if (shouldReport(4))
		    report(4, "Refining label(TOP) using bound " + l + " <= " +
		    bounds().applyTo(l));
		bounds.setBound(v.var(), bounds().applyTo(l));
		refine_LoV(eqn);
	    }
*/

	    // ulp = U(label(U(V)))
	    Label ulp = bounds().applyTo(labelP);
	    if (!eqn.env().leq(ulp, rhsBound)) {
		// Something needs to be done. First, try going through
		// label(P) and making sure every dynamic component is
		// covered (if possible).
		VarMap ub = bounds().copy(); // copy it
		boolean have_pprime = true;
                if (labelP instanceof MeetLabel) {
                    // labelOf(P) is a meet of components, which is not desirable.
                    // How do we deal with this situation? ###
                    throw new InternalCompilerError("Meetlabel where I was hoping there wasn't one");
                }
                    
		for (Iterator j = labelP.components().iterator(); j.hasNext(); ) {
		    // P' in label(P)
		    Label cj = (Label) j.next();
		    if (shouldReport(5))
			report(5, "Trying P' = " + cj);
		    Label pp = ub.applyTo(cj);

		    if (!eqn.env().leq(pp, rhsBound)) {
			if (cj instanceof VarLabel) {
			    ub.setBound((VarLabel)cj,
                                        solverMeet(ts, pp, rhsBound, eqn.env().ph())); 
			} else {
			    // it's hopeless
			    have_pprime=false;
			    break;
			}
		    }
		}
		if (have_pprime && search(eqn, ub)) return;
                
                // All cleverer ideas failed, just remove P entirely, if we can...
                // the destructive update to bounds is ok here
                if (!varBound.isSingleton()) {
                    setBound(v.var(), varBound.minus(P), eqn.constraint());
                }
                else {
                    // removing P is varBound... removes the whole thing...
                    setBound(v.var(), ts.bottomLabel(), eqn.constraint());
                }

                
                if (search(eqn, bounds()))
                    return;
		                
		throw new SemanticException("Search for refinement to " +
			"dynamic constraint " + eqn +" failed. \n" + 
			errorMsg(eqn.constraint()),
                        eqn.position());
	    }
	}
    }

    /**
     * Search recursively for solution to system of constraints. See comments
     * in {@link refine_LoV} for more details.
     */
    boolean search(Equation eqn, VarMap ub) {
        if (shouldReport(2))  {
            report (2, "===== Starting recursive search =====");
        }
	Solver js = new SolverLUB(this);
	js.addEquationToQueueHead(eqn); // make all the choices on eqn at once
	js.setBounds(ub);
	try {
	    setBounds(js.solve_bounds());
	    if (shouldReport(2))
		report(2, "Solution succeeded, finishing up");
	    return true;
	} catch (SemanticException dummy) {
	    if (shouldReport(2))
		report(2, "Solution failed, backtracking");
	    return false;
	}
    }

    /**
     * Handle all processing of a constraint with a dynamic label on the LHS, 
     * including putting the equation back on the proper queue.
     */
    protected void handleDynamicLHS(Equation eqn) throws SemanticException {
	DynamicLabel d = (DynamicLabel)eqn.lhs();
	if (!d.hasVariables()) { // nothing to refine
	    return;
	}
	VarLabel v = (VarLabel) d.label();
	// may need to change prev line if assumptions change

	Label lhsbound = bounds().applyTo(v);
	Label lrhs = bounds().applyTo(eqn.rhs()).labelOf();
	lrhs = lrhs.subst(d.uid(), d);
	Label rhsbound = bounds().applyTo(lrhs);

	if (shouldReport(4)) {
	    report(4, "  lhsbound = " + lhsbound);
	    report(4, "  bounds(rhs) = " + bounds().applyTo(eqn.rhs()));
	    report(4, "  label(bounds(rhs)) = " + lrhs);
	    report(4, "  bounds(label(bounds(rhs)) = " + rhsbound);
	}

	Label meet = solverMeet(ts, lhsbound, rhsbound, eqn.env().ph());
	Label existedBound = bounds().boundOf(v);
	if (!eqn.env().leq(existedBound, meet)) {
            addTrace(v, eqn, meet);
	    setBound(v, meet, eqn.constraint()); 
	    wakeUp(v);
	    if (shouldReport(3))
		report(3, "DYNAMIC LABEL MEET: " + v + " := " + meet);
	}
    }    
    
    /**
     * Return true if every active constraint is a dynamic constraint (that is,
     * if every active constraint has a LabelOfVar on the LHS)
     */
    protected boolean allActivesAreDynamic() {
        for (Iterator i = getQueue().iterator(); i.hasNext();) {
            Equation eqn = (Equation) i.next();
            if (!(eqn.lhs() instanceof LabelOfVar))
                return false;
        }
        return true;
    }

    /**
     * Check that the equation eqn is satisfied. The LHS of eqn cannot 
     * have any variables.
     * 
     * @throws SemanticException if eqn is not satisfied.
     * @throws InternalCompilerError if eqn contains variables
     */
    protected void checkEquation(Equation eqn) throws SemanticException
    {
        if (eqn.lhs().hasVariables()) {
            throw new InternalCompilerError("LHS of equation " + eqn +
                " should not contain variables.");
        }

        // This equation must have been woken up.  We need to
        // check whether it is solvable given the current variables.

        Label lhsLabel = eqn.lhs();
        if (shouldReport(3)) report(3, "LHS = " + lhsLabel);

        Label rhsBound = bounds().applyTo(eqn.rhs());
        if (shouldReport(3)) report(3, "RHS APP = " + rhsBound);

        // Check to see if it is currently satisfiable.
        if (! eqn.env().leq(lhsLabel, rhsBound)) {
            //try bounding the dynamic labels

            if (dynCheck(lhsLabel, rhsBound, eqn.env())) return;
            // This equation isn't satisfiable.
            reportError(eqn.constraint(), eqn.variables());
        }
    }

    /**
     * Find a contradicting equation.
     */
    protected Equation findContradictiveEqn(LabelConstraint c) {
        if (c.rhs() instanceof VarLabel) {                
            // The RHS is a VarLabel, so we may be able to find
            // an equation that contradicts this one.
            VarLabel v = (VarLabel) c.rhs();
            return findTrace(v, bounds().applyTo(c.lhs()), true);
        }
        return null;
    }
}

package jif.extension;

import java.util.*;

import jif.types.*;
import jif.types.label.Label;
import jif.types.principal.Principal;
import jif.visit.LabelChecker;
import polyglot.main.Report;
import polyglot.types.*;
import polyglot.util.*;

/** A checker of subtype relationships. 
 */
public class SubtypeChecker
{
    
    private Type origSupertype = null;
    private Type origSubtype = null;
    
    /** Check that subtype <= supertype */
    public void addSubtypeConstraints(LabelChecker lc, Position pos,
	Type supertype, Type subtype) throws SemanticException
    {
        try {
            
            // make sure that we take the top level labels off the types
            JifTypeSystem ts = lc.jifTypeSystem();
            supertype = ts.unlabel(supertype);
            subtype = ts.unlabel(subtype);                
            
            if (Report.should_report(Report.types, 1))
                Report.report(1, "Adding subtype constraints: " + supertype + " >= " + subtype);

            // record the original types, for error messages.
            origSupertype = supertype;
            origSubtype = subtype;

            if (! recursiveAddSubtypeConstraints(lc, pos, supertype, subtype, false)) {
                throw new SemanticException(subtype + " is not a subtype of " +
                    supertype + ".", pos);
            }
        }
        finally {
            origSupertype = origSubtype = null;
        }
    }

    private Label label(Param param, Position pos) throws SemanticException {
	if (param instanceof Label) {
	    return (Label) param;
	}
        if (param == null) {
            throw new SemanticException("No parameter given; expected a " +
                   "label parameter.",
                pos);            
        }
        
	throw new SemanticException("Parameter " + param + " is not a label.",
	    param.position());
    }

    private Principal principal(Param param, Position pos) throws SemanticException {

	if (param instanceof Principal) {
	    return (Principal) param;
	}

        if (param == null) {
            throw new SemanticException("No parameter given; expected a " + 
                   "principal parameter.",
                pos);            
        }

	throw new SemanticException("Parameter " + param +
	    " is not a principal.", param.position());
    }

    /**
     * Insert constraints so that subtype <= supertype.
     * Walk up the class hierarchy from subtype until we find supertype.
     * If not found, throw a <code>SemanticException</code>.
     * This really should not happen since we're past type checking.
     */
    private void addParamConstraints(LabelChecker lc, Position pos,
	JifClassType supertype, JifClassType subtype) throws SemanticException
    {
	if (Report.should_report(Report.types, 2))
	    Report.report(2, "Adding param constraints: " + supertype + " >= " + subtype);

	JifContext A = lc.jifContext();

	Iterator iter = polyTypeForClass(supertype).params().iterator();
	Iterator supIter = supertype.actuals().iterator();
	Iterator subIter = subtype.actuals().iterator();
	
        int counter = 0;
	while (iter.hasNext() && supIter.hasNext() && subIter.hasNext()) {
            counter++;
            final int count = counter;
	    final ParamInstance pi = (ParamInstance) iter.next();
	    Param supParam = (Param) supIter.next();
	    Param subParam = (Param) subIter.next();

	    if (pi.isInvariantLabel() || pi.isCovariantLabel()) {
                LabelConstraint.Kind kind = 
                    pi.isInvariantLabel() ? LabelConstraint.EQUAL // subParam == supParam
                  : pi.isCovariantLabel() ? LabelConstraint.LEQ   // subParam <= supParam
                  : null;
		
                final Type lOrigSubtype = origSubtype;
                final Type lOrigSupertype = origSupertype;
                lc.constrain(new LabelConstraint(new NamedLabel(
                                                     "sub_param_"+count,
                                                     StringUtil.nth(count) + " param of subtype " + lOrigSubtype,
                                                     label(subParam, pos)), 
                                                 kind, 
                                                 new NamedLabel(
                                                    "sup_param_"+count,
                                                    StringUtil.nth(count) + " param of supertype " + lOrigSupertype,
                                                    label(supParam, pos)), 
                                                 A.labelEnv(),
                                                 pos) {
                         public String msg() {
                             return lOrigSubtype + " is not a subtype of " + 
                                   lOrigSupertype + 
                                ", since the subtype relation between label " + 
                                "parameters is not satisfied.";
                         }
                         public String detailMsg() {
                             String variance = pi.isInvariantLabel() 
                                                      ? "invariant"
                                                      : "covariant";
                             String reln = kind() == EQUAL 
                                                      ? "equal to"
                                                      : "less restrictive than";
                             return lOrigSubtype + " is not a subtype of " + 
                                   lOrigSupertype + ". Subtyping requires " +
                                   "the " + StringUtil.nth(count) + 
                                   " parameter of the subtype to be " +
                                   reln + 
                                   " the " + StringUtil.nth(count) + 
                                   " parameter of the supertype, since that " +
                                   "parameter is " + variance + ".";
                         }
                 }
                 );
	    }
	    else if (pi.isPrincipal()) {
		if (! A.actsFor(principal(supParam, pos), principal(subParam, pos)) ||
		    ! A.actsFor(principal(subParam, pos), principal(supParam, pos))) {		    
		    throw new SemanticDetailedException(
                         origSubtype + " is not a subtype of " + 
                         origSupertype + ", since the principals are not equivalent.", 
                         origSubtype + " is not a subtype of " + 
                         origSupertype + ". Subtyping requires " +
                         "the " + StringUtil.nth(count) + 
                         " parameter of the subtype to be equivalent to the " +
                         StringUtil.nth(count) + " parameter of the supertype.",
                         pos);
		}
	    }
	}

	if (iter.hasNext() || supIter.hasNext() || subIter.hasNext())
	    throw new InternalCompilerError(pos,
		"Instantiation type parameter count mismatch.");
    }

    /** Check that subtype <= supertype */
    private boolean recursiveAddSubtypeConstraints(LabelChecker lc,
	Position pos, Type supertype, Type subtype, final boolean inArrayType) throws SemanticException
    {
	if (Report.should_report(Report.types, 2))
	    Report.report(2, "Adding subtype constraints: " + supertype + " >= " + subtype);

	JifTypeSystem ts = lc.jifTypeSystem();
	JifContext A = lc.jifContext();

	Type unlblSupertype = ts.unlabel(supertype);
	Type unlblSubtype = ts.unlabel(subtype);

	if (ts.isLabeled(supertype) && ts.isLabeled(subtype)) {
            // the two types are labeled. make sure that 
            // the label of supertype is at least as restrictve as that
            // of subtype, or if at least one of them is invariant, that they are equal
            final Type lOrigSubtype = origSubtype;
            final Type lOrigSupertype = origSupertype;
                lc.constrain(new LabelConstraint(new NamedLabel(
                                                                "label of type " + subtype,
                                                                ts.labelOfType(subtype)), 
                                                            LabelConstraint.LEQ, 
                                                            new NamedLabel(
                                                               "label of type " + supertype,
                                                                ts.labelOfType(supertype)), 
                                                            A.labelEnv(),
                                                            pos) {
                                        public String msg() {
                                            String s = lOrigSubtype + " is not a subtype of " + 
                                                  lOrigSupertype + ".";
                                            if (inArrayType) {
                                                s += " The base type of arrays must be equivalent.";
                                            }
                                            return s;
                                        }
                                        public String detailMsg() {
                                            if (inArrayType) {
                                                return lOrigSubtype + " is not a subtype of " + 
                                                lOrigSupertype + ". Subtyping requires " +
                                                "the base types of arrays to be equivalent.";                                                
                                            }
                                            return lOrigSubtype + " is not a subtype of " + 
                                                  lOrigSupertype + ". Subtyping requires " +
                                                  "the label of the subtype to be less " +
                                                  "restrictive than the label of the " +
                                                  "supertype.";
                                        }
                            }
                            );                
        }
        

        if (unlblSupertype instanceof JifClassType && unlblSubtype.isNull())
            return true;
        
        if (unlblSubtype instanceof JifClassType && unlblSupertype instanceof JifClassType) {
	    JifClassType sub = (JifClassType) unlblSubtype;
	    JifClassType sup = (JifClassType) unlblSupertype;

            
            LinkedList subPossibles = new LinkedList();
            subPossibles.add(sub);
            Set checkedPossibles = new HashSet();
            while (!subPossibles.isEmpty()) {
                JifClassType poss = (JifClassType)subPossibles.removeFirst();
                if (ts.equals(polyTypeForClass(poss), polyTypeForClass(sup))) {
                    // Insert constraints between parameters.
                    addParamConstraints(lc, pos, sup, poss);
                    return true;
                }
                // poss isn't the appropriate polytype.
                checkedPossibles.add(poss);
                
                // add the superclass and all interfaces of poss to the set of candidates.
                Type possParent = poss.superType();
                if (possParent instanceof JifClassType && 
                        !checkedPossibles.contains(possParent) && 
                        !subPossibles.contains(possParent)) {
                    subPossibles.add(possParent);
                }
                
                for (Iterator iter = poss.interfaces().iterator(); iter.hasNext(); ) {
                    Type possInterface = (Type) iter.next();
                    if (possInterface instanceof JifClassType && 
                            !checkedPossibles.contains(possInterface) && 
                            !subPossibles.contains(possInterface)) {
                        subPossibles.add(possInterface);
                    }
                }
            }
            return false;
	}
	
        if (unlblSubtype instanceof ArrayType && unlblSupertype instanceof ArrayType) {
            // both subtype and supertype are arrays, say of D and C respectively
            // i.e. subtype == D[], supertype = C[]
            // we insist that C[] >= D[] iff C >= D and D >= C.
            Type subBase = ((ArrayType)unlblSubtype).base();  
            Type supBase = ((ArrayType)unlblSupertype).base();

            if (!recursiveAddSubtypeConstraints(lc, pos, subBase, supBase, true) ||
                !recursiveAddSubtypeConstraints(lc, pos, supBase, subBase, true)) {
                return false; 
            }
        }

	return true;
    }
    
    /**
     * Return the <code>JifPolyType</code> for the given 
     * <code>JifClassType jct</code>. If <code>jct</code> is an 
     * instance of <code>JifPolyType</code>, then <code>jct</code> is returned; 
     * otherwise, if <code>jct</code> is an instance of 
     * <code>JifSubstType</code> then <code>jct.base()</code> 
     * is returned.
     *  
     */
    public static JifPolyType polyTypeForClass(JifClassType jct) {
        if (jct instanceof JifPolyType) {
            return (JifPolyType)jct;
        }
        else if (jct instanceof JifSubstType) {
            return (JifPolyType)((JifSubstType)jct).base();
        }
        throw new InternalCompilerError("Unexpected JifClassType instance." +
            "Expected a JifPolyType or JifSubstType, but got " + 
            jct.getClass().getName(), jct.position());
    }
}

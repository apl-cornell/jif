package jif.types.label;

import java.util.List;

import jif.types.*;
import jif.visit.LabelChecker;

import polyglot.types.*;


/**
 * This class is the common super class for integrity polices and 
 * confidentiality policies.
 */
public interface Policy extends TypeObject { 
    boolean isCanonical();
    boolean isSingleton();
    boolean isRuntimeRepresentable();
    boolean isTop();
    boolean isBottom();
    
    List throwTypes(TypeSystem ts);
    Policy subst(LabelSubstitution substitution) throws SemanticException;
    Policy simplify();
    
    PathMap labelCheck(JifContext A, LabelChecker lc);
}
package jif.types;

import java.util.List;

import jif.types.label.Label;

import polyglot.types.ClassType;
import polyglot.types.SemanticException;

/** Jif class type. 
 */
public interface JifClassType extends ClassType {
    /**
     * The principals that grant authority to objects of this class, i.e.
     * the principals listed in the authority clause. 
     */
    List authority();
    
    /**
     * The principals whose authority is required by the context that
     * creates a new instance of this class. More precisely, this is (possibly
     * instantiated) parameter principals that are in the authority list. 
     * See Andrew's thesis, 4.6.2.
     */    
    List constructorCallAuthority();
    
    Label thisLabel();
    boolean invariant();
    JifClassType setInvariantThis(Label l) throws SemanticException;
    List actuals();
}

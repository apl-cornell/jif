package jif.types;

import java.util.List;

import jif.types.label.Label;
import polyglot.types.ProcedureInstance;

/** Jif procedure instance. A wrapper of all the type information 
 *  related to a procedure. 
 */
public interface JifProcedureInstance extends ProcedureInstance
{
    Label startLabel();
    Label returnLabel();
    List constraints();
    void setStartLabel(Label startLabel, boolean isDefault);
    void setReturnLabel(Label returnLabel, boolean isDefault);
    void setConstraints(List constraints);

    boolean isDefaultStartLabel();
    boolean isDefaultReturnLabel();

    /** A bound of the PC of the caller. In Jif, it's always the begin label.*/
    Label externalPC(); 
    
    String debugString();

    void subst(VarMap bounds);
}

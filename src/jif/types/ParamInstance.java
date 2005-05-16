package jif.types;

import polyglot.types.*;
import polyglot.util.Enum;

/** A parameter instance. A wrapper of all the type information 
 *  related to a label/principal parameter. 
 */
public interface ParamInstance extends VarInstance
{
    public static class Kind extends Enum {
	protected Kind(String name) { super(name); }
    }

    public final static Kind INVARIANT_LABEL = new Kind("label");
    public final static Kind COVARIANT_LABEL = new Kind("covariant label");
    public final static Kind PRINCIPAL       = new Kind("principal");

    JifClassType container();
    ParamInstance container(JifClassType container);

    Kind kind();
    ParamInstance kind(Kind kind);
    ParamInstance name(String name);

    boolean isPrincipal();
    boolean isLabel();
    boolean isInvariantLabel();
    boolean isCovariantLabel();
}

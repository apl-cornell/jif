package jif.types;

import polyglot.types.VarInstance;
import polyglot.util.Enum;
import polyglot.util.SerialVersionUID;

/** A parameter instance. A wrapper of all the type information 
 *  related to a label/principal parameter. 
 */
public interface ParamInstance
        extends polyglot.ext.param.types.Param, VarInstance {
    public static class Kind extends Enum {
        private static final long serialVersionUID =
                SerialVersionUID.generate();

        final boolean isPrincipal;
        final boolean isInvariantLabel;
        final boolean isCovariantLabel;

        public Kind(String name, boolean isPrincipal, boolean isInvariantLabel,
                boolean isCovariantLabel) {
            super(name);
            this.isPrincipal = isPrincipal;
            this.isCovariantLabel = isCovariantLabel;
            this.isInvariantLabel = isInvariantLabel;
        }

        public boolean isPrincipal() {
            return isPrincipal;
        }

        public boolean isCovariantLabel() {
            return isCovariantLabel;
        }

        public boolean isInvariantLabel() {
            return isInvariantLabel;
        }

        @Override
        public Object intern() {
            // This forces the class loader to load ParamInstance when
            // deserializing ParamInstance.Kinds so that the Enum constants
            // defined below don't conflict with the deserialized objects.
            @SuppressWarnings("unused")
            Object o = PRINCIPAL;
            return super.intern();
        }
    }

    public final static Kind INVARIANT_LABEL =
            new Kind("label", false, true, false);
    public final static Kind COVARIANT_LABEL =
            new Kind("covariant label", false, false, true);
    public final static Kind PRINCIPAL =
            new Kind("principal", true, false, false);

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

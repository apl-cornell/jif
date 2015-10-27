package jif.types;

import polyglot.util.Enum;
import polyglot.util.SerialVersionUID;

/** A control flow path. 
 */
public interface Path {
    /** Normal termination */
    public static final Path N = new FixedPath("N");

    /** Return termination */
    public static final Path R = new FixedPath("R");

    /** Normal value label */
    public static final Path NV = new FixedPath("NV");

    public static class FixedPath extends Enum implements Path {
        private static final long serialVersionUID =
                SerialVersionUID.generate();

        protected FixedPath(String name) {
            super(name);
        }
    }
}

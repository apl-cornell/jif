package jif.types;

import polyglot.util.*;

/** A control flow path. 
 */
public interface Path
{
    // Normal termination
    public static final Path N = new FixedPath("N");

    // Return termination
    public static final Path R = new FixedPath("R");

    // Normal value label
    public static final Path NV = new FixedPath("NV");

    // Return value label
    public static final Path RV = new FixedPath("RV");

    public static class FixedPath extends Enum implements Path {
	FixedPath(String name) { super(name); }
    }
}

package jif.types;

import polyglot.types.TypeObject;

/** The root of the Jif class parameter types.
 */
public interface Param extends TypeObject {
    @Override
    JifTypeSystem typeSystem();

    boolean isRuntimeRepresentable();

    @Override
    boolean isCanonical();
}

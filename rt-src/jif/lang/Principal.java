package jif.lang;

/**
 * See the documentation for the Jif source file, $JIF/sig-src/jif/lang/Principal.jif.
 */
public interface Principal {
    String name();

    boolean delegatesTo(final Principal p);

    boolean equals(final Principal p);

    boolean isAuthorized(final Object authPrf, final Closure closure,
            final Label lb, final boolean executeNow);

    ActsForProof findProofUpto(final Principal p, final Object searchState);

    ActsForProof findProofDownto(final Principal q, final Object searchState);
}

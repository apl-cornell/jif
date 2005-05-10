package jif.lang;


/**
 * An InternalPrincipal is used primarily for test purposes, to represent
 * principals like "Alice" and "Bob".
 */
public class InternalPrincipal extends AbstractPrincipal {
    public InternalPrincipal(String name) {
        super(name);
    }
}

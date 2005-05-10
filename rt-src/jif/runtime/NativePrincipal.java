package jif.runtime;

import java.util.Set;

import jif.lang.AbstractPrincipal;

/**
 * A NativePrincipal represents the file system users and groups.
 */
public class NativePrincipal extends AbstractPrincipal {
    public NativePrincipal(String name) {
        super(name);
    }
    public Set superiors() {
        return this.superiors;
    }
}

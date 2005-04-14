package jif.types.principal;

import jif.types.label.AccessPath;
import jif.types.label.AccessPathRoot;

/** Dynamic principal. 
 */
public interface DynamicPrincipal extends Principal {
    AccessPath path();
    public Principal subst(AccessPathRoot r, AccessPath e);
}

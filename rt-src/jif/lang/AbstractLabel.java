package jif.lang;

import java.util.*;

/**
 * A Label is the runtime representation of a Jif label. A Label consists of a
 * set of components, each of which is a {@link jif.lang.Policy Policy}.
 *  
 */
public abstract class AbstractLabel implements Label
{
    public final String toString() {
        return "{" + this.componentString() + "}";
    }
    
    public final Label join(Label l) {
        return LabelUtil.join(this, l);
    }
    
}

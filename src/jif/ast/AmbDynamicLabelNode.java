package jif.ast;

import polyglot.ast.*;

/** An ambiguous dynamic label. */
public interface AmbDynamicLabelNode extends LabelNode, Ambiguous {
    /** Gets the name of the dynamic label. */
    String name();
}

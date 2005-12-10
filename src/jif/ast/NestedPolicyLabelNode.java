package jif.ast;

import java.util.*;

/** This class represents a nested policy label, e.g. in the label
 * {L; {Alice:;Bob!:}; *lbl; {}}, {Alice:;Bob!:} and {} are both nested
 * policy labels. 
 */
public interface NestedPolicyLabelNode extends LabelNode {
}

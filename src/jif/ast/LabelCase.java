package jif.ast;

import jif.types.*;
import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

import java.util.*;

/** An immutable represention of a Jif <code>label case</code> statement
 *  that handles a case of a Jif <code>switch label</code> statement. 
 */
public interface LabelCase extends CompoundStmt {
    LabelNode label();
    LabelCase label(LabelNode label);

    Formal decl();
    LabelCase decl(Formal decl);

    Stmt body();
    LabelCase body(Stmt body);

    boolean isDefault();
}


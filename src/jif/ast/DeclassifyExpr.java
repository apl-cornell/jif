package jif.ast;

import polyglot.ast.*;
import polyglot.types.*;
import polyglot.util.*;
import polyglot.visit.*;

import jif.types.*;

import java.util.*;

/** An immutable representation of the Jif <code>declassify</code> expression. 
 *  <p>Grammer: <code>declassify(expression, label_1, label_2)</code> </p>
 *  If the label of the <code>expression</code> is less restrictive than 
 *  <code>label_2</code>, then declassify its label to <code>label_1</code>.
 */
public interface DeclassifyExpr extends Expr
{
    LabelNode label();
    DeclassifyExpr label(LabelNode label);

    Expr expr();
    DeclassifyExpr expr(Expr expr);
    
    LabelNode bound();
    DeclassifyExpr bound(LabelNode label);
}

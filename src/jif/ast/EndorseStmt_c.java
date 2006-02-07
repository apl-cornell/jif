package jif.ast;

import java.util.List;

import polyglot.ast.*;
import polyglot.ext.jl.ast.Stmt_c;
import polyglot.util.*;
import polyglot.visit.*;

/** An implementation of the <code>EndorseStmt</code> interface.
 */
public class EndorseStmt_c extends DowngradeStmt_c implements EndorseStmt
{
    public EndorseStmt_c(Position pos, LabelNode bound,
                            LabelNode label, Stmt body) {
	super(pos, bound, label, body);
    }

    protected String downgradeKind() {
        return "endorse";
    }

}

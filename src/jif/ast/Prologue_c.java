package jif.ast;

import java.util.List;

import jif.types.JifContext;
import polyglot.ast.Block_c;
import polyglot.ast.Ext;
import polyglot.ast.Stmt;
import polyglot.types.Context;
import polyglot.util.Position;
import polyglot.util.SerialVersionUID;

public class Prologue_c extends Block_c implements Prologue {
    private static final long serialVersionUID = SerialVersionUID.generate();

//    @Deprecated
    public Prologue_c(Position pos, List<Stmt> statements) {
        this(pos, statements, null);
    }

    public Prologue_c(Position pos, List<Stmt> statements, Ext ext) {
        super(pos, statements, ext);
    }

    @Override
    public Context enterScope(Context c) {
        return ((JifContext) c).pushPrologue();
    }
}

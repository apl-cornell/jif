package jif.parse;

import polyglot.ast.*;
import polyglot.visit.*;
import polyglot.types.*;
import polyglot.util.*;

import java.util.*;

public abstract class Amb 
{
    Position pos;
    Grm parser;

    Amb(Grm parser, Position pos) {
	this.parser = parser;
	this.pos = pos;
    }

    public Expr wrap() throws Exception { return new Wrapper(this); }
    public Expr toExpr() throws Exception { parser.die(pos); return null; }
    public Prefix toPrefix() throws Exception { parser.die(pos); return null; }
    public PackageNode toPackage() throws Exception { parser.die(pos); return null; }
    public Receiver toReceiver() throws Exception { parser.die(pos); return null; }
    public TypeNode toType() throws Exception { parser.die(pos); return null; }
    public TypeNode toUnlabeledType() throws Exception { parser.die(pos); return null; }
    public TypeNode toClassType() throws Exception { parser.die(pos); return null; }
    public Expr toNewArray(Position p) throws Exception { parser.die(pos); return null; }
    public Expr toNewArrayPrefix(Position p) throws Exception { parser.die(pos); return null; }
    public String toIdentifier() throws Exception { parser.die(pos); return null; }
    public String toName() throws Exception { parser.die(pos); return null; }
}



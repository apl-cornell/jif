package jif.parse;

import polyglot.ast.*;
import polyglot.visit.*;
import polyglot.types.*;
import polyglot.util.*;

public class Declarator {
    Position pos;
    String name;
    int dims;
    Expr init;

    Declarator(Position pos, String name) {
	this.pos = pos;
	this.name = name;
	this.dims = 0;
	this.init = null;
    }
}

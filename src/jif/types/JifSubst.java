package jif.types;

import polyglot.types.*;
import polyglot.ext.param.types.*;
import polyglot.util.*;
import java.util.*;

import jif.types.label.Label;
import jif.types.principal.Principal;

public interface JifSubst extends Subst
{
    public Assertion substConstraint(Assertion constraint);
    public Label substLabel(Label label);
    // public Label substLabel(Label label, Label thisL);
    public Principal substPrincipal(Principal principal);

    public List substConstraintList(List constraints);
    public List substLabelList(List labels);
    public List substPrincipalList(List principals);

    public Param get(UID uid);
    public void put(UID uid, Param param);
}

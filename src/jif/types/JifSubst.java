package jif.types;

import java.util.List;

import jif.types.label.Label;
import jif.types.principal.Principal;
import polyglot.ext.param.types.Subst;

public interface JifSubst extends Subst
{
    public <Actor extends ActsForParam, Granter extends ActsForParam> Assertion substConstraint(
            Assertion constraint);

    public Label substLabel(Label label);
    // public Label substLabel(Label label, Label thisL);
    public Principal substPrincipal(Principal principal);

    public <C extends Assertion> List<C> substConstraintList(List<C> constraints);
    public List<Label> substLabelList(List<Label> labels);
    public List<Principal> substPrincipalList(List<Principal> principals);

    public Param get(ParamInstance pi);
    public void put(ParamInstance pi, Param param);
}

package jif.types.label;



/** 
 * Dynamic label. 
 */
public interface DynamicLabel extends Label
{
    AccessPath path(); 

    Label subst(AccessPathRoot r, AccessPath e);
}

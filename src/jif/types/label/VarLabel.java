package jif.types.label;


/** The variable label. 
 */
public interface VarLabel extends Label {
    String name();
    
    void setMustRuntimeRepresentable();
    boolean mustRuntimeRepresentable();

}

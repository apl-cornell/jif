package jif.types;

import java.util.List;

import jif.types.label.ArgLabel;
import jif.types.label.Label;
import jif.types.principal.ArgPrincipal;
import jif.types.principal.Principal;
import polyglot.util.InternalCompilerError;

/**
 * This class is used to substitute arbitrary labels for
 * {@link jif.types.label.ArgLabel ArgLabel}s,
 * {@link jif.types.label.DynamicArgLabel DynamicArgLabel}s and
 * {@link jif.types.principal.ArgPrincipal ArgPrincipal}s.
 */
public class ArgLabelSubstitution extends LabelSubstitution {
    /**
     * The labels to replace the ArgLabels with.
     */
    private List argLabels;
    
    /**
     * The labels/principals to replace the ArgPrincipals and 
     * DynamicArgLabels with.
     */
    private List dynamicArgs;
    
    /**
     * Should only arguments with isSignature() returning true be replaced?
     */
    private boolean replaceSignatureOnly;
    
    /**
     * 
     * @param argLabels The labels to replace the ArgLabels with.
     * @param replaceSignatureOnly Should only arguments with isSignature() 
     *         returning true be replaced?
     */
    public ArgLabelSubstitution(List argLabels, boolean replaceSignatureOnly) {
        this(argLabels, null, replaceSignatureOnly);        
    }
    
    /**
     * 
     * @param argLabels The labels to replace the ArgLabels with.
     * @param dynamicArgs The labels/principals to replace the 
     *        ArgPrincipals and DynamicArgLabels with.
     * @param replaceSignatureOnly Should only arguments with isSignature() 
     *         returning true be replaced?
     */
    public ArgLabelSubstitution(List argLabels, 
                                List dynamicArgs,
                                boolean replaceSignatureOnly) {
        this.argLabels = argLabels;
        this.dynamicArgs = dynamicArgs;
        this.replaceSignatureOnly = replaceSignatureOnly;
    }

    
    public Label substLabel(Label L) {
        return L;
        //@@@@@
//        if (L instanceof ArgLabel) {
//            ArgLabel al = (ArgLabel)L;
//
//            if (!this.replaceSignatureOnly || al.isSignature()) {
//                if (al.index() >= this.argLabels.size()) {
//                    throw new InternalCompilerError("Unexpected index: " + al);                    
//                }
//                return (Label)this.argLabels.get(al.index());
//            }
//        }
//        else if (L instanceof DynamicArgLabel && dynamicArgs != null) {
//            DynamicArgLabel dal = (DynamicArgLabel)L;
//
//            if (!this.replaceSignatureOnly || dal.isSignature()) {
//                if (dal.index() >= this.dynamicArgs.size()) {
//                    throw new InternalCompilerError("Unexpected index: " + dal);
//                }
//                Object dynLbl = this.dynamicArgs.get(dal.index());
//                if (dynLbl == null || !(dynLbl instanceof Label)) {
//                    throw new InternalCompilerError(dal.position(),
//                     "The dynamic argument in position " + dal.index() + 
//                      " was not a label!");
//                }
//                return (Label)dynLbl;
//            }
//        }
//        return L;
    }

    public Principal substPrincipal(Principal p) {
        if (p instanceof ArgPrincipal && dynamicArgs != null) {
            ArgPrincipal dap = (ArgPrincipal)p;
            
            if (!this.replaceSignatureOnly || dap.isSignature()) {
                if (dap.index() >= this.dynamicArgs.size()) {
                    throw new InternalCompilerError("Unexpected index: " + dap);
                }
                Object dynPcp = this.dynamicArgs.get(dap.index());
                if (dynPcp == null || !(dynPcp instanceof Principal)) {
                    throw new InternalCompilerError(dap.position(),
                     "The dynamic argument in position " + dap.index() + 
                      " was not a principal!");
                }
                return (Principal)dynPcp;
            }
        }
        return p;
    }        
}

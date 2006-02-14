package jif;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

import jif.types.Solver;
import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.main.UsageError;

/**
 * This object encapsulates various polyglot options.
 */
public class JifOptions extends Options {
    /*
     * Fields for storing values for options.
     */
    /**
     * Should the checking for the robustness condition be disabled?
     */
    public boolean noRobustness;

    
     /**
      * Use a single Solver to infer labels globally, or solve on a class
      * by class basis.
      */
     boolean solveGlobally;

     /**
      * Provide more detailed explanation of solver error messages?
      */
     public boolean explainErrors;

     /**
      * The classpath for the Jif signatures of java.lang objects.
      */
     String sigcp = null;

     /**
      * Additional classpath entries for Jif signatures.
      */
     List addSigcp = new ArrayList();

      /**
      * The classpath for the Jif runtime library
      */
     String rtcp = null;

     /**
      * Additional classpath entries for Jif runtime code.
      */
     List addRtcp = new ArrayList();

    /**
     * Constructor
     */
    public JifOptions(ExtensionInfo extension) {
        super(extension);
        setDefaultValues();
    }

    /**
     * Set default values for options
     */
    public void setDefaultValues() {
        super.setDefaultValues();
        solveGlobally = false;
        explainErrors = false;
        noRobustness = true;
    }

    /**
     * Parse a command
     * @return the next index to process. That is, if calling this method
     *         processes two commands, then the return value should be index+2
     */
    protected int parseCommand(String args[], int index, Set source) throws UsageError {
        if (args[index].equals("-stop_constraint")) {
            index++;
            Solver.stop_constraint = Integer.parseInt(args[index]);
            System.err.println("Will trap on constraint "+
                               Solver.stop_constraint);
            index++;
        }
        else if (args[index].equals("-globalsolve")) {
            index++;
            System.err.println("Will use a single solver to infer labels");
            solveGlobally = true;
        }
        else if (args[index].equals("-explain") || args[index].equals("-e")) {
            index++;
            explainErrors = true;
        }
        else if (args[index].equals("-norobust")) {
            index++;
            noRobustness = true;
        }
        else if (args[index].equals("-sigcp")) {
            index++;
            this.sigcp = args[index++];
        }
        else if (args[index].equals("-addsigcp")) {
            index++;
            this.addSigcp.add(args[index++]);
        }
        else if (args[index].equals("-rtcp")) {
            index++;
            this.rtcp = args[index++];
        }
        else if (args[index].equals("-addrtcp")) {
            index++;
            this.addRtcp.add(args[index++]);
        }
        else if (args[index].equals("-debug")) {
            index++;
            int level=0;
            try {
                level = Integer.parseInt(args[index]);
            } 
            catch (NumberFormatException e) {
            }
            Report.addTopic("debug", level);
            index++;
        }
        else {
            int i = super.parseCommand(args, index, source);
            return i;
        }
        return index;
    }

    /**
     * Print usage information
     */
    public void usage(PrintStream out) {
        super.usage(out);
        usageForFlag(out, "-e -explain", "provide more detailed " +
                                         "explanations of failed label checking.");
        usageForFlag(out, "-norobust", "disable checking of robustness conditions for declassification");
        usageForFlag(out, "-debug <n>", "set debug level to n. Prints more information about labels.");
        usageForFlag(out, "-stop_constraint <n>", "halt when the nth constraint is added");
        usageForFlag(out, "-globalsolve", "infer label variables globally (default: per class)");
        usageForFlag(out, "-sigcp <path>", "path for Jif signatures (e.g. for java.lang.Object)");
        usageForFlag(out, "-addsigcp <path>", "additional path for Jif signatures; prepended to sigcp");
        usageForFlag(out, "-rtcp <path>", "path for Jif runtime classes");
        usageForFlag(out, "-addrtcp <path>", "additional path for Jif runtime classes; prepended to rtcp");
    }

    public String constructJifClasspath() {
        // use the signature classpath if it exists for compiling Jif classes
        String scp = "";
        for (Iterator iter = addSigcp.iterator(); iter.hasNext(); ) {
            scp += ((String)iter.next()) + File.pathSeparator;            
        }
        if (sigcp != null) {
            scp += sigcp + File.pathSeparator;
        }
        return scp + constructFullClasspath();
    }

    protected String constructRuntimeClassPath() {
        String rcp = "";
        for (Iterator iter = addRtcp.iterator(); iter.hasNext(); ) {
            rcp += ((String)iter.next()) + File.pathSeparator;            
        }
        if (rtcp != null) {
            rcp += rtcp + File.pathSeparator;
        }
        return rcp;        
    }
    
    public String constructOutputExtClasspath() {
        // use the runtime classpath if it exists for compiling the output classes
        // Note that we do not want to use the signature class path, since it contains
        // labels and principals as primitives.
        return constructRuntimeClassPath() + constructFullClasspath();
    }

    public String constructPostCompilerClasspath() {
        return constructRuntimeClassPath() + 
            super.constructPostCompilerClasspath() + File.pathSeparator
                + constructFullClasspath();
    }

}

package jif;

import java.util.Set;
import java.util.StringTokenizer;
import java.io.PrintStream;
import java.io.File;
import polyglot.main.Options;
import polyglot.main.Report;
import polyglot.main.UsageError;
import jif.types.Solver;

/**
 * This object encapsulates various polyglot options.
 */
public class JifOptions extends Options {
    /*
     * Fields for storing values for options.
     */
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
      * The classpath for the Jif runtime library
      */
     String rtcp = null;


    /**
     * Constructor
     */
    public JifOptions(ExtensionInfo extension) {
        super(extension);
    }

    /**
     * Set default values for options
     */
    public void setDefaultValues() {
        super.setDefaultValues();
        solveGlobally = false;
        explainErrors = false;
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

        else if (args[index].equals("-sigcp")) {
            index++;
            this.sigcp = args[index++];
        }
        else if (args[index].equals("-rtcp")) {
            index++;
            this.rtcp = args[index++];
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
        usageForFlag(out, "-debug <n>", "set debug level to n. Prints more information about labels.");
        usageForFlag(out, "-stop_constraint <n>", "halt when the nth constraint is added");
        usageForFlag(out, "-globalsolve", "infer label variables globally (default: per class)");
        usageForFlag(out, "-sigcp <path>", "path for Jif signatures (e.g. for java.lang.Object)");
        usageForFlag(out, "-rtcp <path>", "path for Jif runtime classes");
    }

    public String constructJifClasspath() {
        // use the signature classpath if it exists for compiling Jif classes
        if (sigcp != null) {
            return sigcp + File.pathSeparator + constructFullClasspath();
        }
        return constructFullClasspath();
    }

    public String constructOutputExtClasspath() {
        // use the runtime classpath if it exists for compiling the output classes
        // Note that we do not want to use the signature class path, since it contains
        // labels and principals as primitives.
        if (rtcp != null) {
            return rtcp + File.pathSeparator + constructFullClasspath();
        }
        return constructFullClasspath();
    }

    public String constructPostCompilerClasspath() {
        if (rtcp != null) {
            return rtcp + File.pathSeparator
                + super.constructPostCompilerClasspath() + File.pathSeparator
                + constructFullClasspath();
        }
        return super.constructPostCompilerClasspath() + File.pathSeparator
                + constructFullClasspath();
    }

}

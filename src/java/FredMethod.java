/*
 *    Ecotype Simulation models the sequence diversity within a bacterial
 *    clade as the evolutionary result of net ecotype formation and periodic
 *    selection, yielding a certain number of ecotypes.
 *
 *    Copyright (C) 2013-2016  Jason M. Wood, Montana State University
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package ecosim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 *  Object to interact with the fredmethod program.
 *
 *  @author Jason M. Wood
 *  @copyright GNU General Public License
 */
public class FredMethod implements Runnable {

    /**
     *  Run the fredmethod program.
     *
     *  @param masterVariables The MasterVariables object.
     *  @param execs The Execs object.
     *  @param nu The number of environmental sequences.
     *  @param length The length of the sequences being analyzed.
     *  @param binning The Binning object.
     *  @param parameterSet The set of parameters to optimize.
     */
    public FredMethod (MasterVariables masterVariables, Execs execs,
        Integer nu, Integer length, Binning binning,
        ParameterSet parameterSet) {
        this.masterVariables = masterVariables;
        this.execs = execs;
        this.nu = nu;
        this.length = length;
        this.binning = binning;
        this.parameterSet = parameterSet;
        String workingDirectory = masterVariables.getWorkingDirectory ();
        inputFileName = workingDirectory + "fredmethodIn.dat";
        outputFileName = workingDirectory + "fredmethodOut.dat";
        hasRun = false;
    }

    /**
     *  Run the fredmethod program.
     */
    public void run () {
        File inputFile = new File (inputFileName);
        File outputFile = new File (outputFileName);
        // Write the input values for the program to the hclimbIn.dat file.
        writeInputFile (inputFile);
        // Run the fredmethod program.
        execs.runFredMethod (inputFile, outputFile);
        // Get the output provided by the fredmethod program.
        readOutputFile (outputFile);
        // Set the flag stating that the fredmethod program has been run.
        if (result.getNpop () > 0) {
            hasRun = true;
        }
    }

    /**
     *  Returns true if fredmethod has been run, false otherwise.
     *
     *  @return True if fredmethod has been run, false otherwise.
     */
    public boolean hasRun () {
        return hasRun;
    }

    /**
     *  Returns the result of the fredmethod program.
     *
     *  @return The result.
     */
    public ParameterSet getResult () {
        return result;
    }

    /**
     *  Changes the value of hasRun.
     *
     *  @param hasRun The new value of hasRun.
     */
    public void setHasRun (boolean hasRun) {
        this.hasRun = hasRun;
    }

    /**
     *  Change the result stored in this object.
     *
     *  @param result The new result to store.
     */
    public void setResult (ParameterSet result) {
        this.result = result;
    }

    /**
     *  Returns the fredmethod result as a String.
     *
     *  @return the fredmethod result.
     */
    public String toString () {
        return result.toString ();
    }

    /**
     *  Private method to write the input file for the fredmethod program.
     *
     *  @param inputFile The file to write to.
     */
    private void writeInputFile (File inputFile) {
        ArrayList<BinLevel> bins = binning.getBins ();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter (new FileWriter (inputFile));
            writer.write (String.format ("%-20d numcrit\n", bins.size ()));
            // Output the crit levels and the number of bins.
            for (int j = 0; j < bins.size (); j ++) {
                writer.write (String.format (
                    "%-20.6f %-20d\n",
                    bins.get (j).getCrit (),
                    bins.get (j).getLevel ()
                ));
            }
            // Write the omega value.
            writer.write (
                String.format ("%-20.5f omega\n", parameterSet.getOmega ())
            );
            // Write the sigma value.
            writer.write (
                String.format ("%-20.5f sigma\n", parameterSet.getSigma ())
            );
            // Write the npop value.
            writer.write (
                String.format ("%-20d npop\n", parameterSet.getNpop ())
            );
            // Write the nu value.
            writer.write (String.format ("%-20d nu\n", nu));
            // Write the nrep value.
            writer.write (String.format ("%-20d nrep\n", nrep));
            // Create the random number seed; an odd integer less than nine
            // digits long.
            long iii = (long)(100000000 * Math.random ());
            if (iii % 2 == 0) {
                iii ++;
            }
            // Write the random number seed.
            writer.write (
                String.format ("%-20d iii (random number seed)\n", iii)
            );
            // Write the length of the sequences.
            writer.write (
                String.format (
                    "%-20d lengthseq (after deleting gaps, etc.)\n",
                    length
                )
            );
            // Write the whichavg value.
            writer.write (String.format ("%-20d whichavg\n", 1));
        }
        catch (IOException e) {
            System.out.println ("Error writing the input file.");
        }
        finally {
            if (writer != null) {
                try {
                    writer.close ();
                }
                catch (IOException e) {
                    System.out.println ("Error closing the input file.");
                }
            }
        }
    }

    /**
     *  Private method to read the output file from the fredmethod program.
     *
     *  @param outputFile The file to read from.
     */
    private void readOutputFile (File outputFile) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader (new FileReader (outputFile));
            String nextLine = reader.readLine ();
            while (nextLine != null) {
                StringTokenizer st = new StringTokenizer (nextLine);
                // There should only be one line containing omega, sigma,
                // npop, and the likelihood of that result.
                Double omega = new Double (st.nextToken ());
                Double sigma = new Double (st.nextToken ());
                Long npop = new Long (st.nextToken ());
                Double likelihood = new Double (st.nextToken ());
                result = new ParameterSet (npop, omega, sigma, likelihood);
                nextLine = reader.readLine ();
            }
        }
        catch (IOException e) {
            System.out.println ("Error reading the output file.");
        }
        finally {
            if (reader != null) {
                try {
                    reader.close ();
                }
                catch (IOException e) {
                    System.out.println ("Error closing the output file.");
                }
            }
        }
    }

    private String inputFileName;
    private String outputFileName;

    private MasterVariables masterVariables;
    private Execs execs;
    private Integer nu;
    private Integer length;
    private Binning binning;
    private ParameterSet parameterSet;
    private ParameterSet result;

    private Integer nrep = 10000;

    private boolean hasRun;

}
/*
 *    Ecotype Simulation models the sequence diversity within a bacterial
 *    clade as the evolutionary result of net ecotype formation and periodic
 *    selection, yielding a certain number of ecotypes.
 *
 *    Copyright (C) 2015  Jason M. Wood, Montana State University
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 *  Object to estimate the parameter values from the binning results.
 *
 *  @author Jason M. Wood
 *  @copyright GNU General Public License
 */
public class ParameterEstimate {

    public ParameterEstimate (Integer length, Binning binning) {
        // Calculate the list of points that the sigma and omega lines will
        // be fitted to.
        List<Point> points = getPoints (length, binning);
        // Fit the sigma line to the points.
        Integer [] sigmaBounds = fitLinePoints (points, 1);
        Line sigma = new Line (points.subList (
            sigmaBounds[0], sigmaBounds[1]
        ));
        // Fit the omega line to the points.
        Integer [] omegaBounds = fitLinePoints (points, sigmaBounds[1] + 1);
        Line omega = new Line (points.subList (
            omegaBounds[0], omegaBounds[1]
        ));
        // Omega is estimated from the slope of the omega line.
        Double omegaEstimate = -1.0d * omega.m;
        // Sigma is estimated from the slope of the sigma line.
        Double sigmaEstimate = -1.0d * sigma.m;
        // Npop is estimated by calculating the intersection of the omega
        // and sigma lines.
        Long npopEstimate = Math.round (Math.pow (
            2, omega.m * (sigma.b - omega.b) / (omega.m - sigma.m) + omega.b
        ));
        // Store the estimated parameter values.
        estimate = new ParameterSet<Double> (
            omegaEstimate, sigmaEstimate, npopEstimate, 0.0d
        );
    }

    /**
     *  Return the parameter estimate.
     *
     *  @return The parameter estimate.
     */
    public ParameterSet<Double> getEstimate () {
        return estimate;
    }

    /**
     *  Return the parameter estimate as a String.
     *
     *  @return The parameter estimate as a String.
     */
    public String toString () {
        return estimate.toString ();
    }

    /**
     *  Find all of the points that can fit a line.
     *
     *  @return The bounds of the points array that fit a line.
     */
    private Integer [] fitLinePoints (List<Point> points, Integer start) {
        Integer [] bounds = { start, start + 2 };
        // Catch errors before they happen.
        if (bounds[0] > points.size () || bounds[1] > points.size ()) {
            throw new ArrayIndexOutOfBoundsException (
                "Bounds exceed the size of the points list while fitting the line."
            );
        }
        // Calculate the line using the current set of points.
        Line line = new Line (
            points.subList (bounds[0], bounds[1])
        );
        // Slurp up any points that are close to the line.
        for (int i = bounds[1]; i < points.size (); i ++) {
            Double error = squaredError (points.get (i), line);
            if (error > 0.1) break;
            bounds[1] = i;
        }
        return bounds;
    }

    /**
     *  Calculate the squared error of the point compared to the line.
     *
     *  @param points The point.
     *  @param line The line.
     *  @return The squared error of the point compared to the line.
     */
    private Double squaredError (Point point, Line line) {
        // Calculate the distance of the point from the line as the error.
        Double error = Math.abs (-1 * line.m * point.x + point.y - line.b) /
            Math.sqrt (line.m * line.m + 1);
        // Square the error.
        return error * error;
    }

    /**
     *  Calculate the list of points from the binning results.
     *
     *  @param length The length of the sequences.
     *  @param binning The binning results to transform.
     *  @return The transformed binning results as a list of XY points.
     */
    private List<Point> getPoints (Integer length, Binning binning) {
        List<Point> points = new ArrayList<Point> ();
        Integer previous = -1;
        for (BinLevel bin: binning.getBins ()) {
            Double crit = bin.getCrit ();
            Integer level = bin.getLevel ();
            // Don't include binning results == 1.
            if (level == 1) continue;
            // Don't include duplicate levels.
            if (level == previous) continue;
            // Transform the sequence criterion value into the number of SNPs.
            Double x = (1.0d - crit) * length;
            // Transform the number of sequence clusters (bins) into log base 2 scale.
            Double y = Math.log (level) / Math.log (2);
            // Add the XY point to the list.
            points.add (new Point (x, y));
            // Save the level in previous.
            previous = level;
        }
        // Return the points in reverse order.
        Collections.reverse (points);
        return points;
    }

    /**
     * The estimate for the parameter values.
     */
    private ParameterSet<Double> estimate;

    /**
     *  A private class to calculate the best fit line from from a selection
     *  of points.
     */
    private class Line {
        public Line (List<Point> points) {
            Double sumX = 0.0d;
            Double sumY = 0.0d;
            Double sumXY = 0.0d;
            Double sumX2 = 0.0d;
            for (Point point: points) {
                sumX += point.x;
                sumY += point.y;
                sumXY += point.x * point.y;
                sumX2 += point.x * point.x;
            }
            // Calculate the slope and Y intercept of the line.
            int n = points.size ();
            b = (sumY * sumX2 - sumX * sumXY) / (n * sumX2 - sumX * sumX);
            m = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        }
        public Double b;
        public Double m;
    }

    /**
     *  A private class to store a simple XY point value.
     */
    private class Point {
        public Point (Double x, Double y) {
            this.x = x;
            this.y = y;
        }
        public Double x;
        public Double y;
    }

}

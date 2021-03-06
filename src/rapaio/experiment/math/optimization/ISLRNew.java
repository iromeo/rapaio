/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
 *    Copyright 2016 Aurelian Tutuianu
 *    Copyright 2017 Aurelian Tutuianu
 *    Copyright 2018 Aurelian Tutuianu
 *    Copyright 2019 Aurelian Tutuianu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package rapaio.experiment.math.optimization;

import rapaio.data.*;
import rapaio.datasets.*;
import rapaio.graphics.plot.*;
import rapaio.math.linear.*;
import rapaio.math.linear.dense.*;
import rapaio.printer.*;
import rapaio.printer.format.*;
import rapaio.printer.idea.*;
import rapaio.sys.*;
import rapaio.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;
import static rapaio.graphics.Plotter.*;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 10/12/17.
 * <p>
 * Implemented using as reference: "Iterative Reweighted Least Squares  - C. Sidney Burrus"
 * link: file:///home/ati/Dropbox/papers/numerical-anlysis/iterative-reweighted-least-squares-12.pdf
 */
public class ISLRNew {

    /**
     * We minimize ||e||_p, where e = Ax - b
     *
     * @param A     factor matrix mxn
     * @param b     result vector 1xm
     * @param p     the order of norm
     * @param maxIt number of maximum iterations
     * @param tol   tolerance
     */
    public static Pair<RV, VarDouble> basicISLR(RM A, RV b, double p, int maxIt, double tol) {

        if (A.colCount() < 4) {
            maxIt = 10;
        }

        // initial L2 solution

        RV x = QRDecomposition.from(A).solve(b.asMatrix()).mapCol(0);
        VarDouble err = VarDouble.empty().withName("errors");

        for (int it = 0; it < maxIt; it++) {

            // error vector
            RV e = A.dot(x).minus(b);

            // error weights for IRLS
            RV w = SolidRV.from(e.count(), pos -> pow(abs(e.get(pos)), (p - 2) / 2));

            // normalize weight matrix
            RM W = SolidRM.empty(w.count(), w.count());
            double wsum = w.valueStream().sum();
            for (int i = 0; i < w.count(); i++) {
                W.set(i, i, w.get(i) / wsum);
            }

            // apply weights
            RM WA = W.dot(A);

            // weighted L2 solution

            RM A1 = WA.t().dot(WA);
            RV b1 = WA.t().dot(W).dot(b);

            x = QRDecomposition.from(A1).solve(b1.asMatrix()).mapCol(0);

            double ee = x.norm(p);
            err.addDouble(ee);

            // break if the improvement is then tolerance

            if (it > 0 && Math.abs(err.getDouble(err.rowCount() - 2) - ee) < tol) {
                break;
            }
        }

        return Pair.from(x, err);
    }


    /**
     * IRLS1.m to find the optimal solution to Ax=b
     * minimizing the L_p norm ||Ax-b||_p, using IRLS.
     * Newton iterative update of solution, x, for M > N.
     * For 2 < p < infty, use homotopy parameter K = 1.01 to 2
     * For 0 < p < 2, use K = approx 0.7 - 0.9
     *
     * @param A
     * @param b
     * @param p
     * @param K
     * @param iterMax
     * @return
     */
    public static Pair<RV, VarDouble> islrH(RM A, RV b, double p, double K, int iterMax, double tol) {

        if (A.colCount() < 5) {
            iterMax = 10;
        }
        if (A.colCount() < 4) {
            K = 1.5;
        }
        if (A.colCount() < 3) {
            p = 10;
        }


        // initial homotopy value
        double pk = 2;

        // initial L2 solution
        RV x = QRDecomposition.from(A).solve(b.asMatrix()).mapCol(0);

        VarDouble err = VarDouble.empty().withName("errors");

        for (int k = 0; k < iterMax; k++) {
            if (p >= 2) {
                pk = Math.min(p, K * p);
            } else {
                pk = Math.max(p, K * pk);
            }

            // error vector
            RV e = A.dot(x).minus(b);

            // error weights for IRLS
            double pkk = pk;
            RV w = SolidRV.from(e.count(), pos -> pow(abs(e.get(pos)), (pkk - 2) / 2));

            // normalize weight matrix
            RM W = SolidRM.empty(w.count(), w.count());
            double wsum = w.valueStream().sum();
            for (int i = 0; i < w.count(); i++) {
                W.set(i, i, w.get(i) / wsum);
            }

            // apply weights
            RM WA = W.dot(A);

            // weighted L2 solution

            RM A1 = WA.t().dot(WA);
            RV b1 = WA.t().dot(W).dot(b);

            RV x1 = QRDecomposition.from(A1).solve(b1.asMatrix()).mapCol(0);

            // Newton's parameter
            double q = 1.0 / (pk - 1);

            double nn;
            if (p > 2) {
                // partial update for p>2
                x = x1.dot(q).plus(x.dot(1 - q));
                nn = p;
            } else {
                // no partial update for p<=2
                x = x1;
                nn = 2;
            }
            err.addDouble(e.norm(nn));

            // break if the improvement is then tolerance

            if (k > 0 && Math.abs(err.getDouble(err.rowCount() - 2) - err.getDouble(err.rowCount() - 1)) < tol) {
                break;
            }
        }
        return Pair.from(x, err);
    }

    public static void main(String[] args) throws IOException {

        Printer p = new IdeaPrinter();

        Frame df = Datasets.loadISLAdvertising().removeVars(VRange.of(0));

        VarDouble intercept = VarDouble.fill(df.rowCount(), 1);
        Frame dfa = SolidFrame.byVars(intercept).bindVars(df.mapVars(VRange.of(0, 1, 2)));
        RM A = SolidRM.copy(dfa);
        RV b = SolidRM.copy(df.mapVars(VRange.of(3))).mapCol(0);


        double[] pp = new double[]{2.5, 5, 10, 100, 1, 1.5};
        double[] k = new double[]{1.01, 1.01, 1.01, 1.01, 0.9, 0.9};

        List<Pair<RV, VarDouble>> numVars = new ArrayList<>();

        Plot plot = plot();
        for (int i = 0; i < pp.length; i++) {
            double prob = pp[i];
            double h = k[i];
            Pair<RV, VarDouble> pair = basicISLR(A, b, prob, 10_000, 1e-20);
            plot.lines(pair._2);

            WS.println("Solution 1 for p=" + Format.floatFlex(prob));
            WS.println("Min :" + Format.floatFlex(A.dot(pair._1).minus(b).norm(prob)));
            p.printSummary(pair._1);
            WS.println();


            Pair<RV, VarDouble> pair2 = islrH(A, b, prob, h, 10_000, 1e-20);
            plot.lines(pair2._2, color(1));

            WS.println("Solution 2 for p=" + Format.floatFlex(prob));
            WS.println("Min :" + Format.floatFlex(A.dot(pair2._1).minus(b).norm(prob)));
            p.printSummary(pair2._1);
            WS.println();

        }
        WS.draw(plot);

//        LinearRegression lm =
//                LinearRegression.newLm().withInputFilters(FFAddIntercept.filter());
//
//        lm.predict(df, "Sales");
//        lm.predict(df, true).printSummary();
    }
}

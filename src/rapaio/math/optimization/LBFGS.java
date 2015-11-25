/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 *    Copyright 2013 Aurelian Tutuianu
 *    Copyright 2014 Aurelian Tutuianu
 *    Copyright 2015 Aurelian Tutuianu
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

package rapaio.math.optimization;

import rapaio.math.linear.Linear;
import rapaio.math.linear.RM;
import rapaio.math.linear.RV;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 11/24/15.
 */
@Deprecated
public class LBFGS {

    private final Gradient gradient;
    private final Updater updater;

    private int numCorrections = 10;
    private double convergenceTol = 1E-4;
    private int maxNumIterations = 100;
    private double regParam = 0.0;
    private double lambda;
    private int C; // number of classes
    public LBFGS(Gradient gradient, Updater updater) {
        this.gradient = gradient;
        this.updater = updater;
    }

    public RV optimize(RM x, RV y, double lambda, int C) {

        this.lambda = lambda;
        this.C = C;

        RV w = Linear.newRVEmpty(x.colCount());
        RM wtx = Linear.newRMEmpty(x.rowCount(), C);



        return null;
    }


    public void computeGradient(RM x, RV y, RV w, RM wtx) {
        RV g = w.copy();
        g.dot(-lambda);
        for (int i = 0; i < x.rowCount(); i++) {

        }
    }
}
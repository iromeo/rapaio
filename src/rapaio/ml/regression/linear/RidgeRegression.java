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
package rapaio.ml.regression.linear;

import rapaio.core.stat.Mean;
import rapaio.core.stat.Variance;
import rapaio.data.*;
import rapaio.data.filter.FFilter;
import rapaio.math.linear.RM;
import rapaio.math.linear.dense.QRDecomposition;
import rapaio.math.linear.dense.SolidRM;
import rapaio.ml.common.Capabilities;
import rapaio.ml.regression.Regression;
import rapaio.sys.WS;

import java.util.Arrays;
import java.util.HashMap;

/**
 * @author VHG6KOR
 */
public class RidgeRegression extends AbstractLinearRegression {

    private static final long serialVersionUID = -6014222985456365210L;

    /**
     * Builds a new ridge regression model.
     *
     * @param lambda regularization parameter: 0 means no regularization, infinity means all coefficients shrink to 0
     * @return new ridge regression model
     */
    public static RidgeRegression newRidgeLm(double lambda) {
        return new RidgeRegression()
                .withLambda(lambda)
                .withIntercept(true)
                .withCentering(true)
                .withScaling(true);
    }

    /*
    Regularization strength; must be a positive float. Regularization improves the conditioning
    of the problem and reduces the variance of the estimates.
    Larger values specify stronger regularization
     */
    private double lambda = 0.0;

    protected HashMap<String, Double> inputMean = new HashMap<>();
    protected HashMap<String, Double> inputSd = new HashMap<>();
    protected HashMap<String, Double> targetMean = new HashMap<>();

    @Override
    public Regression newInstance() {
        return new RidgeRegression()
                .withIntercept(intercept)
                .withLambda(lambda)
                .withCentering(centering)
                .withScaling(scaling);
    }

    @Override
    public String name() {
        return "RidgeRegression";
    }

    @Override
    public String fullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(name());
        sb.append("(lambda=").append(WS.formatFlex(lambda));
        sb.append(")");
        return sb.toString();
    }

    public RidgeRegression withLambda(double lambda) {
        this.lambda = lambda;
        return this;
    }

    @Override
    public RidgeRegression withIntercept(boolean intercept) {
        return (RidgeRegression) super.withIntercept(intercept);
    }

    @Override
    public RidgeRegression withCentering(boolean centering) {
        return (RidgeRegression) super.withCentering(centering);
    }

    @Override
    public RidgeRegression withScaling(boolean scaling) {
        return (RidgeRegression) super.withScaling(scaling);
    }

    @Override
    public RidgeRegression withInputFilters(FFilter... filters) {
        return (RidgeRegression) super.withInputFilters(filters);
    }

    @Override
    public Capabilities capabilities() {
        return new Capabilities()
                .withInputTypes(VarType.NUMERIC, VarType.INDEX, VarType.BINARY, VarType.ORDINAL)
                .withTargetTypes(VarType.NUMERIC)
                .withInputCount(1, 1_000_000)
                .withTargetCount(1, 1_000_000)
                .withAllowMissingInputValues(false)
                .withAllowMissingTargetValues(false);
    }

    @Override
    public RidgeRegression train(Frame df, String... targetVarNames) {
        return (RidgeRegression) super.train(df, targetVarNames);
    }

    @Override
    public RidgeRegression train(Frame df, Var weights, String... targetVarNames) {
        return (RidgeRegression) super.train(df, weights, targetVarNames);
    }

    @Override
    protected TrainSetup prepareTraining(TrainSetup trainSetup) {
        if (!intercept) {
            return super.prepareTraining(trainSetup);
        }
        NumVar inter = NumVar.fill(trainSetup.df.rowCount(), 1.0).withName(INTERCEPT);
        Frame prepared = BoundFrame.byVars(SolidFrame.byVars(inter), trainSetup.df);
        return super.prepareTraining(TrainSetup.valueOf(prepared, trainSetup.w, trainSetup.targetVars));
    }

    @Override
    protected boolean coreTrain(Frame df, Var weights) {
        if (lambda < 0) {
            throw new IllegalArgumentException("lambda - regularization strength cannot be negative");
        }

        for (String inputName : inputNames) {
            if (INTERCEPT.equals(inputName)) {
                inputMean.put(INTERCEPT, 0.0);
                inputSd.put(INTERCEPT, 1.0);
                continue;
            }
            inputMean.put(inputName, centering ? Mean.from(df.rvar(inputName)).value() : 0);
            inputSd.put(inputName, scaling ? Variance.from(df.rvar(inputName)).sdValue() : 1);
        }
        for (String targetName : targetNames) {
            targetMean.put(targetName, centering ? Mean.from(df.rvar(targetName)).value() : 0);
        }

        String[] selNames = Arrays.copyOfRange(inputNames, intercept ? 1 : 0, inputNames.length);
        RM X = SolidRM.empty(df.rowCount() + selNames.length, selNames.length);
        RM Y = SolidRM.empty(df.rowCount() + selNames.length, targetNames.length);

        double sqrt = Math.sqrt(this.lambda);
        for (int i = 0; i < selNames.length; i++) {
            int varIndex = df.varIndex(selNames[i]);
            for (int j = 0; j < df.rowCount(); j++) {
                X.set(j, i, (df.value(j, varIndex) - inputMean.get(selNames[i])) / (inputSd.get(selNames[i])));
            }
            X.set(i + df.rowCount(), i, sqrt);
        }
        for (int i = 0; i < targetNames.length; i++) {
            int varIndex = df.varIndex(targetNames[i]);
            for (int j = 0; j < df.rowCount(); j++) {
                Y.set(j, i, (df.value(j, varIndex) - targetMean.get(targetNames[i])));
            }
        }

        RM rawBeta = QRDecomposition.from(X).solve(Y);
        int offset = intercept ? 1 : 0;
        beta = SolidRM.empty(rawBeta.rowCount() + offset, rawBeta.colCount());
        for (int i = 0; i < rawBeta.rowCount(); i++) {
            for (int j = 0; j < rawBeta.colCount(); j++) {
                beta.set(i + offset, j, rawBeta.get(i, j) / inputSd.get(inputNames[i + offset]));
            }
        }
        if (intercept) {
            for (int i = 0; i < beta.colCount(); i++) {
                double ym = targetMean.get(targetNames[i]);
                for (int j = 0; j < rawBeta.rowCount(); j++) {
                    ym -= beta.get(j + 1, i) * inputMean.get(inputNames[j + offset]);
                }
                beta.set(0, i, ym);
            }
        }
        return true;
    }

    @Override
    protected FitSetup prepareFit(FitSetup fitSetup) {
        if (!intercept) {
            return super.prepareFit(fitSetup);
        }
        NumVar inter = NumVar.fill(fitSetup.df.rowCount(), 1.0).withName(INTERCEPT);
        Frame prepared = BoundFrame.byVars(SolidFrame.byVars(inter), fitSetup.df);
        return super.prepareFit(FitSetup.valueOf(prepared, fitSetup.withResiduals));
    }
}
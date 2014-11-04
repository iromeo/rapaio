/*
* Apache License
* Version 2.0, January 2004
* http://www.apache.org/licenses/
*
*    Copyright 2013 Aurelian Tutuianu
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
*/

package rapaio.ml.classifier.tree;

import rapaio.core.sample.DiscreteSampling;
import rapaio.core.stat.ConfusionMatrix;
import rapaio.data.*;
import rapaio.ml.classifier.AbstractClassifier;
import rapaio.ml.classifier.CPrediction;
import rapaio.ml.classifier.Classifier;
import rapaio.ml.classifier.RunningClassifier;
import rapaio.ml.classifier.tools.DensityVector;
import rapaio.ml.classifier.tree.ctree.CTree;
import rapaio.ml.classifier.varselect.RandomVarSelector;
import rapaio.ml.classifier.varselect.VarSelector;
import rapaio.util.Pair;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * @author <a href="mailto:padreati@yahoo.com>Aurelian Tutuianu</a>
 */
public class CForest extends AbstractClassifier implements RunningClassifier {

    int runs = 0;
    boolean oobCompute = false;
    Classifier c = CTree.newC45();
    double sampling = 1;
    BaggingMethod baggingMethod = BaggingMethods.DISTRIBUTION_SUM;
    //
    double totalOobInstances = 0;
    double totalOobError = 0;
    double oobError = Double.NaN;
    List<Classifier> predictors = new ArrayList<>();

    public static CForest buildRandomForest(int runs, int mcols, double sampling) {
        return new CForest()
                .withClassifier(CTree.newCART())
                .withBaggingMethod(BaggingMethods.DISTRIBUTION_SUM)
                .withRuns(runs)
                .withVarSelector(new RandomVarSelector(mcols))
                .withSampling(sampling);
    }

    public static CForest buildRandomForest(int runs, int mcols, double sampling, Classifier c) {
        return new CForest()
                .withClassifier(c)
                .withBaggingMethod(BaggingMethods.DISTRIBUTION_SUM)
                .withRuns(runs)
                .withVarSelector(new RandomVarSelector(mcols))
                .withSampling(sampling);
    }


    @Override
    public Classifier newInstance() {
        return new CForest()
                .withVarSelector(varSelector)
                .withRuns(runs)
                .withBaggingMethod(baggingMethod)
                .withClassifier(c);
    }

    @Override
    public String name() {
        return "CForest";
    }

    @Override
    public String fullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(name()).append("(");
        sb.append("baggingMethod=").append(baggingMethod.name()).append(",");
        sb.append("colSelector=").append(varSelector.name()).append(",");
        sb.append("runs=").append(runs).append(",");
        sb.append("c=").append(c.fullName());
        sb.append(")");
        return sb.toString();
    }

    @Override
    public CForest withVarSelector(VarSelector varSelector) {
        this.varSelector = varSelector;
        return this;
    }

    public CForest withRuns(int runs) {
        this.runs = runs;
        return this;
    }


    public CForest withOobError(boolean oobCompute) {
        this.oobCompute = oobCompute;
        return this;
    }

    public boolean getOobCompute() {
        return oobCompute;
    }

    public double getOobError() {
        return oobError;
    }

    public CForest withSampling(double sampling) {
        this.sampling = sampling;
        return this;
    }

    public double getSampling() {
        return sampling;
    }

    public BaggingMethod getBaggingMethod() {
        return baggingMethod;
    }

    public CForest withBaggingMethod(BaggingMethod baggingMethod) {
        this.baggingMethod = baggingMethod;
        return this;
    }

    public Classifier getClassifier() {
        return c;
    }

    public CForest withClassifier(Classifier c) {
        this.c = c;
        return this;
    }

    public Pair<List<Frame>, List<Numeric>> produceSamples(Frame df, Numeric weights) {
        List<Frame> frames = new ArrayList<>();
        List<Numeric> weightsList = new ArrayList<>();

        if (sampling <= 0) {
            // no sampling
            frames.add(df.stream().toMappedFrame());
            frames.add(MappedFrame.newByRow(df));

            weightsList.add(weights);
            weightsList.add(Numeric.newEmpty());

            return new Pair<>(frames, weightsList);
        }

        Mapping train = Mapping.newEmpty();
        Mapping oob = Mapping.newEmpty();

        weightsList.add(Numeric.newEmpty());
        weightsList.add(Numeric.newEmpty());

        int[] sample = new DiscreteSampling().sampleWR((int) (df.rowCount() * sampling), df.rowCount());
        HashSet<Integer> rows = new HashSet<>();
        for (int row : sample) {
            rows.add(row);
            train.add(row);
            weightsList.get(0).addValue(weights.value(row));
        }
        for (int i = 0; i < df.rowCount(); i++) {
            if (rows.contains(i)) continue;
            oob.add(i);
            weightsList.get(1).addValue(weights.value(i));
        }

        frames.add(MappedFrame.newByRow(df, train));
        frames.add(MappedFrame.newByRow(df, oob));

        return new Pair<>(frames, weightsList);
    }

    @Override
    public void learn(Frame df, Numeric weights, String... targetVarNames) {

        List<String> targetVarsList = new VarRange(targetVarNames).parseVarNames(df);
        if (targetVarsList.size() != 1) {
            throw new IllegalArgumentException("Forest classifiers can learn only one target variable");
        }
        this.targetVars = targetVarsList.toArray(new String[targetVarsList.size()]);
        this.dict = new HashMap<>();
        this.dict.put(firstTargetVar(), df.var(firstTargetVar()).dictionary());

        predictors.clear();

        totalOobInstances = 0;
        totalOobError = 0;

        for (int i = 0; i < runs; i++) {
            buildWeakPredictor(df, weights);
        }

        if (oobCompute) {
            oobError = totalOobError / totalOobInstances;
        }
    }

    @Override
    public void learnFurther(Frame df, Numeric weights, String targetVars, int additionalRuns) {

        if (this.targetVars != null && dict != null) {
            this.runs += additionalRuns;
        } else {
            this.runs = additionalRuns;
            learn(df, targetVars);
            return;
        }

        for (int i = predictors.size(); i < runs; i++) {
            buildWeakPredictor(df, weights);
        }
    }

    private void buildWeakPredictor(Frame df, Numeric weights) {
        Classifier weak = c.newInstance();
        weak.withVarSelector(varSelector);

        Pair<List<Frame>, List<Numeric>> samples = produceSamples(df, weights);
        Frame train = samples.first.get(0);
        Frame oob = samples.first.get(1);

        weak.learn(train, samples.second.get(0), firstTargetVar());
        if (oobCompute) {
            CPrediction cp = weak.predict(oob);
            totalOobInstances += oob.rowCount();
            totalOobError += 1 - new ConfusionMatrix(oob.var(firstTargetVar()), cp.firstClasses()).accuracy();
        }
        predictors.add(weak);
    }

    @Override
    public CPrediction predict(Frame df, boolean withClasses, boolean withDensities) {
        CPrediction cp = CPrediction.newEmpty(df.rowCount(), true, true);
        cp.addTarget(firstTargetVar(), firstDictionary());

        List<Frame> treeDensities = new ArrayList<>();
        predictors.forEach(p -> {
            CPrediction cpTree = p.predict(df);
            treeDensities.add(cpTree.firstDensity());
        });

        baggingMethod.computeDensity(firstDictionary(), treeDensities, cp.firstClasses(), cp.firstDensity());

        return cp;
    }

    @Override
    public void buildSummary(StringBuilder sb) {
        throw new NotImplementedException();
    }

    // components

    public static interface BaggingMethod extends Serializable {

        String name();

        void computeDensity(String[] dictionary, List<Frame> treeDensities, Nominal classes, Frame densities);
    }

    public static enum BaggingMethods implements BaggingMethod {

        VOTING {
            @Override
            public void computeDensity(String[] dictionary, List<Frame> treeDensities, Nominal classes, Frame densities) {
                treeDensities.forEach(d -> {
                    for (int i = 0; i < d.rowCount(); i++) {
                        DensityVector dv = new DensityVector(dictionary);
                        for (int j = 0; j < dictionary.length; j++) {
                            dv.update(j, d.value(i, j));
                        }
                        int best = dv.findBestIndex();
                        densities.setValue(i, best, densities.value(i, best) + 1);
                    }
                });
                for (int i = 0; i < classes.rowCount(); i++) {
                    DensityVector dv = new DensityVector(dictionary);
                    for (int j = 0; j < dictionary.length; j++) {
                        dv.update(j, densities.value(i, j));
                    }
                    classes.setValue(i, dv.findBestIndex());
                }
            }
        },
        DISTRIBUTION_SUM {
            @Override
            public void computeDensity(String[] dictionary, List<Frame> treeDensities, Nominal classes, Frame densities) {
                treeDensities.forEach(d -> {
                    for (int i = 0; i < d.rowCount(); i++) {
                        for (int j = 0; j < dictionary.length; j++) {
                            densities.setValue(i, j, densities.value(i, j) + d.value(i, j));
                        }
                    }
                });
                for (int i = 0; i < classes.rowCount(); i++) {
                    DensityVector dv = new DensityVector(dictionary);
                    for (int j = 0; j < dictionary.length; j++) {
                        dv.update(j, densities.value(i, j));
                    }
                    classes.setValue(i, dv.findBestIndex());
                }
            }
        }
    }
}
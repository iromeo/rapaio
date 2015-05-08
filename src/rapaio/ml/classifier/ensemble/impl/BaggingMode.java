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

package rapaio.ml.classifier.ensemble.impl;

import rapaio.data.Frame;
import rapaio.data.Nominal;
import rapaio.ml.classifier.tools.DensityVector;

import java.io.Serializable;
import java.util.List;

/**
 * Describes and implements how a class is obtained from density for ensemble methods.
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 4/16/15.
 */
@Deprecated
public enum BaggingMode implements Serializable {

    VOTING {
        @Override
        public void computeDensity(String[] dictionary, List<Frame> treeDensities, Nominal classes, Frame densities) {
            treeDensities.forEach(d -> {
                for (int i = 0; i < d.rowCount(); i++) {
                    DensityVector dv = new DensityVector(dictionary);
                    for (int j = 1; j < dictionary.length; j++) {
                        dv.update(j, d.value(i, j));
                    }
                    int best = dv.findBestIndex();
                    densities.setValue(i, best, densities.value(i, best) + 1);
                }
            });
            for (int i = 0; i < classes.rowCount(); i++) {
                DensityVector dv = new DensityVector(dictionary);
                for (int j = 1; j < dictionary.length; j++) {
                    dv.update(j, densities.value(i, j));
                }
                dv.normalize(false);
                for (int j = 1; j < dictionary.length; j++) {
                    densities.setValue(i, j, dv.get(j));
                }
                classes.setValue(i, dv.findBestIndex());
            }
        }
    },
    DISTRIBUTION {
        @Override
        public void computeDensity(String[] dictionary, List<Frame> treeDensities, Nominal classes, Frame densities) {
            for (int i = 0; i < densities.rowCount(); i++) {
                for (int j = 0; j < densities.varCount(); j++) {
                    densities.setValue(i, j, 0);
                }
            }
            treeDensities.forEach(d -> {
                for (int i = 0; i < densities.rowCount(); i++) {
                    double t = 0.0;
                    for (int j = 0; j < densities.varCount(); j++) {
                        t += d.value(i, j);
                    }
                    for (int j = 0; j < densities.varCount(); j++) {
                        densities.setValue(i, j, densities.value(i, j) + d.value(i, j) / t);
                    }
                }
            });
            for (int i = 0; i < classes.rowCount(); i++) {
                DensityVector dv = new DensityVector(dictionary);
                for (int j = 0; j < dictionary.length; j++) {
                    dv.update(j, densities.value(i, j));
                }
                dv.normalize(false);
                for (int j = 0; j < dictionary.length; j++) {
                    densities.setValue(i, j, dv.get(j));
                }
                classes.setValue(i, dv.findBestIndex());
            }
        }
    };

    abstract void computeDensity(String[] dictionary, List<Frame> treeDensities, Nominal classes, Frame densities);
}
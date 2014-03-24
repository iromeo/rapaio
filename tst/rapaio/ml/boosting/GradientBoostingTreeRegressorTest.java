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

package rapaio.ml.boosting;

import org.junit.Test;
import rapaio.core.stat.MAE;
import rapaio.data.Frame;
import rapaio.data.Numeric;
import rapaio.data.VectorType;
import rapaio.datasets.Datasets;
import rapaio.graphics.Plot;
import rapaio.graphics.plot.Lines;
import rapaio.io.Csv;
import rapaio.ml.boost.GradientBoostingTreeRegressor;
import rapaio.ml.boost.gbt.L2BoostingLossFunction;
import rapaio.ml.simple.L2ConstantRegressor;
import rapaio.ml.tree.DecisionStumpRegressor;
import rapaio.printer.LocalPrinter;
import rapaio.workspace.Summary;
import rapaio.workspace.Workspace;

import java.io.IOException;

/**
 * User: Aurelian Tutuianu <padreati@yahoo.com>
 */
public class GradientBoostingTreeRegressorTest {

    @Test
    public void testProstate() throws IOException {

        Workspace.setPrinter(new LocalPrinter());
//        Frame df = Datasets.loadProstateCancer();
//        Summary.summary(df);
//        df = BaseFilters.removeCols(df, "train");
//        String targetColName = "lpsa";
        int ROUNDS = 100;
        int RUN = 1;
        Frame df = new Csv().withDefaultType(VectorType.NUMERIC)
                .withNumericFields("spam")
                .read(Datasets.class, "spam-base.csv");

        Summary.summary(df);
        String targetColName = "spam";

        GradientBoostingTreeRegressor gbt = new GradientBoostingTreeRegressor()
                .setBootstrap(0.8)
                .setShrinkage(0.6)
                .setLossFunction(new L2BoostingLossFunction())
                .setRegressor(new DecisionStumpRegressor())
                .setInitialRegressor(new L2ConstantRegressor())
                .setRounds(0);

        gbt.learn(df, targetColName);

        Numeric index = new Numeric();
        Numeric mae = new Numeric();
        gbt.predict(df);
        index.addValue(1);
        mae.addValue(new MAE(gbt.getFitValues(), df.col(targetColName)).getValue());
        for (int i = 1; i <= ROUNDS; i++) {
            gbt.learnFurther(RUN);
            gbt.predict(df);

            index.addValue(i);
            mae.addValue(new MAE(gbt.getFitValues(), df.col(targetColName)).getValue());

            Workspace.draw(new Plot().add(new Lines(index, mae)));
        }
    }
}

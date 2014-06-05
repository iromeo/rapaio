
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

import junit.framework.Assert;
import org.junit.Test;
import rapaio.data.Frame;
import rapaio.data.filters.BaseFilters;
import rapaio.datasets.Datasets;
import rapaio.ml.classifier.tools.DensityTable;
import rapaio.ws.Summary;

import java.io.IOException;

/**
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
public class ID3ClassifierTest {

    @Test
    public void testBasicID3Entropy() throws IOException {
        Frame df = Datasets.loadPlay();
        df = BaseFilters.retainNominal(df);
        final String className = "class";

        TreeClassifier id3 = TreeClassifier.buildID3();
        id3.learn(df, className);
        id3.predict(df);

        DensityTable dtWindy = new DensityTable(df.col("windy"), df.col("class"));
        DensityTable dtOutlook = new DensityTable(df.col("outlook"), df.col("class"));
        String splitCol = (dtWindy.getSplitEntropy() < dtOutlook.getSplitEntropy()) ? "windy" : "outlook";
        id3.summary();
        Assert.assertTrue(id3.root.children.get(0).groupName.startsWith(splitCol));

        Summary.summary(id3);
    }

    @Test
    public void testBasicID3InfoGain() throws IOException {
        Frame df = Datasets.loadPlay();
        df = BaseFilters.retainNominal(df);
        final String className = "class";

        TreeClassifier id3 = new TreeClassifier()
                .withNominalMethod(TreeClassifier.NominalMethods.FULL)
                .withNumericMethod(TreeClassifier.NumericMethods.IGNORE)
                .withSplitter(TreeClassifier.Splitters.REMAINS_IGNORED)
                .withFunction(TreeClassifier.Functions.INFO_GAIN);
        id3.learn(df, className);
        id3.predict(df);
        id3.summary();

        DensityTable dtWindy = new DensityTable(df.col("windy"), df.col("class"));
        DensityTable dtOutlook = new DensityTable(df.col("outlook"), df.col("class"));
        String splitCol = (dtWindy.getInfoGain() > dtOutlook.getInfoGain()) ? "windy" : "outlook";
        Assert.assertTrue(id3.root.children.get(0).groupName.startsWith(splitCol));

        Summary.summary(id3);
    }
}
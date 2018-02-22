package rapaio.ml.regression.linear;

import com.sun.org.apache.xpath.internal.operations.Minus;
import org.junit.Assert;
import org.junit.Test;
import rapaio.core.stat.Maximum;
import rapaio.core.stat.Minimum;
import rapaio.data.Frame;
import rapaio.data.NumVar;
import rapaio.data.Var;
import rapaio.datasets.Datasets;
import rapaio.graphics.Plotter;
import rapaio.ml.regression.RFit;
import rapaio.printer.IdeaPrinter;
import rapaio.sys.WS;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertEquals;
import static rapaio.graphics.Plotter.*;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 2/1/18.
 */
public class RidgeRegressionTest {

    private static final double TOL = 1e-20;

//    @Test
    public void basicTest() throws IOException, URISyntaxException {

        // test the results for ridge are the same as those for linear regression when lamba equals 0

        RidgeRegression rlm = RidgeRegression.newRidgeLm(0).withCentering(false).withScaling(false);
        LinearRegression lm = LinearRegression.newLm();

        Frame df = Datasets.loadISLAdvertising().removeVars("ID");
        df.printSummary();

        LinearRFit lmFit = lm.train(df, "Sales").fit(df, true);
        lmFit.printSummary();
        LinearRFit ridgeFit = rlm.train(df, "Sales").fit(df, true);
        ridgeFit.printSummary();

        for (int i = 0; i < 3; i++) {
            assertEquals(lmFit.beta_hat.get(i, 0), ridgeFit.beta_hat.get(i, 0), TOL);
        }
    }

    @Test
    public void scalingTest() throws IOException {


        Frame df = Datasets.loadISLAdvertising().removeVars("ID");
        df.printSummary();

        RidgeRegression.newRidgeLm(0).withIntercept(false).withCentering(true).withScaling(true)
                .train(df, "Sales").fit(df, true).printSummary();
        LinearRegression.newLm().withIntercept(true).withCentering(true).withScaling(true)
                .train(df, "Sales").fit(df, true).printSummary();
    }

    @Test
    @Deprecated
    public void ridgeCoefficients() throws IOException {

        NumVar lambda = NumVar.seq(0, 10, 0.5);

        Frame df = Datasets.loadISLAdvertising().removeVars("ID");

        for (int i = 0; i < lambda.rowCount(); i++) {
            RidgeRegression rr = RidgeRegression.newRidgeLm(lambda.value(i));
            LinearRFit fit = rr.train(df, "Sales").fit(df, true);

            fit.printSummary();
        }
    }
}
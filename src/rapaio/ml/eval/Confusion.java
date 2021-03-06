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

package rapaio.ml.eval;

import rapaio.data.*;
import rapaio.printer.*;
import rapaio.printer.format.*;

import java.util.List;
import java.util.stream.IntStream;

import static rapaio.printer.format.Format.*;

/**
 * Confusion matrix utility.
 * <p>
 * User: <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
public class Confusion implements DefaultPrintable {

    public static Confusion from(Var actual, Var predict) {
        return new Confusion(actual, predict);
    }

    public static Confusion from(Var actual, Var predict, boolean percents) {
        return new Confusion(actual, predict, percents);
    }

    private final Var actual;
    private final Var predict;
    private final List<String> factors;
    private final int[][] cmf;
    private final boolean binary;
    private final boolean percents;
    private double acc;
    private double mcc;
    private double f1;
    private double g;
    private double precision;
    private double recall;
    private double completeCases = 0;
    private double acceptedCases = 0;
    private double errorCases = 0;

    public Confusion(Var actual, Var predict) {
        this(actual, predict, false);
    }

    public Confusion(Var actual, Var predict, boolean percents) {
        validate(actual, predict);
        this.actual = actual;
        this.predict = predict;
        this.factors = actual.levels();
        this.cmf = new int[factors.size() - 1][factors.size() - 1];
        this.percents = percents;
        this.binary = actual.levels().size() == 3;
        compute();
    }

    private void validate(Var actual, Var predict) {
        if (!actual.type().isNominal()) {
            throw new IllegalArgumentException("actual values var must be nominal");
        }
        if (!predict.type().isNominal()) {
            throw new IllegalArgumentException("predict values var must be nominal");
        }
        if (actual.levels().size() != predict.levels().size()) {
            throw new IllegalArgumentException("actual and predict does not have the same nominal levels");
        }
        for (int i = 0; i < actual.levels().size(); i++) {
            if (!actual.levels().get(i).equals(predict.levels().get(i))) {
                throw new IllegalArgumentException(
                        String.format("not the same nominal levels (actual:%s, predict:%s)",
                                String.join(",", actual.levels()),
                                String.join(",", predict.levels())));
            }
        }
    }

    private void compute() {
        for (int i = 0; i < actual.rowCount(); i++) {
            if (actual.getInt(i) != 0 && predict.getInt(i) != 0) {
                completeCases++;
                cmf[actual.getInt(i) - 1][predict.getInt(i) - 1]++;
            }
        }
        acc = IntStream.range(0, cmf.length).mapToDouble(i -> cmf[i][i]).sum();
        acceptedCases = acc;
        errorCases = completeCases - acceptedCases;

        if (completeCases == 0) {
            acc = 0;
        } else {
            acc = acc / completeCases;
        }

        if (binary) {
            double tp = cmf[0][0];
            double tn = cmf[1][1];
            double fp = cmf[1][0];
            double fn = cmf[0][1];

            mcc = (tp * tn - fp * fn) / Math.sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn));
            f1 = 2 * tp / (2 * tp + fp + fn);
            precision = tp / (tp + fp);
            recall = tp / (tp + fn);
            g = Math.sqrt(precision * recall);
        }
    }

    @Override
    public String summary() {
        StringBuilder sb = new StringBuilder();
        addConfusionMatrix(sb);
        addDetails(sb);
        return sb.toString();
    }

    private void addDetails(StringBuilder sb) {
        sb.append(String.format("\nComplete cases %d from %d\n", (int) Math.rint(completeCases), actual.rowCount()));
        sb.append(String.format("Acc: %s         (Accuracy )\n", floatFlex(acc)));
        if (binary) {
            sb.append(String.format("F1:  %s         (F1 score / F-measure)\n", floatFlex(f1)));
            sb.append(String.format("MCC: %s         (Matthew correlation coefficient)\n", floatFlex(mcc)));
            sb.append(String.format("Pre: %s         (Precision)\n", floatFlex(precision)));
            sb.append(String.format("Rec: %s         (Recall)\n", floatFlex(recall)));
            sb.append(String.format("G:   %s         (G-measure)\n", floatFlex(g)));
        }
    }

    private void addConfusionMatrix(StringBuilder sb) {
        sb.append("> Confusion\n");

        sb.append("\n");
        TextTable tt = TextTable.empty(factors.size() + 3, factors.size() + 3);

        tt.textCenter(0, 0, "Ac\\Pr");

        for (int i = 0; i < factors.size() - 1; i++) {
            tt.textRight(i + 2, 0, factors.get(i + 1));
            tt.textCenter(i + 2, 1, "|");
            tt.textCenter(i + 2, factors.size() + 1, "|");
            tt.textRight(0, i + 2, factors.get(i + 1));
            tt.textRight(1, i + 2, line(factors.get(i + 1).length()));
            tt.textRight(factors.size() + 1, i + 2, line(factors.get(i + 1).length()));
        }
        tt.textRight(factors.size() + 2, 0, "total");
        tt.textRight(0, factors.size() + 2, "total");

        tt.textCenter(1, 0, line("Ac\\Pr".length()));
        tt.textCenter(factors.size() + 1, 0, line("Ac\\Pr".length()));
        tt.textCenter(1, factors.size() + 2, line("Ac\\Pr".length()));
        tt.textCenter(factors.size() + 1, factors.size() + 2, line("Ac\\Pr".length()));

        tt.textCenter(0, 1, "|");
        tt.textCenter(1, 1, "|");
        tt.textCenter(factors.size() + 1, 1, "|");
        tt.textCenter(factors.size() + 2, 1, "|");

        tt.textCenter(0, factors.size() + 1, "|");
        tt.textCenter(1, factors.size() + 1, "|");
        tt.textCenter(factors.size() + 1, factors.size() + 1, "|");
        tt.textCenter(factors.size() + 2, factors.size() + 1, "|");

        int[] rowTotals = new int[factors.size() - 1];
        int[] colTotals = new int[factors.size() - 1];
        int grandTotal = 0;

        for (int i = 0; i < factors.size() - 1; i++) {
            for (int j = 0; j < factors.size() - 1; j++) {
                tt.textRight(i + 2, j + 2, ((i == j) ? ">" : " ") + cmf[i][j]);
                grandTotal += cmf[i][j];
                rowTotals[i] += cmf[i][j];
                colTotals[j] += cmf[i][j];
            }
        }
        for (int i = 0; i < factors.size() - 1; i++) {
            tt.textRight(factors.size() + 2, i + 2, String.valueOf(colTotals[i]));
            tt.textRight(i + 2, factors.size() + 2, String.valueOf(rowTotals[i]));
        }
        tt.textRight(factors.size() + 2, factors.size() + 2, String.valueOf(grandTotal));
        sb.append(tt.getDefaultText());

        if (percents && completeCases > 0.) {

            tt = TextTable.empty(factors.size() + 3, factors.size() + 3);

            tt.textCenter(0, 0, "Ac\\Pr");

            for (int i = 0; i < factors.size() - 1; i++) {
                tt.textRight(i + 2, 0, factors.get(i + 1));
                tt.textCenter(i + 2, 1, "|");
                tt.textCenter(i + 2, factors.size() + 1, "|");
                tt.textRight(0, i + 2, factors.get(i + 1));
                tt.textRight(1, i + 2, line(factors.get(i + 1).length()));
                tt.textRight(factors.size() + 1, i + 2, line(factors.get(i + 1).length()));
            }
            tt.textRight(factors.size() + 2, 0, "total");
            tt.textRight(0, factors.size() + 2, "total");

            tt.textCenter(1, 0, line("Ac\\Pr".length()));
            tt.textCenter(factors.size() + 1, 0, line("Ac\\Pr".length()));
            tt.textCenter(1, factors.size() + 2, line("Ac\\Pr".length()));
            tt.textCenter(factors.size() + 1, factors.size() + 2, line("Ac\\Pr".length()));

            tt.textCenter(0, 1, "|");
            tt.textCenter(1, 1, "|");
            tt.textCenter(factors.size() + 1, 1, "|");
            tt.textCenter(factors.size() + 2, 1, "|");

            tt.textCenter(0, factors.size() + 1, "|");
            tt.textCenter(1, factors.size() + 1, "|");
            tt.textCenter(factors.size() + 1, factors.size() + 1, "|");
            tt.textCenter(factors.size() + 2, factors.size() + 1, "|");

            for (int i = 0; i < factors.size() - 1; i++) {
                for (int j = 0; j < factors.size() - 1; j++) {
                    tt.textRight(i + 2, j + 2, ((i == j) ? ">" : " ") + Format.floatShort(cmf[i][j] / completeCases));
                }
            }
            for (int i = 0; i < factors.size() - 1; i++) {
                tt.textRight(factors.size() + 2, i + 2, Format.floatShort(colTotals[i] / completeCases));
                tt.textRight(i + 2, factors.size() + 2, Format.floatShort(rowTotals[i] / completeCases));
            }
            tt.textRight(factors.size() + 2, factors.size() + 2, Format.floatShort(grandTotal / completeCases));
            sb.append(tt.getDefaultText());

        }
        sb.append("\n");
    }

    private String line(int len) {
        char[] lineChars = new char[len];
        for (int i = 0; i < len; i++) {
            lineChars[i] = '-';
        }
        return String.valueOf(lineChars);
    }

    public double accuracy() {
        return acc;
    }

    public double error() {
        return 1.0 - acc;
    }

    /**
     * Number of cases which were correctly predicted
     */
    public int acceptedCases() {
        return (int) Math.rint(acceptedCases);
    }

    /**
     * Number of cases which were not predicted correctly
     */
    public int errorCases() {
        return (int) Math.rint(errorCases);
    }

    public int completeCases() {
        return (int) Math.rint(completeCases);
    }

    public int[][] matrix() {
        return cmf;
    }
}

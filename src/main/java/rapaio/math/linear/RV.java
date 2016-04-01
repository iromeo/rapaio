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

package rapaio.math.linear;

import rapaio.core.stat.Mean;
import rapaio.core.stat.Variance;
import rapaio.data.Numeric;
import rapaio.math.linear.dense.SolidRM;
import rapaio.printer.Printable;
import rapaio.sys.WS;

import java.io.Serializable;
import java.util.stream.DoubleStream;

/**
 * Double vector
 * <p>
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 2/3/16.
 */
public interface RV extends Serializable, Printable {

    double get(int i);

    void set(int i, double value);

    void increment(int i, double value);

    int count();

    RV dot(double scalar);

    default RV plus(double x) {
        for (int i = 0; i < count(); i++) {
            increment(i, x);
        }
        return this;
    }

    default RV plus(RV B) {
        if (count() != B.count())
            throw new IllegalArgumentException(String.format(
                    "Vectors are not conform for addition: [%d] + [%d]", count(), B.count()));
        for (int i = 0; i < count(); i++) {
            increment(i, B.get(i));
        }
        return this;
    }

    default RV minus(double x) {
        return plus(-x);
    }

    default RV minus(RV B) {
        if (count() != B.count())
            throw new IllegalArgumentException(String.format(
                    "Matrices are not conform for substraction: [%d] + [%d]", count(), B.count()));
        for (int i = 0; i < count(); i++) {
            increment(i, -B.get(i));
        }
        return this;
    }

    double norm(double p);

    RV normalize(double p);

    /**
     * Dot product between two vectors is equal to the sum of the
     * product of elements from each given position.
     * <p>
     * sum_{i=1}^{n}a_i*b_i
     *
     * @param b
     * @return
     */
    default double dotProd(RV b) {
        int max = Math.max(count(), b.count());
        double s = 0;
        for (int i = 0; i < max; i++) {
            s += get(i) * b.get(i);
        }
        return s;
    }

    default Mean mean() {
        Numeric values = Numeric.newEmpty();
        for (int i = 0; i < count(); i++) {
            values.addValue(get(i));
        }
        return new Mean(values);
    }

    default Variance var() {
        Numeric values = Numeric.newEmpty();
        for (int i = 0; i < count(); i++) {
            values.addValue(get(i));
        }
        return new Variance(values);
    }


    RV solidCopy();

    default RM asMatrix() {
        SolidRM res = SolidRM.empty(count(), 1);
        for (int i = 0; i < count(); i++) {
            res.set(i, 0, get(i));
        }
        return res;
    }

    default RM asMatrixT() {
        SolidRM res = SolidRM.empty(1, count());
        for (int i = 0; i < count(); i++) {
            res.set(0, i, get(i));
        }
        return res;
    }

    DoubleStream valueStream();

    //////////////////
    // printSummary
    //////////////////

    default String summary() {

        StringBuilder sb = new StringBuilder();

        String[][] m = new String[count()][1];
        int max = 1;
        for (int i = 0; i < count(); i++) {
            m[i][0] = WS.formatShort(get(i));
            max = Math.max(max, m[i][0].length() + 1);
        }
        max = Math.max(max, String.format("[,%d]", count()).length());
        max = Math.max(max, String.format("[%d,]", 1).length());

        int hCount = (int) Math.floor(WS.getPrinter().textWidth() / (double) max);
        int vCount = Math.min(count() + 1, 101);
        int hLast = 0;
        while (true) {

            // take vertical stripes
            if (hLast >= 1)
                break;

            int hStart = hLast;
            int hEnd = Math.min(hLast + hCount, 1);
            int vLast = 0;

            while (true) {

                // print rows
                if (vLast >= count())
                    break;

                int vStart = vLast;
                int vEnd = Math.min(vLast + vCount, count());

                for (int i = vStart; i <= vEnd; i++) {
                    for (int j = hStart; j <= hEnd; j++) {
                        if (i == vStart && j == hStart) {
                            sb.append(String.format("%" + (max) + "s| ", ""));
                            continue;
                        }
                        if (i == vStart) {
                            sb.append(String.format("%" + Math.max(1, max - 1) + "d|", j - 1));
                            continue;
                        }
                        if (j == hStart) {
                            sb.append(String.format("%" + Math.max(1, max - 1) + "d |", i - 1));
                            continue;
                        }
                        sb.append(String.format("%" + max + "s", m[i - 1][j - 1]));
                    }
                    sb.append("\n");
                }
                vLast = vEnd;
            }
            hLast = hEnd;
        }
        return sb.toString();
    }
}

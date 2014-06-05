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

package rapaio.ml.refactor.boost.gbt;

import rapaio.core.stat.Mean;
import rapaio.data.Numeric;
import rapaio.data.Var;

/**
 * User: Aurelian Tutuianu <padreati@yahoo.com>
 */
public class L2BoostingLossFunction implements BoostingLossFunction {

    @Override
    public double findMinimum(Var y, Var fx) {
        return new Mean(gradient(y, fx)).getValue();
    }

    @Override
    public Numeric gradient(Var y, Var fx) {
        Numeric delta = new Numeric();
        for (int i = 0; i < y.rowCount(); i++) {
            delta.addValue(y.value(i) - fx.value(i));
        }
        return delta;
    }
}
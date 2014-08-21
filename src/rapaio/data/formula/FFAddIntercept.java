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

package rapaio.data.formula;

import rapaio.data.Frame;
import rapaio.data.Numeric;
import rapaio.data.SolidFrame;
import rapaio.data.Var;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Adds an intercept column: a numeric column with all values equal with 1.0,
 * used in general for linear regression like setups.
 * <p>
 * In case there is already a column called intercept on the
 * first position, nothing will happen.
 *
 * @author <a href="mailto:padreati@yahoo.com>Aurelian Tutuianu</a>
 */
@Deprecated
public class FFAddIntercept implements FrameFilter {

    @Override
    public Frame apply(Frame df) {
        if (df.varNames()[0].equals("intercept")) {
            return df;
        }
        List<Var> vars = new ArrayList<>();
        List<String> names = new ArrayList<>();

        vars.add(Numeric.newFill(df.rowCount(), 1.0));
        names.add("intercept");

        Arrays.stream(df.varNames()).forEach(varName -> {
            vars.add(df.var(varName));
            names.add(varName);
        });
        return new SolidFrame(df.rowCount(), vars, names);
    }
}

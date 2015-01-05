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

package rapaio.tutorial.pages;

import java.io.IOException;
import java.net.URISyntaxException;

import static rapaio.WS.heading;
import static rapaio.WS.p;

/**
 * User: Aurelian Tutuianu <padreati@yahoo.com>
 */
@Deprecated
public class LinearRegression2Page implements TutorialPage {

    @Override
    public String getPageName() {
        return "LinearRegression2";
    }

    @Override
    public String getPageTitle() {
        return "Linear Regression: Simple linear regression";
    }

    @Override
    public void render() throws IOException, URISyntaxException {

        heading(3, "Linear Regression with vectors and matrices - part 2");

        p("This tutorial aims to present how one can do by hand " +
                "linear regression using only vectors and matrices " +
                "operations. For practical purposes it should be used " +
                "linear regression models. ");

        heading(4, "Simple linear regression");

    }
}
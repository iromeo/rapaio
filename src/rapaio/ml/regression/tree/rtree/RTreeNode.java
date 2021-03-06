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

package rapaio.ml.regression.tree.rtree;

import rapaio.data.Frame;
import rapaio.data.Mapping;
import rapaio.data.Var;
import rapaio.data.VarDouble;
import rapaio.ml.common.predicate.RowPredicate;
import rapaio.ml.regression.boost.gbt.GBTRegressionLoss;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a> on 11/2/17.
 */
public class RTreeNode implements Serializable {

    private static final long serialVersionUID = 385363626560575837L;
    private final RTreeNode parent;
    private final int id;
    private final String groupName;
    private final RowPredicate predicate;
    private final int depth;

    private boolean leaf = true;
    private double value;
    private double weight;

    private List<RTreeNode> children = new ArrayList<>();
    private RTreeCandidate bestCandidate;

    public RTreeNode(int id, RTreeNode parent, String groupName, RowPredicate predicate, int depth) {
        this.id = id;
        this.parent = parent;
        this.groupName = groupName;
        this.predicate = predicate;
        this.depth = depth;
    }

    public RTreeNode getParent() {
        return parent;
    }

    public int id() {
        return id;
    }

    public String groupName() {
        return groupName;
    }

    public RowPredicate predicate() {
        return predicate;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

    public List<RTreeNode> children() {
        return children;
    }

    public void setBestCandidate(RTreeCandidate bestCandidate) {
        this.bestCandidate = bestCandidate;
    }

    public RTreeCandidate bestCandidate() {
        return bestCandidate;
    }

    public double value() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double weight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int depth() {
        return depth;
    }

    public void boostUpdate(Frame x, Var y, Var fx, GBTRegressionLoss lossFunction, RTreeSplitter splitter) {
        if (leaf) {
            value = lossFunction.findMinimum(y, fx);
            return;
        }

        List<RowPredicate> groupPredicates = new ArrayList<>();
        for (RTreeNode child : children) {
            groupPredicates.add(child.predicate());
        }

        List<Mapping> mappings = splitter.performSplitMapping(x, VarDouble.fill(x.rowCount(), 1), groupPredicates);

        for (int i = 0; i < children.size(); i++) {
            children.get(i).boostUpdate(
                    x.mapRows(mappings.get(i)),
                    y.mapRows(mappings.get(i)),
                    fx.mapRows(mappings.get(i)),
                    lossFunction, splitter);
        }
    }
}

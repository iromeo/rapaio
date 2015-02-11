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
 */

package rapaio.data.matrix;

/**
 * LU Decomposition.
 * <p>
 * For an m-by-n rapaio.data.matrix A with m >= n, the LU decomposition is an m-by-n unit
 * lower triangular rapaio.data.matrix L, an n-by-n upper triangular rapaio.data.matrix U, and a
 * permutation var piv of length m so that A(piv,:) = L*U. If m < n, then L
 * is m-by-m and U is m-by-n. <P>
 * The LU decompostion with pivoting always exists, even if the matrix is
 * singular, so the constructor will never fail. The primary use of the LU
 * decomposition is in the solution of square systems of simultaneous linear
 * equations. This will fail if isNonsingular() returns false.
 */

public class LUDecomposition implements java.io.Serializable {

    /*
     * ------------------------ Class variables ------------------------
     */
    /**
     * Array for internal storage of decomposition.
     *
     * @serial internal array storage.
     */
    private double[][] LU;

    /**
     * Row and column dimensions, and pivot sign.
     *
     * @serial column dimension.
     * @serial row dimension.
     * @serial pivot sign.
     */
    private int m, n, pivsign;

    /**
     * Internal storage of pivot var.
     *
     * @serial pivot var.
     */
    private int[] piv;

    /*
     * ------------------------ Constructor ------------------------
     */

    /**
     * LU Decomposition Structure to access L, U and piv.
     *
     * @param A Rectangular rapaio.data.matrix
     */
    public LUDecomposition(Matrix A) {

        // Use a "left-looking", dot-product, Crout/Doolittle algorithm.
        LU = A.getArrayCopy();
        m = A.getRows();
        n = A.getCols();
        piv = new int[m];
        for (int i = 0; i < m; i++) {
            piv[i] = i;
        }
        pivsign = 1;
        double[] LUrowi;
        double[] LUcolj = new double[m];

        // Outer loop.
        for (int j = 0; j < n; j++) {

            // Make a copy of the j-th column to localize references.
            for (int i = 0; i < m; i++) {
                LUcolj[i] = LU[i][j];
            }

            // Apply previous transformations.
            for (int i = 0; i < m; i++) {
                LUrowi = LU[i];

                // Most of the time is spent in the following dot product.
                int kmax = Math.min(i, j);
                double s = 0.0;
                for (int k = 0; k < kmax; k++) {
                    s += LUrowi[k] * LUcolj[k];
                }

                LUrowi[j] = LUcolj[i] -= s;
            }

            // Find pivot and exchange if necessary.
            int p = j;
            for (int i = j + 1; i < m; i++) {
                if (Math.abs(LUcolj[i]) > Math.abs(LUcolj[p])) {
                    p = i;
                }
            }
            if (p != j) {
                for (int k = 0; k < n; k++) {
                    double t = LU[p][k];
                    LU[p][k] = LU[j][k];
                    LU[j][k] = t;
                }
                int k = piv[p];
                piv[p] = piv[j];
                piv[j] = k;
                pivsign = -pivsign;
            }

            // Compute multipliers.
            if (j < m & LU[j][j] != 0.0) {
                for (int i = j + 1; i < m; i++) {
                    LU[i][j] /= LU[j][j];
                }
            }
        }
    }

    /*
     * ------------------------ Temporary, experimental code.
     * ------------------------ *\
     *
     * \** LU Decomposition, computed by Gaussian elimination. <P> This
     * constructor computes L and U with the "daxpy"-based elimination algorithm
     * used in LINPACK and MATLAB. In Java, we suspect the dot-product, Crout
     * algorithm will be faster. We have temporarily included this constructor
     * until timing experiments confirm this suspicion. <P> @param A Rectangular
     * matrix @param linpackflag Use Gaussian elimination. Actual value ignored.
     * @return Structure to access L, U and piv. \
     *
     * public LUDecomposition (Matrix A, int linpackflag) { // Initialize. LU =
     * A.getArrayCopy(); m = A.getRowDimension(); n = A.getColumnDimension();
     * piv = new int[m]; for (int i = 0; i < m; i++) { piv[i] = i; } pivsign =
     * 1; // Main loop. for (int k = 0; k < n; k++) { // Find pivot. int p = k;
     * for (int i = k+1; i < m; i++) { if (Math.abs(LU[i][k]) >
     * Math.abs(LU[p][k])) { p = i; } } // Exchange if necessary. if (p != k) {
     * for (int j = 0; j < n; j++) { double t = LU[p][j]; LU[p][j] = LU[k][j];
     * LU[k][j] = t; } int t = piv[p]; piv[p] = piv[k]; piv[k] = t; pivsign =
     * -pivsign; } // Compute multipliers and eliminate k-th column. if
     * (LU[k][k] != 0.0) { for (int i = k+1; i < m; i++) { LU[i][k] /= LU[k][k];
     * for (int j = k+1; j < n; j++) { LU[i][j] -= LU[i][k]*LU[k][j]; } } } } }
     *
     * \* ------------------------ End of temporary code.
     * ------------------------
     */

    /*
     * ------------------------ Public Methods ------------------------
     */

    /**
     * Is the rapaio.data.matrix nonsingular?
     *
     * @return true if U, and hence A, is nonsingular.
     */
    public boolean isNonsingular() {
        for (int j = 0; j < n; j++) {
            if (LU[j][j] == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Return lower triangular factor
     *
     * @return L
     */
    public Matrix getL() {
        Matrix L = new Matrix(m, n);
        for (int i = 0; i < m; i++) {
            for (int j = 0; j <= i && j < n; j++) {
                if (i > j) {
                    L.set(i, j, LU[i][j]);
                } else {
                    L.set(i, j, 1.0);
                }
            }
        }
        return L;
    }

    /**
     * Return upper triangular factor
     *
     * @return U
     */
    public Matrix getU() {
        Matrix U = new Matrix(n, n);
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                if (i <= j) {
                    U.set(i, j, LU[i][j]);
                }
            }
        }
        return U;
    }

    /**
     * Return pivot permutation var
     *
     * @return piv
     */
    public int[] getPivot() {
        int[] p = new int[m];
        System.arraycopy(piv, 0, p, 0, m);
        return p;
    }

    /**
     * Return pivot permutation var as a one-dimensional double array
     *
     * @return (double) piv
     */
    public double[] getDoublePivot() {
        double[] vals = new double[m];
        for (int i = 0; i < m; i++) {
            vals[i] = (double) piv[i];
        }
        return vals;
    }

    /**
     * Determinant
     *
     * @return det(A)
     * @throws IllegalArgumentException Matrix must be square
     */
    public double det() {
        if (m != n) {
            throw new IllegalArgumentException("Matrix must be square.");
        }
        double d = (double) pivsign;
        for (int j = 0; j < n; j++) {
            d *= LU[j][j];
        }
        return d;
    }

    /**
     * Solve A*X = B
     *
     * @param B A Matrix with as many getRowCount as A and any number of columns.
     * @return X so that L*U*X = B(piv,:)
     * @throws IllegalArgumentException Matrix row dimensions must agree.
     * @throws RuntimeException         Matrix is singular.
     */
    public Matrix solve(Matrix B) {
        if (B.getRows() != m) {
            throw new IllegalArgumentException("Matrix row dimensions must agree.");
        }
        if (!this.isNonsingular()) {
            throw new RuntimeException("Matrix is singular.");
        }

        // Copy right hand side with pivoting
        int nx = B.getCols();
        Matrix X = B.getMatrix(piv, 0, nx - 1);

        // Solve L*Y = B(piv,:)
        for (int k = 0; k < n; k++) {
            for (int i = k + 1; i < n; i++) {
                for (int j = 0; j < nx; j++) {
                    X.set(i, j, X.get(i, j) - X.get(k, j) * LU[i][k]);
                }
            }
        }
        // Solve U*X = Y;
        for (int k = n - 1; k >= 0; k--) {
            for (int j = 0; j < nx; j++) {
                X.set(k, j, X.get(k, j) / LU[k][k]);
            }
            for (int i = 0; i < k; i++) {
                for (int j = 0; j < nx; j++) {
                    X.set(i, j, X.get(i, j) - X.get(k, j) * LU[i][k]);
                }
            }
        }
        return X;
    }

}

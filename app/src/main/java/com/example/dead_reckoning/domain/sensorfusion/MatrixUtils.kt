package com.example.dead_reckoning.domain.sensorfusion

import kotlin.math.abs

object MatrixUtils {

    fun create(rows: Int, cols: Int, defaultVal: Double = 0.0): Array<DoubleArray> =
        Array(rows) { DoubleArray(cols) { defaultVal } }

    fun identity(size: Int): Array<DoubleArray> {
        val result = create(size, size)
        for (i in 0 until size) {
            result[i][i] = 1.0
        }
        return result
    }

    fun add(A: Array<DoubleArray>, B: Array<DoubleArray>): Array<DoubleArray> {
        val rows = A.size
        val cols = A[0].size
        val result = create(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[i][j] = A[i][j] + B[i][j]
            }
        }
        return result
    }

    fun subtract(A: Array<DoubleArray>, B: Array<DoubleArray>): Array<DoubleArray> {
        val rows = A.size
        val cols = A[0].size
        val result = create(rows, cols)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[i][j] = A[i][j] - B[i][j]
            }
        }
        return result
    }

    fun multiply(A: Array<DoubleArray>, B: Array<DoubleArray>): Array<DoubleArray> {
        val rowsA = A.size
        val colsA = A[0].size
        val colsB = B[0].size
        val result = create(rowsA, colsB)
        for (i in 0 until rowsA) {
            for (k in 0 until colsA) {
                for (j in 0 until colsB) {
                    result[i][j] += A[i][k] * B[k][j]
                }
            }
        }
        return result
    }

    fun transpose(A: Array<DoubleArray>): Array<DoubleArray> {
        val rows = A.size
        val cols = A[0].size
        val result = create(cols, rows)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[j][i] = A[i][j]
            }
        }
        return result
    }

    /**
     * Inverts a square matrix using Gauss-Jordan elimination with partial pivoting.
     */
    fun invert(A: Array<DoubleArray>): Array<DoubleArray> {
        val n = A.size
        val augmented = Array(n) { i ->
            DoubleArray(2 * n) { j ->
                if (j < n) A[i][j] else if (j - n == i) 1.0 else 0.0
            }
        }

        for (i in 0 until n) {
            var maxRow = i
            for (k in i + 1 until n) {
                if (abs(augmented[k][i]) > abs(augmented[maxRow][i])) {
                    maxRow = k
                }
            }

            val temp = augmented[i]
            augmented[i] = augmented[maxRow]
            augmented[maxRow] = temp

            var pivot = augmented[i][i]
            if (abs(pivot) < 1e-12) {
                pivot = if (pivot >= 0) 1e-12 else -1e-12
            }

            for (j in 0 until 2 * n) {
                augmented[i][j] /= pivot
            }

            for (k in 0 until n) {
                if (k != i) {
                    val factor = augmented[k][i]
                    for (j in 0 until 2 * n) {
                        augmented[k][j] -= factor * augmented[i][j]
                    }
                }
            }
        }

        val result = create(n, n)
        for (i in 0 until n) {
            for (j in 0 until n) {
                result[i][j] = augmented[i][j + n]
            }
        }
        return result
    }
}

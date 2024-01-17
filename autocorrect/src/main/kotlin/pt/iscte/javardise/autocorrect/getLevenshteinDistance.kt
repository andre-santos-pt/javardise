package pt.iscte.javardise.autocorrect

import kotlin.math.min

fun getLevenshteinDistance(X: String, Y: String): Int {
    val m = X.length
    val n = Y.length
    val T = Array(m + 1) { IntArray(n + 1) }
    for (i in 1..m) {
        T[i][0] = i
    }
    for (j in 1..n) {
        T[0][j] = j
    }
    var cost: Int
    for (i in 1..m) {
        for (j in 1..n) {
            cost = if (X[i - 1] == Y[j - 1]) 0 else 1
            T[i][j] = min(min(T[i - 1][j] + 1, T[i][j - 1] + 1),
                T[i - 1][j - 1] + cost)
        }
    }
    return T[m][n]
}

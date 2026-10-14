import java.util.ArrayList;

// Solves Lights Out for one board size using light chasing.
//
// Clicking some cells of the first line and then chasing (clicking under every lit cell, line
// by line) leaves every line dark except possibly the last. What is left lit there is C * top,
// where top is the first-line clicks and C is an n-by-n "chasing matrix". A board is solved by
// choosing top so that C * top cancels what chasing with no first-line clicks would leave.
//
// C is never built as a matrix. It equals c(M), a polynomial in the matrix M that maps a line
// of clicks to "each click's two neighbours", and every line of the board can be written
// uniquely as p(M) e0 for a polynomial p of degree below n (e0 = the line with only its first
// cell set). Applying C then becomes multiplying polynomials, mod the polynomial chi with
// chi(M) = 0. So instead of row-reducing an n-by-n matrix (about n^3 / 64 operations), solving
// C * top = d is polynomial arithmetic: one greatest-common-divisor calculation when the
// solver is created, and a multiplication per solve, each about n^2 / 64 operations.
//
// The polynomials involved are the ones the chasing recurrence produces:
//     q_0 = 0, q_1 = 1, q_(i+1) = x * q_i + q_(i-1)
// chi = q_(n+1), the line with only cell j set is q_(j+1)(M) e0, and c(x) = q_(n+1)(x + 1).
//
// A line is one row of the board, stored the way Board stores it: one bit per cell.
// Click patterns are returned as one long bit vector with the lines laid end to end,
// so cell i is line i / n, position i % n. Chasing runs from line 0 to line n - 1.
public class ChaseSolver {
    private final int n;
    // Number of 64-bit words needed to hold one line of the board (n bits)
    private final int lineWords;
    // Number of 64-bit words needed to hold a full click pattern (n * n bits)
    private final int fullWords;

    // The longest search for the fewest-clicks solution that will be attempted, in seconds.
    // Boards whose search is estimated to take longer get the basic solution instead.
    // Change this number (and restart) to allow longer searches.
    public static final double MAX_SEARCH_SECONDS = 20;

    // chi = q_(n+1): chi(M) = 0, so polynomials standing for lines are taken mod chi
    private final long[] chi;
    // g = gcd(c, chi). A board can be solved exactly when g divides its polynomial, and the
    // degree of g is the nullity.
    private final long[] g;
    // s * c = g (mod chi), used to undo the multiplication by c
    private final long[] multiplier;

    // Dimension of the nullspace: how many independent ways there are to click without
    // changing the board
    private final int nullity;

    // Basis for the nullspace of the full n^2-by-n^2 matrix, one click pattern per vector.
    // Left empty when the search is over the time limit: the basis is only used by the
    // search, and for large boards with high nullity it would take hundreds of MB or more.
    private final long[][] nullBasis;

    public ChaseSolver(int n) {
        // Click patterns are indexed by cell number in an int, so n * n has to fit in one
        if ((long) n * n > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Board size " + n + " is too large; the most is 46340");
        }
        this.n = n;
        this.lineWords = (n + 63) >>> 6;
        this.fullWords = (n * n + 63) >>> 6;

        // chi = q_(n+1) and q_(n+1)(x + 1), both from the chasing recurrence
        int polyWords = BinaryPolynomial.wordsFor(n + 1);
        long[] qPrev = new long[polyWords];          // q_0 = 0
        long[] q = new long[polyWords];              // q_1 = 1
        q[0] = 1;
        long[] pPrev = new long[polyWords];          // same recurrence with x + 1 in place of x
        long[] p = new long[polyWords];
        p[0] = 1;
        for (int i = 1; i <= n; i++) {
            // q_(i+1) = x * q_i + q_(i-1), computed in place over q_(i-1)
            BinaryPolynomial.addShifted(qPrev, q, BinaryPolynomial.degree(q), 1);
            long[] t = qPrev;
            qPrev = q;
            q = t;
            // p_(i+1) = (x + 1) * p_i + p_(i-1)
            int dp = BinaryPolynomial.degree(p);
            BinaryPolynomial.addShifted(pPrev, p, dp, 1);
            BinaryPolynomial.addShifted(pPrev, p, dp, 0);
            t = pPrev;
            pPrev = p;
            p = t;
        }
        chi = q;
        long[] c = BinaryPolynomial.mod(p, chi);

        long[][] gs = BinaryPolynomial.gcdAndMultiplier(c, chi);
        g = gs[0];
        multiplier = gs[1];
        nullity = BinaryPolynomial.degree(g);

        // Nullspace of C: the lines h(M) e0, M h(M) e0, ..., M^(nullity-1) h(M) e0 with
        // h = chi / g. Chasing each one on an empty board turns it into a full click pattern
        // that changes nothing.
        ArrayList<long[]> basis = new ArrayList<>();
        if (nullity > 0 && canSearch()) {
            long[] h = BinaryPolynomial.divide(chi, g)[0];
            long[] line = toLine(h);
            for (int j = 0; j < nullity; j++) {
                basis.add(pack(chase(null, line)));
                line = neighbourEffect(line);
            }
        }
        nullBasis = basis.toArray(new long[0][]);
    }

    public int getNullity() {
        return nullity;
    }

    // Estimated time in seconds to search every solution for the one with the fewest clicks
    public double estimatedSearchSeconds() {
        return SolutionSearch.estimateSeconds(nullity, (long) n * n);
    }

    // Whether the search fits within MAX_SEARCH_SECONDS and its memory limit
    public boolean canSearch() {
        return estimatedSearchSeconds() <= MAX_SEARCH_SECONDS && basisFitsInMemory();
    }

    // The nullspace basis takes one bit per cell for each of its 'nullity' vectors. It is only
    // built if that fits in a quarter of the memory Java is allowed to use.
    public boolean basisFitsInMemory() {
        double basisBytes = (double) nullity * fullWords * 8;
        return basisBytes <= Runtime.getRuntime().maxMemory() / 4.0;
    }

    // state[line] holds one bit per cell of that line: bit pos is 1 when the cell is lit.
    // It is only read, never changed. Returns one click pattern that solves the board plus
    // the nullspace basis, or solvable = false if no solution exists.
    public SolveResult solve(long[][] state) {
        // d = what stays lit past the last line when chasing with no first-line clicks
        long[] d = chase(state, new long[lineWords])[n];

        long[] top = firstLineClicks(d);
        if (top == null) return new SolveResult(null, null, false);

        long[] x0 = pack(chase(state, top));
        return new SolveResult(x0, nullBasis, true);
    }

    // For a board that is dark everywhere except its last line: lastLineLit[pos] says which
    // cells of that line are lit. Returns which cells of the first line to click so that
    // chasing afterwards leaves the whole board dark, or null if that is impossible.
    public boolean[] firstLineClicks(boolean[] lastLineLit) {
        long[] d = new long[lineWords];
        for (int b = 0; b < n; b++) {
            if (lastLineLit[b]) setBit(d, b);
        }
        long[] top = firstLineClicks(d);
        if (top == null) return null;

        boolean[] clicks = new boolean[n];
        for (int b = 0; b < n; b++) clicks[b] = getBit(top, b);
        return clicks;
    }

    // Solves C * top = d, where d is what stays lit past the last line when chasing with no
    // first-line clicks. Returns null if there is no solution.
    //
    // As polynomials this is c * t = d (mod chi). Since s * c = g (mod chi), it has a solution
    // exactly when g divides d, and then t = s * (d / g) works.
    private long[] firstLineClicks(long[] d) {
        long[][] qr = BinaryPolynomial.divide(toPolynomial(d), g);
        if (!BinaryPolynomial.isZero(qr[1])) return null;
        long[] t = BinaryPolynomial.mod(BinaryPolynomial.multiply(multiplier, qr[0]), chi);
        return toLine(t);
    }

    // The polynomial p with p(M) e0 = line. The line with only cell j set is q_(j+1)(M) e0, so
    // p is the sum of q_(j+1) over the set cells j. The q's are generated as needed.
    private long[] toPolynomial(long[] line) {
        int polyWords = BinaryPolynomial.wordsFor(n + 1);
        long[] p = new long[polyWords];
        long[] qPrev = new long[polyWords];          // q_0
        long[] q = new long[polyWords];              // q_1
        q[0] = 1;
        int qDegree = 0;
        for (int j = 0; j < n; j++) {
            // q is now q_(j+1), of degree j
            if (getBit(line, j)) {
                for (int w = 0; w <= (qDegree >>> 6); w++) p[w] ^= q[w];
            }
            BinaryPolynomial.addShifted(qPrev, q, qDegree, 1);
            long[] t = qPrev;
            qPrev = q;
            q = t;
            qDegree++;
        }
        return p;
    }

    // The line p(M) e0, by Horner's rule: start from zero and, from the highest power down,
    // apply M and add e0 wherever p has a term
    private long[] toLine(long[] p) {
        long[] line = new long[lineWords];
        long[] next = new long[lineWords];
        for (int j = BinaryPolynomial.degree(p); j >= 0; j--) {
            neighbourEffectInto(next, line);
            long[] t = line;
            line = next;
            next = t;
            if (getBit(p, j)) line[0] ^= 1;
        }
        return line;
    }

    // Clicks 'top' on the first line, then on each later line clicks under every cell
    // that is still lit. state is the starting board (null means empty) and is not modified.
    // Returns n + 1 lines: entries 0..n-1 are the clicks made on each line, and entry n
    // is what is still lit on the last line afterwards.
    private long[][] chase(long[][] state, long[] top) {
        long[][] clicks = new long[n + 1][];
        clicks[0] = top;
        for (int a = 0; a < n; a++) {
            // Line a after the clicks on lines a-1 and a = the clicks needed on line a+1
            long[] lit = sameLineEffect(clicks[a]);
            if (a > 0) xorInto(lit, clicks[a - 1]);
            if (state != null) xorInto(lit, state[a]);
            clicks[a + 1] = lit;
        }
        return clicks;
    }

    // Which cells of a line get toggled by clicks on that same line: each click
    // toggles itself and its two neighbours
    private long[] sameLineEffect(long[] line) {
        long[] out = new long[lineWords];
        for (int w = 0; w < lineWords; w++) {
            long v = line[w];
            long up = v << 1;
            if (w > 0) up |= line[w - 1] >>> 63;
            long down = v >>> 1;
            if (w < lineWords - 1) down |= line[w + 1] << 63;
            out[w] = v ^ up ^ down;
        }
        // Drop anything shifted past the end of the line
        int extra = n & 63;
        if (extra != 0) out[lineWords - 1] &= (1L << extra) - 1;
        return out;
    }

    // Lays the n click lines end to end into one n * n bit vector, copying a word (64 cells)
    // at a time. Line a starts at bit a * n, which is usually not on a word boundary, so each
    // word of the line is split across two neighbouring words of the result.
    private long[] pack(long[][] clicks) {
        long[] v = new long[fullWords];
        for (int a = 0; a < n; a++) {
            for (int w = 0; w < lineWords; w++) {
                long word = clicks[a][w];
                if (word == 0) continue;
                int start = a * n + (w << 6);
                int idx = start >>> 6;
                int shift = start & 63;
                v[idx] |= word << shift;
                if (shift != 0 && idx + 1 < fullWords) v[idx + 1] |= word >>> (64 - shift);
            }
        }
        return v;
    }

    // Which cells of a line are next to a click on that same line (the matrix M above):
    // each click's left and right neighbours
    private long[] neighbourEffect(long[] line) {
        long[] out = new long[lineWords];
        neighbourEffectInto(out, line);
        return out;
    }

    // Same as neighbourEffect, writing into out instead of a new array
    private void neighbourEffectInto(long[] out, long[] line) {
        for (int w = 0; w < lineWords; w++) {
            long v = line[w];
            long up = v << 1;
            if (w > 0) up |= line[w - 1] >>> 63;
            long down = v >>> 1;
            if (w < lineWords - 1) down |= line[w + 1] << 63;
            out[w] = up ^ down;
        }
        // Drop anything shifted past the end of the line
        int extra = n & 63;
        if (extra != 0) out[lineWords - 1] &= (1L << extra) - 1;
    }

    private static void xorInto(long[] dst, long[] src) {
        for (int i = 0; i < dst.length; i++) dst[i] ^= src[i];
    }

    private static boolean getBit(long[] row, int bit) {
        return ((row[bit >>> 6] >>> (bit & 63)) & 1L) != 0;
    }

    private static void setBit(long[] row, int bit) {
        row[bit >>> 6] |= 1L << (bit & 63);
    }
}
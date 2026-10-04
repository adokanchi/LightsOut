import java.util.ArrayList;
import java.util.Arrays;

public class ChaseSolver {
    private final int n;
    // Number of 64-bit words needed to hold one line of the board (n bits)
    private final int lineWords;
    // Number of 64-bit words needed to hold a full click pattern (n * n bits)
    private final int fullWords;

    // Row-reduced [C | I]. Bits 0..n-1 are the reduced chasing matrix,
    // bits n..2n-1 are the row operations that were applied to get there.
    private final long[][] reduced;
    // pivotRow[col] = row of reduced holding the pivot for that column, or -1 if the column is free
    private final int[] pivotRow;
    private final int rank;

    // Basis for the nullspace of the full n^2-by-n^2 matrix, one click pattern per vector
    private final long[][] nullBasis;

    public ChaseSolver(int n) {
        this.n = n;
        this.lineWords = (n + 63) >>> 6;
        this.fullWords = (n * n + 63) >>> 6;

        // Build [C | I]. Column j of C is what stays lit past the last line after
        // clicking cell j of the first line on an empty board and chasing.
        //
        // Only column 0 is found by actually chasing. C is a polynomial in the matrix M that
        // maps a line of clicks to "each click's two neighbours", and the unit vectors satisfy
        // e(j+1) = M e(j) + e(j-1), so the columns satisfy the same rule:
        //     column(j+1) = M * column(j) + column(j-1)
        // C is also symmetric, so column j can be stored directly as row j.
        int matWords = (2 * n + 63) >>> 6;
        reduced = new long[n][matWords];
        long[] first = new long[lineWords];
        setBit(first, 0);
        long[] previous = new long[lineWords];   // column j - 1 (all zero before column 0)
        long[] current = chase(null, first)[n];  // column j
        for (int j = 0; j < n; j++) {
            System.arraycopy(current, 0, reduced[j], 0, lineWords);
            long[] next = neighbourEffect(current);
            xorInto(next, previous);
            previous = current;
            current = next;
        }
        for (int i = 0; i < n; i++) setBit(reduced[i], n + i);

        // Gauss-Jordan elimination on the n-by-n system
        pivotRow = new int[n];
        Arrays.fill(pivotRow, -1);
        int row = 0;
        for (int col = 0; col < n && row < n; col++) {
            int pivot = -1;
            for (int r = row; r < n; r++) {
                if (getBit(reduced[r], col)) {
                    pivot = r;
                    break;
                }
            }
            if (pivot == -1) continue;

            long[] tmp = reduced[pivot];
            reduced[pivot] = reduced[row];
            reduced[row] = tmp;
            pivotRow[col] = row;

            for (int r2 = 0; r2 < n; r2++) {
                if (r2 != row && getBit(reduced[r2], col)) xorInto(reduced[r2], reduced[row]);
            }
            row++;
        }
        rank = row;

        // Nullspace of C: one first-line click pattern per free column. Chasing each one
        // on an empty board turns it into a full click pattern that changes nothing.
        ArrayList<long[]> basis = new ArrayList<>();
        for (int f = 0; f < n; f++) {
            if (pivotRow[f] != -1) continue;

            long[] top = new long[lineWords];
            setBit(top, f);
            for (int p = 0; p < n; p++) {
                int pr = pivotRow[p];
                if (pr != -1 && getBit(reduced[pr], f)) setBit(top, p);
            }
            basis.add(pack(chase(null, top)));
        }
        nullBasis = basis.toArray(new long[0][]);
    }

    public int getNullity() {
        return nullBasis.length;
    }

    // on[line][pos] is true when that cell is lit. Returns one click pattern that solves
    // the board plus the nullspace basis, or solvable = false if no solution exists.
    public SolveResult solve(boolean[][] on) {
        long[][] state = new long[n][lineWords];
        for (int a = 0; a < n; a++) {
            for (int b = 0; b < n; b++) {
                if (on[a][b]) setBit(state[a], b);
            }
        }

        // d = what stays lit past the last line when chasing with no first-line clicks
        long[] d = chase(state, new long[lineWords])[n];

        // Apply the stored row operations to d, giving the right-hand side of the reduced system
        long[] rhs = new long[lineWords];
        for (int r = 0; r < n; r++) {
            int parity = 0;
            for (int w = 0; w < lineWords; w++) {
                parity ^= Long.bitCount(extractWord(reduced[r], n, w) & d[w]) & 1;
            }
            if (parity != 0) setBit(rhs, r);
        }

        // Rows past the rank are all zero on the left, so they must be zero on the right
        for (int r = rank; r < n; r++) {
            if (getBit(rhs, r)) return new SolveResult(null, null, false);
        }

        // First-line clicks, with every free variable set to 0
        long[] top = new long[lineWords];
        for (int col = 0; col < n; col++) {
            int pr = pivotRow[col];
            if (pr != -1 && getBit(rhs, pr)) setBit(top, col);
        }

        long[] x0 = pack(chase(state, top));
        return new SolveResult(x0, nullBasis, true);
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
    // the same as sameLineEffect but without the clicked cells themselves
    private long[] neighbourEffect(long[] line) {
        long[] out = sameLineEffect(line);
        xorInto(out, line);
        return out;
    }

    // Reads 64 bits of row starting at bit (offset + 64 * w)
    private static long extractWord(long[] row, int offset, int w) {
        int start = offset + (w << 6);
        int idx = start >>> 6;
        int shift = start & 63;
        long v = row[idx] >>> shift;
        if (shift != 0 && idx + 1 < row.length) v |= row[idx + 1] << (64 - shift);
        return v;
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

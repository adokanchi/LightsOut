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

    // Largest nullity for which every solution is searched for the one with the fewest clicks.
    // The search checks 2^nullity click patterns, so each step up doubles the time.
    public static final int MAX_SEARCH_NULLITY = 28;

    // Number of free columns of C = dimension of the nullspace
    private final int nullity;

    // Basis for the nullspace of the full n^2-by-n^2 matrix, one click pattern per vector.
    // Left empty when the nullity is above MAX_SEARCH_NULLITY: the basis is only used by the
    // search, and for large boards with high nullity it would take hundreds of MB or more.
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
        rank = rowReduce();
        nullity = n - rank;

        // Nullspace of C: one first-line click pattern per free column. Chasing each one
        // on an empty board turns it into a full click pattern that changes nothing.
        ArrayList<long[]> basis = new ArrayList<>();
        for (int f = 0; f < n && nullity <= MAX_SEARCH_NULLITY; f++) {
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

    // Row-reduces [C | I] in place and fills in pivotRow. Returns the rank.
    //
    // This is Gauss-Jordan elimination done several columns at a time (the "Method of the
    // Four Russians"). Plain elimination clears one column per row XOR. Here the columns are
    // taken in blocks: for each block, every XOR combination of the block's pivot rows is put
    // in a table, and then each other row has all of the block's columns cleared with a single
    // XOR of the table entry that matches its bits. The result is exactly what clearing one
    // column at a time produces, in about a quarter of the row XORs.
    private int rowReduce() {
        // Columns per block. Bigger blocks mean fewer passes over the matrix but a table of
        // 2^blockSize rows to build each time; these values were the fastest when measured.
        final int blockSize = n < 4000 ? 8 : 10;
        final int matWords = reduced[0].length;

        long[][] table = new long[1 << blockSize][matWords];
        // For each pivot found in the current block: its column's offset within the block,
        // and the pivot row's bits in the block's columns
        int[] pivotOffset = new int[blockSize];
        int[] pivotBits = new int[blockSize];

        int row = 0;
        for (int blockStart = 0; blockStart < n && row < n; blockStart += blockSize) {
            int width = Math.min(blockSize, n - blockStart);
            // Every row from 'row' down is already zero in all earlier columns, so nothing
            // before this word can change and the XORs can start here
            int firstWord = blockStart >>> 6;

            // Step 1: find the pivot rows for this block's columns. They end up in rows
            // row, row + 1, ... and are kept reduced against each other, so that within the
            // block each one has a 1 in its own pivot column and 0 in the others'.
            int found = 0;
            for (int offset = 0; offset < width && row + found < n; offset++) {
                // Look for a row that still has a 1 in this column once the pivots already
                // found in this block are cleared from it. That is worked out on the row's
                // few block bits alone, without touching the full row.
                int pivot = -1;
                int pivotBlockBits = 0;
                for (int r = row + found; r < n; r++) {
                    int bits = reduceBlockBits(extractBits(reduced[r], blockStart, width), pivotOffset, pivotBits, found);
                    if (((bits >>> offset) & 1) != 0) {
                        pivot = r;
                        pivotBlockBits = bits;
                        break;
                    }
                }
                // No pivot: this column is free
                if (pivot == -1) continue;

                // Now clear the earlier pivots of this block from the chosen row for real
                long[] pivotRowBits = reduced[pivot];
                int bits = extractBits(pivotRowBits, blockStart, width);
                for (int i = 0; i < found; i++) {
                    if (((bits >>> pivotOffset[i]) & 1) != 0) {
                        bits ^= pivotBits[i];
                        xorFrom(pivotRowBits, reduced[row + i], firstWord);
                    }
                }

                // Move it up to sit just below the pivots already found
                reduced[pivot] = reduced[row + found];
                reduced[row + found] = pivotRowBits;

                // Clear the new pivot's column from the earlier pivots of this block
                for (int i = 0; i < found; i++) {
                    if (((pivotBits[i] >>> offset) & 1) != 0) {
                        xorFrom(reduced[row + i], pivotRowBits, firstWord);
                        pivotBits[i] ^= pivotBlockBits;
                    }
                }

                pivotOffset[found] = offset;
                pivotBits[found] = pivotBlockBits;
                pivotRow[blockStart + offset] = row + found;
                found++;
            }
            if (found == 0) continue;

            // Step 2: build the table. Entry number e is the XOR of the pivot rows whose bit
            // is set in e (bit i = the i-th pivot of this block). Going through the entries in
            // Gray-code order means each one is the previous entry XOR a single pivot row.
            int entries = 1 << found;
            Arrays.fill(table[0], firstWord, matWords, 0L);
            int previousEntry = 0;
            for (int g = 1; g < entries; g++) {
                int entry = g ^ (g >>> 1);
                long[] source = table[previousEntry];
                long[] add = reduced[row + Integer.numberOfTrailingZeros(g)];
                long[] target = table[entry];
                for (int w = firstWord; w < matWords; w++) target[w] = source[w] ^ add[w];
                previousEntry = entry;
            }

            // Step 3: clear the block's pivot columns from every other row. The row's bits in
            // those columns say which pivot rows it needs, which is exactly a table entry.
            for (int r = 0; r < n; r++) {
                if (r >= row && r < row + found) continue;
                int bits = extractBits(reduced[r], blockStart, width);
                int entry;
                if (found == width) {
                    // Every column of the block has a pivot, in order: the bits are the entry
                    entry = bits;
                } else {
                    entry = 0;
                    for (int i = 0; i < found; i++) {
                        entry |= ((bits >>> pivotOffset[i]) & 1) << i;
                    }
                }
                if (entry != 0) xorFrom(reduced[r], table[entry], firstWord);
            }

            row += found;
        }
        return row;
    }

    // The bits a row would have in the current block's columns after clearing from it the
    // pivots found so far in the block
    private static int reduceBlockBits(int bits, int[] pivotOffset, int[] pivotBits, int found) {
        for (int i = 0; i < found; i++) {
            if (((bits >>> pivotOffset[i]) & 1) != 0) bits ^= pivotBits[i];
        }
        return bits;
    }

    // Reads 'count' bits (at most 32) of row starting at bit 'start', as a number whose
    // bit 0 is the bit at 'start'
    private static int extractBits(long[] row, int start, int count) {
        int word = start >>> 6;
        int shift = start & 63;
        long value = row[word] >>> shift;
        if (shift + count > 64 && word + 1 < row.length) value |= row[word + 1] << (64 - shift);
        return (int) (value & ((1L << count) - 1));
    }

    // dst ^= src, for the words from firstWord on
    private static void xorFrom(long[] dst, long[] src, int firstWord) {
        for (int w = firstWord; w < dst.length; w++) dst[w] ^= src[w];
    }

    public int getNullity() {
        return nullity;
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

    // Solves C * top = d using the stored row reduction, with every free variable set to 0.
    // d is what stays lit past the last line when chasing with no first-line clicks.
    // Returns null if there is no solution.
    private long[] firstLineClicks(long[] d) {
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
            if (getBit(rhs, r)) return null;
        }

        long[] top = new long[lineWords];
        for (int col = 0; col < n; col++) {
            int pr = pivotRow[col];
            if (pr != -1 && getBit(rhs, pr)) setBit(top, col);
        }
        return top;
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
import java.util.stream.IntStream;
import java.util.stream.LongStream;

// Finds, among all solutions of a board, the one with the fewest clicks.
//
// Every solution is x0 XOR (some combination of the k nullspace basis vectors), so there are
// 2^k of them. Two methods are available and the faster one for the board is used:
//
// - Gray-code search: steps through all 2^k solutions, each differing from the previous by one
//   basis vector, counting the clicks of each. Costs about 2^k steps per 64 cells.
//
// - Walsh-Hadamard transform: counts the clicks of all 2^k solutions at once. Cells whose
//   clicked/unclicked state responds to the basis vectors in the same way are merged, and a
//   fast transform (like an FFT) turns that summary into the click count of every solution.
//   Costs about k * 2^k additions plus one pass over the board, so board size hardly matters.
//
// Both give the same answer: the fewest clicks, with ties going to the solution that comes
// first in Gray-code order.
public class SolutionSearch {
    // How fast each method runs, used to estimate search times and to pick the faster method.
    // Measured on an 8-core Ryzen 7 7730U laptop (with SearchBenchmark) and accurate there to
    // within about 15%. Faster machines will finish sooner than estimated.
    //
    // Gray-code search: one step = XOR and count 64 cells. While the working data fits in the
    // processor's cache it runs about 13 times faster than when it has to come from memory.
    private static final double GRAY_STEPS_PER_SECOND_IN_CACHE = 1.3e10;
    private static final double GRAY_STEPS_PER_SECOND_FROM_MEMORY = 1.0e9;
    // Working data (all basis vectors plus one) up to this many bytes counts as fitting in cache
    private static final double GRAY_CACHE_BYTES = 8e6;
    // Transform: summarising the board costs this much per cell, plus this much per cell for
    // each basis vector
    private static final double SUMMARY_SECONDS_PER_CELL = 0.9e-9;
    private static final double SUMMARY_SECONDS_PER_CELL_PER_VECTOR = 0.37e-9;
    // Transform: butterfly operations per second (a table of 2^m entries needs m * 2^m)
    private static final double BUTTERFLIES_PER_SECOND = 3.3e9;

    // Estimated seconds to find the fewest-clicks solution, using whichever method is faster
    public static double estimateSeconds(int k, long cells) {
        if (k == 0) return 0;
        return Math.min(grayEstimate(k, cells), transformEstimate(k, cells));
    }

    // Returns the solution with the fewest clicks. 'cells' is the number of cells on the board.
    public static long[] fewestClicks(long[] x0, long[][] basis, long cells) {
        int k = basis.length;
        if (k == 0) return x0;
        if (picksTransform(k, cells)) {
            return transformSearch(x0, basis, cells);
        }
        return graySearch(x0, basis);
    }

    // Whether fewestClicks uses the transform (true) or the Gray-code search (false)
    static boolean picksTransform(int k, long cells) {
        return transformEstimate(k, cells) < grayEstimate(k, cells);
    }

    private static double grayEstimate(int k, long cells) {
        double words = (cells + 63) / 64;
        // 2^k steps, plus setting up each chunk: a copy of the starting solution and, on
        // average, XORs with half of the chunk-numbering basis vectors
        int chunkBits = Math.min(k, 8);
        double steps = Math.pow(2, k) + Math.pow(2, chunkBits) * (1 + chunkBits / 2.0);
        boolean fitsInCache = (k + 1) * words * 8 <= GRAY_CACHE_BYTES;
        double rate = fitsInCache ? GRAY_STEPS_PER_SECOND_IN_CACHE : GRAY_STEPS_PER_SECOND_FROM_MEMORY;
        return steps * words / rate;
    }

    private static double transformEstimate(int k, long cells) {
        int m = Math.min(k, tableBits());
        double passes = Math.pow(2, k - m);
        double summary = cells * (SUMMARY_SECONDS_PER_CELL + SUMMARY_SECONDS_PER_CELL_PER_VECTOR * k);
        double butterflies = (double) m * Math.pow(2, m) / BUTTERFLIES_PER_SECOND;
        return passes * (summary + butterflies);
    }

    // The transform needs a table of 2^m ints. This picks the largest m whose table uses at
    // most a quarter of the memory Java is allowed. Bigger searches are done as several
    // passes over a table of that size.
    static int tableBits() {
        long budget = Runtime.getRuntime().maxMemory() / 4;
        int bits = 63 - Long.numberOfLeadingZeros(Math.max(budget / 4, 2));
        return Math.max(1, Math.min(bits, tableBitsLimit));
    }

    // Upper limit on the table size above, as a power of two. Java arrays cannot hold 2^31 entries,
    // so 30 is the most allowed. Tests lower it to exercise searches done in several passes.
    static int tableBitsLimit = 30;

    // ===================== Walsh-Hadamard transform =====================
    //
    // Cell i is clicked in solution c when x0_i XOR <p_i, c> is 1, where p_i says which basis
    // vectors include cell i (bit j set = basis vector j clicks it). So the number of clicks is
    //     (cells - S(c)) / 2,   where   S(c) = sum over cells of (-1)^(x0_i XOR <p_i, c>)
    // Grouping cells by p_i:  S(c) = sum over patterns v of f(v) * (-1)^<v, c>,
    // with f(v) = sum over cells with pattern v of (-1)^x0_i. That is exactly the Walsh-Hadamard
    // transform of f, which a fast transform computes for every c at once. The fewest clicks
    // is then the largest S(c).
    //
    // When 2^k is too big for one table, the top k - m bits of c are fixed for each pass, and
    // the rest are handled by a transform of size 2^m.
    static long[] transformSearch(long[] x0, long[][] basis, long cells) {
        int k = basis.length;
        int m = Math.min(k, tableBits());
        int passBits = k - m;
        int[] table = new int[1 << m];

        long bestValue = Long.MIN_VALUE;
        long bestPosition = 0;
        long bestCoefficients = 0;
        for (long fixedTop = 0; fixedTop < (1L << passBits); fixedTop++) {
            java.util.Arrays.fill(table, 0);
            summarise(table, x0, basis, cells, m, fixedTop);
            transform(table);
            long[] best = bestEntry(table, fixedTop, m);
            if (best[0] > bestValue || (best[0] == bestValue && best[1] < bestPosition)) {
                bestValue = best[0];
                bestPosition = best[1];
                bestCoefficients = best[2];
            }
        }
        return solutionWith(x0, basis, bestCoefficients);
    }

    // Adds each cell's (-1)^x0_i into the table entry for the low m bits of its pattern.
    // The high bits of the pattern are folded in using this pass's fixed top bits of c.
    private static void summarise(int[] table, long[] x0, long[][] basis, long cells, int m, long fixedTop) {
        int k = basis.length;
        long lowMask = (1L << m) - 1;
        long[] pattern = new long[64];
        int words = (int) ((cells + 63) / 64);
        for (int w = 0; w < words; w++) {
            // Patterns of the 64 cells in this word: bit j of pattern[t] = basis j clicks cell t
            for (int j = 0; j < k; j++) {
                long bits = basis[j][w];
                while (bits != 0) {
                    pattern[Long.numberOfTrailingZeros(bits)] |= 1L << j;
                    bits &= bits - 1;
                }
            }
            long clicked = x0[w];
            int cellsHere = (int) Math.min(64, cells - 64L * w);
            for (int t = 0; t < cellsHere; t++) {
                long p = pattern[t];
                pattern[t] = 0;
                int sign = ((clicked >>> t) & 1) == 0 ? 1 : -1;
                if ((Long.bitCount((p >>> m) & fixedTop) & 1) != 0) sign = -sign;
                table[(int) (p & lowMask)] += sign;
            }
        }
    }

    // Fast Walsh-Hadamard transform, in place. Afterwards entry c holds
    // sum over v of (old entry v) * (-1)^(number of bits set in both v and c).
    // Every value stays within +-cells, which fits in an int up to the largest board size.
    static void transform(int[] a) {
        int size = a.length;
        // First the layers that only mix entries within small blocks, a block per thread,
        // so each block stays in the processor's cache
        int block = Math.min(size, 1 << 13);
        IntStream blocks = IntStream.range(0, size / block);
        if (size >= (1 << 16)) blocks = blocks.parallel();
        blocks.forEach(b -> {
            int start = b * block;
            for (int h = 1; h < block; h <<= 1) {
                for (int i = start; i < start + block; i += 2 * h) {
                    for (int j = i; j < i + h; j++) {
                        int u = a[j];
                        int v = a[j + h];
                        a[j] = u + v;
                        a[j + h] = u - v;
                    }
                }
            }
        });
        // Then the wide layers, with the pairs shared out between threads
        for (int h = block; h < size; h <<= 1) {
            final int half = h;
            int pairs = size / 2;
            int piece = 1 << 14;
            IntStream.range(0, (pairs + piece - 1) / piece).parallel().forEach(q -> {
                int end = Math.min(pairs, (q + 1) * piece);
                for (int pair = q * piece; pair < end; pair++) {
                    int j = ((pair & ~(half - 1)) << 1) | (pair & (half - 1));
                    int u = a[j];
                    int v = a[j + half];
                    a[j] = u + v;
                    a[j + half] = u - v;
                }
            });
        }
    }

    // The largest entry of the table. Returns {value, its Gray-code position, its coefficients}.
    // Ties go to the earliest Gray-code position, as in the Gray-code search.
    private static long[] bestEntry(int[] table, long fixedTop, int m) {
        int size = table.length;
        int piece = 1 << 16;
        return IntStream.range(0, (size + piece - 1) / piece).parallel().mapToObj(q -> {
            long bestValue = Long.MIN_VALUE, bestPosition = 0, bestCoefficients = 0;
            int end = Math.min(size, (q + 1) * piece);
            for (int u = q * piece; u < end; u++) {
                long value = table[u];
                if (value < bestValue) continue;
                long coefficients = (fixedTop << m) | u;
                long position = grayPosition(coefficients);
                if (value > bestValue || position < bestPosition) {
                    bestValue = value;
                    bestPosition = position;
                    bestCoefficients = coefficients;
                }
            }
            return new long[] {bestValue, bestPosition, bestCoefficients};
        }).reduce((x, y) -> (y[0] > x[0] || (y[0] == x[0] && y[1] < x[1])) ? y : x).get();
    }

    // Position in the Gray-code sequence of the solution with these coefficients
    // (the inverse of p -> p XOR (p >> 1))
    private static long grayPosition(long coefficients) {
        long p = coefficients;
        p ^= p >>> 1;
        p ^= p >>> 2;
        p ^= p >>> 4;
        p ^= p >>> 8;
        p ^= p >>> 16;
        p ^= p >>> 32;
        return p;
    }

    // x0 XOR the basis vectors whose bits are set in 'coefficients'
    private static long[] solutionWith(long[] x0, long[][] basis, long coefficients) {
        long[] v = x0.clone();
        while (coefficients != 0) {
            long[] b = basis[Long.numberOfTrailingZeros(coefficients)];
            for (int w = 0; w < v.length; w++) v[w] ^= b[w];
            coefficients &= coefficients - 1;
        }
        return v;
    }

    // ===================== Gray-code search =====================
    //
    // The solutions are visited in Gray-code order, where each one differs from the one
    // before by a single basis vector, so stepping costs one XOR of the working vector.
    // To use every processor core, the sequence is cut into equal chunks that are searched
    // at the same time, each with its own working vector. The chunks are consecutive pieces
    // of the one sequence and ties go to the earliest position, so the answer is the same
    // however many cores there are.
    static long[] graySearch(long[] x0, long[][] basis) {
        int k = basis.length;
        // 2^chunkBits chunks. 256 is plenty to keep 16 threads evenly busy.
        int chunkBits = Math.min(k, 8);
        int stepBits = k - chunkBits;

        // Each chunk reports {fewest clicks it found, position in the sequence where it found them}
        long[] best = IntStream.range(0, 1 << chunkBits).parallel()
                .mapToObj(chunk -> searchChunk(x0, basis, (long) chunk << stepBits, 1L << stepBits))
                .reduce((a, b) -> (b[0] < a[0] || (b[0] == a[0] && b[1] < a[1])) ? b : a)
                .get();

        return solutionWith(x0, basis, best[1] ^ (best[1] >>> 1));
    }

    // Searches 'count' consecutive positions of the Gray-code sequence starting at 'start'.
    // Returns {fewest clicks found, the first position that has that few}.
    private static long[] searchChunk(long[] x0, long[][] basis, long start, long count) {
        long[] cur = solutionWith(x0, basis, start ^ (start >>> 1));
        int W = cur.length;
        int bestW = 0;
        for (long word : cur) bestW += Long.bitCount(word);
        long bestAt = start;

        for (long i = 1; i < count && bestW > 0; i++) {
            // Gray code: step i flips exactly one coefficient, the lowest set bit of i
            long[] b = basis[Long.numberOfTrailingZeros(i)];
            int wgt = 0;
            for (int w = 0; w < W; w++) {
                long v = cur[w] ^ b[w];
                cur[w] = v;
                wgt += Long.bitCount(v);
            }
            if (wgt < bestW) {
                bestW = wgt;
                bestAt = start + i;
            }
        }
        return new long[] {bestW, bestAt};
    }
}
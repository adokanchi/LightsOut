// Arithmetic on polynomials whose coefficients are 0 or 1, added and multiplied mod 2.
// A polynomial is stored as a long[] of bits: bit i (bit i % 64 of word i / 64) is the
// coefficient of x^i. Arrays may be longer than needed; the extra high bits are zero.
// Adding two polynomials is XOR, and multiplying by x^s is a shift left by s bits.
public final class BinaryPolynomial {
    private BinaryPolynomial() {
    }

    // Highest power of x with coefficient 1, or -1 for the zero polynomial
    public static int degree(long[] p) {
        return degreeFrom(p, p.length - 1);
    }

    // Like degree, but only looks at words up to and including 'topWord'
    private static int degreeFrom(long[] p, int topWord) {
        for (int w = Math.min(topWord, p.length - 1); w >= 0; w--) {
            if (p[w] != 0) return (w << 6) + 63 - Long.numberOfLeadingZeros(p[w]);
        }
        return -1;
    }

    public static boolean isZero(long[] p) {
        return degree(p) < 0;
    }

    // Number of longs needed for a polynomial of the given degree
    public static int wordsFor(int degree) {
        return Math.max(1, (degree >>> 6) + 1);
    }

    // a + b
    public static long[] add(long[] a, long[] b) {
        long[] r = new long[Math.max(a.length, b.length)];
        for (int i = 0; i < a.length; i++) r[i] = a[i];
        for (int i = 0; i < b.length; i++) r[i] ^= b[i];
        return r;
    }

    // dst += src * x^shift. dst must be long enough to hold the result.
    public static void addShifted(long[] dst, long[] src, int srcDegree, int shift) {
        if (srcDegree < 0) return;
        int wordShift = shift >>> 6;
        int bitShift = shift & 63;
        int srcWords = (srcDegree >>> 6) + 1;
        for (int i = 0; i < srcWords; i++) {
            long v = src[i];
            if (v == 0) continue;
            dst[i + wordShift] ^= v << bitShift;
            if (bitShift != 0 && i + wordShift + 1 < dst.length) dst[i + wordShift + 1] ^= v >>> (64 - bitShift);
        }
    }

    // a * b
    public static long[] multiply(long[] a, long[] b) {
        int da = degree(a);
        int db = degree(b);
        if (da < 0 || db < 0) return new long[1];
        long[] r = new long[wordsFor(da + db)];
        // For each x^i in b, add a * x^i
        for (int w = 0; w <= (db >>> 6); w++) {
            long bits = b[w];
            while (bits != 0) {
                int i = (w << 6) + Long.numberOfTrailingZeros(bits);
                addShifted(r, a, da, i);
                bits &= bits - 1;
            }
        }
        return r;
    }

    // Long division: returns {quotient, remainder} with a = quotient * b + remainder and
    // degree(remainder) < degree(b). b must not be zero.
    public static long[][] divide(long[] a, long[] b) {
        int db = degree(b);
        if (db < 0) throw new ArithmeticException("division by the zero polynomial");
        long[] remainder = a.clone();
        int dr = degree(remainder);
        long[] quotient = new long[wordsFor(Math.max(0, dr - db))];
        while (dr >= db) {
            int shift = dr - db;
            quotient[shift >>> 6] |= 1L << (shift & 63);
            addShifted(remainder, b, db, shift);
            // The top term is now gone; nothing above it was ever set
            dr = degreeFrom(remainder, dr >>> 6);
        }
        return new long[][] {quotient, remainder};
    }

    // a mod b
    public static long[] mod(long[] a, long[] b) {
        return divide(a, b)[1];
    }

    // Extended Euclidean algorithm. Returns {g, s} where g = gcd(a, m) and s * a = g (mod m).
    public static long[][] gcdAndMultiplier(long[] a, long[] m) {
        long[] r0 = a, r1 = m;
        long[] s0 = {1}, s1 = {0};
        // Invariant: s0 * a = r0 and s1 * a = r1 (mod m)
        while (!isZero(r1)) {
            long[][] qr = divide(r0, r1);
            r0 = r1;
            r1 = qr[1];
            long[] next = add(s0, multiply(qr[0], s1));
            s0 = s1;
            s1 = next;
        }
        return new long[][] {r0, mod(s0, m)};
    }
}
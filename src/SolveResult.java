public class SolveResult {
    final long[] particular;
    final long[][] nullBasis;
    final boolean solvable;

    SolveResult(long[] particular, long[][] nullBasis, boolean solvable) {
        this.particular = particular;
        this.nullBasis = nullBasis;
        this.solvable = solvable;
    }
}

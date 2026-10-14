import java.util.Arrays;

// Times the two fewest-clicks search methods (Gray-code and Walsh-Hadamard transform) on a
// set of boards and prints the results, so the speed figures in SolutionSearch can be set
// from real measurements. Not used by the game itself: run this class's main method on its own.
// Takes a few minutes. The window is not opened.
public class SearchBenchmark {
    // Board sizes to time. A mix of small boards with high nullity and large boards with low
    // nullity, which is where the choice between the two methods matters.
    private static final int[] SIZES = {
            62, 64, 154, 194, 47, 39,            // small boards, nullity 24 to 32
            1739, 2834,                          // medium boards, nullity 22 and 28
            6004, 6000, 6029, 6034, 6019, 6059   // large boards, nullity 4 to 22
    };

    // A method is skipped on a board if a deliberately pessimistic guess says it would take
    // longer than this
    private static final double SKIP_IF_LONGER_THAN_SECONDS = 90;

    public static void main(String[] args) {
        System.out.println("Processor threads: " + Runtime.getRuntime().availableProcessors()
                + ", Java max memory: " + Runtime.getRuntime().maxMemory() / 1_000_000 + " MB"
                + ", transform table: 2^" + SolutionSearch.tableBits() + " entries");
        System.out.println("Warming up...");
        warmUp();

        System.out.println();
        System.out.printf("%7s %8s %10s %12s %12s %12s %14s%n",
                "n", "nullity", "Gray (s)", "transform (s)", "estimate (s)", "picks", "same answer");
        for (int n : SIZES) {
            ChaseSolver solver = new ChaseSolver(n);
            int k = solver.getNullity();
            long cells = (long) n * n;
            Board board = new Board(n);
            board.scramble();
            SolveResult result = solver.solve(board.getLitRows());
            if (result.nullBasis.length != k || k == 0) {
                System.out.printf("%7d %8d   (not searched: over the time or memory limit, or nullity 0)%n", n, k);
                continue;
            }

            long words = (cells + 63) / 64;
            double grayGuess = Math.pow(2, k) * words / 1.5e9;
            double transformGuess = Math.pow(2, Math.max(0, k - SolutionSearch.tableBits()))
                    * (cells / 3e7 + k * Math.pow(2, Math.min(k, SolutionSearch.tableBits())) / 5e8);

            long[] grayAnswer = null;
            long[] transformAnswer = null;
            String grayTime = "skipped";
            String transformTime = "skipped";
            if (grayGuess <= SKIP_IF_LONGER_THAN_SECONDS) {
                long start = System.nanoTime();
                grayAnswer = SolutionSearch.graySearch(result.particular, result.nullBasis);
                grayTime = String.format("%.3f", (System.nanoTime() - start) / 1e9);
            }
            if (transformGuess <= SKIP_IF_LONGER_THAN_SECONDS) {
                long start = System.nanoTime();
                transformAnswer = SolutionSearch.transformSearch(result.particular, result.nullBasis, cells);
                transformTime = String.format("%.3f", (System.nanoTime() - start) / 1e9);
            }
            String same = (grayAnswer == null || transformAnswer == null) ? "-"
                    : String.valueOf(Arrays.equals(grayAnswer, transformAnswer));
            String picks = SolutionSearch.picksTransform(k, cells) ? "transform" : "Gray";
            System.out.printf("%7d %8d %10s %12s %12.3f %12s %14s%n",
                    n, k, grayTime, transformTime, solver.estimatedSearchSeconds(), picks, same);
        }
        System.out.println();
        System.out.println("Done. Copy everything above to share the results.");
    }

    // Runs both methods a few times on small boards first, so Java has compiled them to fast
    // code before anything is timed
    private static void warmUp() {
        for (int n : new int[] {30, 62}) {
            ChaseSolver solver = new ChaseSolver(n);
            Board board = new Board(n);
            board.scramble();
            SolveResult result = solver.solve(board.getLitRows());
            for (int i = 0; i < 3; i++) {
                SolutionSearch.graySearch(result.particular, result.nullBasis);
                SolutionSearch.transformSearch(result.particular, result.nullBasis, (long) n * n);
            }
        }
    }
}
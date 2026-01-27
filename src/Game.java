import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

public class Game implements MouseListener, KeyListener, ActionListener {
    private GameView window;

    private Board board;

    // rowsInput is the string the user inputs to change the row number
    private String rowsInput;

    public Game() {
        rowsInput = "";
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(int numRows) {
        board = new Board(numRows);
    }

    public int getNumRows() {
        return board.getBoard().length;
    }

    public String getRowsInput() {
        return rowsInput;
    }

    // Executes the animated solve option
    public void solveAnim() {
        clock.start();
    }

    // Compute which cells need to be clicked for an optimal solution, mark with hint border
    // Generates a "good" solution when the board structure doesn't lead to crazy complications
    public void solvePerfect() {
        board.clearHints();
        int n = board.getBoard().length;
        boolean[][] on = new boolean[n][n];
        for (int r = 0; r < n; r++)
            for (int c = 0; c < n; c++)
                on[r][c] = board.getBoard()[r][c].isOn();

        long[][] aug = buildAugmentedSystem(on);
        int N = n * n;

        SolveResult res = solveAndNullspace(aug, N);
        if (!res.solvable) {
            // no solution
            return;
        }

        long[] best = minimizePopcount(res.particular, res.nullBasis, N, 24);

        // write hints
        for (int i = 0; i < N; i++) {
            int r = i / n, c = i % n;
            board.getBoard()[r][c].setHint(getBit(best, i));
        }
    }

    private SolveResult solveAndNullspace(long[][] aug, int N) {
        // aug: N rows, bits 0..N-1 are A, bit N is rhs
        int cols = N + 1;
        int W = (cols + 63) >>> 6;

        int[] where = new int[N];
        java.util.Arrays.fill(where, -1);

        int row = 0;
        for (int col = 0; col < N && row < N; col++) {
            int pivot = -1;
            for (int r = row; r < N; r++) {
                if (getBit(aug[r], col)) { pivot = r; break; }
            }
            if (pivot == -1) continue;

            if (pivot != row) {
                long[] tmp = aug[pivot];
                aug[pivot] = aug[row];
                aug[row] = tmp;
            }

            where[col] = row;

            // eliminate all other 1s in this pivot column (Gauss–Jordan)
            for (int r2 = 0; r2 < N; r2++) {
                if (r2 != row && getBit(aug[r2], col)) xorRow(aug[r2], aug[row]);
            }


            row++;
        }

        // inconsistency check
        for (int r = 0; r < N; r++) {
            boolean anyLeft = false;
            for (int c = 0; c < N; c++) {
                if (getBit(aug[r], c)) { anyLeft = true; break; }
            }
            if (!anyLeft && getBit(aug[r], N)) {
                return new SolveResult(null, null, false);
            }
        }

        // back-sub to get one particular solution (free vars = 0)
        long[] x0 = new long[W];
        for (int col = N - 1; col >= 0; col--) {
            int r = where[col];
            if (r == -1) continue; // free var remains 0

            boolean rhs = getBit(aug[r], N);
            for (int c2 = col + 1; c2 < N; c2++) {
                if (getBit(aug[r], c2) && getBit(x0, c2)) rhs = !rhs;
            }
            if (rhs) setBit(x0, col);
        }

        // build nullspace basis: one vector per free column f
        java.util.ArrayList<long[]> basis = new java.util.ArrayList<>();
        for (int f = 0; f < N; f++) {
            if (where[f] != -1) continue; // pivot column, not free

            long[] v = new long[W];
            setBit(v, f); // free var = 1

            // for each pivot column p, set v[p] = A[row(p)][f]
            for (int p = 0; p < N; p++) {
                int pr = where[p];
                if (pr == -1) continue;
                if (getBit(aug[pr], f)) setBit(v, p);
            }
            basis.add(v);
        }

        long[][] nullBasis = basis.toArray(new long[0][]);
        return new SolveResult(x0, nullBasis, true);
    }

    private long[] minimizePopcount(long[] x0, long[][] basis, int N, int maxK) {
        int k = basis.length;
        if (k == 0) {
            return x0;
        }

        if (k > maxK) {
            // too many free vars to brute force exactly
            window.setErrString("Too complicated for optimal solution. Showing basic solution.");
            return x0;
        }

        long[] best = x0.clone();
        int bestW = popcount(best);

        int total = 1 << k;
        long[] cur = new long[best.length];

        for (int mask = 0; mask < total; mask++) {
            // cur = x0 XOR (xor of selected basis vectors)
            System.arraycopy(x0, 0, cur, 0, cur.length);
            int m = mask;
            int idx = 0;
            while (m != 0) {
                if ((m & 1) != 0) {
                    for (int w = 0; w < cur.length; w++) cur[w] ^= basis[idx][w];
                }
                idx++;
                m >>>= 1;
            }
            int wgt = popcount(cur);
            if (wgt < bestW) {
                bestW = wgt;
                best = cur.clone();
                if (bestW == 0) break;
            }
        }
        return best;
    }

    private long[][] buildAugmentedSystem(boolean[][] on) {
        int n = on.length;
        int N = n * n;
        int cols = N + 1;
        int W = (cols + 63) >>> 6;

        long[][] aug = new long[N][W];

        for (int r = 0; r < n; r++) {
            for (int c = 0; c < n; c++) {
                int cell = r * n + c;

                // presses that affect this cell:
                setBit(aug[cell], cell);
                if (c > 0) setBit(aug[cell], cell - 1);
                if (c < n - 1) setBit(aug[cell], cell + 1);
                if (r > 0) setBit(aug[cell], cell - n);
                if (r < n - 1) setBit(aug[cell], cell + n);

                // RHS = state[cell]
                if (on[r][c]) setBit(aug[cell], N);
            }
        }
        return aug;
    }

    private int popcount(long[] v) {
        int s = 0;
        for (long x : v) s += Long.bitCount(x);
        return s;
    }

    // Gives user a hint, highlighting squares to click in red
    public void getHints() {
        board.clearHints();
        // Hints for propagation
        boolean hintGiven = false;
        for (int i = 0; i < getNumRows()-1; i++) {
            for (int j = 0; j < getNumRows(); j++) {
                if (board.getBoard()[j][i].isOn()) {
                    board.getBoard()[j][i+1].setHint(true);
                    hintGiven = true;
                }
            }
            if (hintGiven) {
                return;
            }
        }

        if (board.isSolved()) {
            return;
        }
        // If all rows are solved other than the last

        // Figures out how top-row clicks impact the bottom row after propagation by
        // testing every possibility on a separate board and storing them in topRowMatrix
        Board b2 = new Board(getNumRows());
        int[][] topRowMatrix = new int[getNumRows()][getNumRows()];
        for (int i = 0; i < getNumRows(); i++) {
            // Click the first-row cell at position i
            b2.toggleAllAdj(i,0);
            b2.propagate();
            // Store the changes in topRowMatrix as 1s and the constants as 0
            for (int j = 0; j < getNumRows(); j++) {
                topRowMatrix[i][j] = b2.getBoard()[j][getNumRows() - 1].isOn() ? 1 : 0;
            }
            b2.solve();
        }

        // botRow is the real board's bottom row
        int[] botRow = new int[getNumRows()];
        for (int i = 0; i < getNumRows(); i++) {
            botRow[i] = board.getBoard()[i][getNumRows() - 1].isOn() ? 1 : 0;
        }

        // Find a linear combination of top-row moves that would convert the bottom row to solved
        int[] linCombs = solveLinCombMod2(topRowMatrix, botRow);

        // Hint squares
        for (int i = 0; i < linCombs.length; i++) {
            if (linCombs[i] == 1) {
                board.getBoard()[i][0].setHint(true);
            }
        }
    }

    // Performs Gaussian Elimination on arrs to find x satisfying arrs*x=target, returns x
    public int[] solveLinCombMod2(int[][] arrs, int[] target) {
        int n = arrs.length;
        if (n == 0) return new int[0];

        int cols = n + 1;
        int W = (cols + 63) >>> 6;

        long[][] mat = new long[n][W];
        for (int r = 0; r < n; r++) {
            // M[r][c] = arrs[c][r]
            for (int c = 0; c < n; c++) {
                if ((arrs[c][r] & 1) != 0) setBit(mat[r], c);
            }
            if ((target[r] & 1) != 0) setBit(mat[r], n);
        }

        // where = pivot locations
        int[] where = new int[n];
        Arrays.fill(where, -1);

        int row = 0;
        for (int col = 0; col < n && row < n; col++) {
            // Find pivot
            int pivot = -1;
            for (int r = row; r < n; r++) {
                if (getBit(mat[r], col)) { pivot = r; break; }
            }
            if (pivot == -1) continue;

            // Swap rows
            if (pivot != row) {
                long[] tmp = mat[pivot];
                mat[pivot] = mat[row];
                mat[row] = tmp;
            }
            where[col] = row;

            // Eliminate below
            for (int r = row + 1; r < n; r++) {
                if (getBit(mat[r], col)) xorRow(mat[r], mat[row]);
            }

            row++;
        }

        // Back substitution (free variables set to 0)
        int[] x = new int[n];
        for (int col = n - 1; col >= 0; col--) {
            int r = where[col];
            if (r == -1) {
                x[col] = 0;
                continue;
            }

            boolean rhs = getBit(mat[r], n);
            // compute dot product of row with current x for columns > col
            for (int c2 = col + 1; c2 < n; c2++) {
                if (getBit(mat[r], c2) && x[c2] == 1) rhs = !rhs;
            }
            x[col] = rhs ? 1 : 0;
        }

        return x;
    }

    private static void xorRow(long[] dst, long[] src) {
        for (int i = 0; i < dst.length; i++) dst[i] ^= src[i];
    }

    private static boolean getBit(long[] row, int bit) {
        return ((row[bit >>> 6] >>> (bit & 63)) & 1L) != 0;
    }

    private static void setBit(long[] row, int bit) {
        row[bit >>> 6] |= 1L << (bit & 63);
    }

    // Turns x/y coordinates of a click into info on which cell was clicked
    public int[] coordsToIndices(int xCoord, int yCoord) {
        int boardSize = board.getCellSize() * getNumRows();
        // xTL =  top left corner x-coordinate, yTL = top left corner y-coordinate
        int xTL = (GameView.WINDOW_WIDTH - boardSize) / 2;
        int yTL = (GameView.WINDOW_HEIGHT - boardSize) / 2;
        // Number of cells = (distance to top left corner) / (distance per cell)
        int xCellIndex = (xCoord - xTL) / board.getCellSize();
        int yCellIndex = (yCoord - yTL) / board.getCellSize();
        return new int[] {xCellIndex, yCellIndex};
    }

    public void runGame() {
        window = new GameView(this);
        this.window.addMouseListener(this);
        this.window.addKeyListener(this);
        Toolkit.getDefaultToolkit().sync();
    }

    public void mouseClicked(MouseEvent e) {
        if (board == null) {
            return;
        }
        if (window != null) window.clearErrString();
        int x = e.getX();
        int y = e.getY();
        int[] coords = coordsToIndices(x,y);
        int row = coords[0];
        int col = coords[1];

        // Attempts to toggle the cell. Runs code inside if outside the array
        if (!board.toggleAllAdj(row,col)) {
            final int BUTTON_OFFSET = 50;
            final int BUTTON_SIZE = 100;

            // Top left = hint
            if (x < BUTTON_SIZE+BUTTON_OFFSET && y < BUTTON_SIZE+BUTTON_OFFSET) {
                getHints();
            }
            // Bottom right = propagate
            if (x > GameView.WINDOW_WIDTH-BUTTON_SIZE-BUTTON_OFFSET && y > GameView.WINDOW_HEIGHT-BUTTON_SIZE-BUTTON_OFFSET) {
                board.propagate();
            }
            // Bottom left = scramble
            if (x < BUTTON_SIZE+BUTTON_OFFSET && y > GameView.WINDOW_HEIGHT-BUTTON_SIZE-BUTTON_OFFSET) {
                board.scramble();
            }
            // Top right = solve
            if (x > GameView.WINDOW_WIDTH-BUTTON_SIZE-BUTTON_OFFSET && y < BUTTON_SIZE+BUTTON_OFFSET) {
                solveAnim();
            }
            // Middle right = perfect solve
            if (x > GameView.WINDOW_WIDTH-BUTTON_SIZE-BUTTON_OFFSET && y > (GameView.WINDOW_HEIGHT - BUTTON_SIZE) / 2 && y < (GameView.WINDOW_HEIGHT + BUTTON_SIZE) / 2) {
                solvePerfect();
            }
        }
        window.repaint();
    }
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void keyTyped(KeyEvent e) {
        // If enter is pressed, change board size
        if (e.getKeyChar() == (KeyEvent.VK_ENTER)) {
            if (rowsInput.isEmpty()) return;
            setBoard(Integer.parseInt(rowsInput));
            board.scramble();
            rowsInput = "";
            window.repaint();
            return;
        }
        // If escape is pressed, clear the input field
        if (e.getKeyChar() == (KeyEvent.VK_ESCAPE)) {
            rowsInput = "";
            window.repaint();
            return;
        }
        // If a number is pressed, add it to the input field
        if (Character.isDigit(e.getKeyChar())) {
            rowsInput += e.getKeyChar();
            window.repaint();
            return;
        }

        // If backspace is pressed, delete last character from the input field
        if (e.getKeyChar() == KeyEvent.VK_BACK_SPACE) {
            if (rowsInput.isEmpty()) return;
            rowsInput = rowsInput.substring(0, rowsInput.length() - 1);
            window.repaint();
        }
    }
    public void keyReleased(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {}

    Timer clock = new Timer(500, this);
    public void actionPerformed(ActionEvent e) {
        // Click the first hint square
        // If anything is clicked, return immediately
        for (int i = 0; i < getNumRows(); i++) {
            for (int j = 0; j < getNumRows(); j++) {
                if (board.getBoard()[j][i].isHint()) {
                    board.toggleAllAdj(j,i);
                    window.repaint();
                    return;
                }
            }
        }

        // If nothing was clicked because the board is solved, stop calling actionPerformed
        if (board.isSolved()) {
            clock.stop();
        }

        // If nothing was clicked but the board still isn't solved, there
        // are no remaining hint squares, so call getHints() to fix that
        getHints();
        window.repaint();
    }

    public static void main(String[] args) {
        Game game = new Game();
        game.runGame();
    }
}

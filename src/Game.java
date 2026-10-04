import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;

public class Game implements MouseListener, KeyListener, ActionListener {
    private GameView window;
    private Board board;
    private ChaseSolver solver;
    public static final int MAX_BOARD_SIZE = 8000;

    // rowsInput is the string the user inputs to change the row number
    private String rowsInput;

    public Game() {
        rowsInput = "";
    }

    public Board getBoard() {
        return board;
    }

    public void setBoard(int numRows) {
        // Build both before replacing anything, so a failure leaves the current board intact
        Board newBoard = new Board(numRows);
        ChaseSolver newSolver = new ChaseSolver(numRows);
        board = newBoard;
        solver = newSolver;
    }

    private int parseBoardSize(String text) {
        int size;
        try {
            size = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
        if (size < 1 || size > MAX_BOARD_SIZE) return -1;
        return size;
    }

    public int getNumRows() {
        return board.getBoard().length;
    }

    public String getRowsInput() {
        return rowsInput;
    }

    // Executes the animated solve option
    public void solveAnim() {
        if (getNumRows() < 10) slowClock.start();
        else if (getNumRows() < 25) clock.start();
        else if (getNumRows() < 50) fastClock.start();
        else if (getNumRows() < 300) veryFastClock.start();
        else board.clickHints();
    }

    // Compute which cells need to be clicked for an optimal solution, mark with hint border
    // Generates a "good" solution when the board structure doesn't lead to crazy complications
    public void solvePerfect() {
        board.clearHints();
        int n = board.getBoard().length;
        boolean[][] on = new boolean[n][n];
        for (int row = 0; row < n; row++) {
            for (int col = 0; col < n; col++) {
                on[col][row] = board.getBoard()[col][row].isOn();
            }
        }

        int N = n * n;

        SolveResult res = solver.solve(on);
        if (!res.solvable) {
            // no solution
            return;
        }

        long[] best = minimizePopcount(res.particular, res.nullBasis);

        // write hints
        for (int i = 0; i < N; i++) {
            int col = i / n;
            int row = i % n;
            board.getBoard()[col][row].setHint(getBit(best, i));
        }
    }

    private long[] minimizePopcount(long[] x0, long[][] basis) {
        int k = basis.length;
        window.setStatusText("Nullity " + k);
        if (k == 0) {
            return x0;
        }

        final int maxK = 28;
        if (k > maxK) {
            window.setStatusText("Nullity " + k + " - too complicated for optimal solution. Showing basic solution.");
            return x0;
        }

        long[] cur = x0.clone();
        long[] best = x0.clone();
        int bestW = popcount(best);
        int W = cur.length;

        long total = 1L << k;
        for (long i = 1; i < total && bestW > 0; i++) {
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
                System.arraycopy(cur, 0, best, 0, W);
            }
        }
        return best;
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
        for (int i = 0; i < getNumRows() - 1; i++) {
            for (int j = 0; j < getNumRows(); j++) {
                if (board.getBoard()[j][i].isOn()) {
                    board.getBoard()[j][i + 1].setHint(true);
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
            b2.toggleAllAdj(i, 0);
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
                if (getBit(mat[r], col)) {
                    pivot = r;
                    break;
                }
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
        int boardSize = board.getPixelSize();
        // xTL =  top left corner x-coordinate, yTL = top left corner y-coordinate
        int xTL = (window.getPanelWidth() - boardSize) / 2;
        int yTL = (window.getPanelHeight() - boardSize) / 2;
        if (xCoord < xTL || yCoord < yTL) {
            return new int[] {-1, -1};
        }
        // Number of cells = (distance to top left corner) / (distance per cell)
        int xCellIndex = (int) ((long) (xCoord - xTL) * getNumRows() / boardSize);
        int yCellIndex = (int) ((long) (yCoord - yTL) * getNumRows() / boardSize);
        return new int[] {xCellIndex, yCellIndex};
    }

    public void stopClocks() {
        slowClock.stop();
        clock.stop();
        fastClock.stop();
        veryFastClock.stop();
    }

    public void runGame() {
        window = new GameView(this);
        this.window.addPanelMouseListener(this);
        this.window.addKeyListener(this);
        Toolkit.getDefaultToolkit().sync();
    }

    public void mouseClicked(MouseEvent e) {
        if (board == null) {
            return;
        }
        if (window != null) window.clearStatusText();
        int x = e.getX();
        int y = e.getY();
        int[] coords = coordsToIndices(x, y);
        int col = coords[0];
        int row = coords[1];

        // Attempts to toggle the cell. Runs code inside if outside the array
        if (!board.toggleAllAdj(col, row)) {
            final int BUTTON_OFFSET = 50;
            final int BUTTON_SIZE = 100;

            int w = window.getPanelWidth();
            int h = window.getPanelHeight();

            // Top left = hint
            if (x < BUTTON_SIZE + BUTTON_OFFSET && y < BUTTON_SIZE + BUTTON_OFFSET) {
                getHints();
            }
            // Bottom right = propagate
            if (x > w - BUTTON_SIZE - BUTTON_OFFSET && y > h - BUTTON_SIZE - BUTTON_OFFSET) {
                board.propagate();
            }
            // Bottom left = scramble
            if (x < BUTTON_SIZE + BUTTON_OFFSET && y > h - BUTTON_SIZE - BUTTON_OFFSET) {
                board.scramble();
            }
            // Top right = solve
            if (x > w - BUTTON_SIZE - BUTTON_OFFSET && y < BUTTON_SIZE + BUTTON_OFFSET) {
                solveAnim();
            }
            // Middle right = perfect solve
            if (x > w - BUTTON_SIZE - BUTTON_OFFSET && y > (h - BUTTON_SIZE) / 2 && y < (h + BUTTON_SIZE) / 2) {
                solvePerfect();
            }
        } else {
            if (board.getBoard()[col][row].isHint()) {
                board.getBoard()[col][row].setHint(false);
            } else {
                board.clearHints();
                stopClocks();
            }
        }
        window.repaint();
    }
    public void mousePressed(MouseEvent e) { }
    public void mouseReleased(MouseEvent e) { }
    public void mouseEntered(MouseEvent e) { }
    public void mouseExited(MouseEvent e) { }
    public void keyTyped(KeyEvent e) {
        // If enter is pressed, change board size
        if (e.getKeyChar() == (KeyEvent.VK_ENTER)) {
            if (rowsInput.isEmpty()) return;
            int size = parseBoardSize(rowsInput);
            rowsInput = "";
            if (size == -1) {
                window.setStatusText("Board size must be a whole number from 1 to " + MAX_BOARD_SIZE + ".");
                window.repaint();
                return;
            }
            stopClocks();

            try {
                setBoard(size);
                board.scramble();
                window.clearStatusText();
            } catch (OutOfMemoryError outOfMemory) {
                window.setStatusText("Not enough memory for a " + size + "x" + size + " board.");
            }
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
    public void keyReleased(KeyEvent e) { }
    public void keyPressed(KeyEvent e) { }

    Timer slowClock = new Timer(500, this);
    Timer clock = new Timer(200, this);
    Timer fastClock = new Timer(50, this);
    Timer veryFastClock = new Timer(10, this);
    public void actionPerformed(ActionEvent e) {
        // Click the first hint square
        // If anything is clicked, return immediately
        for (int i = 0; i < getNumRows(); i++) {
            for (int j = 0; j < getNumRows(); j++) {
                if (board.getBoard()[j][i].isHint()) {
                    board.toggleAllAdj(j, i);
                    board.getBoard()[j][i].setHint(false);
                    window.repaint();
                    return;
                }
            }
        }

        // If nothing was clicked because the board is solved, stop calling actionPerformed
        if (board.isSolved()) {
            stopClocks();
        }

        // If nothing was clicked but the board still isn't solved, there
        // are no remaining hint squares, so call getHints() to fix that
        getHints();
        window.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Game game = new Game();
            game.runGame();
        });
    }
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Game implements MouseListener, KeyListener, ActionListener {
    private GameView window;
    private Board board;
    private ChaseSolver solver;

    // rowsInput is the string the user inputs to change the row number
    private String rowsInput;

    public Game() {
        rowsInput = "";
    }

    public Board getBoard() {
        return board;
    }


    // Largest board size that can be typed in.
    public static final int MAX_BOARD_SIZE = 20000;

    public void setBoard(int numRows) {
        // Build both before replacing anything, so a failure leaves the current board intact
        Board newBoard = new Board(numRows);
        ChaseSolver newSolver = new ChaseSolver(numRows);
        board = newBoard;
        solver = newSolver;
    }

    // Turns the typed text into a board size, or returns -1 if it is not a whole number
    // from 1 to MAX_BOARD_SIZE
    private int parseBoardSize(String text) {
        int size;
        try {
            size = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            // Not a number that fits in an int
            return -1;
        }
        if (size < 1 || size > MAX_BOARD_SIZE) return -1;
        return size;
    }

    public int getNumRows() {
        return board.getSize();
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
        // The solver reads the board's own rows of lit cells directly, so nothing is copied
        SolveResult res = solver.solve(board.getLitRows());
        if (!res.solvable) {
            // no solution
            return;
        }

        long[] best = minimizePopcount(res.particular, res.nullBasis);

        // write hints
        board.setHints(best);
    }

    private long[] minimizePopcount(long[] x0, long[][] basis) {
        int k = solver.getNullity();
        window.setStatusText("Nullity " + k);
        if (k == 0) {
            return x0;
        }

        // Above the cap the solver does not build the basis at all, so it must not be used here
        if (k > ChaseSolver.MAX_SEARCH_NULLITY) {
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
        // Hints for propagation: hint underneath the lit cells of the first row that has any
        int firstLitRow = board.firstLitRow();
        if (firstLitRow == -1) {
            // Already solved
            return;
        }
        if (firstLitRow < getNumRows() - 1) {
            board.hintBelowRow(firstLitRow);
            return;
        }

        // All rows are solved other than the last
        boolean[] botRow = new boolean[getNumRows()];
        for (int i = 0; i < getNumRows(); i++) {
            botRow[i] = board.isOn(i, getNumRows() - 1);
        }

        // Find the top-row clicks that, after propagating, would leave the bottom row solved.
        boolean[] topRowClicks = solver.firstLineClicks(botRow);
        if (topRowClicks == null) {
            // No solution exists from this position
            return;
        }

        // Hint squares
        for (int i = 0; i < topRowClicks.length; i++) {
            if (topRowClicks[i]) {
                board.setHint(i, 0, true);
            }
        }
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
            if (board.isHint(col, row)) {
                board.setHint(col, row, false);
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
                if (board.isHint(j, i)) {
                    board.toggleAllAdj(j, i);
                    board.setHint(j, i, false);
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
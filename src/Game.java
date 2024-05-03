import java.awt.event.*;
import java.util.Arrays;

import static java.lang.Thread.sleep;

public class Game implements MouseListener, KeyListener {
    private GameView window;
    private Board board;
    private String rowsInput;
    private final int[][][] topRows = {
            {{1}}, // 1x1

            {{1,0},{0,1}}, // 2x2

            {{0,1,1},{1,1,1},{1,1,0}}, // 3x3

            {{0,0,0,0},{0,0,0,0},{0,0,0,0},{0,0,0,0}}, // 4x4

            {{0,1,1,0,1},
            {1,1,1,0,0},
            {1,1,0,1,1},
            {0,0,1,1,1},
            {1,0,1,1,0}}, // 5x5

            {{1,0,0,0,1,0},
            {0,1,0,1,0,1},
            {0,0,0,0,1,0},
            {0,1,0,0,0,0},
            {1,0,1,0,1,0,},
            {0,1,0,0,0,1}}, // 6x6

            {{1,0,1,1,0,1,1},
            {0,0,1,1,0,1,1},
            {1,1,0,0,0,0,0},
            {1,1,0,1,0,1,1},
            {0,0,0,0,0,1,1},
            {1,1,0,1,1,0,0},
            {1,1,0,1,1,0,1}}, // 7x7

            {{0,0,0,0,1,0,0,0},
            {0,0,0,1,0,1,0,0},
            {0,0,1,0,1,0,1,0},
            {0,1,0,1,0,1,0,1},
            {1,0,1,0,1,0,1,0},
            {0,1,0,1,0,1,0,0},
            {0,0,1,0,1,0,0,0},
            {0,0,0,1,0,0,0,0}}, // 8x8

            {{1,0,1,0,1,0,1,0,1},
            {0,0,0,0,0,0,0,0,0},
            {1,0,1,0,1,0,1,0,1},
            {0,0,0,0,0,0,0,0,0},
            {1,0,1,0,1,0,1,0,1},
            {0,0,0,0,0,0,0,0,0},
            {1,0,1,0,1,0,1,0,1},
            {0,0,0,0,0,0,0,0,0},
            {1,0,1,0,1,0,1,0,1}}, // 9x9

            {{1,0,1,0,0,0,1,0,1,0},
            {0,0,0,1,0,1,0,0,0,1},
            {1,0,0,0,0,0,0,0,0,0},
            {0,1,0,1,0,1,0,0,0,1},
            {0,0,0,0,0,0,1,0,1,0},
            {0,1,0,1,0,0,0,0,0,0},
            {1,0,0,0,1,0,1,0,1,0},
            {0,0,0,0,0,0,0,0,0,1},
            {1,0,0,0,1,0,1,0,0,0},
            {0,1,0,1,0,0,0,1,0,1}}, // 10x10

            {{0,0,0,1,1,1,0,0,0,1,1},
            {0,0,1,1,0,1,1,0,1,1,1},
            {0,1,1,0,1,0,1,0,1,1,0},
            {1,1,0,1,0,1,1,0,0,0,0},
            {1,0,1,0,1,1,0,1,1,1,0},
            {1,1,0,1,1,0,1,1,0,1,1},
            {0,1,1,1,0,1,1,0,1,0,1},
            {0,0,0,0,1,1,0,1,0,1,1},
            {0,1,1,0,1,0,1,0,1,1,0},
            {1,1,1,0,1,1,0,1,1,0,0},
            {1,1,0,0,0,1,1,1,0,0,0}}, // 11x11

            {{1,0,0,0,1,0,0,0,0,0,0,0},
            {0,1,0,1,0,1,0,0,0,0,0,0},
            {0,0,0,0,1,0,1,0,0,0,0,0},
            {0,1,0,0,0,1,0,1,0,0,0,0},
            {1,0,1,0,0,0,1,0,1,0,0,0},
            {0,1,0,1,0,0,0,1,0,1,0,0},
            {0,0,1,0,1,0,0,0,1,0,1,0},
            {0,0,0,1,0,1,0,0,0,1,0,1},
            {0,0,0,0,1,0,1,0,0,0,1,0},
            {0,0,0,0,0,1,0,1,0,0,0,0},
            {0,0,0,0,0,0,1,0,1,0,1,0},
            {0,0,0,0,0,0,0,1,0,0,0,1}} // 12x12
    };

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
        return board.getNumRows();
    }

    public String getRowsInput() {
        return rowsInput;
    }

    // Gives a random solvable scramble by starting with a solved board and
    // either clicking or not clicking on each square with a 50/50 chance
    public void scramble() {
        if (board == null) {
            return;
        }
        for (int i = 0; i < getNumRows(); i++) {
            for (int j = 0; j < getNumRows(); j++) {
                if ((int) (Math.random() + 0.5) == 1) {
                    board.toggleAllAdj(i,j);
                }
            }
        }
    }

    public void solve() {
        board.solve();
    }

    public void clickHintSquares() {
        for (int i = 0; i < getNumRows(); i++) {
            for (int j = 0; j < getNumRows(); j++) {
                if (board.getBoard()[i][j].isHint()) {
                    board.toggleAllAdj(i,j);
                    window.repaint();
                    try {
                        sleep(300);
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public void solve2() {
        while (!board.isSolved()) {
            getHints();
            window.repaint();
            try {
                sleep(300);
            } catch (Exception ignored) {}
            clickHintSquares();
            window.repaint();
        }
    }

    // Goes row by row clicking below every white square so that only the last row is unsolved
    public void propagate() {
        for (int i = 0; i < getNumRows()-1; i++) {
            for (int j = 0; j < getNumRows(); j++) {
                if (board.getBoard()[j][i].isOn()) {
                    board.toggleAllAdj(j,i+1);
                }
            }
        }
    }

    // Gives the user a hint, highlighting squares in red
    public void getHints()  {
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

        // Hints for setting up final propagation
        if (board.getNumRows() > 12) {return;} // Top row hints only exist for board sizes up to 12x12

        int[] botRow = new int[getNumRows()];
        for (int i = 0; i < getNumRows(); i++) {
            botRow[i] = board.getBoard()[i][getNumRows() - 1].isOn() ? 1 : 0;
        }
        int[] linCombs = findLinCombs(topRows[getNumRows() - 1], botRow);
        for (int i = 0; i < linCombs.length; i++) {
            if (linCombs[i] == 1) {
                board.getBoard()[i][0].setHint(true);
            }
        }
    }

    public void getHints2() {
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

        Board b2 = new Board(getNumRows());
        int[][] topRowMatrix = new int[getNumRows()][getNumRows()];
        for (int i = 0; i < getNumRows(); i++) {
            b2.toggleAllAdj(i,0);
            b2.propagate();
            for (int j = 0; j < getNumRows(); j++) {
                topRowMatrix[i][j] = b2.getBoard()[j][getNumRows() - 1].isOn() ? 1 : 0;
            }
            b2.solve();
        }

        int[] botRow = new int[getNumRows()];
        for (int i = 0; i < getNumRows(); i++) {
            botRow[i] = board.getBoard()[i][getNumRows() - 1].isOn() ? 1 : 0;
        }
        int[] linCombs = findLinCombs(topRowMatrix, botRow);
        for (int i = 0; i < linCombs.length; i++) {
            if (linCombs[i] == 1) {
                board.getBoard()[i][0].setHint(true);
            }
        }
    }

    // Treating arrs like a matrix and target like a vector, solves the matrix equation arrs*x=target
    // Or in other words, finds a linear combination of columns of arrs that makes target
    public int[] findLinCombs(int[][] arrs, int[] target) {
        for (int i = 0; i < Math.pow(2, arrs.length); i++) { // for every possible combination of top row clicks
            // j is a copy of i, so we can modify it without changing the loop
            int j = i;
            int count = 0;
            int[] linComb = new int[arrs.length];

            // Compute the linear combination arrs*j
            while (j > 0) {
                if (j % 2 == 1) {
                    j -= 1;
                    linComb = addVectors(linComb, arrs[count]);
                }
                count++;
                j /= 2;
            }

            // Reduces each entry of the combination mod 2, as in this game, clicking one cell twice returns it to its original state
            linComb = reduceMod2(linComb);

            // Checks if i is a valid solution to the matrix equation arrs*x=target
            if (Arrays.equals(linComb, target)) {
                return convBinaryArray(i);
            }
        }
        // This return should never be reached unless the board is in an unsolvable state
        return null;
    }

    // Takes any decimal number, expresses it in binary, and returns an array where each component is a binary digit.
    public int[] convBinaryArray(int i) {
        int[] arr = new int[getNumRows()];
        int j = 0;
        while (i > 0 && j < getNumRows()) {
            if (i % 2 == 1) {
                arr[j] = 1;
            }
            i /= 2;
            j++;
        }
        return arr;
    }

    // Reduces each entry of arr mod 2
    public int[] reduceMod2(int[] arr) {
        int[] arr2 = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            arr2[i] = arr[i] % 2;
        }
        return arr2;
    }

    // Adds each entry of a1 and a2 as if they were vectors
    public int[] addVectors(int[] a1, int[] a2) {
        int[] sum = new int[a1.length];
        for (int i = 0; i < a1.length; i++) {
            sum[i] = a1[i] + a2[i];
        }
        return sum;
    }

    // Turns x/y coordinates of a click into info on which cell was clicked
    public int[] coordsToIndices(int xCoord, int yCoord) {
        int boardSize = board.getCellSize() * getNumRows();
        int xTL = (GameView.WINDOW_WIDTH - boardSize) / 2;
        int xCellIndex = (xCoord - xTL) / board.getCellSize();
        int yTL = (GameView.WINDOW_HEIGHT - boardSize) / 2;
        int yCellIndex = (yCoord - yTL) / board.getCellSize();
        return new int[] {xCellIndex, yCellIndex};
    }

    public void setup() {
        window = new GameView(this);
        this.window.addMouseListener(this);
        this.window.addKeyListener(this);
    }

    public void runGame() {
        setup();
    }

    public void mouseClicked(MouseEvent e) {
        if (board == null) {
            return;
        }
        int x = e.getX();
        int y = e.getY();
        int[] coords = coordsToIndices(x,y);
        int row = coords[0];
        int col = coords[1];

        // Attempts to toggle the cell. Runs code inside if outside the array
        if (!board.toggleAllAdj(row,col)) {
            // Top left = hint
            if (x < 100 && y < 100) {
                getHints2();
            }
            // Bottom right = propagate
            if (x > GameView.WINDOW_WIDTH - 100 && y > GameView.WINDOW_HEIGHT - 100) {
                propagate();
            }
            // Bottom left = scramble
            if (x < 100 && y > GameView.WINDOW_HEIGHT - 100) {
                scramble();
            }
            // Top right = solve
            if (x > GameView.WINDOW_WIDTH - 100 && y < 100) {
                solve();
            }
        }
        window.repaint();
    }
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void keyTyped(KeyEvent e) {
        if (e.getKeyChar() == (KeyEvent.VK_ENTER)) {
            setBoard(Integer.parseInt(rowsInput));
            scramble();
            rowsInput = "";
            window.repaint();
            return;
        }
        if (e.getKeyChar() == (KeyEvent.VK_ESCAPE)) {
            rowsInput = "";
            window.repaint();
            return;
        }
        if (Character.isDigit(e.getKeyChar())) {
            rowsInput += e.getKeyChar();
            window.repaint();
        }
    }
    public void keyReleased(KeyEvent e) {}
    public void keyPressed(KeyEvent e) {}
    public static void main(String[] args) {
        Game game = new Game();
        game.runGame();
    }
}

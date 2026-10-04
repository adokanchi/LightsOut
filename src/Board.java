import java.awt.*;
import java.awt.image.BufferedImage;

public class Board {
    private final BoardCell[][] board;
    private final int cellSize;

    public static final int MAX_BOARD_PIXELS = 700;

    public Board(int numRows) {
        // Standard cell size is 50 pixels. If that would exceed the maximum, shrink to fit.
        // Past MAX_BOARD_PIXELS cells per side this is 0 and the board is drawn as a scaled-down picture.

        final int STANDARD_CELL_SIZE = 50;
        cellSize = Math.min(STANDARD_CELL_SIZE, MAX_BOARD_PIXELS / numRows);

        this.board = new BoardCell[numRows][numRows];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numRows; j++) {
                board[i][j] = new BoardCell();
            }
        }
    }

    public BoardCell[][] getBoard() {
        return board;
    }

    // Side length of the drawn board in pixels
    public int getPixelSize() {
        if (cellSize == 0) return MAX_BOARD_PIXELS;
        return cellSize * board.length;
    }

    // Sets board state to solved
    public void solve() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                board[i][j].setState(false);
                board[i][j].setHint(false);
            }
        }
    }

    public boolean isSolved() {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                if (board[i][j].isOn()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void clickHints() {
        int n = board.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (board[i][j].isHint()) {
                    toggleAllAdj(i, j);
                    board[i][j].setHint(false);
                }
            }
        }
    }

    public void clearHints() {
        int n = board.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                board[i][j].setHint(false);
            }
        }
    }

    // Checks if row/col are within bounds of array, then toggles selected cell and all adjacent cells
    // Returns true if the click was within array bounds and cells were toggled, returns false if the
    // click was outside the board and nothing was toggled
    public boolean toggleAllAdj(int col, int row) {
        // If outside array bounds, return false
        if (row < 0 || col < 0 || row >= board.length || col >= board.length) {
            return false;
        }

        board[col][row].toggle();
        // Attempts to toggle each of the 4 cells around the clicked cell
        if (col - 1 >= 0) {
            board[col - 1][row].toggle();
        }
        if (col + 1 <= board.length - 1) {
            board[col + 1][row].toggle();
        }
        if (row - 1 >= 0) {
            board[col][row - 1].toggle();
        }
        if (row + 1 <= board.length - 1) {
            board[col][row + 1].toggle();
        }
        return true;
    }

    // "Propagates" the board, clicking under all unsolved cells
    public void propagate() {
        int n = board.length;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n; j++) {
                if (board[j][i].isOn()) {
                    toggleAllAdj(j, i + 1);
                }
            }
        }
        clearHints();
    }

    // Gives a random solvable scramble by starting with a solved board and
    // either clicking or not clicking on each square with a 50/50 chance
    public void scramble() {
        solve();
        int n = board.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (Math.random() < 0.5) {
                    toggleAllAdj(i, j);
                }
            }
        }
    }

    public void draw(Graphics g, int xTLCorner, int yTLCorner) {
        int size = getPixelSize();
        g.setColor(Color.BLACK);
        g.fillRect(xTLCorner - 1, yTLCorner - 1, size + 2, size + 2);

        if (cellSize == 0) {
            drawScaledDown(g, xTLCorner, yTLCorner);
        } else {
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board.length; j++) {
                    int x = xTLCorner + cellSize * i;
                    int y = yTLCorner + cellSize * j;
                    board[i][j].draw(g, x, y, cellSize);
                }
            }
        }
    }

    // Used when there are more cells than pixels. Each pixel covers a block of cells and is
    // shaded by how many of them are lit (dark gray = none, white = all) and tinted red by
    // how many are hinted. A block with even one lit or hinted cell is visibly different
    // from an empty one, so a solved board is the only one that looks uniformly dark.
    private void drawScaledDown(Graphics g, int xTLCorner, int yTLCorner) {
        int n = board.length;
        int p = MAX_BOARD_PIXELS;
        int[][] total = new int[p][p];
        int[][] lit = new int[p][p];
        int[][] hinted = new int[p][p];
        for (int i = 0; i < n; i++) {
            int px = (int) ((long) i * p / n);
            for (int j = 0; j < n; j++) {
                int py = (int) ((long) j * p / n);
                total[px][py]++;
                if (board[i][j].isOn()) lit[px][py]++;
                if (board[i][j].isHint()) hinted[px][py]++;
            }
        }

        final int DARK = Color.DARK_GRAY.getRed();
        // Smallest share of the full brightness / redness shown for a block that has any lit / hinted cell
        final double MIN_VISIBLE = 0.25;
        BufferedImage img = new BufferedImage(p, p, BufferedImage.TYPE_INT_RGB);
        for (int px = 0; px < p; px++) {
            for (int py = 0; py < p; py++) {
                double litShare = 0;
                if (lit[px][py] > 0) {
                    litShare = MIN_VISIBLE + (1 - MIN_VISIBLE) * lit[px][py] / total[px][py];
                }
                double hintShare = 0;
                if (hinted[px][py] > 0) {
                    hintShare = MIN_VISIBLE + (1 - MIN_VISIBLE) * hinted[px][py] / total[px][py];
                }
                double gray = DARK + (255 - DARK) * litShare;
                int red = (int) Math.round(gray + (255 - gray) * hintShare);
                int other = (int) Math.round(gray * (1 - hintShare));
                img.setRGB(px, py, (red << 16) | (other << 8) | other);
            }
        }
        g.drawImage(img, xTLCorner, yTLCorner, null);
    }
}

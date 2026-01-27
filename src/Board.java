import java.awt.*;
public class Board {
    private final BoardCell[][] board;
    private final int cellSize;

    public Board(int numRows) {
        // Standard cell size is
        final int STANDARD_CELL_SIZE = 50;
        if (numRows < 12) {
            cellSize = STANDARD_CELL_SIZE;
        }
        else {
            cellSize = (12 * STANDARD_CELL_SIZE) / numRows;
        }
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

    public int getCellSize() {
        return cellSize;
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

        if (board[col][row].isHint()) {
            board[col][row].setHint(false);
        }
        else {
            clearHints();
        }

        board[col][row].toggle();
        // Attempts to toggle each of the 4 cells around the clicked cell
        if (col - 1 >= 0) {
            board[col-1][row].toggle();
        }
        if (col + 1 <= board.length - 1) {
            board[col+1][row].toggle();
        }
        if (row - 1 >= 0) {
            board[col][row-1].toggle();
        }
        if (row + 1 <= board.length - 1) {
            board[col][row+1].toggle();
        }
        return true;
    }

    // "Propagates" the board, clicking under all unsolved cells
    public void propagate() {
        int n = board.length;
        for (int i = 0; i < n-1; i++) {
            for (int j = 0; j < n; j++) {
                if (board[j][i].isOn()) {
                    toggleAllAdj(j,i+1);
                }
            }
        }
    }

    // Gives a random solvable scramble by starting with a solved board and
    // either clicking or not clicking on each square with a 50/50 chance
    public void scramble() {
        solve();
        int n = board.length;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if ((int) (Math.random() + 0.5) == 1) {
                    toggleAllAdj(i,j);
                }
            }
        }
    }

    // Draws each cell of board individually
    public void draw(Graphics g, int xTLCorner, int yTLCorner) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board.length; j++) {
                int x = xTLCorner + cellSize * i;
                int y = yTLCorner + cellSize * j;
                board[i][j].draw(g, x, y, cellSize);
            }
        }
    }
}

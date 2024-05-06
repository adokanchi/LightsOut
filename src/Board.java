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
            }
        }
    }

    // Returns true if the whole board is solved, false if there are any unsolved cells
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

    // Checks if row/col are within bounds of array, then toggles selected cell and all adjacent cells
    // Returns true if the click was within array bounds and cells were toggled, returns false if the
    // click was outside the board and nothing was toggled
    public boolean toggleAllAdj(int row, int col) {
        // If outside array bounds, return false
        if (row < 0 || col < 0 || row >= board.length || col >= board.length) {
            return false;
        }
        board[row][col].toggle();
        board[row][col].setHint(false);
        // Attempts to toggle each of the 4 cells around the clicked cell
        if (row - 1 >= 0) {
            board[row-1][col].toggle();
        }
        if (row + 1 <= board.length - 1) {
            board[row+1][col].toggle();
        }
        if (col - 1 >= 0) {
            board[row][col-1].toggle();
        }
        if (col + 1 <= board.length - 1) {
            board[row][col+1].toggle();
        }
        return true;
    }

    // "Propagates" the board, going row by row clicking underneath all board cells so that all but the last row becomes solved
    public void propagate() {
        for (int i = 0; i < board.length-1; i++) {
            for (int j = 0; j < board.length; j++) {
                if (board[j][i].isOn()) {
                    toggleAllAdj(j,i+1);
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

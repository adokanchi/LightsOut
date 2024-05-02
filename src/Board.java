import java.awt.*;
public class Board {
    private final int numRows;
    private final BoardCell[][] board;
    private final int cellSize;

    public Board(int numRows) {
        if (numRows < 12) {
            cellSize = 50;
        }
        else
        {
            cellSize = 600 / numRows;
        }
        this.numRows = numRows;
        this.board = new BoardCell[numRows][numRows];
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numRows; j++) {
                board[i][j] = new BoardCell(false);
            }
        }
    }

    public BoardCell[][] getBoard() {
        return board;
    }

    public int getNumRows() {
        return numRows;
    }

    public int getCellSize() {
        return cellSize;
    }

    public void solveAll() {
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numRows; j++) {
                board[i][j].setState(false);
            }
        }
    }

    public boolean isSolved() {
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numRows; j++) {
                if (board[i][j].isOn()) {
                    return false;
                }
            }
        }
        return true;
    }

    // Toggles selected cell and all adjacent cells (if within bounds of array)
    public boolean toggleAllAdj(int row, int col) {
        // If outside array bounds, return false
        if (row < 0 || col < 0 || row >= numRows || col >= numRows) {
            return false;
        }
        board[row][col].toggle();
        board[row][col].setHint(false);
        // Attempts to toggle each of the 4 cells around the clicked cell
        if (row - 1 >= 0) {
            board[row-1][col].toggle();
        }
        if (row + 1 <= numRows - 1) {
            board[row+1][col].toggle();
        }
        if (col - 1 >= 0) {
            board[row][col-1].toggle();
        }
        if (col + 1 <= numRows - 1) {
            board[row][col+1].toggle();
        }
        return true;
    }

    // Draws each cell of board
    public void draw(Graphics g, int xTLCorner, int yTLCorner) {
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numRows; j++) {
                int x = xTLCorner + cellSize * i;
                int y = yTLCorner + cellSize * j;
                board[i][j].draw(g, x, y, cellSize);
            }
        }
    }
}

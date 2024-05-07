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
        scramble();
        // setBoard() still needs to be called for the game to become playable
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

    // Gives a random solvable scramble by starting with a solved board and
    // either clicking or not clicking on each square with a 50/50 chance
    public void scramble() {
        if (board == null) {
            return;
        }
        board.solve();
        for (int i = 0; i < getNumRows(); i++) {
            for (int j = 0; j < getNumRows(); j++) {
                if ((int) (Math.random() + 0.5) == 1) {
                    board.toggleAllAdj(i,j);
                }
            }
        }
    }

    // Executes the animated solve option
    public void solveAnim() {
        clock.start();
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

    // Removes the hint property from all cells
    public void clearHints() {
        for (int i = 0; i < getNumRows(); i++) {
            for (int j = 0; j < getNumRows(); j++) {
                board.getBoard()[i][j].setHint(false);
            }
        }
    }

    // Gives user a hint, highlighting squares to click in red
    public void getHints() {
        clearHints();
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
        /*
         Figures out how top-row clicks impact the bottom row after propagation by
         testing every possibility on a separate board and storing them in topRowMatrix
        */
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
        int[] linCombs = findLinCombs(topRowMatrix, botRow);

        // Hint squares
        for (int i = 0; i < linCombs.length; i++) {
            if (linCombs[i] == 1) {
                board.getBoard()[i][0].setHint(true);
            }
        }
    }

    // Treating arrs like a matrix and target like a vector, finds a linear
    // combination of columns of arrs that makes target
    // Or in other words, solves the matrix equation arrs*x=target and returns the vector x
    public int[] findLinCombs(int[][] arrs, int[] target) {
        // For console info
        String finalLinComb = "";
        for (int i = 0; i < arrs.length; i++) {
            finalLinComb += "1";
        }
        for (int i = 0; i < Math.pow(2, arrs.length); i++) { // for every possible combination of top row clicks
            // Print some console info
            if (i % Math.pow(2,Math.min(21,arrs.length - 3)) == 0) {
                System.out.println(Integer.toBinaryString(i) + " being checked");
                System.out.println(finalLinComb + "is the last combination to be checked");
                System.out.println((Math.log(i) / Math.log(2)) + " binary digits have been checked out of " + arrs.length);
                String percentage = String.format("%.10f",(100.0 * (i + 1) / Math.pow(2,arrs.length)));
                System.out.println("Approximately " + (percentage) + "% done\n");
            }

            // j is a copy of i, so we can modify it without changing the loop
            int j = i;
            int count = 0;
            int[] linComb = new int[arrs.length];

            // Compute the linear combination arrs*i
            while (j > 0) {
                if (j % 2 == 1) {
                    j -= 1;
                    linComb = addVectors(linComb, arrs[count]);
                }
                count++;
                j /= 2;
            }

            // Reduces each entry of the combination mod 2, as in this game,
            // clicking one cell twice returns it to its original state
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
        while (i > 0) {
            // Check last binary digit of i
            if (i % 2 == 1) {
                arr[j] = 1;
            }
            // Remove last binary digit of i
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
                propagate();
            }
            // Bottom left = scramble
            if (x < BUTTON_SIZE+BUTTON_OFFSET && y > GameView.WINDOW_HEIGHT-BUTTON_SIZE-BUTTON_OFFSET) {
                scramble();
            }
            // Top right = solve
            if (x > GameView.WINDOW_WIDTH-BUTTON_SIZE-BUTTON_OFFSET && y < BUTTON_SIZE+BUTTON_OFFSET) {
                solveAnim();
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
            setBoard(Integer.parseInt(rowsInput));
            scramble();
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
                if (board.getBoard()[i][j].isHint()) {
                    board.toggleAllAdj(i,j);
                    window.repaint();
                    return;
                }
            }
        }

        // If nothing was clicked, if the board is solved, stop calling actionPerformed
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

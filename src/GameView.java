import javax.swing.*;
import java.awt.*;

public class GameView extends JFrame {
    private final Game game;
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 700;

    public GameView(Game game) {
        // Initial window properties
        this.setTitle("LIGHTS OUT!");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.setVisible(true);
        this.game = game;
    }

    // Clears whole window
    public void clearWindow(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0,0,WINDOW_WIDTH, WINDOW_HEIGHT);
    }

    // Draws board
    public void drawBoard(Graphics g) {
        if (game.getBoard() == null) {
            return;
        }
        int boardSize = game.getBoard().getCellSize() * game.getNumRows();
        int xMargin = (WINDOW_WIDTH - boardSize) / 2;
        int yMargin = (WINDOW_HEIGHT - boardSize) / 2;
        game.getBoard().draw(g, xMargin, yMargin);
    }

    public void paint(Graphics g) {
        clearWindow(g);
        drawBoard(g);

        // Draws buttons in each corner
        g.setColor(Color.CYAN);
        g.fillRect(0,0,100,100); // Hint
        g.fillRect(WINDOW_WIDTH-100,WINDOW_HEIGHT-100,100,100); // Propagate
        g.fillRect(0,WINDOW_HEIGHT-100,100,100); // Scramble
        g.fillRect(WINDOW_WIDTH-100,0,100,100); // Solve

        // Writes button text in each corner
        g.setColor(Color.BLACK);
        g.drawString("HINT",50,50);
        g.drawString("PROPAGATE",WINDOW_WIDTH-90,WINDOW_HEIGHT-20);
        g.drawString("SCRAMBLE",20,WINDOW_HEIGHT-20);
        g.drawString("SOLVE",WINDOW_WIDTH-90,50);

        // Write the board update text
    }
}

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

    // Draws board one cell at a time
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
        final int BUTTON_OFFSET = 50;
        final int button_size = 100;
        g.setColor(Color.CYAN);
        g.fillRect(BUTTON_OFFSET,BUTTON_OFFSET,button_size,button_size); // Hint
        g.fillRect(WINDOW_WIDTH-BUTTON_OFFSET-button_size,WINDOW_HEIGHT-BUTTON_OFFSET-button_size,button_size,button_size); // Propagate
        g.fillRect(BUTTON_OFFSET,WINDOW_HEIGHT-BUTTON_OFFSET-button_size,button_size,button_size); // Scramble
        g.fillRect(WINDOW_WIDTH-BUTTON_OFFSET-button_size,BUTTON_OFFSET,button_size,button_size); // Solve

        // Writes button text in each corner
        final int TEXT_OFFSET = 50;
        g.setColor(Color.BLACK);
        g.drawString("HINT",BUTTON_OFFSET+TEXT_OFFSET,BUTTON_OFFSET+TEXT_OFFSET);
        g.drawString("PROPAGATE",WINDOW_WIDTH-BUTTON_OFFSET-TEXT_OFFSET,WINDOW_HEIGHT-BUTTON_OFFSET-TEXT_OFFSET);
        g.drawString("SCRAMBLE",20,WINDOW_HEIGHT-20);
        g.drawString("SOLVE",WINDOW_WIDTH-90,50);

        // Write the board update text
        g.drawString("Type a number to change board size:",25,WINDOW_HEIGHT/2);
        g.drawString(game.getRowsInput(),25,WINDOW_HEIGHT/2 + 50);
        g.drawString("Press enter to confirm, press escape to cancel",25,WINDOW_HEIGHT/2 + 100);
    }
}

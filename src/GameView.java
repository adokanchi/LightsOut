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
        final int BUTTON_SIZE = 100;
        final int ARC_SIZE = 30;
        g.setColor(Color.CYAN);
        g.fillRoundRect(BUTTON_OFFSET,BUTTON_OFFSET,BUTTON_SIZE,BUTTON_SIZE,ARC_SIZE,ARC_SIZE); // Hint
        g.fillRoundRect(WINDOW_WIDTH-BUTTON_OFFSET-BUTTON_SIZE,WINDOW_HEIGHT-BUTTON_OFFSET-BUTTON_SIZE,BUTTON_SIZE,BUTTON_SIZE,ARC_SIZE,ARC_SIZE); // Propagate
        g.fillRoundRect(BUTTON_OFFSET,WINDOW_HEIGHT-BUTTON_OFFSET-BUTTON_SIZE,BUTTON_SIZE,BUTTON_SIZE,ARC_SIZE,ARC_SIZE); // Scramble
        g.fillRoundRect(WINDOW_WIDTH-BUTTON_OFFSET-BUTTON_SIZE,BUTTON_OFFSET,BUTTON_SIZE,BUTTON_SIZE,ARC_SIZE,ARC_SIZE); // Solve

        // Writes button text in each corner
        final int SCRAMBLE_TEXT_OFFSET_X = 20;
        final int HINT_TEXT_OFFSET_X = 40;
        final int SOLVE_TEXT_OFFSET_X = 70;
        final int PROPAGATE_TEXT_OFFSET_X = 85;
        final int TEXT_OFFSET_Y = 50;
        g.setColor(Color.BLACK);
        g.drawString("HINT",BUTTON_OFFSET+HINT_TEXT_OFFSET_X,BUTTON_OFFSET+TEXT_OFFSET_Y);
        g.drawString("PROPAGATE",WINDOW_WIDTH-BUTTON_OFFSET-PROPAGATE_TEXT_OFFSET_X,WINDOW_HEIGHT-BUTTON_OFFSET-TEXT_OFFSET_Y);
        g.drawString("SCRAMBLE",BUTTON_OFFSET+SCRAMBLE_TEXT_OFFSET_X,WINDOW_HEIGHT-BUTTON_OFFSET-TEXT_OFFSET_Y);
        g.drawString("SOLVE",WINDOW_WIDTH-BUTTON_OFFSET-SOLVE_TEXT_OFFSET_X,BUTTON_OFFSET+TEXT_OFFSET_Y);

        // Write the board update text
        final int LINE_SPACE = 50;
        final int BOARDSIZE_TEXT_OFFSET = 25;
        g.drawString("Type a number to change board size:",BOARDSIZE_TEXT_OFFSET,WINDOW_HEIGHT/2-LINE_SPACE);
        g.drawString(game.getRowsInput(),BOARDSIZE_TEXT_OFFSET,WINDOW_HEIGHT/2);
        g.drawString("Press enter to confirm, press escape to cancel",25,WINDOW_HEIGHT/2 + LINE_SPACE);
    }
}

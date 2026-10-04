import javax.swing.*;
import java.awt.*;

public class GameView extends JFrame {
    private final Game game;
    public static final int WINDOW_WIDTH = 1200;
    public static final int WINDOW_HEIGHT = 700;
    private String errString;

    private BoardPanel panel;

    public GameView(Game game) {
        // Initial window properties
        setTitle("LIGHTS OUT!");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setVisible(true);
        this.game = game;
        this.errString = "";
        panel = new BoardPanel();
        setContentPane(panel);
    }

    /*
    public void clearWindow(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0,0,WINDOW_WIDTH, WINDOW_HEIGHT);
    }

     */
    public void setErrString(String errString) {
        this.errString = errString;
    }

    public void clearErrString() {
        this.errString = "";
    }

    /*

    // Draws board one cell at a time
    public void drawBoard(Graphics g) {
        if (game == null) return;
        if (game.getBoard() == null) return;

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
        g.fillRoundRect(WINDOW_WIDTH-BUTTON_OFFSET-BUTTON_SIZE,(WINDOW_HEIGHT-BUTTON_SIZE)/2,BUTTON_SIZE,BUTTON_SIZE,ARC_SIZE,ARC_SIZE); // Optimal Solve

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
        g.drawString("PERFECT SOLVE", WINDOW_WIDTH - BUTTON_OFFSET - BUTTON_SIZE, WINDOW_HEIGHT / 2);

        // Write the board update text
        final int LINE_SPACE = 50;
        final int BOARDSIZE_TEXT_OFFSET = 25;
        g.drawString("Type a number to change board size:",BOARDSIZE_TEXT_OFFSET,WINDOW_HEIGHT/2-LINE_SPACE);
        if (game != null) g.drawString(game.getRowsInput(),BOARDSIZE_TEXT_OFFSET,WINDOW_HEIGHT/2);
        g.drawString("Press enter to confirm, press escape to cancel",25,WINDOW_HEIGHT/2 + LINE_SPACE);

        // Write error text
        final int ERR_TEXT_OFFSET_Y = 20;
        FontMetrics fm = g.getFontMetrics();
        if (errString != null) g.drawString(errString, (WINDOW_WIDTH - fm.stringWidth(errString)) / 2, WINDOW_HEIGHT - ERR_TEXT_OFFSET_Y);
    }

     */

    private class BoardPanel extends JPanel {
        BoardPanel() {
            setDoubleBuffered(true);
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                drawBoard(g2);
                drawUI(g2);
            } finally {
                g2.dispose();
            }
        }

        private void drawBoard(Graphics g) {
            if (game == null || game.getBoard() == null) return;

            int boardSize = game.getBoard().getCellSize() * game.getNumRows();
            int xMargin = (getWidth() - boardSize) / 2;
            int yMargin = (getHeight() - boardSize) / 2;
            game.getBoard().draw(g, xMargin, yMargin);
        }

        private void drawUI(Graphics g) {
            int w = getWidth();
            int h = getHeight();

            // Buttons
            final int BUTTON_OFFSET = 50;
            final int BUTTON_SIZE = 100;
            final int ARC_SIZE = 30;

            g.setColor(Color.CYAN);
            g.fillRoundRect(BUTTON_OFFSET, BUTTON_OFFSET, BUTTON_SIZE, BUTTON_SIZE, ARC_SIZE, ARC_SIZE); // Hint
            g.fillRoundRect(w - BUTTON_OFFSET - BUTTON_SIZE, h - BUTTON_OFFSET - BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, ARC_SIZE, ARC_SIZE); // Propagate
            g.fillRoundRect(BUTTON_OFFSET, h - BUTTON_OFFSET - BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, ARC_SIZE, ARC_SIZE); // Scramble
            g.fillRoundRect(w - BUTTON_OFFSET - BUTTON_SIZE, BUTTON_OFFSET, BUTTON_SIZE, BUTTON_SIZE, ARC_SIZE, ARC_SIZE); // Solve
            g.fillRoundRect(w - BUTTON_OFFSET - BUTTON_SIZE, (h - BUTTON_SIZE) / 2, BUTTON_SIZE, BUTTON_SIZE, ARC_SIZE, ARC_SIZE); // Optimal Solve

            // Text
            g.setColor(Color.BLACK);
            final int SCRAMBLE_TEXT_OFFSET_X = 20;
            final int HINT_TEXT_OFFSET_X = 40;
            final int SOLVE_TEXT_OFFSET_X = 70;
            final int PROPAGATE_TEXT_OFFSET_X = 85;
            final int TEXT_OFFSET_Y = 50;

            g.drawString("HINT", BUTTON_OFFSET + HINT_TEXT_OFFSET_X, BUTTON_OFFSET + TEXT_OFFSET_Y);
            g.drawString("PROPAGATE", w - BUTTON_OFFSET - PROPAGATE_TEXT_OFFSET_X, h - BUTTON_OFFSET - TEXT_OFFSET_Y);
            g.drawString("SCRAMBLE", BUTTON_OFFSET + SCRAMBLE_TEXT_OFFSET_X, h - BUTTON_OFFSET - TEXT_OFFSET_Y);
            g.drawString("SOLVE", w - BUTTON_OFFSET - SOLVE_TEXT_OFFSET_X, BUTTON_OFFSET + TEXT_OFFSET_Y);
            g.drawString("PERFECT SOLVE", w - BUTTON_OFFSET - BUTTON_SIZE, h / 2);

            // Board update text
            final int LINE_SPACE = 50;
            final int BOARDSIZE_TEXT_OFFSET = 25;
            g.drawString("Type a number to change board size:", BOARDSIZE_TEXT_OFFSET, h / 2 - LINE_SPACE);
            if (game != null) g.drawString(game.getRowsInput(), BOARDSIZE_TEXT_OFFSET, h / 2);
            g.drawString("Press enter to confirm, press escape to cancel", 25, h / 2 + LINE_SPACE);

            // Error text centered
            final int ERR_TEXT_OFFSET_Y = 20;
            FontMetrics fm = g.getFontMetrics();
            int x = (w - fm.stringWidth(errString)) / 2;
            g.drawString(errString, Math.max(0, x), h - ERR_TEXT_OFFSET_Y);
        }
    }
}

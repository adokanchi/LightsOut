import javax.swing.*;
import java.awt.*;

public class GameView extends JFrame {
    private final Game game;
    public static final int WINDOW_WIDTH = 1500;
    public static final int WINDOW_HEIGHT = 800;
    private String statusText;

    private BoardPanel panel;

    public GameView(Game game) {
        // Initial window properties
        setTitle("LIGHTS OUT!");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        this.game = game;
        this.statusText = "";
        panel = new BoardPanel();
        setContentPane(panel);
        setVisible(true);
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public void clearStatusText() {
        this.statusText = "";
    }

    // The board and buttons are drawn on the panel, which is smaller than the window
    // (no title bar or borders), so clicks have to be measured against the panel too
    public void addPanelMouseListener(java.awt.event.MouseListener listener) {
        panel.addMouseListener(listener);
    }

    public int getPanelWidth() {
        return panel.getWidth();
    }

    public int getPanelHeight() {
        return panel.getHeight();
    }

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

            int boardSize = game.getBoard().getPixelSize();
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
            int x = (w - fm.stringWidth(statusText)) / 2;
            g.drawString(statusText, Math.max(0, x), h - ERR_TEXT_OFFSET_Y);
        }
    }
}

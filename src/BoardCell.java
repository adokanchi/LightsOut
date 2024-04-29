import java.awt.*;
public class BoardCell {
    private boolean isOn;
    private boolean isHint;

    public BoardCell(boolean isOn) {
        this.isOn = isOn;
        this.isHint = false;
    }

    public void setHint(boolean isHint) {
        this.isHint = isHint;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setState(boolean isOn) {
        this.isOn = isOn;
    }

    public void toggle() {
        isOn = !isOn;
    }

    public void draw(Graphics g, int x, int y) {
        // Outline color
        g.setColor(Color.BLACK);
        if (isHint) {
            g.setColor(Color.RED);
        }
        // Outline
        g.fillRect(x,y,GameView.CELL_SIZE,GameView.CELL_SIZE);

        // Center color
        g.setColor(Color.DARK_GRAY);
        if (isOn) {
            g.setColor(Color.WHITE);
        }
        // Center
        g.fillRect(x+2,y+2,GameView.CELL_SIZE-4,GameView.CELL_SIZE-4);
    }
}
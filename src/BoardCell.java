import java.awt.*;

public class BoardCell {
    private boolean isOn;
    // isHint is true when the cell is marked as a cell to click by the hint tool
    private boolean isHint;

    public BoardCell() {
        this.isOn = false;
        this.isHint = false;
    }

    public void setHint(boolean isHint) {
        this.isHint = isHint;
    }

    public boolean isHint() {
        return isHint;
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

    public void draw(Graphics g, int x, int y, int size) {
        // Outline is 2 pixels, shrinking to 1 when 2 would leave a center of 1 pixel or less,
        // and to 0 when even a 1-pixel outline would leave no center at all
        int outline = 2;
        if (size - 2 * outline <= 1) outline = 1;
        if (size - 2 * outline <= 0) outline = 0;

        Color center = isOn ? Color.WHITE : Color.DARK_GRAY;

        // Too small for an outline: the whole cell is one color, and a hint is solid red
        if (outline == 0) {
            g.setColor(isHint ? Color.RED : center);
            g.fillRect(x, y, size, size);
            return;
        }

        // Hinted cells get a red outline twice as thick, unless that would shrink the
        // center to 1 pixel or less, in which case it stays as thick as a normal outline
        if (isHint && size - 4 * outline > 1) {
            outline *= 2;
        }

        // Outline
        g.setColor(isHint ? Color.RED : Color.BLACK);
        g.fillRect(x, y, size, size);

        // Center
        g.setColor(center);
        g.fillRect(x + outline, y + outline, size - 2 * outline, size - 2 * outline);
    }
}

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.SplittableRandom;

// The board is stored as bits: each row is an array of longs holding 64 cells per long.
// Bit col of lit[row] is 1 when the cell in that column and row is lit, and hint[row] works
// the same way for cells marked as hints. This is the layout the solver works in, so the
// solver can read the rows directly, and whole-board operations handle 64 cells at a time.
public class Board {
    // Number of cells along each side
    private final int size;
    // Number of longs needed to hold one row (size bits)
    private final int rowWords;
    // Covers the bits of the last long of a row that are real cells. Bits past the end of a
    // row must always stay 0.
    private final long lastWordMask;
    private final long[][] lit;
    private final long[][] hint;
    private final int cellSize;

    public static final int MAX_BOARD_PIXELS = 700;

    // ---- Used only when there are more cells than pixels (cellSize == 0) ----
    // The board is then drawn as a picture where each pixel covers a block of cells. The picture
    // is kept between repaints. A single cell changing updates the one pixel it belongs to;
    // anything that changes the whole board marks the picture stale so it is rebuilt on the
    // next draw.
    private BufferedImage picture;
    private boolean pictureStale = true;
    // Number of lit / hinted cells under each pixel, stored row by row: index = py * MAX_BOARD_PIXELS + px
    private int[] litCount;
    private int[] hintCount;
    // blockStart[p] is the first column (or row) covered by pixel p; blockStart[MAX_BOARD_PIXELS] = size
    private int[] blockStart;

    public Board(int numRows) {
        // Standard cell size is 50 pixels. If that would exceed the maximum, shrink to fit.
        // Past MAX_BOARD_PIXELS cells per side this is 0 and the board is drawn as a scaled-down picture.

        final int STANDARD_CELL_SIZE = 50;
        cellSize = Math.min(STANDARD_CELL_SIZE, MAX_BOARD_PIXELS / numRows);

        size = numRows;
        rowWords = (numRows + 63) >>> 6;
        lastWordMask = (numRows & 63) == 0 ? -1L : (1L << (numRows & 63)) - 1;
        lit = new long[numRows][rowWords];
        hint = new long[numRows][rowWords];

        if (cellSize == 0) {
            int p = MAX_BOARD_PIXELS;
            litCount = new int[p * p];
            hintCount = new int[p * p];
            blockStart = new int[p + 1];
            for (int i = 0; i < numRows; i++) {
                // Cell i belongs to pixel i * p / size; record the first cell of each pixel
                int pixel = (int) ((long) i * p / numRows);
                if (i == 0 || pixel != (int) ((long) (i - 1) * p / numRows)) blockStart[pixel] = i;
            }
            blockStart[p] = numRows;
        }
    }

    public int getSize() {
        return size;
    }

    public boolean isOn(int col, int row) {
        return getBit(lit[row], col);
    }

    public boolean isHint(int col, int row) {
        return getBit(hint[row], col);
    }

    public void setHint(int col, int row, boolean isHint) {
        if (getBit(hint[row], col) == isHint) return;
        hint[row][col >>> 6] ^= 1L << (col & 63);
        cellChanged(hintCount, col, row, isHint ? 1 : -1);
    }

    // The lit rows themselves, for the solver to read: bit col of row [row]. It is not a copy,
    // so the caller must not change it.
    public long[][] getLitRows() {
        return lit;
    }

    // Replaces every hint at once. clicks holds one bit per cell, rows laid end to end:
    // bit (row * size + col) is 1 when that cell should be hinted.
    public void setHints(long[] clicks) {
        for (int row = 0; row < size; row++) {
            long[] hintRow = hint[row];
            for (int w = 0; w < rowWords; w++) {
                // This row starts at bit row * size, usually not on a word boundary, so each
                // word of the row comes from two neighboring words of clicks
                long start = (long) row * size + ((long) w << 6);
                int idx = (int) (start >>> 6);
                int shift = (int) (start & 63);
                long word = clicks[idx] >>> shift;
                if (shift != 0 && idx + 1 < clicks.length) word |= clicks[idx + 1] << (64 - shift);
                hintRow[w] = word;
            }
            hintRow[rowWords - 1] &= lastWordMask;
        }
        pictureStale = true;
    }

    // Side length of the drawn board in pixels
    public int getPixelSize() {
        if (cellSize == 0) return MAX_BOARD_PIXELS;
        return cellSize * size;
    }

    // Sets board state to solved
    public void solve() {
        for (int row = 0; row < size; row++) {
            Arrays.fill(lit[row], 0L);
            Arrays.fill(hint[row], 0L);
        }
        pictureStale = true;
    }

    public boolean isSolved() {
        for (int row = 0; row < size; row++) {
            for (long word : lit[row]) {
                if (word != 0) return false;
            }
        }
        return true;
    }

    // Index of the first row that has a lit cell, or -1 if the board is solved
    public int firstLitRow() {
        for (int row = 0; row < size; row++) {
            for (long word : lit[row]) {
                if (word != 0) return row;
            }
        }
        return -1;
    }

    // Hints the cell underneath every lit cell of the given row
    public void hintBelowRow(int row) {
        // If the picture is in use and up to date, keep it that way: take the old hints of
        // that row out of the block counts, then put the new ones in
        boolean updatePicture = cellSize == 0 && !pictureStale;
        if (updatePicture) addRowToCounts(hintCount, hint[row + 1], row + 1, -1, true);
        System.arraycopy(lit[row], 0, hint[row + 1], 0, rowWords);
        if (updatePicture) addRowToCounts(hintCount, hint[row + 1], row + 1, 1, true);
    }

    // Clicks every hinted cell, then clears the hints
    public void clickHints() {
        applyClicks(hint);
        clearHints();
    }

    public void clearHints() {
        for (int row = 0; row < size; row++) {
            Arrays.fill(hint[row], 0L);
        }
        if (cellSize == 0 && !pictureStale) {
            // Cheaper than rebuilding the whole picture: the lit counts are still right,
            // so only the pixels that had hints under them need redrawing
            int p = MAX_BOARD_PIXELS;
            for (int i = 0; i < hintCount.length; i++) {
                if (hintCount[i] != 0) {
                    hintCount[i] = 0;
                    recolor(i % p, i / p);
                }
            }
        }
    }

    // Checks if row/col are within bounds of array, then toggles selected cell and all adjacent cells
    // Returns true if the click was within array bounds and cells were toggled, returns false if the
    // click was outside the board and nothing was toggled
    public boolean toggleAllAdj(int col, int row) {
        // If outside array bounds, return false
        if (row < 0 || col < 0 || row >= size || col >= size) {
            return false;
        }

        toggle(col, row);
        // Attempts to toggle each of the 4 cells around the clicked cell
        if (col - 1 >= 0) {
            toggle(col - 1, row);
        }
        if (col + 1 <= size - 1) {
            toggle(col + 1, row);
        }
        if (row - 1 >= 0) {
            toggle(col, row - 1);
        }
        if (row + 1 <= size - 1) {
            toggle(col, row + 1);
        }
        return true;
    }

    private void toggle(int col, int row) {
        long bit = 1L << (col & 63);
        lit[row][col >>> 6] ^= bit;
        boolean nowLit = (lit[row][col >>> 6] & bit) != 0;
        cellChanged(litCount, col, row, nowLit ? 1 : -1);
    }

    // "Propagates" the board, clicking under all unsolved cells
    public void propagate() {
        long[] clicks = new long[rowWords];
        for (int row = 0; row < size - 1; row++) {
            // The clicks on the next row are exactly the cells still lit in this one
            System.arraycopy(lit[row], 0, clicks, 0, rowWords);
            Arrays.fill(lit[row], 0L);
            xorSameRowEffect(lit[row + 1], clicks);
            if (row + 2 < size) xorInto(lit[row + 2], clicks);
        }
        pictureStale = true;
        clearHints();
    }

    // Gives a random solvable scramble by starting with a solved board and
    // either clicking or not clicking on each square with a 50/50 chance
    public void scramble() {
        solve();
        // 64 coin flips at a time: every bit of a random long is an independent 50/50
        SplittableRandom random = new SplittableRandom();
        long[][] clicks = new long[size][rowWords];
        for (int row = 0; row < size; row++) {
            for (int w = 0; w < rowWords; w++) {
                clicks[row][w] = random.nextLong();
            }
            clicks[row][rowWords - 1] &= lastWordMask;
        }
        applyClicks(clicks);
    }

    // Clicks every cell whose bit is set in clicks, a whole row at a time. Clicking is just
    // toggling, so the order does not matter and each row's clicks can be applied together:
    // they toggle themselves and their left/right neighbors in their own row, and the cells
    // directly above and below.
    private void applyClicks(long[][] clicks) {
        for (int row = 0; row < size; row++) {
            xorSameRowEffect(lit[row], clicks[row]);
            if (row > 0) xorInto(lit[row], clicks[row - 1]);
            if (row < size - 1) xorInto(lit[row], clicks[row + 1]);
        }
        pictureStale = true;
    }

    // Toggles in dst every cell that a row of clicks affects within its own row:
    // each clicked cell and the cells to its left and right
    private void xorSameRowEffect(long[] dst, long[] clicks) {
        for (int w = 0; w < rowWords; w++) {
            long v = clicks[w];
            long left = v << 1;
            if (w > 0) left |= clicks[w - 1] >>> 63;
            long right = v >>> 1;
            if (w < rowWords - 1) right |= clicks[w + 1] << 63;
            dst[w] ^= v ^ left ^ right;
        }
        // Drop anything shifted past the end of the row
        dst[rowWords - 1] &= lastWordMask;
    }

    private static void xorInto(long[] dst, long[] src) {
        for (int i = 0; i < dst.length; i++) dst[i] ^= src[i];
    }

    private static boolean getBit(long[] row, int bit) {
        return ((row[bit >>> 6] >>> (bit & 63)) & 1L) != 0;
    }

    public void draw(Graphics g, int xTLCorner, int yTLCorner) {
        int pixelSize = getPixelSize();
        g.setColor(Color.BLACK);
        g.fillRect(xTLCorner - 1, yTLCorner - 1, pixelSize + 2, pixelSize + 2);

        if (cellSize == 0) {
            if (pictureStale) rebuildPicture();
            g.drawImage(picture, xTLCorner, yTLCorner, null);
        } else {
            for (int row = 0; row < size; row++) {
                long[] litRow = lit[row];
                long[] hintRow = hint[row];
                int y = yTLCorner + cellSize * row;
                for (int col = 0; col < size; col++) {
                    int x = xTLCorner + cellSize * col;
                    drawCell(g, x, y, getBit(litRow, col), getBit(hintRow, col));
                }
            }
        }
    }

    // ---- The scaled-down picture, used when there are more cells than pixels ----
    // Each pixel covers a block of cells and is shaded by how many of them are lit (dark gray =
    // none, white = all) and tinted red by how many are hinted. A block with even one lit or
    // hinted cell is visibly different from an empty one, so a solved board is the only one
    // that looks uniformly dark.

    // Recounts every block from scratch and redraws every pixel
    private void rebuildPicture() {
        int p = MAX_BOARD_PIXELS;
        if (picture == null) picture = new BufferedImage(p, p, BufferedImage.TYPE_INT_RGB);
        Arrays.fill(litCount, 0);
        Arrays.fill(hintCount, 0);
        for (int row = 0; row < size; row++) {
            addRowToCounts(litCount, lit[row], row, 1, false);
            addRowToCounts(hintCount, hint[row], row, 1, false);
        }
        pictureStale = false;
        for (int py = 0; py < p; py++) {
            for (int px = 0; px < p; px++) {
                recolor(px, py);
            }
        }
    }

    // Adds (sign = 1) or removes (sign = -1) one whole row's cells from the block counts,
    // optionally redrawing each pixel whose count changed
    private void addRowToCounts(int[] counts, long[] rowBits, int row, int sign, boolean redraw) {
        boolean empty = true;
        for (long word : rowBits) {
            if (word != 0) {
                empty = false;
                break;
            }
        }
        if (empty) return;

        int p = MAX_BOARD_PIXELS;
        int py = (int) ((long) row * p / size);
        int base = py * p;
        for (int px = 0; px < p; px++) {
            int count = countBits(rowBits, blockStart[px], blockStart[px + 1]);
            if (count != 0) {
                counts[base + px] += sign * count;
                if (redraw) recolor(px, py);
            }
        }
    }

    // Records that one cell became lit / hinted (change = 1) or stopped being so (change = -1)
    private void cellChanged(int[] counts, int col, int row, int change) {
        if (cellSize != 0 || pictureStale) return;
        int p = MAX_BOARD_PIXELS;
        int px = (int) ((long) col * p / size);
        int py = (int) ((long) row * p / size);
        counts[py * p + px] += change;
        recolor(px, py);
    }

    // Number of 1 bits in positions from (inclusive) to to (exclusive)
    private static int countBits(long[] bits, int from, int to) {
        int firstWord = from >>> 6;
        int lastWord = (to - 1) >>> 6;
        long firstMask = -1L << (from & 63);
        long lastMask = -1L >>> (63 - ((to - 1) & 63));
        if (firstWord == lastWord) {
            return Long.bitCount(bits[firstWord] & firstMask & lastMask);
        }
        int count = Long.bitCount(bits[firstWord] & firstMask) + Long.bitCount(bits[lastWord] & lastMask);
        for (int w = firstWord + 1; w < lastWord; w++) count += Long.bitCount(bits[w]);
        return count;
    }

    // Sets one pixel of the picture from the counts for its block
    private void recolor(int px, int py) {
        int p = MAX_BOARD_PIXELS;
        int total = (blockStart[px + 1] - blockStart[px]) * (blockStart[py + 1] - blockStart[py]);
        int litCells = litCount[py * p + px];
        int hintCells = hintCount[py * p + px];

        final int DARK = Color.DARK_GRAY.getRed();
        // Smallest share of the full brightness / redness shown for a block that has any lit / hinted cell
        final double MIN_VISIBLE = 0.25;
        double litShare = 0;
        if (litCells > 0) {
            litShare = MIN_VISIBLE + (1 - MIN_VISIBLE) * litCells / total;
        }
        double hintShare = 0;
        if (hintCells > 0) {
            hintShare = MIN_VISIBLE + (1 - MIN_VISIBLE) * hintCells / total;
        }
        double gray = DARK + (255 - DARK) * litShare;
        int red = (int) Math.round(gray + (255 - gray) * hintShare);
        int other = (int) Math.round(gray * (1 - hintShare));
        picture.setRGB(px, py, (red << 16) | (other << 8) | other);
    }

    // Draws one cell as a square of cellSize pixels with its top left corner at (x, y)
    private void drawCell(Graphics g, int x, int y, boolean isOn, boolean isHint) {
        // Outline is 2 pixels, shrinking to 1 when 2 would leave a center of 1 pixel or less,
        // and to 0 when even a 1-pixel outline would leave no center at all
        int outline = 2;
        if (cellSize - 2 * outline <= 1) outline = 1;
        if (cellSize - 2 * outline <= 0) outline = 0;

        Color center = isOn ? Color.WHITE : Color.DARK_GRAY;

        // Too small for an outline: the whole cell is one color, and a hint is solid red
        if (outline == 0) {
            g.setColor(isHint ? Color.RED : center);
            g.fillRect(x, y, cellSize, cellSize);
            return;
        }

        // Hinted cells get a red outline twice as thick, unless that would shrink the
        // center to 1 pixel or less, in which case it stays as thick as a normal outline
        if (isHint && cellSize - 4 * outline > 1) {
            outline *= 2;
        }

        // Outline
        g.setColor(isHint ? Color.RED : Color.BLACK);
        g.fillRect(x, y, cellSize, cellSize);

        // Center
        g.setColor(center);
        g.fillRect(x + outline, y + outline, cellSize - 2 * outline, cellSize - 2 * outline);
    }
}
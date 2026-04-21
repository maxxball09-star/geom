import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
public class Main extends JPanel {
    private ArrayList<Point> points = new ArrayList<>();
    private int width, height;
    private final double xMin = -10.0;
    private final double xMax = 10.0;
    private final double yMin = -10.0;
    private final double yMax = 10.0;
    Main()
    {
        setBackground(Color.WHITE);
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double worldX = toWorldX(e.getX());
                double worldY = toWorldY(e.getY());
                if (worldX >= xMin && worldX <= xMax && worldY >= yMin && worldY <= yMax) {
                    points.add(new Point(worldX, worldY));
                    repaint();
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {}
            @Override
            public void mouseExited(MouseEvent e) {}
        });
    }
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        width = getWidth();
        height = getHeight();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        g2.setColor(Color.GRAY);
        for (int x = (int) xMin; x <= xMax; x++) {
            int sx = toScreenX(x);
            g2.drawLine(sx, 0, sx, height);
        }
        for (int y = (int) yMin; y <= yMax; y++) {
            int sy = toScreenY(y);
            g2.drawLine(0, sy, width, sy);
        }
        g2.setColor(Color.BLACK);
        int zeroX = toScreenX(0);
        int zeroY = toScreenY(0);
        g2.drawLine(zeroX, 0, zeroX, height);
        g2.drawLine(0, zeroY, width, zeroY);
        g2.fillPolygon(new int[]{zeroX, zeroX - 4, zeroX + 4}, new int[]{0, 5, 5}, 3);
        g2.fillPolygon(new int[]{width, width - 5, width - 5}, new int[]{zeroY, zeroY - 4, zeroY + 4}, 3);
        g2.drawString("X", width - 10, zeroY - 5);
        g2.drawString("Y", zeroX + 5, 15);
        g2.setColor(Color.RED);
        for (Point p : points) {
            int sx = toScreenX(p.x);
            int sy = toScreenY(p.y);
            g2.fillOval(sx - 4, sy - 4, 8, 8);
        }
    }
    private int toScreenX(double x) {
        return (int) ((x - xMin) / (xMax - xMin) * width);
    }
    private int toScreenY(double y) {
        return (int) (height - (y - yMin) / (yMax - yMin) * height);
    }
    private double toWorldX(int screenX) {
        return xMin + (screenX * (xMax - xMin)) / (double) width;
    }
    private double toWorldY(int screenY) {
        return yMax - (screenY * (yMax - yMin)) / (double) height;
    }
    private static class Point {
        double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
    }
    public static void main(String[] args) {
        {
            JFrame frame = new JFrame("Coordinate Plane - Click to add points");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.add(new Main());
            frame.setVisible(true);
        }
    }
}
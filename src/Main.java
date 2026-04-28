import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Main extends JPanel {
    private ArrayList<Triangle> triangles = new ArrayList<>();
    private ArrayList<Point> currentPoints = new ArrayList<>();
    private int width, height;
    private final double xMin = -10.0, xMax = 10.0, yMin = -10.0, yMax = 10.0;

    private Triangle bestA = null, bestB = null;
    private ArrayList<Point> intersectPolygon = new ArrayList<>();
    private boolean showResult = false;

    public Main() {
        setBackground(Color.WHITE);
        setFocusable(true);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (showResult) return;
                double wx = toWorldX(e.getX());
                double wy = toWorldY(e.getY());
                if (wx >= xMin && wx <= xMax && wy >= yMin && wy <= yMax) {
                    currentPoints.add(new Point(wx, wy));
                    if (currentPoints.size() == 3) {
                        triangles.add(new Triangle(currentPoints.get(0), currentPoints.get(1), currentPoints.get(2)));
                        currentPoints.clear();
                        repaint();
                    }
                    repaint();
                }
            }
        });
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    findMaxIntersection();
                }
            }
        });
    }

    public void findMaxIntersection() {
        if (triangles.size() < 2) {
            JOptionPane.showMessageDialog(this, "Нужно хотя бы два треугольника");
            return;
        }
        double maxArea = -1;
        Triangle bestTriA = null, bestTriB = null;
        ArrayList<Point> bestPoly = new ArrayList<>();

        for (int i = 0; i < triangles.size(); i++) {
            for (int j = i + 1; j < triangles.size(); j++) {
                ArrayList<Point> poly = intersectTriangles(triangles.get(i), triangles.get(j));
                double area = polygonArea(poly);
                if (area > maxArea + 1e-9) {
                    maxArea = area;
                    bestTriA = triangles.get(i);
                    bestTriB = triangles.get(j);
                    bestPoly = poly;
                }
            }
        }
        if (maxArea >= 0) {
            bestA = bestTriA;
            bestB = bestTriB;
            intersectPolygon = bestPoly;
            showResult = true;
            repaint();
        }
    }

    private ArrayList<Point> intersectTriangles(Triangle t1, Triangle t2) {
        ArrayList<Point> poly = new ArrayList<>();
        for (Point p : t1.vertices) if (pointInTriangle(p, t2)) poly.add(p);
        for (Point p : t2.vertices) if (pointInTriangle(p, t1)) poly.add(p);
        for (int i = 0; i < 3; i++) {
            Point a1 = t1.vertices[i];
            Point b1 = t1.vertices[(i+1)%3];
            for (int j = 0; j < 3; j++) {
                Point a2 = t2.vertices[j];
                Point b2 = t2.vertices[(j+1)%3];
                Point inter = segmentIntersection(a1, b1, a2, b2);
                if (inter != null) poly.add(inter);
            }
        }
        if (poly.isEmpty()) return new ArrayList<>();
        return convexHull(poly);
    }

    private boolean pointInTriangle(Point p, Triangle t) {
        double o1 = orient(t.vertices[0], t.vertices[1], p);
        double o2 = orient(t.vertices[1], t.vertices[2], p);
        double o3 = orient(t.vertices[2], t.vertices[0], p);
        return (o1 >= 0 && o2 >= 0 && o3 >= 0) || (o1 <= 0 && o2 <= 0 && o3 <= 0);
    }

    private double orient(Point a, Point b, Point c) {
        return (b.x - a.x)*(c.y - a.y) - (b.y - a.y)*(c.x - a.x);
    }

    private Point segmentIntersection(Point a, Point b, Point c, Point d) {
        double o1 = orient(a, b, c);
        double o2 = orient(a, b, d);
        double o3 = orient(c, d, a);
        double o4 = orient(c, d, b);
        if (o1*o2 > 0 || o3*o4 > 0) return null;
        double denom = (a.x - b.x)*(c.y - d.y) - (a.y - b.y)*(c.x - d.x);
        if (Math.abs(denom) < 1e-9) return null;
        double t = ((a.x - c.x)*(c.y - d.y) - (a.y - c.y)*(c.x - d.x)) / denom;
        double u = ((a.x - c.x)*(a.y - b.y) - (a.y - c.y)*(a.x - b.x)) / denom;
        if (t >= -1e-9 && t <= 1+1e-9 && u >= -1e-9 && u <= 1+1e-9) {
            double x = a.x + t*(b.x - a.x);
            double y = a.y + t*(b.y - a.y);
            return new Point(x, y);
        }
        return null;
    }

    private ArrayList<Point> convexHull(ArrayList<Point> points) {
        if (points.size() <= 1) return points;
        points.sort((p1, p2) -> {
            if (p1.x != p2.x) return Double.compare(p1.x, p2.x);
            return Double.compare(p1.y, p2.y);
        });
        ArrayList<Point> hull = new ArrayList<>();
        for (int phase = 0; phase < 2; phase++) {
            int start = hull.size();
            for (Point p : points) {
                while (hull.size() >= start + 2 && orient(hull.get(hull.size()-2), hull.get(hull.size()-1), p) <= 0)
                    hull.remove(hull.size()-1);
                hull.add(p);
            }
            hull.remove(hull.size()-1);
            points.sort((p1, p2) -> {
                if (p1.x != p2.x) return Double.compare(p2.x, p1.x);
                return Double.compare(p2.y, p1.y);
            });
        }
        return hull;
    }

    private double polygonArea(ArrayList<Point> poly) {
        if (poly.size() < 3) return 0;
        double area = 0;
        for (int i = 0; i < poly.size(); i++) {
            Point p1 = poly.get(i);
            Point p2 = poly.get((i+1)%poly.size());
            area += p1.x * p2.y - p2.x * p1.y;
        }
        return Math.abs(area) / 2.0;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        width = getWidth();
        height = getHeight();

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);
        drawGrid(g2);
        drawAxes(g2);

        g2.setColor(Color.GRAY);
        for (Triangle t : triangles) drawTriangle(g2, t);

        if (showResult && bestA != null && bestB != null) {
            g2.setColor(new Color(0, 0, 255, 100));
            drawTriangle(g2, bestA);
            g2.setColor(new Color(0, 255, 0, 100));
            drawTriangle(g2, bestB);
            if (!intersectPolygon.isEmpty()) {
                g2.setColor(new Color(255, 0, 0, 80));
                fillPolygon(g2, intersectPolygon);
                g2.setColor(Color.RED);
                drawPolygon(g2, intersectPolygon);
            }
        }

        g2.setColor(Color.RED);
        for (Point p : currentPoints) {
            int sx = toScreenX(p.x), sy = toScreenY(p.y);
            g2.fillOval(sx-4, sy-4, 8, 8);
        }
    }

    private void drawTriangle(Graphics2D g2, Triangle t) {
        int[] x = new int[3], y = new int[3];
        for (int i = 0; i < 3; i++) {
            x[i] = toScreenX(t.vertices[i].x);
            y[i] = toScreenY(t.vertices[i].y);
        }
        g2.drawPolygon(x, y, 3);
    }

    private void fillPolygon(Graphics2D g2, ArrayList<Point> poly) {
        int[] x = new int[poly.size()], y = new int[poly.size()];
        for (int i = 0; i < poly.size(); i++) {
            x[i] = toScreenX(poly.get(i).x);
            y[i] = toScreenY(poly.get(i).y);
        }
        g2.fillPolygon(x, y, poly.size());
    }

    private void drawPolygon(Graphics2D g2, ArrayList<Point> poly) {
        for (int i = 0; i < poly.size(); i++) {
            Point p1 = poly.get(i);
            Point p2 = poly.get((i+1)%poly.size());
            g2.drawLine(toScreenX(p1.x), toScreenY(p1.y), toScreenX(p2.x), toScreenY(p2.y));
        }
    }

    private void drawGrid(Graphics2D g2) {
        g2.setColor(Color.LIGHT_GRAY);
        for (int x = (int)xMin; x <= xMax; x++) {
            int sx = toScreenX(x);
            g2.drawLine(sx, 0, sx, height);
        }
        for (int y = (int)yMin; y <= yMax; y++) {
            int sy = toScreenY(y);
            g2.drawLine(0, sy, width, sy);
        }
    }

    private void drawAxes(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        int zeroX = toScreenX(0), zeroY = toScreenY(0);
        g2.drawLine(zeroX, 0, zeroX, height);
        g2.drawLine(0, zeroY, width, zeroY);
        g2.fillPolygon(new int[]{zeroX, zeroX-4, zeroX+4}, new int[]{0,5,5}, 3);
        g2.fillPolygon(new int[]{width, width-5, width-5}, new int[]{zeroY, zeroY-4, zeroY+4}, 3);
        g2.drawString("X", width-10, zeroY-5);
        g2.drawString("Y", zeroX+5, 15);
    }

    private int toScreenX(double x) { return (int)((x - xMin)/(xMax-xMin)*width); }
    private int toScreenY(double y) { return (int)(height - (y - yMin)/(yMax-yMin)*height); }
    private double toWorldX(int sx) { return xMin + sx*(xMax-xMin)/width; }
    private double toWorldY(int sy) { return yMax - sy*(yMax-yMin)/height; }

    private static class Point { double x, y; Point(double x, double y) { this.x=x; this.y=y; } }
    private static class Triangle { Point[] vertices = new Point[3]; Triangle(Point a, Point b, Point c) { vertices[0]=a; vertices[1]=b; vertices[2]=c; } }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Пересечение треугольников (Enter для расчёта)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        Main panel = new Main();
        frame.add(panel);
        frame.setVisible(true);
    }
}
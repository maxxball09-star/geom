import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class TriangleOverlayApp extends JFrame {
    private List<Triangle> triangles = new ArrayList<>();
    private TrianglePanel panel;
    private JLabel statusLabel;
    private double scale = 1.0;
    private Point lastMousePoint;
    private List<Point2D.Double> currentMousePoints = new ArrayList<>();
    private boolean mouseInputMode = false;

    public TriangleOverlayApp() {
        setTitle("Пересечение треугольников");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);

        panel = new TrianglePanel();
        JScrollPane scrollPane = new JScrollPane(panel);
        add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        JButton loadButton = new JButton("Загрузить из файла");
        JButton keyboardButton = new JButton("Ввод с клавиатуры");
        JButton mouseButton = new JButton("Ввод мышкой (3 точки)");
        JButton findButton = new JButton("Найти пересечение");
        JButton clearButton = new JButton("Очистить");
        statusLabel = new JLabel("Готов");

        controlPanel.add(loadButton);
        controlPanel.add(keyboardButton);
        controlPanel.add(mouseButton);
        controlPanel.add(findButton);
        controlPanel.add(clearButton);
        controlPanel.add(statusLabel);
        add(controlPanel, BorderLayout.SOUTH);

        loadButton.addActionListener(e -> loadFromFile());
        keyboardButton.addActionListener(e -> inputFromKeyboard());
        mouseButton.addActionListener(e -> {
            mouseInputMode = true;
            currentMousePoints.clear();
            statusLabel.setText("Кликните 3 точки для треугольника");
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        });
        findButton.addActionListener(e -> findAndHighlight());
        clearButton.addActionListener(e -> {
            triangles.clear();
            panel.resetResults();
            statusLabel.setText("Очищено");
        });

        panel.addMouseWheelListener(e -> {
            double oldScale = scale;
            scale *= (e.getWheelRotation() < 0) ? 1.1 : 0.9;
            scale = Math.max(0.1, Math.min(10.0, scale));
            double scaleChange = scale / oldScale;
            panel.offsetX = e.getX() - scaleChange * (e.getX() - panel.offsetX);
            panel.offsetY = e.getY() - scaleChange * (e.getY() - panel.offsetY);
            panel.repaint();
        });

        panel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                lastMousePoint = e.getPoint();
                if (mouseInputMode && currentMousePoints.size() < 3) {
                    double worldX = (e.getX() - panel.offsetX) / scale;
                    double worldY = (e.getY() - panel.offsetY) / scale;
                    currentMousePoints.add(new Point2D.Double(worldX, worldY));
                    statusLabel.setText("Точка " + currentMousePoints.size() + " добавлена");
                    if (currentMousePoints.size() == 3) {
                        triangles.add(new Triangle(
                                currentMousePoints.get(0),
                                currentMousePoints.get(1),
                                currentMousePoints.get(2)
                        ));
                        statusLabel.setText("Треугольник добавлен. Всего: " + triangles.size());
                        currentMousePoints.clear();
                        mouseInputMode = false;
                        panel.setCursor(Cursor.getDefaultCursor());
                    }
                    panel.repaint();
                }
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (!mouseInputMode) {
                    panel.offsetX += e.getX() - lastMousePoint.x;
                    panel.offsetY += e.getY() - lastMousePoint.y;
                    lastMousePoint = e.getPoint();
                    panel.repaint();
                }
            }
        });

        setVisible(true);
    }

    private void loadFromFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Текстовые файлы", "txt"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader br = new BufferedReader(new FileReader(chooser.getSelectedFile()))) {
                triangles.clear();
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length == 6) {
                        double[] c = Arrays.stream(parts).mapToDouble(Double::parseDouble).toArray();
                        triangles.add(new Triangle(
                                new Point2D.Double(c[0], c[1]),
                                new Point2D.Double(c[2], c[3]),
                                new Point2D.Double(c[4], c[5])
                        ));
                    }
                }
                statusLabel.setText("Загружено " + triangles.size() + " треугольников");
                panel.resetResults();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка чтения файла: " + ex.getMessage());
            }
        }
    }

    private void inputFromKeyboard() {
        JTextField field = new JTextField(20);
        if (JOptionPane.showConfirmDialog(this, field,
                "Введите координаты (x1 y1 x2 y2 x3 y3):", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                String[] parts = field.getText().trim().split("\\s+");
                if (parts.length == 6) {
                    double[] c = Arrays.stream(parts).mapToDouble(Double::parseDouble).toArray();
                    triangles.add(new Triangle(
                            new Point2D.Double(c[0], c[1]),
                            new Point2D.Double(c[2], c[3]),
                            new Point2D.Double(c[4], c[5])
                    ));
                    statusLabel.setText("Добавлен треугольник. Всего: " + triangles.size());
                    panel.resetResults();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Неверный формат чисел");
            }
        }
    }

    private void findAndHighlight() {
        if (triangles.size() < 2) {
            JOptionPane.showMessageDialog(this, "Нужно хотя бы 2 треугольника");
            return;
        }
        double maxArea = -1;
        int bestI = -1, bestJ = -1;
        Area bestIntersection = null;

        for (int i = 0; i < triangles.size(); i++) {
            for (int j = i + 1; j < triangles.size(); j++) {
                Area a1 = triangles.get(i).getArea();
                Area a2 = triangles.get(j).getArea();
                Area inter = (Area) a1.clone();
                inter.intersect(a2);
                double area = getAreaSize(inter);
                if (area > maxArea) {
                    maxArea = area;
                    bestI = i;
                    bestJ = j;
                    bestIntersection = inter;
                }
            }
        }
        if (bestIntersection != null && maxArea > 1e-9) {
            panel.setResult(triangles.get(bestI), triangles.get(bestJ), bestIntersection);
            statusLabel.setText(String.format(
                    "Максимальная площадь пересечения: %.2f (треугольники %d и %d)",
                    maxArea, bestI, bestJ));
        } else {
            statusLabel.setText("Нет пересекающихся треугольников");
            panel.resetResults();
        }
    }

    private double getAreaSize(Area area) {
        PathIterator pi = area.getPathIterator(null);
        double areaSize = 0;
        double[] coords = new double[6];
        double lastX = 0, lastY = 0, firstX = 0, firstY = 0;
        boolean first = true;
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                if (first) {
                    firstX = coords[0]; firstY = coords[1];
                    first = false;
                } else {
                    areaSize += lastX * coords[1] - coords[0] * lastY;
                }
                lastX = coords[0]; lastY = coords[1];
            }
            pi.next();
        }
        areaSize += lastX * firstY - firstX * lastY;
        return Math.abs(areaSize) / 2.0;
    }

    class TrianglePanel extends JPanel {
        double offsetX = 600, offsetY = 400;
        private Triangle highlight1, highlight2;
        private Area intersectionArea;

        public TrianglePanel() {
            setPreferredSize(new Dimension(3000, 3000));
            setBackground(Color.WHITE);
        }

        public void setResult(Triangle t1, Triangle t2, Area inter) {
            highlight1 = t1;
            highlight2 = t2;
            intersectionArea = inter;
            repaint();
        }

        public void resetResults() {
            highlight1 = null;
            highlight2 = null;
            intersectionArea = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            AffineTransform transform = new AffineTransform();
            transform.translate(offsetX, offsetY);
            transform.scale(scale, scale);

            // Координатная сетка
            g2.setColor(new Color(220, 220, 220));
            double gridStep = 50; // мировой шаг
            double stepX = gridStep * scale;
            double stepY = gridStep * scale;
            if (stepX > 5) {
                double startX = offsetX % stepX;
                for (double x = startX; x < getWidth(); x += stepX)
                    g2.drawLine((int)x, 0, (int)x, getHeight());
                double startY = offsetY % stepY;
                for (double y = startY; y < getHeight(); y += stepY)
                    g2.drawLine(0, (int)y, getWidth(), (int)y);
            }

            // Оси
            g2.setColor(Color.GRAY);
            g2.drawLine((int)offsetX, 0, (int)offsetX, getHeight());
            g2.drawLine(0, (int)offsetY, getWidth(), (int)offsetY);

            // Треугольники
            for (int i = 0; i < triangles.size(); i++) {
                Triangle t = triangles.get(i);
                Polygon p = t.getPolygon(transform);
                if (t == highlight1 || t == highlight2) {
                    g2.setColor(new Color(255, 0, 0, 100));
                    g2.fill(p);
                    g2.setColor(Color.RED);
                    g2.setStroke(new BasicStroke(2));
                } else {
                    g2.setColor(Color.BLUE);
                    g2.setStroke(new BasicStroke(1));
                }
                g2.draw(p);
            }

            // Пересечение
            if (intersectionArea != null) {
                Area transformed = intersectionArea.createTransformedArea(transform);
                g2.setColor(new Color(0, 255, 0, 150));
                g2.fill(transformed);
                g2.setColor(Color.GREEN.darker());
                g2.setStroke(new BasicStroke(2));
                g2.draw(transformed);
            }

            // Точки ввода
            g2.setColor(Color.MAGENTA);
            for (Point2D.Double pt : currentMousePoints) {
                double x = pt.x * scale + offsetX;
                double y = pt.y * scale + offsetY;
                g2.fillOval((int)x - 4, (int)y - 4, 8, 8);
            }
        }
    }

    static class Triangle {
        Point2D.Double p1, p2, p3;
        Triangle(Point2D.Double p1, Point2D.Double p2, Point2D.Double p3) {
            this.p1 = p1; this.p2 = p2; this.p3 = p3;
        }
        Area getArea() {
            Path2D.Double path = new Path2D.Double();
            path.moveTo(p1.x, p1.y);
            path.lineTo(p2.x, p2.y);
            path.lineTo(p3.x, p3.y);
            path.closePath();
            return new Area(path);
        }
        Polygon getPolygon(AffineTransform transform) {
            Point2D.Double[] pts = {p1, p2, p3};
            int[] xs = new int[3], ys = new int[3];
            for (int i = 0; i < 3; i++) {
                Point2D tp = transform.transform(new Point2D.Double(pts[i].x, pts[i].y), null);
                xs[i] = (int) tp.getX();
                ys[i] = (int) tp.getY();
            }
            return new Polygon(xs, ys, 3);
        }
    }

    public static void main(String[] args) {
        new TriangleOverlayApp();
    }
}
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.sound.sampled.*;
import javax.swing.*;

public class UltraExoticCalculatorFinal {

    static int mouseX = 0, mouseY = 0;
    static JTextArea history;

    // ===== PARTICLES =====
    static class Particle {
        float x, y, dx, dy, size;

        Particle(int w, int h) {
            x = (float) (Math.random() * w);
            y = (float) (Math.random() * h);
            dx = (float) (Math.random() - 0.5);
            dy = (float) (Math.random() * 2 + 0.5);
            size = 2 + (float) Math.random() * 3;
        }

        void move(int w, int h) {
            x += dx + (mouseX - x) * 0.0007;
            y += dy + (mouseY - y) * 0.0007;

            if (y > h) y = 0;
            if (x > w) x = 0;
            if (x < 0) x = w;
        }
    }

    public static void main(String[] args) {

        JFrame frame = new JFrame("✦ ULTRA EXOTIC TERMINAL ✦");
        frame.setSize(700, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        ArrayList<Particle> particles = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            particles.add(new Particle(700, 500));
        }

        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;

                // Gradient background
                g2.setPaint(new GradientPaint(0, 0, new Color(5, 5, 20),
                        getWidth(), getHeight(), new Color(80, 0, 120)));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Particles
                g2.setColor(new Color(0, 255, 200));
                for (Particle p : particles) {
                    g2.fillOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
                }

                // Scanlines
                g2.setColor(new Color(0, 0, 0, 40));
                for (int i = 0; i < getHeight(); i += 3) {
                    g2.drawLine(0, i, getWidth(), i);
                }

                // Vignette
                g2.setPaint(new RadialGradientPaint(
                        new Point(getWidth() / 2, getHeight() / 2),
                        getWidth() / 2,
                        new float[]{0f, 1f},
                        new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 150)}
                ));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        panel.setLayout(new BorderLayout());

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });

        // ===== INPUT PANEL =====
        JPanel top = new JPanel(new GridLayout(2, 2, 10, 10));
        top.setOpaque(false);

        JTextField n1 = styledField();
        JTextField n2 = styledField();
        JTextField result = styledField();
        result.setEditable(false);

        addTypingSound(n1);
        addTypingSound(n2);

        top.add(label("INPUT A"));
        top.add(n1);
        top.add(label("INPUT B"));
        top.add(n2);

        // ===== BUTTONS =====
        JPanel center = new JPanel();
        center.setOpaque(false);

        JButton calc = new JButton("⚡ COMPUTE");
        JButton modeBtn = new JButton("AI MODE: OFF");

        styleButton(calc);
        styleButton(modeBtn);

        final boolean[] aiMode = {false};

        modeBtn.addActionListener(e -> {
            aiMode[0] = !aiMode[0];
            modeBtn.setText(aiMode[0] ? "AI MODE: ON" : "AI MODE: OFF");
            synthBeep(800, 50);
        });

        calc.addActionListener(e -> {
            synthBeep(1200, 60);

            try {
                double a = Double.parseDouble(n1.getText());
                double b = Double.parseDouble(n2.getText());
                double sum = a + b;

                if (aiMode[0]) {
                    fakeTyping(result, "Analyzing quantum aggregate...\nResult = " + sum);
                } else {
                    animateNumber(result, sum);
                }

                history.append("➤ " + a + " + " + b + " = " + sum + "\n");

            } catch (Exception ex) {
                synthBeep(200, 200);
                result.setText("ERROR");
            }
        });

        center.add(calc);
        center.add(modeBtn);

        // ===== HISTORY =====
        history = new JTextArea();
        history.setEditable(false);
        history.setBackground(new Color(0, 0, 0, 180));
        history.setForeground(new Color(0, 255, 200));
        JScrollPane scroll = new JScrollPane(history);

        // ===== BOTTOM =====
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(result, BorderLayout.NORTH);
        bottom.add(scroll, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(center, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        frame.setContentPane(panel);

        // ===== ANIMATION TIMER (FIXED) =====
        new javax.swing.Timer(30, e -> {
            for (Particle p : particles) {
                p.move(panel.getWidth(), panel.getHeight());
            }
            panel.repaint();
        }).start();

        frame.setVisible(true);
    }

    // ===== UI HELPERS =====
    static JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Consolas", Font.BOLD, 16));
        f.setBackground(new Color(10, 10, 30));
        f.setForeground(new Color(0, 255, 200));
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createLineBorder(new Color(0, 255, 200), 2));
        return f;
    }

    static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(new Color(200, 200, 255));
        return l;
    }

    static void styleButton(JButton b) {
        b.setFocusPainted(false);

        new javax.swing.Timer(80, new ActionListener() {
            float h = 0;

            public void actionPerformed(ActionEvent e) {
                b.setBackground(Color.getHSBColor(h, 1f, 1f));
                h += 0.02;
                if (h > 1) h = 0;
            }
        }).start();
    }

    // ===== SOUND =====
    static void synthBeep(int hz, int ms) {
        try {
            float sr = 44100;
            byte[] buf = new byte[(int) (ms * sr / 1000)];
            for (int i = 0; i < buf.length; i++) {
                double angle = i / (sr / hz) * 2 * Math.PI;
                buf[i] = (byte) (Math.sin(angle) * 127);
            }

            AudioFormat af = new AudioFormat(sr, 8, 1, true, false);
            SourceDataLine line = AudioSystem.getSourceDataLine(af);
            line.open(af);
            line.start();
            line.write(buf, 0, buf.length);
            line.drain();
            line.close();

        } catch (Exception ignored) {}
    }

    // ===== EFFECTS =====
    static void animateNumber(JTextField f, double target) {
        new Thread(() -> {
            double val = 0;
            for (int i = 0; i < 20; i++) {
                val += target / 20;
                f.setText(String.format("✦ %.2f ✦", val));
                synthBeep(600 + i * 20, 10);
                sleep(20);
            }
            f.setText("✦ " + target + " ✦");
        }).start();
    }

    static void fakeTyping(JTextField f, String text) {
        new Thread(() -> {
            f.setText("");
            for (char c : text.toCharArray()) {
                f.setText(f.getText() + c);
                synthBeep(1000, 5);
                sleep(15);
            }
        }).start();
    }

    static void addTypingSound(JTextField f) {
        f.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                synthBeep(900, 5);
            }
        });
    }

    static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {}
    }
}
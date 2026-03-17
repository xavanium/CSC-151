import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class M5_Project_Kuttler {

    // Constants
    static final double COST_PER_CUBIC_YARD = 150.0;
    static final double BAG_COVERAGE        = 0.45;
    static final double TRUCK_CAPACITY      = 10.0;

    // Thresholds for color-coding (cubic yards)
    static final double SMALL_JOB  = 5.0;
    static final double MEDIUM_JOB = 20.0;

    static final Color GREEN  = new Color(39, 174, 96);
    static final Color YELLOW = new Color(241, 196, 15);
    static final Color RED    = new Color(192, 57, 43);
    static final Color DARK   = new Color(52, 73, 94);
    static final Color LIGHT  = new Color(245, 245, 245);

    // ── Warehouse Icon ────────────────────────────────────────────────
    static class WarehouseIcon extends JPanel {
        WarehouseIcon() {
            setPreferredSize(new Dimension(120, 80));
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int bx = w / 2 - 45, by = h - 20, bw = 90, bh = 38;

            // Concrete slab
            g2.setColor(new Color(180, 170, 155));
            g2.fillRoundRect(bx - 5, by + bh - 6, bw + 10, 10, 4, 4);

            // Building body
            g2.setColor(new Color(150, 160, 175));
            g2.fillRect(bx, by, bw, bh);

            // Roof
            int[] rx = {bx - 8, bx + bw / 2, bx + bw + 8};
            int[] ry = {by, by - 22, by};
            g2.setColor(new Color(80, 100, 120));
            g2.fillPolygon(rx, ry, 3);

            // Door
            g2.setColor(new Color(90, 80, 70));
            g2.fillRect(bx + bw / 2 - 10, by + bh - 18, 20, 18);

            // Windows
            g2.setColor(new Color(200, 230, 255, 200));
            g2.fillRect(bx + 8, by + 8, 16, 12);
            g2.fillRect(bx + bw - 24, by + 8, 16, 12);

            // Outline
            g2.setColor(DARK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(bx, by, bw, bh);
            g2.drawPolygon(rx, ry, 3);

            // Label
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            g2.setColor(new Color(200, 200, 200));
            g2.drawString("Jim's Warehouse", bx + 4, by + bh + 14);
        }
    }

    // ── Bar Chart Panel ───────────────────────────────────────────────
    static class BarChartPanel extends JPanel {
        private int bags = 0, trucks = 0;
        private double cost = 0;
        private boolean hasData = false;

        BarChartPanel() {
            setPreferredSize(new Dimension(500, 140));
            setBackground(LIGHT);
            setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(DARK, 2),
                "Job Breakdown Chart",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 12), DARK));
        }

        void update(int bags, int trucks, double cost) {
            this.bags = bags; this.trucks = trucks; this.cost = cost;
            this.hasData = true;
            repaint();
        }

        void reset() { hasData = false; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!hasData) {
                g2.setColor(Color.GRAY);
                g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
                g2.drawString("Enter values and click Calculate to see chart.", 110, 75);
                return;
            }

            int chartX = 50, chartY = 28, chartH = 75;
            int barW = 65, gap = 45;

            // Bags use log scale; trucks and cost stay on linear scale
            double logBags   = bags   > 0 ? Math.log10(bags + 1) : 0;
            double linTrucks = trucks;
            double linCost   = cost / 100.0;
            double maxLog    = Math.max(logBags, 1);
            double maxLin    = Math.max(linTrucks, Math.max(linCost, 1));

            String[] barLabels = {"Bags (log)", "Trucks", "Cost ($\u00f7100)"};
            Color[]  colors    = {
                new Color(52, 152, 219),
                new Color(155, 89, 182),
                new Color(230, 126, 34)
            };
            double[] scaledH   = { logBags / maxLog, linTrucks / maxLin, linCost / maxLin };
            String[] valLabels = { String.valueOf(bags), String.valueOf(trucks), String.format("$%.0f", cost) };

            for (int i = 0; i < 3; i++) {
                int x  = chartX + i * (barW + gap);
                int bh = (int) (chartH * scaledH[i]);
                if (bh < 2 && scaledH[i] > 0) bh = 2;
                int y  = chartY + chartH - bh;

                g2.setColor(colors[i]);
                g2.fillRoundRect(x, y, barW, bh, 6, 6);
                g2.setColor(colors[i].darker());
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(x, y, barW, bh, 6, 6);

                g2.setColor(DARK);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                int sw = g2.getFontMetrics().stringWidth(valLabels[i]);
                g2.drawString(valLabels[i], x + barW / 2 - sw / 2, y - 4);

                g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                int lw = g2.getFontMetrics().stringWidth(barLabels[i]);
                g2.drawString(barLabels[i], x + barW / 2 - lw / 2, chartY + chartH + 15);
            }

            // Baseline
            g2.setColor(DARK);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(chartX - 5, chartY + chartH,
                        chartX + 3 * (barW + gap) - gap + 5, chartY + chartH);
        }
    }

    // ── Progress Bar factory ──────────────────────────────────────────
    static JProgressBar makeProgressBar() {
        JProgressBar pb = new JProgressBar(0, 100);
        pb.setStringPainted(true);
        pb.setString("Ready");
        pb.setValue(0);
        pb.setForeground(GREEN);
        pb.setBackground(new Color(220, 220, 220));
        pb.setFont(new Font("SansSerif", Font.BOLD, 11));
        pb.setPreferredSize(new Dimension(500, 22));
        return pb;
    }

    static void animateProgressBar(JProgressBar pb, Runnable onComplete) {
        pb.setValue(0);
        pb.setString("Calculating...");
        pb.setForeground(new Color(52, 152, 219));
        Timer timer = new Timer(15, null);
        timer.addActionListener(new ActionListener() {
            int progress = 0;
            public void actionPerformed(ActionEvent e) {
                progress += 4;
                pb.setValue(Math.min(progress, 100));
                if (progress >= 100) {
                    timer.stop();
                    pb.setString("Done!");
                    pb.setForeground(GREEN);
                    onComplete.run();
                }
            }
        });
        timer.start();
    }

    // ── Color-code helpers ────────────────────────────────────────────
    static Color  jobColor(double v) { return v < SMALL_JOB ? GREEN : v < MEDIUM_JOB ? YELLOW : RED; }
    static String jobLabel(double v) { return v < SMALL_JOB ? "Small Job" : v < MEDIUM_JOB ? "Medium Job" : "Large Job"; }

    // ── Main ──────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    static void createAndShowGUI() {
        JFrame frame = new JFrame("Jim's Concrete Calculator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(580, 840);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(8, 8));

        // ── Story + Icon ──────────────────────────────────────────────
        JPanel storyPanel = new JPanel(new BorderLayout(10, 0));
        storyPanel.setBackground(DARK);
        storyPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JPanel textLines = new JPanel();
        textLines.setBackground(DARK);
        textLines.setLayout(new BoxLayout(textLines, BoxLayout.Y_AXIS));
        for (String line : new String[]{
            "Jim inherited his business from his uncle.",
            "He builds metal warehouses.",
            "However, he needs to build his foundation on concrete,",
            "and he doesn't know a thing about concrete!",
            "So, let's help Jim out."
        }) {
            JLabel lbl = new JLabel(line);
            lbl.setForeground(Color.WHITE);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            textLines.add(lbl);
        }
        storyPanel.add(textLines,       BorderLayout.CENTER);
        storyPanel.add(new WarehouseIcon(), BorderLayout.EAST);

        // ── Inputs ───────────────────────────────────────────────────
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(DARK, 2),
            "Foundation Dimensions & Cost", 0, 0,
            new Font("SansSerif", Font.BOLD, 13), DARK));
        inputPanel.setBackground(LIGHT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        String[]     labels   = {"Length (ft):", "Width (ft):", "Depth (inches):", "Cost per cubic yard ($):"};
        JTextField[] fields   = new JTextField[4];
        String[]     defaults = {"", "", "", String.valueOf(COST_PER_CUBIC_YARD)};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0.4;
            inputPanel.add(new JLabel(labels[i]), gbc);
            fields[i] = new JTextField(defaults[i], 10);
            gbc.gridx = 1; gbc.weightx = 0.6;
            inputPanel.add(fields[i], gbc);
        }

        // ── Progress Bar ─────────────────────────────────────────────
        JProgressBar progressBar = makeProgressBar();
        JPanel pbPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        pbPanel.setBackground(Color.WHITE);
        pbPanel.add(progressBar);

        // ── Results ───────────────────────────────────────────────────
        JPanel resultsPanel = new JPanel(new GridBagLayout());
        resultsPanel.setBackground(LIGHT);
        resultsPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(GREEN, 2),
            "Results", 0, 0,
            new Font("SansSerif", Font.BOLD, 13), GREEN));

        JLabel jobSizeLabel = new JLabel(" ");
        jobSizeLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        jobSizeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        GridBagConstraints rgbc = new GridBagConstraints();
        rgbc.insets = new Insets(5, 15, 5, 15);
        rgbc.gridx = 0; rgbc.gridy = 0; rgbc.gridwidth = 2;
        rgbc.anchor = GridBagConstraints.CENTER;
        resultsPanel.add(jobSizeLabel, rgbc);
        rgbc.gridwidth = 1;

        String[] resultLabels = {
            "Area:", "Volume (cubic ft):", "Volume (cubic yards):",
            "Estimated Cost:", "80lb Bags Needed:", "Trucks Needed (min-max):"
        };
        JLabel[] resultValues = new JLabel[resultLabels.length];

        for (int i = 0; i < resultLabels.length; i++) {
            rgbc.gridy = i + 1;
            rgbc.gridx = 0; rgbc.anchor = GridBagConstraints.EAST; rgbc.weightx = 0.5;
            JLabel nl = new JLabel(resultLabels[i]);
            nl.setFont(new Font("SansSerif", Font.BOLD, 12));
            resultsPanel.add(nl, rgbc);

            rgbc.gridx = 1; rgbc.anchor = GridBagConstraints.WEST; rgbc.weightx = 0.5;
            resultValues[i] = new JLabel("--");
            resultValues[i].setFont(new Font("SansSerif", Font.PLAIN, 12));
            resultValues[i].setForeground(GREEN);
            resultsPanel.add(resultValues[i], rgbc);
        }

        // ── Bar Chart ─────────────────────────────────────────────────
        BarChartPanel barChart = new BarChartPanel();

        // ── Buttons ───────────────────────────────────────────────────
        JButton calcButton = new JButton("Calculate");
        calcButton.setBackground(DARK); calcButton.setForeground(Color.WHITE);
        calcButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        calcButton.setFocusPainted(false);
        calcButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // ── Enter key triggers Calculate on all fields ───────────────
        for (JTextField field : fields) {
            field.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        boolean allFilled = true;
                        for (JTextField f : fields) {
                            if (f.getText().trim().isEmpty()) {
                                allFilled = false;
                                break;
                            }
                        }
                        if (!allFilled) {
                            JOptionPane.showMessageDialog(frame,
                                "Please fill in all fields before pressing Enter.",
                                "Missing Input", JOptionPane.WARNING_MESSAGE);
                        } else {
                            calcButton.doClick();
                        }
                    }
                }
            });
        }

        JButton resetButton = new JButton("Reset");
        resetButton.setBackground(RED); resetButton.setForeground(Color.WHITE);
        resetButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        resetButton.setFocusPainted(false);
        resetButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(calcButton);
        buttonPanel.add(resetButton);

        // ── Center ────────────────────────────────────────────────────
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        centerPanel.add(inputPanel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(buttonPanel);
        centerPanel.add(Box.createVerticalStrut(6));
        centerPanel.add(pbPanel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(resultsPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(barChart);

        // ── Listeners ─────────────────────────────────────────────────
        calcButton.addActionListener(e -> {
            try {
                double length    = Double.parseDouble(fields[0].getText().trim());
                double width     = Double.parseDouble(fields[1].getText().trim());
                double depthIn   = Double.parseDouble(fields[2].getText().trim());
                double costPerYd = Double.parseDouble(fields[3].getText().trim());

                if (length <= 0 || width <= 0 || depthIn <= 0 || costPerYd < 0) {
                    JOptionPane.showMessageDialog(frame,
                        "Please enter positive values for all fields.",
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                double depthFt    = depthIn / 12.0;
                double area       = length * width;
                double volumeCuFt = area * depthFt;
                double volumeCuYd = volumeCuFt / 27.0;
                double cost       = volumeCuYd * costPerYd;
                int    bags       = (int) Math.ceil(volumeCuFt / BAG_COVERAGE);
                int    trucksMin  = (int) Math.ceil(volumeCuYd / 10.0); // max capacity
                int    trucksMax  = (int) Math.ceil(volumeCuYd / 8.0);  // min capacity
                int    trucks     = trucksMin;

                calcButton.setEnabled(false);
                animateProgressBar(progressBar, () -> {
                    Color  jColor = jobColor(volumeCuYd);
                    String jLabel = jobLabel(volumeCuYd);
                    jobSizeLabel.setText("\u25cf " + jLabel);
                    jobSizeLabel.setForeground(jColor);
                    for (JLabel lv : resultValues) lv.setForeground(jColor);
                    resultValues[0].setText(String.format("%.2f sq ft",  area));
                    resultValues[1].setText(String.format("%.2f cu ft",  volumeCuFt));
                    resultValues[2].setText(String.format("%.2f cu yd",  volumeCuYd));
                    resultValues[3].setText(String.format("$%.2f",       cost));
                    resultValues[4].setText(bags   + " bags");
                    resultValues[5].setText(trucksMin + " – " + trucksMax + " truck(s)");
                    barChart.update(bags, trucksMax, cost);
                    calcButton.setEnabled(true);
                });

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                    "Please fill in all fields with valid numbers.",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        resetButton.addActionListener(e -> {
            fields[0].setText(""); fields[1].setText("");
            fields[2].setText(""); fields[3].setText(String.valueOf(COST_PER_CUBIC_YARD));
            for (JLabel lv : resultValues) { lv.setText("--"); lv.setForeground(GREEN); }
            jobSizeLabel.setText(" ");
            progressBar.setValue(0); progressBar.setString("Ready"); progressBar.setForeground(GREEN);
            barChart.reset();
        });

        frame.add(storyPanel,  BorderLayout.NORTH);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }
}
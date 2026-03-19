import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Random;

public class M5_Project_Kuttler {

    // ── Constants ─────────────────────────────────────────────────────
    static final double BAG_COVERAGE = 0.45;
    static final double SMALL_JOB   = 5.0;
    static final double MEDIUM_JOB  = 20.0;

    static final String[] MIX_NAMES = {"Standard", "High-Strength", "Lightweight"};
    static final double[] MIX_COSTS = {150.0, 210.0, 175.0};
    static final String[] MIX_DESCS = {
        "General purpose. Good for slabs & foundations.",
        "Extra durability for heavy loads & structures.",
        "Reduced weight, easier to work with."
    };

    // ── Theme System ──────────────────────────────────────────────────
    static class Theme {
        String name;
        Color bg, panelBg, border, text, subtext, accent, accentAlt, headerBg, headerText, histBg, histText;
        Theme(String name, Color bg, Color panelBg, Color border, Color text, Color subtext,
              Color accent, Color accentAlt, Color headerBg, Color headerText, Color histBg, Color histText) {
            this.name=name; this.bg=bg; this.panelBg=panelBg; this.border=border;
            this.text=text; this.subtext=subtext; this.accent=accent; this.accentAlt=accentAlt;
            this.headerBg=headerBg; this.headerText=headerText; this.histBg=histBg; this.histText=histText;
        }
    }

    static final Theme[] THEMES = {
        new Theme("Light",
            new Color(240,242,245), new Color(255,255,255),
            new Color(52,73,94), new Color(30,30,30), new Color(120,120,120),
            new Color(39,174,96), new Color(52,152,219),
            new Color(52,73,94), Color.WHITE,
            new Color(30,40,50), new Color(160,220,160)),
        new Theme("Dark",
            new Color(25,28,36), new Color(35,39,50),
            new Color(80,100,140), new Color(220,220,230), new Color(140,145,160),
            new Color(80,200,120), new Color(90,160,230),
            new Color(18,20,28), new Color(210,215,230),
            new Color(18,20,28), new Color(120,210,150)),
        new Theme("Blueprint",
            new Color(10,30,70), new Color(15,45,100),
            new Color(80,140,220), new Color(180,210,255), new Color(100,150,210),
            new Color(80,200,255), new Color(255,200,60),
            new Color(5,18,50), new Color(160,200,255),
            new Color(5,18,50), new Color(80,200,255))
    };

    static int currentTheme = 0;
    static Theme theme() { return THEMES[currentTheme]; }

    // ── Glowing Card Panel ────────────────────────────────────────────
    static class CardPanel extends JPanel {
        private int arc;
        private float glowAlpha = 0f;       // 0..1 pulsing glow
        private boolean glowing = false;
        private Timer glowTimer;

        CardPanel(int arc) {
            this.arc = arc;
            setOpaque(false);
        }

        void startGlow() {
            glowing = true; glowAlpha = 0f;
            if (glowTimer != null) glowTimer.stop();
            glowTimer = new Timer(30, null);
            glowTimer.addActionListener(new ActionListener() {
                double t = 0;
                public void actionPerformed(ActionEvent e) {
                    t += 0.06;
                    glowAlpha = (float)(0.5 + 0.5 * Math.sin(t));
                    repaint();
                    if (t > Math.PI * 6) { // ~3 full pulses then fade out
                        glowTimer.stop(); glowing = false; glowAlpha = 0f; repaint();
                    }
                }
            });
            glowTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth()-4, h = getHeight()-4;
            // Drop shadow
            g2.setColor(new Color(0,0,0,40));
            g2.fillRoundRect(4, 6, w, h, arc, arc);
            // Card bg
            g2.setColor(theme().panelBg);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            // Glow border
            if (glowing && glowAlpha > 0) {
                Color ac = theme().accent;
                for (int i = 4; i >= 1; i--) {
                    float a = glowAlpha * (0.15f * i);
                    g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), (int)(a*255)));
                    g2.setStroke(new BasicStroke(i * 2.5f));
                    g2.drawRoundRect(0, 0, w, h, arc, arc);
                }
            }
            // Normal border
            g2.setColor(theme().border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, w, h, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Confetti Particle ─────────────────────────────────────────────
    static class Particle {
        float x, y, vx, vy, rot, rotV, size;
        Color color;
        int life, maxLife;
        int shape; // 0=rect, 1=circle, 2=triangle

        static final Color[] COLORS = {
            new Color(255,80,80), new Color(80,200,120), new Color(80,160,255),
            new Color(255,200,50), new Color(200,80,255), new Color(255,140,50)
        };

        Particle(int cx, int cy) {
            Random r = new Random();
            x = cx; y = cy;
            double angle = r.nextDouble() * Math.PI * 2;
            float speed = 2f + r.nextFloat() * 6f;
            vx = (float)(Math.cos(angle) * speed);
            vy = (float)(Math.sin(angle) * speed) - 4f;
            rot = r.nextFloat() * 360f;
            rotV = (r.nextFloat() - 0.5f) * 12f;
            size = 6f + r.nextFloat() * 8f;
            color = COLORS[r.nextInt(COLORS.length)];
            maxLife = 60 + r.nextInt(40);
            life = maxLife;
            shape = r.nextInt(3);
        }

        void update() { x+=vx; y+=vy; vy+=0.18f; vx*=0.98f; rot+=rotV; life--; }
        boolean dead() { return life<=0; }
        float alpha() { return Math.max(0f, (float)life/maxLife); }
    }

    // ── Confetti Overlay (glass pane layer) ───────────────────────────
    static class ConfettiPanel extends JPanel {
        private ArrayList<Particle> particles = new ArrayList<>();
        private Timer confettiTimer;

        ConfettiPanel() { setOpaque(false); setLayout(null); }

        void burst(int cx, int cy) {
            particles.clear();
            for (int i=0; i<120; i++) particles.add(new Particle(cx, cy));
            if (confettiTimer != null) confettiTimer.stop();
            confettiTimer = new Timer(16, e -> {
                particles.removeIf(Particle::dead);
                for (Particle p : particles) p.update();
                repaint();
                if (particles.isEmpty()) { confettiTimer.stop(); }
            });
            confettiTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (Particle p : new ArrayList<>(particles)) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha()));
                g2.setColor(p.color);
                AffineTransform old = g2.getTransform();
                g2.rotate(Math.toRadians(p.rot), p.x, p.y);
                int s = (int)p.size;
                if      (p.shape==0) g2.fillRect((int)p.x-s/2, (int)p.y-s/2, s, s/2);
                else if (p.shape==1) g2.fillOval((int)p.x-s/2, (int)p.y-s/2, s, s);
                else {
                    int[] px={(int)p.x,(int)(p.x+s/2),(int)(p.x-s/2)};
                    int[] py={(int)(p.y-s/2),(int)(p.y+s/2),(int)(p.y+s/2)};
                    g2.fillPolygon(px,py,3);
                }
                g2.setTransform(old);
            }
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
    }

    // ── Concrete Pour Results Card ────────────────────────────────────
    // Shows a gray liquid that fills up behind the results
    static class ConcreteResultsCard extends JPanel {
        private float fillLevel = 0f;  // 0..1
        private Timer fillTimer;
        private boolean hasData = false;

        ConcreteResultsCard() { setOpaque(false); }

        void animateFill() {
            hasData = true; fillLevel = 0f;
            if (fillTimer != null) fillTimer.stop();
            fillTimer = new Timer(20, null);
            fillTimer.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fillLevel = Math.min(1f, fillLevel + 0.018f);
                    repaint();
                    if (fillLevel >= 1f) fillTimer.stop();
                }
            });
            fillTimer.start();
        }

        void reset() { fillLevel = 0f; hasData = false; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth()-4, h = getHeight()-4, arc = 16;

            // Shadow
            g2.setColor(new Color(0,0,0,40));
            g2.fillRoundRect(4, 6, w, h, arc, arc);

            // Card background
            g2.setColor(theme().panelBg);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            // Concrete fill (clips to card shape)
            if (hasData && fillLevel > 0f) {
                Shape clip = new RoundRectangle2D.Float(0, 0, w, h, arc, arc);
                g2.setClip(clip);
                int fillH = (int)(h * fillLevel);
                int fillY = h - fillH;

                // Wavy surface
                int[] waveX = new int[w+2];
                int[] waveY = new int[w+2];
                long t = System.currentTimeMillis();
                for (int x2=0; x2<=w; x2++) {
                    waveX[x2] = x2;
                    waveY[x2] = fillY + (int)(4 * Math.sin((x2 * 0.04) + t * 0.003));
                }
                // Build polygon for liquid
                int[] px = new int[w+4];
                int[] py = new int[w+4];
                px[0]=0; py[0]=h;
                for (int x2=0; x2<=w; x2++) { px[x2+1]=waveX[x2]; py[x2+1]=waveY[x2]; }
                px[w+2]=w; py[w+2]=h;
                px[w+3]=0; py[w+3]=h;

                // Concrete gradient: darker gray at bottom, lighter at top
                GradientPaint gp = new GradientPaint(0, fillY, new Color(160,160,165,180),
                                                      0, h,     new Color(110,110,115,200));
                g2.setPaint(gp);
                g2.fillPolygon(px, py, w+4);

                // Aggregate texture dots
                g2.setColor(new Color(90,90,95,80));
                Random rng = new Random(42);
                for (int i=0; i<30; i++) {
                    int dx = rng.nextInt(w), dy = fillY + rng.nextInt(Math.max(1,fillH));
                    g2.fillOval(dx, dy, 3+rng.nextInt(4), 3+rng.nextInt(4));
                }
                g2.setClip(null);
            }

            // Border (normal + glow handled by caller)
            g2.setColor(theme().border);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, w, h, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Warehouse Icon ────────────────────────────────────────────────
    static class WarehouseIcon extends JPanel {
        WarehouseIcon() { setPreferredSize(new Dimension(110,85)); setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),bx=w/2-40,by=h-22,bw=80,bh=36;
            g2.setColor(new Color(180,170,155)); g2.fillRoundRect(bx-5,by+bh-5,bw+10,9,4,4);
            g2.setColor(new Color(150,160,175)); g2.fillRect(bx,by,bw,bh);
            int[] rx={bx-7,bx+bw/2,bx+bw+7},ry={by,by-20,by};
            g2.setColor(new Color(80,100,120)); g2.fillPolygon(rx,ry,3);
            g2.setColor(new Color(90,80,70)); g2.fillRect(bx+bw/2-9,by+bh-16,18,16);
            g2.setColor(new Color(200,230,255,200));
            g2.fillRect(bx+7,by+7,14,10); g2.fillRect(bx+bw-21,by+7,14,10);
            g2.setColor(theme().border); g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(bx,by,bw,bh); g2.drawPolygon(rx,ry,3);
            g2.setFont(new Font("SansSerif",Font.BOLD,8));
            g2.setColor(new Color(180,180,180)); g2.drawString("Jim's Warehouse",bx+2,by+bh+13);
        }
    }

    // ── Gauge Panel (3 speedometer gauges) ───────────────────────────
    static class GaugePanel extends JPanel {
        private int bags=0, trucksMin=0, trucksMax=0; private double cost=0;
        private boolean hasData=false;
        // Animated sweep angles (0..1)
        private float aBags=0, aTrucksMin=0, aTrucksMax=0, aCost=0;
        private Timer sweepTimer;

        // Each gauge has a sensible max scale
        private int maxBags=500, maxTrucks=20;
        private double maxCost=10000;

        GaugePanel() {
            setPreferredSize(new Dimension(560, 200));
            setOpaque(false);
        }

        void update(int b, int tMin, int tMax, double c) {
            bags=b; trucksMin=tMin; trucksMax=tMax; cost=c; hasData=true;
            maxBags   = Math.max(500,  (int)(b    * 1.25));
            maxTrucks = Math.max(20,   (int)(tMax * 1.25));
            maxCost   = Math.max(10000, c * 1.25);
            aBags=0; aTrucksMin=0; aTrucksMax=0; aCost=0;
            if (sweepTimer!=null) sweepTimer.stop();
            sweepTimer = new Timer(16, null);
            sweepTimer.addActionListener(new ActionListener() {
                float tb  =(float)Math.min(1.0,(double)bags/maxBags);
                float ttMn=(float)Math.min(1.0,(double)trucksMin/maxTrucks);
                float ttMx=(float)Math.min(1.0,(double)trucksMax/maxTrucks);
                float tc  =(float)Math.min(1.0,cost/maxCost);
                public void actionPerformed(ActionEvent e) {
                    float speed=0.025f;
                    aBags      = Math.min(tb,   aBags+speed);
                    aTrucksMin = Math.min(ttMn, aTrucksMin+speed);
                    aTrucksMax = Math.min(ttMx, aTrucksMax+speed);
                    aCost      = Math.min(tc,   aCost+speed);
                    repaint();
                    if (aBags>=tb && aTrucksMin>=ttMn && aTrucksMax>=ttMx && aCost>=tc) sweepTimer.stop();
                }
            });
            sweepTimer.start();
        }

        void reset() {
            hasData=false; aBags=0; aTrucksMin=0; aTrucksMax=0; aCost=0;
            if (sweepTimer!=null) sweepTimer.stop();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int W=getWidth()-4, H=getHeight()-4;
            g2.setColor(new Color(0,0,0,30)); g2.fillRoundRect(4,6,W,H,18,18);
            g2.setColor(theme().panelBg);     g2.fillRoundRect(0,0,W,H,18,18);
            g2.setColor(theme().border); g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0,0,W,H,18,18);

            g2.setFont(new Font("SansSerif",Font.BOLD,12)); g2.setColor(theme().text);
            g2.drawString("Job Gauges",14,20);

            if (!hasData) {
                g2.setColor(theme().subtext); g2.setFont(new Font("SansSerif",Font.ITALIC,12));
                g2.drawString("Enter values and click Calculate to see gauges.", 70, 110);
                return;
            }

            int spacing=getWidth()/3;
            int[] cx={spacing/2, spacing+spacing/2, spacing*2+spacing/2};
            int cy=getHeight()/2+10, r=68;
            Color colBags=new Color(52,152,219), colTruck=new Color(155,89,182), colCost=new Color(230,126,34);

            // Bags gauge (single needle)
            drawGauge(g2,cx[0],cy,r,aBags,-1,colBags,"80lb Bags",
                String.valueOf(bags),"max "+maxBags);

            // Trucks gauge (two needles: min=bright, max=dimmer)
            drawGauge(g2,cx[1],cy,r,aTrucksMin,aTrucksMax,colTruck,"Trucks",
                trucksMin+"–"+trucksMax,"max "+maxTrucks);

            // Cost gauge (single needle)
            drawGauge(g2,cx[2],cy,r,aCost,-1,colCost,"Est. Cost",
                String.format("$%.0f",cost),"max $"+(int)maxCost);
        }

        // sweepMin = primary needle; sweepMax = second needle (-1 = none)
        private void drawGauge(Graphics2D g2, int cx, int cy, int r,
                                float sweepMin, float sweepMax, Color col,
                                String label, String value, String maxLbl) {
            int startAngle=195, totalArc=210;

            // Track
            g2.setStroke(new BasicStroke(10f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(150,150,155,80));
            g2.drawArc(cx-r,cy-r,r*2,r*2,startAngle,-totalArc);

            // Fill arc to primary sweep
            if (sweepMin>0) {
                g2.setColor(col);
                g2.drawArc(cx-r,cy-r,r*2,r*2,startAngle,-(int)(totalArc*sweepMin));
            }

            // Tick marks
            g2.setStroke(new BasicStroke(1.2f));
            for (int t2=0;t2<=10;t2++) {
                double a=Math.toRadians(startAngle-(totalArc*t2/10.0));
                int inner=(t2%5==0)?r-14:r-9;
                g2.setColor(t2%5==0?theme().text:theme().subtext);
                g2.drawLine((int)(cx+Math.cos(a)*r),(int)(cy-Math.sin(a)*r),
                            (int)(cx+Math.cos(a)*inner),(int)(cy-Math.sin(a)*inner));
            }

            // Second needle (max trucks) — drawn first so min needle renders on top
            if (sweepMax>=0) {
                double angMax=Math.toRadians(startAngle-totalArc*sweepMax);
                int nx2=(int)(cx+Math.cos(angMax)*(r-22));
                int ny2=(int)(cy-Math.sin(angMax)*(r-22));
                g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                // Dimmer / semi-transparent color for max needle
                g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),140));
                g2.drawLine(cx,cy,nx2,ny2);
                // Small diamond tip on max needle
                int[] dx={nx2,nx2+4,nx2,nx2-4}, dy={ny2-4,ny2,ny2+4,ny2};
                g2.fillPolygon(dx,dy,4);
            }

            // Primary needle (min trucks / only needle for bags & cost)
            double angMin=Math.toRadians(startAngle-totalArc*sweepMin);
            int nx=(int)(cx+Math.cos(angMin)*(r-18));
            int ny=(int)(cy-Math.sin(angMin)*(r-18));
            g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g2.setColor(col.brighter());
            g2.drawLine(cx,cy,nx,ny);

            // Hub
            g2.setColor(theme().text); g2.fillOval(cx-5,cy-5,10,10);
            g2.setColor(col);          g2.fillOval(cx-3,cy-3, 6, 6);

            // Value
            g2.setFont(new Font("SansSerif",Font.BOLD,16)); g2.setColor(col);
            FontMetrics fm=g2.getFontMetrics();
            g2.drawString(value,cx-fm.stringWidth(value)/2,cy+20);

            // Label
            g2.setFont(new Font("SansSerif",Font.BOLD,11)); g2.setColor(theme().text);
            fm=g2.getFontMetrics();
            g2.drawString(label,cx-fm.stringWidth(label)/2,cy+34);

            // Legend for two-needle gauge
            if (sweepMax>=0) {
                g2.setFont(new Font("SansSerif",Font.PLAIN,9)); g2.setColor(col.brighter());
                g2.drawString("min",cx-18,cy+46);
                g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),160));
                g2.drawString("max",cx+4,cy+46);
            }

            // Max scale label
            g2.setFont(new Font("SansSerif",Font.PLAIN,9)); g2.setColor(theme().subtext);
            fm=g2.getFontMetrics();
            g2.drawString(maxLbl,cx-fm.stringWidth(maxLbl)/2,cy+r-2);
        }
    }

    // ── Truck Animation Panel ─────────────────────────────────────────
    static class TruckPanel extends JPanel {
        private float truckX=-160f, progress=0f; private int wheelAngle=0; private boolean running=false;
        private Timer animTimer;
        TruckPanel(){setPreferredSize(new Dimension(560,80));setOpaque(false);}
        void start(Runnable onComplete){
            truckX=-160f; progress=0f; wheelAngle=0; running=true;
            if(animTimer!=null&&animTimer.isRunning())animTimer.stop();
            animTimer=new Timer(16,null);
            animTimer.addActionListener(new ActionListener(){
                int step=0; final int total=90;
                public void actionPerformed(ActionEvent e){
                    step++; progress=(float)step/total;
                    float eased=progress<0.5f?2*progress*progress:(float)(1-Math.pow(-2*progress+2,2)/2);
                    int tw=getWidth()>0?getWidth():560;
                    truckX=-160f+eased*(tw+160f);
                    wheelAngle=(step*8)%360; repaint();
                    if(step>=total){animTimer.stop();running=false;onComplete.run();}
                }
            });
            animTimer.start();
        }
        void reset(){if(animTimer!=null)animTimer.stop();running=false;truckX=-160f;progress=0f;repaint();}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),ground=h-14;
            g2.setColor(new Color(80,80,85)); g2.fillRoundRect(0,ground,w,12,4,4);
            g2.setColor(new Color(255,220,60,180));
            g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10f,new float[]{14f,10f},(float)(progress*24)));
            g2.drawLine(0,ground+6,w,ground+6); g2.setStroke(new BasicStroke(1f));
            if(!running&&progress==0f)return;
            int tx=(int)truckX,ty=ground-52;
            // Drum
            int dX=tx+55,dY=ty+4,dW=68,dH=40;
            g2.setPaint(new GradientPaint(dX,dY,new Color(220,100,30),dX+dW,dY+dH,new Color(160,60,10)));
            g2.fillRoundRect(dX,dY,dW,dH,14,14);
            g2.setColor(new Color(255,255,255,60)); g2.setStroke(new BasicStroke(3f));
            for(int s=0;s<4;s++){int sx=dX+8+s*14+(wheelAngle/20);if(sx<dX+dW-4)g2.drawLine(sx,dY+4,sx-8,dY+dH-4);}
            g2.setStroke(new BasicStroke(1.5f)); g2.setColor(new Color(120,50,10));
            g2.drawRoundRect(dX,dY,dW,dH,14,14);
            int[] cX2={tx+118,tx+130,tx+125,tx+112},cY2={ty+35,ty+33,ty+50,ty+50};
            g2.setColor(new Color(140,70,20)); g2.fillPolygon(cX2,cY2,4);
            // Cab
            int cabX=tx+2,cabY=ty+14,cabW=58,cabH=38;
            g2.setPaint(new GradientPaint(cabX,cabY,new Color(60,130,200),cabX+cabW,cabY+cabH,new Color(30,80,140)));
            g2.fillRoundRect(cabX,cabY,cabW,cabH,10,10);
            g2.setPaint(new GradientPaint(cabX+6,cabY-12,new Color(70,150,220),cabX+6,cabY+4,new Color(50,110,180)));
            g2.fillRoundRect(cabX+6,cabY-12,cabW-12,22,10,10);
            g2.setColor(new Color(180,230,255,200)); g2.fillRoundRect(cabX+8,cabY-8,cabW-22,18,6,6);
            g2.setColor(new Color(100,180,230,80)); g2.fillRect(cabX+14,cabY-6,6,14);
            g2.setColor(new Color(20,60,110)); g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(cabX,cabY,cabW,cabH,10,10); g2.drawRoundRect(cabX+6,cabY-12,cabW-12,22,10,10);
            g2.setColor(new Color(40,100,170)); g2.setStroke(new BasicStroke(1f));
            g2.drawLine(cabX+30,cabY+4,cabX+30,cabY+cabH-4);
            g2.setColor(new Color(50,50,60)); g2.fillRoundRect(cabX,cabY+cabH-6,cabW,10,4,4);
            g2.setColor(new Color(255,240,150)); g2.fillOval(cabX+4,cabY+cabH-14,10,8);
            g2.setColor(new Color(200,180,80)); g2.setStroke(new BasicStroke(1f));
            g2.drawOval(cabX+4,cabY+cabH-14,10,8);
            g2.setColor(new Color(60,60,65)); g2.fillRect(cabX+42,cabY-20,5,16);
            if(running){
                int pa=60+(int)(40*Math.sin(wheelAngle*0.1));
                for(int p=0;p<3;p++){int ps=7+p*4,py2=cabY-22-p*9,px2=cabX+42+p*2;
                    g2.setColor(new Color(180,180,180,Math.max(0,pa-p*15)));g2.fillOval(px2,py2,ps,ps);}
            }
            g2.setColor(new Color(40,40,45)); g2.fillRect(tx+2,ty+50,130,6);
            drawWheel(g2,tx+18,ground-12,13,wheelAngle);
            drawWheel(g2,tx+90,ground-12,13,wheelAngle);
            drawWheel(g2,tx+112,ground-12,13,wheelAngle);
            g2.setColor(new Color(255,255,255,180)); g2.setFont(new Font("SansSerif",Font.BOLD,7));
            g2.drawString("CONCRETE",dX+8,dY+dH/2+3);
        }
        private void drawWheel(Graphics2D g2,int cx,int cy,int r,int angle){
            g2.setColor(new Color(30,30,30)); g2.fillOval(cx-r,cy-r,r*2,r*2);
            g2.setColor(new Color(190,190,200)); g2.fillOval(cx-r+3,cy-r+3,(r-3)*2,(r-3)*2);
            g2.setColor(new Color(80,80,90)); g2.fillOval(cx-4,cy-4,8,8);
            g2.setColor(new Color(130,130,140)); g2.setStroke(new BasicStroke(1.5f));
            for(int s=0;s<5;s++){double a=Math.toRadians(angle+s*72);
                g2.drawLine(cx,cy,(int)(cx+Math.cos(a)*(r-5)),(int)(cy+Math.sin(a)*(r-5)));}
            g2.setColor(new Color(20,20,20)); g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(cx-r,cy-r,r*2,r*2);
        }
    }

    // ── Progress Bar ──────────────────────────────────────────────────
    static JProgressBar makeProgressBar(){
        JProgressBar pb=new JProgressBar(0,100);
        pb.setStringPainted(true); pb.setString("Ready"); pb.setValue(0);
        pb.setForeground(new Color(39,174,96)); pb.setBackground(new Color(220,220,220));
        pb.setFont(new Font("SansSerif",Font.BOLD,11)); pb.setPreferredSize(new Dimension(520,22));
        return pb;
    }

    static void animateProgressBar(JProgressBar pb, TruckPanel truck, Runnable onComplete){
        pb.setValue(0); pb.setString("Calculating..."); pb.setForeground(new Color(52,152,219));
        Timer t=new Timer(15,null);
        t.addActionListener(new ActionListener(){int p=0;
            public void actionPerformed(ActionEvent e){
                p+=4; pb.setValue(Math.min(p,100));
                if(p>=100){t.stop();pb.setString("Done!");pb.setForeground(new Color(39,174,96));}
            }
        });
        t.start();
        truck.start(onComplete);
    }

    // ── Count-up animators ────────────────────────────────────────────
    static void animateCount(JLabel lbl,double target,String pre,String suf,int dec){
        Timer t=new Timer(16,null);
        t.addActionListener(new ActionListener(){int s=0;final int total=30;
            public void actionPerformed(ActionEvent e){
                s++; double v=target*((double)s/total);
                lbl.setText(pre+String.format("%."+dec+"f",v)+suf);
                if(s>=total){t.stop();lbl.setText(pre+String.format("%."+dec+"f",target)+suf);}
            }
        }); t.start();
    }
    static void animateInt(JLabel lbl,int target,String suf){
        Timer t=new Timer(16,null);
        t.addActionListener(new ActionListener(){int s=0;final int total=30;
            public void actionPerformed(ActionEvent e){
                s++; lbl.setText((int)(target*((double)s/total))+suf);
                if(s>=total){t.stop();lbl.setText(target+suf);}
            }
        }); t.start();
    }

    // ── Slide-in animator for a row ───────────────────────────────────
    // Each row starts translated right and fades in over ~20 frames
    static void slideIn(JComponent comp, int delayMs) {
        comp.setVisible(false);
        Timer t = new Timer(delayMs, null);
        t.setRepeats(false);
        t.addActionListener(ev -> {
            comp.setVisible(true);
            // We animate via a lightweight wrapper repaint trick using an alpha timer
            Timer fade = new Timer(16, null);
            fade.addActionListener(new ActionListener() {
                int step = 0; final int total = 18;
                public void actionPerformed(ActionEvent e2) {
                    step++;
                    float alpha = (float) step / total;
                    comp.putClientProperty("slideAlpha", alpha);
                    comp.repaint();
                    if (step >= total) { fade.stop(); comp.putClientProperty("slideAlpha", 1f); }
                }
            });
            fade.start();
        });
        t.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────
    static Color jobColor(double v){return v<SMALL_JOB?new Color(39,174,96):v<MEDIUM_JOB?new Color(241,196,15):new Color(192,57,43);}
    static String jobLabel(double v){return v<SMALL_JOB?"Small Job":v<MEDIUM_JOB?"Medium Job":"Large Job";}

    // ── Globals ───────────────────────────────────────────────────────
    static JFrame mainFrame;
    static ConfettiPanel confettiLayer;

    public static void main(String[] args){SwingUtilities.invokeLater(()->createAndShowGUI());}

    static void createAndShowGUI(){
        mainFrame=new JFrame("Jim's Concrete Calculator");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(620,980);
        mainFrame.setLocationRelativeTo(null);
        // Confetti lives on the glass pane
        confettiLayer=new ConfettiPanel();
        confettiLayer.setBounds(0,0,620,980);
        mainFrame.setGlassPane(confettiLayer);
        confettiLayer.setVisible(true);
        buildUI();
        mainFrame.setVisible(true);
    }

    static void buildUI(){
        mainFrame.getContentPane().removeAll();

        // ── Theme bar ─────────────────────────────────────────────────
        JPanel themeBar=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,6));
        themeBar.setBackground(theme().headerBg);
        JLabel tLbl=new JLabel("Theme: "); tLbl.setForeground(theme().headerText);
        tLbl.setFont(new Font("SansSerif",Font.BOLD,11)); themeBar.add(tLbl);
        String[] tNames={"☀ Light","🌙 Dark","📐 Blueprint"};
        for(int i=0;i<tNames.length;i++){
            final int idx=i; JButton tb=new JButton(tNames[i]);
            tb.setFont(new Font("SansSerif",Font.BOLD,11)); tb.setFocusPainted(false);
            tb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            tb.setBackground(idx==currentTheme?theme().accent:theme().panelBg);
            tb.setForeground(idx==currentTheme?Color.WHITE:theme().text);
            tb.setBorder(BorderFactory.createEmptyBorder(4,10,4,10));
            tb.addActionListener(e->{currentTheme=idx;buildUI();}); themeBar.add(tb);
        }

        // ── Header ────────────────────────────────────────────────────
        JPanel header=new JPanel(new FlowLayout(FlowLayout.CENTER,16,10));
        header.setBackground(theme().headerBg);
        JLabel title=new JLabel("Jim's Concrete Calculator");
        title.setFont(new Font("SansSerif",Font.BOLD,20)); title.setForeground(theme().headerText);
        header.add(new WarehouseIcon()); header.add(title);
        JPanel topPanel=new JPanel(new BorderLayout());
        topPanel.setBackground(theme().headerBg);
        topPanel.add(themeBar,BorderLayout.NORTH); topPanel.add(header,BorderLayout.CENTER);

        // ── Unit toggle card ──────────────────────────────────────────
        CardPanel unitCard=new CardPanel(16);
        unitCard.setLayout(new FlowLayout(FlowLayout.LEFT,12,8));
        JLabel uLbl=new JLabel("Units:"); uLbl.setFont(new Font("SansSerif",Font.BOLD,12)); uLbl.setForeground(theme().text);
        JRadioButton impBtn=new JRadioButton("Imperial (ft / in)",true);
        JRadioButton metBtn=new JRadioButton("Metric (m / cm)");
        for(JRadioButton rb:new JRadioButton[]{impBtn,metBtn}){rb.setOpaque(false);rb.setForeground(theme().text);rb.setFont(new Font("SansSerif",Font.PLAIN,12));}
        ButtonGroup ug=new ButtonGroup(); ug.add(impBtn); ug.add(metBtn);
        unitCard.add(uLbl); unitCard.add(impBtn); unitCard.add(metBtn);

        // ── Mix card ──────────────────────────────────────────────────
        CardPanel mixCard=new CardPanel(16); mixCard.setLayout(new GridBagLayout());
        GridBagConstraints mc=new GridBagConstraints(); mc.insets=new Insets(5,12,5,12); mc.fill=GridBagConstraints.HORIZONTAL;
        JLabel mTitle=new JLabel("🪨  Concrete Mix"); mTitle.setFont(new Font("SansSerif",Font.BOLD,13)); mTitle.setForeground(theme().text);
        mc.gridx=0;mc.gridy=0;mc.gridwidth=2; mixCard.add(mTitle,mc); mc.gridwidth=1;
        JComboBox<String> mixCombo=new JComboBox<>(MIX_NAMES); mixCombo.setFont(new Font("SansSerif",Font.PLAIN,12));
        JLabel mDesc=new JLabel(MIX_DESCS[0]); mDesc.setFont(new Font("SansSerif",Font.ITALIC,11)); mDesc.setForeground(theme().subtext);
        JLabel mCost=new JLabel("Cost: $"+(int)MIX_COSTS[0]+" / cu yd"); mCost.setFont(new Font("SansSerif",Font.BOLD,12)); mCost.setForeground(theme().accent);
        mc.gridx=0;mc.gridy=1;mc.weightx=0.35; JLabel mL=new JLabel("Mix Type:"); mL.setForeground(theme().text); mixCard.add(mL,mc);
        mc.gridx=1;mc.weightx=0.65; mixCard.add(mixCombo,mc);
        mc.gridx=0;mc.gridy=2;mc.gridwidth=2; mixCard.add(mDesc,mc);
        mc.gridy=3; mixCard.add(mCost,mc);
        mixCombo.addActionListener(e->{int i=mixCombo.getSelectedIndex();mDesc.setText(MIX_DESCS[i]);mCost.setText("Cost: $"+(int)MIX_COSTS[i]+" / cu yd");});

        // ── Input card ────────────────────────────────────────────────
        CardPanel inputCard=new CardPanel(16); inputCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(6,12,6,12); gbc.fill=GridBagConstraints.HORIZONTAL;
        JLabel iTitle=new JLabel("📐  Foundation Dimensions"); iTitle.setFont(new Font("SansSerif",Font.BOLD,13)); iTitle.setForeground(theme().text);
        gbc.gridx=0;gbc.gridy=0;gbc.gridwidth=2; inputCard.add(iTitle,gbc); gbc.gridwidth=1;
        JLabel[] dLbls={new JLabel("Length (ft):"),new JLabel("Width (ft):"),new JLabel("Depth (in):")};
        JTextField[] fields=new JTextField[3];
        for(int i=0;i<3;i++){
            dLbls[i].setForeground(theme().text); dLbls[i].setFont(new Font("SansSerif",Font.PLAIN,12));
            gbc.gridx=0;gbc.gridy=i+1;gbc.weightx=0.4; inputCard.add(dLbls[i],gbc);
            fields[i]=new JTextField("",10); fields[i].setBackground(theme().bg);
            fields[i].setForeground(theme().text); fields[i].setCaretColor(theme().text);
            fields[i].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme().border,1,true),
                BorderFactory.createEmptyBorder(4,6,4,6)));
            gbc.gridx=1;gbc.weightx=0.6; inputCard.add(fields[i],gbc);
        }
        impBtn.addActionListener(e->{dLbls[0].setText("Length (ft):");dLbls[1].setText("Width (ft):");dLbls[2].setText("Depth (in):");});
        metBtn.addActionListener(e->{dLbls[0].setText("Length (m):");dLbls[1].setText("Width (m):");dLbls[2].setText("Depth (cm):");});

        // ── Truck + progress bar ──────────────────────────────────────
        TruckPanel truckPanel=new TruckPanel();
        truckPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        truckPanel.setMaximumSize(new Dimension(560,80));
        JProgressBar progressBar=makeProgressBar();
        JPanel pbWrap=new JPanel(new FlowLayout(FlowLayout.CENTER,0,2));
        pbWrap.setOpaque(false); pbWrap.add(progressBar);

        // ── Results card (concrete pour background) ───────────────────
        ConcreteResultsCard resultsCard=new ConcreteResultsCard();
        resultsCard.setLayout(new GridBagLayout());
        GridBagConstraints rgbc=new GridBagConstraints(); rgbc.insets=new Insets(5,14,5,14);

        JLabel rTitle=new JLabel("📊  Results"); rTitle.setFont(new Font("SansSerif",Font.BOLD,13)); rTitle.setForeground(theme().text);
        rgbc.gridx=0;rgbc.gridy=0;rgbc.gridwidth=2;rgbc.anchor=GridBagConstraints.CENTER;
        resultsCard.add(rTitle,rgbc);
        JLabel jobSizeLbl=new JLabel(" "); jobSizeLbl.setFont(new Font("SansSerif",Font.BOLD,13));
        rgbc.gridy=1; resultsCard.add(jobSizeLbl,rgbc); rgbc.gridwidth=1;

        String[] icons={"□","▦","▣","$","❖","▶"};
        String[] rLabels={"Area:","Volume (cu ft):","Volume (cu yd):","Estimated Cost:","80lb Bags Needed:","Trucks Needed (min–max):"};
        JLabel[] rValues=new JLabel[rLabels.length];
        JPanel[] rRows=new JPanel[rLabels.length]; // for slide-in

        for(int i=0;i<rLabels.length;i++){
            rgbc.gridy=i+2; rgbc.gridx=0; rgbc.anchor=GridBagConstraints.EAST; rgbc.weightx=0.5;
            JLabel nl=new JLabel(icons[i]+"  "+rLabels[i]); nl.setFont(new Font("SansSerif",Font.BOLD,12)); nl.setForeground(theme().text);
            resultsCard.add(nl,rgbc);
            rgbc.gridx=1; rgbc.anchor=GridBagConstraints.WEST; rgbc.weightx=0.5;
            rValues[i]=new JLabel("--"); rValues[i].setFont(new Font("SansSerif",Font.PLAIN,12)); rValues[i].setForeground(theme().accent);
            resultsCard.add(rValues[i],rgbc);
        }

        // ── Tip label ─────────────────────────────────────────────────
        JLabel tipLbl=new JLabel(" "); tipLbl.setFont(new Font("SansSerif",Font.ITALIC,12));
        tipLbl.setForeground(theme().subtext); tipLbl.setHorizontalAlignment(SwingConstants.CENTER);

        // ── Bar chart ─────────────────────────────────────────────────
        GaugePanel gaugePanel=new GaugePanel();

        // ── History ───────────────────────────────────────────────────
        DefaultListModel<String> histModel=new DefaultListModel<>();
        JList<String> histList=new JList<>(histModel);
        histList.setFont(new Font("Monospaced",Font.PLAIN,11));
        histList.setBackground(theme().histBg); histList.setForeground(theme().histText);
        histList.setSelectionBackground(theme().border);
        JScrollPane histScroll=new JScrollPane(histList);
        histScroll.setPreferredSize(new Dimension(520,100));
        histScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(theme().border,2),"📋  Calculation History",
            TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,
            new Font("SansSerif",Font.BOLD,12),theme().text));

        // ── Buttons ───────────────────────────────────────────────────
        JButton calcBtn=new JButton("⚙ Calculate"); styleBtn(calcBtn,theme().accent,Color.WHITE);
        JButton resetBtn=new JButton("↺ Reset"); styleBtn(resetBtn,new Color(192,57,43),Color.WHITE);
        JButton clearBtn=new JButton("🗑 Clear History"); styleBtn(clearBtn,new Color(100,100,110),Color.WHITE);

        for(JTextField f:fields){
            f.addKeyListener(new KeyAdapter(){
                public void keyPressed(KeyEvent e){
                    if(e.getKeyCode()==KeyEvent.VK_ENTER){
                        boolean ok=true;
                        for(JTextField fx:fields)if(fx.getText().trim().isEmpty()){ok=false;break;}
                        if(!ok)JOptionPane.showMessageDialog(mainFrame,"Please fill in all fields before pressing Enter.","Missing Input",JOptionPane.WARNING_MESSAGE);
                        else calcBtn.doClick();
                    }
                }
            });
        }

        JPanel btnPanel=new JPanel(new FlowLayout(FlowLayout.CENTER,14,6)); btnPanel.setOpaque(false);
        btnPanel.add(calcBtn); btnPanel.add(resetBtn); btnPanel.add(clearBtn);

        // ── Center ────────────────────────────────────────────────────
        JPanel center=new JPanel(); center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));
        center.setBackground(theme().bg); center.setBorder(BorderFactory.createEmptyBorder(12,18,12,18));

        for(JComponent c:new JComponent[]{unitCard,mixCard,inputCard}){
            c.setAlignmentX(Component.CENTER_ALIGNMENT); center.add(c); center.add(Box.createVerticalStrut(10));
        }
        for(JComponent c:new JComponent[]{btnPanel,truckPanel,pbWrap,resultsCard,tipLbl,gaugePanel,histScroll})
            c.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(btnPanel);     center.add(Box.createVerticalStrut(8));
        center.add(truckPanel);   center.add(Box.createVerticalStrut(4));
        center.add(pbWrap);       center.add(Box.createVerticalStrut(10));
        center.add(resultsCard);  center.add(Box.createVerticalStrut(4));
        center.add(tipLbl);       center.add(Box.createVerticalStrut(10));
        center.add(gaugePanel);     center.add(Box.createVerticalStrut(10));
        center.add(histScroll);   center.add(Box.createVerticalStrut(8));

        JScrollPane scroll=new JScrollPane(center);
        scroll.setBorder(null); scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(theme().bg);

        // ── Calculate ─────────────────────────────────────────────────
        calcBtn.addActionListener(e->{
            try{
                boolean metric=metBtn.isSelected();
                double rL=Double.parseDouble(fields[0].getText().trim());
                double rW=Double.parseDouble(fields[1].getText().trim());
                double rD=Double.parseDouble(fields[2].getText().trim());
                double length=metric?rL*3.28084:rL, width=metric?rW*3.28084:rW, depthIn=metric?rD/2.54:rD;
                if(length<=0||width<=0||depthIn<=0){JOptionPane.showMessageDialog(mainFrame,"Please enter positive values.","Input Error",JOptionPane.ERROR_MESSAGE);return;}
                int mix=mixCombo.getSelectedIndex(); double costPerYd=MIX_COSTS[mix];
                double depthFt=depthIn/12.0,area=length*width,volFt=area*depthFt,volYd=volFt/27.0,cost=volYd*costPerYd;
                int bags=(int)Math.ceil(volFt/BAG_COVERAGE),tMin=(int)Math.ceil(volYd/10.0),tMax=(int)Math.ceil(volYd/8.0);
                String tip=volYd<1?"\uD83D\uDCA1 Tip: Small pour \u2014 bagged concrete may be more practical than a truck.":tMax==tMin?"\uD83D\uDCA1 Tip: Order "+(tMin+1)+" trucks as a buffer.":"\uD83D\uDCA1 Tip: Budget for "+tMax+" trucks; fewer if loads are full.";
                String uStr=metric?"m":"ft";
                String hist=String.format("#%d | %s | %.1f%s\u00d7%.1f%s\u00d7%.0f%s | %.2fyd\u00b3 | %d bags | %d\u2013%d trucks | $%.2f",
                    histModel.size()+1,MIX_NAMES[mix],rL,uStr,rW,uStr,rD,metric?"cm":"in",volYd,bags,tMin,tMax,cost);

                calcBtn.setEnabled(false);
                animateProgressBar(progressBar,truckPanel,()->{
                    Color jc=jobColor(volYd);
                    jobSizeLbl.setText("\u25cf "+jobLabel(volYd)+" ("+MIX_NAMES[mix]+" Mix)");
                    jobSizeLbl.setForeground(jc);
                    for(JLabel lv:rValues)lv.setForeground(jc);

                    // Concrete pour fill
                    resultsCard.animateFill();

                    // Slide-in each row with staggered delay
                    for(int i=0;i<rValues.length;i++) slideIn(rValues[i], i*80);

                    // Count-up animators (staggered to match slide)
                    new Timer(0*80,  ev->{ animateCount(rValues[0],area,"","  sq ft",2); ((Timer)ev.getSource()).stop(); }).start();
                    new Timer(1*80,  ev->{ animateCount(rValues[1],volFt,"","  cu ft",2); ((Timer)ev.getSource()).stop(); }).start();
                    new Timer(2*80,  ev->{ animateCount(rValues[2],volYd,"","  cu yd",2); ((Timer)ev.getSource()).stop(); }).start();
                    new Timer(3*80,  ev->{ animateCount(rValues[3],cost,"$","",2); ((Timer)ev.getSource()).stop(); }).start();
                    new Timer(4*80,  ev->{ animateInt(rValues[4],bags,"  bags"); ((Timer)ev.getSource()).stop(); }).start();
                    new Timer(5*80,  ev->{ rValues[5].setText(tMin+"\u2013"+tMax+" truck(s)"); ((Timer)ev.getSource()).stop(); }).start();

                    tipLbl.setText(tip);
                    gaugePanel.update(bags,tMin,tMax,cost);
                    histModel.add(0,hist);

                    // Confetti burst from center-top of screen
                    int cx=mainFrame.getWidth()/2, cy=mainFrame.getHeight()/3;
                    confettiLayer.setBounds(0,0,mainFrame.getWidth(),mainFrame.getHeight());
                    confettiLayer.burst(cx,cy);

                    calcBtn.setEnabled(true);
                });
            }catch(NumberFormatException ex){
                JOptionPane.showMessageDialog(mainFrame,"Please fill in all fields with valid numbers.","Input Error",JOptionPane.ERROR_MESSAGE);
            }
        });

        resetBtn.addActionListener(e->{
            for(JTextField f:fields)f.setText("");
            for(JLabel lv:rValues){lv.setText("--");lv.setForeground(theme().accent);lv.setVisible(true);}
            jobSizeLbl.setText(" "); tipLbl.setText(" ");
            progressBar.setValue(0); progressBar.setString("Ready"); progressBar.setForeground(new Color(39,174,96));
            gaugePanel.reset(); truckPanel.reset(); resultsCard.reset();
        });
        clearBtn.addActionListener(e->histModel.clear());

        mainFrame.add(topPanel,BorderLayout.NORTH);
        mainFrame.add(scroll,BorderLayout.CENTER);
        mainFrame.revalidate(); mainFrame.repaint();
    }

    static void styleBtn(JButton b,Color bg,Color fg){
        b.setBackground(bg); b.setForeground(fg);
        b.setFont(new Font("SansSerif",Font.BOLD,13));
        b.setFocusPainted(false); b.setBorderPainted(false); b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(8,18,8,18));
    }
}
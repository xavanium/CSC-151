import javax.swing.*;
import javax.swing.border.*;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.print.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class M5_Project_Kuttler {

    // ── Constants ─────────────────────────────────────────────────────
    static final double BAG_COVERAGE = 0.45;
    static final double SMALL_JOB   = 5.0;
    static final double MEDIUM_JOB  = 20.0;

    static final String[] MIX_NAMES = {"Standard","High-Strength","Lightweight"};
    static final double[] MIX_COSTS = {150.0, 210.0, 175.0};
    static final String[] MIX_DESCS = {
        "General purpose. Good for slabs & foundations.",
        "Extra durability for heavy loads & structures.",
        "Reduced weight, easier to work with."
    };

    // ── Theme ─────────────────────────────────────────────────────────
    static class Theme {
        String name;
        Color bg,panelBg,border,text,subtext,accent,accentAlt,headerBg,headerText,histBg,histText;
        Theme(String n,Color bg,Color pb,Color bo,Color tx,Color su,Color ac,Color aa,Color hb,Color ht,Color lb,Color lt){
            name=n;this.bg=bg;panelBg=pb;border=bo;text=tx;subtext=su;accent=ac;accentAlt=aa;headerBg=hb;headerText=ht;histBg=lb;histText=lt;
        }
    }
    static final Theme[] THEMES={
        new Theme("Light",new Color(240,242,245),new Color(255,255,255),new Color(52,73,94),new Color(30,30,30),new Color(120,120,120),new Color(39,174,96),new Color(52,152,219),new Color(52,73,94),Color.WHITE,new Color(30,40,50),new Color(160,220,160)),
        new Theme("Dark",new Color(25,28,36),new Color(35,39,50),new Color(80,100,140),new Color(220,220,230),new Color(140,145,160),new Color(80,200,120),new Color(90,160,230),new Color(18,20,28),new Color(210,215,230),new Color(18,20,28),new Color(120,210,150)),
        new Theme("Blueprint",new Color(10,30,70),new Color(15,45,100),new Color(80,140,220),new Color(180,210,255),new Color(100,150,210),new Color(80,200,255),new Color(255,200,60),new Color(5,18,50),new Color(160,200,255),new Color(5,18,50),new Color(80,200,255))
    };
    static int currentTheme=0;
    static Theme theme(){return THEMES[currentTheme];}

    // ── Last calculation (for print/compare) ──────────────────────────
    static double lastArea=0,lastVolFt=0,lastVolYd=0,lastCost=0;
    static int lastBags=0,lastTMin=0,lastTMax=0,lastMix=0;
    static double lastWaste=0;

    // ── Sound: play horn.wav from same directory as the .java file ───
    static void playHorn(){
        new Thread(()->{
            try{
                // Look for horn.wav next to the running class
                java.io.File wavFile = new java.io.File("horn.wav");
                if(!wavFile.exists()){
                    // Also try the directory of the class file
                    String classDir = M5_Project_Kuttler.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI().getPath();
                    wavFile = new java.io.File(new java.io.File(classDir).getParent(), "horn.wav");
                }
                if(!wavFile.exists()) return; // silently skip if file missing
                AudioInputStream ais = AudioSystem.getAudioInputStream(wavFile);
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
                // Wait for clip to finish then close
                clip.addLineListener(event -> {
                    if(event.getType() == LineEvent.Type.STOP){
                        clip.close();
                    }
                });
            }catch(Exception ignored){}
        }).start();
    }


    // ── Card Panel ────────────────────────────────────────────────────
    static class CardPanel extends JPanel {
        private int arc; private float glowAlpha=0f; private Timer glowTimer;
        CardPanel(int arc){this.arc=arc;setOpaque(false);}
        void startGlow(){
            glowAlpha=0f;
            if(glowTimer!=null)glowTimer.stop();
            glowTimer=new Timer(30,null);
            glowTimer.addActionListener(new ActionListener(){double t=0;
                public void actionPerformed(ActionEvent e){
                    t+=0.06; glowAlpha=(float)(0.5+0.5*Math.sin(t)); repaint();
                    if(t>Math.PI*6){glowTimer.stop();glowAlpha=0f;repaint();}
                }
            });
            glowTimer.start();
        }
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth()-4,h=getHeight()-4;
            g2.setColor(new Color(0,0,0,40)); g2.fillRoundRect(4,6,w,h,arc,arc);
            g2.setColor(theme().panelBg);     g2.fillRoundRect(0,0,w,h,arc,arc);
            if(glowAlpha>0){Color ac=theme().accent;
                for(int i=4;i>=1;i--){float a=glowAlpha*(0.15f*i);
                    g2.setColor(new Color(ac.getRed(),ac.getGreen(),ac.getBlue(),(int)(a*255)));
                    g2.setStroke(new BasicStroke(i*2.5f)); g2.drawRoundRect(0,0,w,h,arc,arc);}}
            g2.setColor(theme().border); g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0,0,w,h,arc,arc); g2.dispose(); super.paintComponent(g);
        }
    }

    // ── Confetti ──────────────────────────────────────────────────────
    static class Particle {
        float x,y,vx,vy,rot,rotV,size; Color color; int life,maxLife,shape;
        static final Color[] COLORS={new Color(255,80,80),new Color(80,200,120),new Color(80,160,255),new Color(255,200,50),new Color(200,80,255),new Color(255,140,50)};
        Particle(int cx,int cy){Random r=new Random();x=cx;y=cy;double a=r.nextDouble()*Math.PI*2;float sp=2f+r.nextFloat()*6f;vx=(float)(Math.cos(a)*sp);vy=(float)(Math.sin(a)*sp)-4f;rot=r.nextFloat()*360f;rotV=(r.nextFloat()-0.5f)*12f;size=6f+r.nextFloat()*8f;color=COLORS[r.nextInt(COLORS.length)];maxLife=60+r.nextInt(40);life=maxLife;shape=r.nextInt(3);}
        void update(){x+=vx;y+=vy;vy+=0.18f;vx*=0.98f;rot+=rotV;life--;}
        boolean dead(){return life<=0;}
        float alpha(){return Math.max(0f,(float)life/maxLife);}
    }
    static class ConfettiPanel extends JPanel {
        private ArrayList<Particle> parts=new ArrayList<>(); private Timer t;
        ConfettiPanel(){setOpaque(false);setLayout(null);}
        void burst(int cx,int cy){
            parts.clear();
            for(int i=0;i<120;i++) parts.add(new Particle(cx,cy));
            if(t!=null) t.stop();
            t=new Timer(16,null);
            // Use ActionListener so we can reference t via the event source
            t.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    parts.removeIf(Particle::dead);
                    for(Particle p:parts) p.update();
                    repaint();
                    if(parts.isEmpty()) ((Timer)e.getSource()).stop();
                }
            });
            t.start();
        }
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);for(Particle p:new ArrayList<>(parts)){g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,p.alpha()));g2.setColor(p.color);AffineTransform old=g2.getTransform();g2.rotate(Math.toRadians(p.rot),p.x,p.y);int s=(int)p.size;if(p.shape==0)g2.fillRect((int)p.x-s/2,(int)p.y-s/2,s,s/2);else if(p.shape==1)g2.fillOval((int)p.x-s/2,(int)p.y-s/2,s,s);else{int[]px={(int)p.x,(int)(p.x+s/2),(int)(p.x-s/2)};int[]py={(int)(p.y-s/2),(int)(p.y+s/2),(int)(p.y+s/2)};g2.fillPolygon(px,py,3);}g2.setTransform(old);}g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,1f));}
    }

    // ── Concrete pour results card ────────────────────────────────────
    static class ConcreteResultsCard extends JPanel {
        private float fillLevel=0f; private Timer ft; private boolean hasData=false;
        ConcreteResultsCard(){setOpaque(false);}
        void animateFill(){hasData=true;fillLevel=0f;if(ft!=null)ft.stop();ft=new Timer(20,null);ft.addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e){fillLevel=Math.min(1f,fillLevel+0.018f);repaint();if(fillLevel>=1f)ft.stop();}});ft.start();}
        void reset(){fillLevel=0f;hasData=false;repaint();}
        @Override protected void paintComponent(Graphics g){
            Graphics2D g2=(Graphics2D)g.create();g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth()-4,h=getHeight()-4,arc=16;
            g2.setColor(new Color(0,0,0,40));g2.fillRoundRect(4,6,w,h,arc,arc);
            g2.setColor(theme().panelBg);g2.fillRoundRect(0,0,w,h,arc,arc);
            if(hasData&&fillLevel>0f){Shape clip=new RoundRectangle2D.Float(0,0,w,h,arc,arc);g2.setClip(clip);int fillH=(int)(h*fillLevel),fillY=h-fillH;int[]wx=new int[w+2],wy=new int[w+2];long time=System.currentTimeMillis();for(int x2=0;x2<=w;x2++){wx[x2]=x2;wy[x2]=fillY+(int)(4*Math.sin(x2*0.04+time*0.003));}int[]px=new int[w+4],py=new int[w+4];px[0]=0;py[0]=h;for(int x2=0;x2<=w;x2++){px[x2+1]=wx[x2];py[x2+1]=wy[x2];}px[w+2]=w;py[w+2]=h;px[w+3]=0;py[w+3]=h;g2.setPaint(new GradientPaint(0,fillY,new Color(160,160,165,180),0,h,new Color(110,110,115,200)));g2.fillPolygon(px,py,w+4);g2.setColor(new Color(90,90,95,80));Random rng=new Random(42);for(int i=0;i<30;i++){int dx=rng.nextInt(w),dy=fillY+rng.nextInt(Math.max(1,fillH));g2.fillOval(dx,dy,3+rng.nextInt(4),3+rng.nextInt(4));}g2.setClip(null);}
            g2.setColor(theme().border);g2.setStroke(new BasicStroke(1.5f));g2.drawRoundRect(0,0,w,h,arc,arc);g2.dispose();super.paintComponent(g);
        }
    }

    // ── Warehouse icon ────────────────────────────────────────────────
    static class WarehouseIcon extends JPanel {
        WarehouseIcon(){setPreferredSize(new Dimension(110,85));setOpaque(false);}
        @Override protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);int w=getWidth(),h=getHeight(),bx=w/2-40,by=h-22,bw=80,bh=36;g2.setColor(new Color(180,170,155));g2.fillRoundRect(bx-5,by+bh-5,bw+10,9,4,4);g2.setColor(new Color(150,160,175));g2.fillRect(bx,by,bw,bh);int[]rx={bx-7,bx+bw/2,bx+bw+7},ry={by,by-20,by};g2.setColor(new Color(80,100,120));g2.fillPolygon(rx,ry,3);g2.setColor(new Color(90,80,70));g2.fillRect(bx+bw/2-9,by+bh-16,18,16);g2.setColor(new Color(200,230,255,200));g2.fillRect(bx+7,by+7,14,10);g2.fillRect(bx+bw-21,by+7,14,10);g2.setColor(theme().border);g2.setStroke(new BasicStroke(1.5f));g2.drawRect(bx,by,bw,bh);g2.drawPolygon(rx,ry,3);g2.setFont(new Font("SansSerif",Font.BOLD,8));g2.setColor(new Color(180,180,180));g2.drawString("Jim's Warehouse",bx+2,by+bh+13);}
    }

    // ── Gauge Panel ───────────────────────────────────────────────────
    static class GaugePanel extends JPanel {
        private int bags=0,trucksMin=0,trucksMax=0; private double cost=0; private boolean hasData=false;
        private float aBags=0,aTrucksMin=0,aTrucksMax=0,aCost=0; private Timer sweepTimer;
        private int maxBags=500,maxTrucks=20; private double maxCost=10000;
        GaugePanel(){setPreferredSize(new Dimension(560,200));setOpaque(false);}
        void update(int b,int tMn,int tMx,double c){bags=b;trucksMin=tMn;trucksMax=tMx;cost=c;hasData=true;maxBags=Math.max(500,(int)(b*1.25));maxTrucks=Math.max(20,(int)(tMx*1.25));maxCost=Math.max(10000,c*1.25);aBags=0;aTrucksMin=0;aTrucksMax=0;aCost=0;if(sweepTimer!=null)sweepTimer.stop();sweepTimer=new Timer(16,null);sweepTimer.addActionListener(new ActionListener(){float tb=(float)Math.min(1.0,(double)bags/maxBags),ttMn=(float)Math.min(1.0,(double)trucksMin/maxTrucks),ttMx=(float)Math.min(1.0,(double)trucksMax/maxTrucks),tc=(float)Math.min(1.0,cost/maxCost);public void actionPerformed(ActionEvent e){float sp=0.025f;aBags=Math.min(tb,aBags+sp);aTrucksMin=Math.min(ttMn,aTrucksMin+sp);aTrucksMax=Math.min(ttMx,aTrucksMax+sp);aCost=Math.min(tc,aCost+sp);repaint();if(aBags>=tb&&aTrucksMin>=ttMn&&aTrucksMax>=ttMx&&aCost>=tc)sweepTimer.stop();}});sweepTimer.start();}
        void reset(){hasData=false;aBags=0;aTrucksMin=0;aTrucksMax=0;aCost=0;if(sweepTimer!=null)sweepTimer.stop();repaint();}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int W=getWidth()-4,H=getHeight()-4;g2.setColor(new Color(0,0,0,30));g2.fillRoundRect(4,6,W,H,18,18);g2.setColor(theme().panelBg);g2.fillRoundRect(0,0,W,H,18,18);g2.setColor(theme().border);g2.setStroke(new BasicStroke(1.5f));g2.drawRoundRect(0,0,W,H,18,18);
            g2.setFont(new Font("SansSerif",Font.BOLD,12));g2.setColor(theme().text);g2.drawString("Job Gauges",14,20);
            if(!hasData){g2.setColor(theme().subtext);g2.setFont(new Font("SansSerif",Font.ITALIC,12));g2.drawString("Enter values and click Calculate to see gauges.",70,110);return;}
            int sp=getWidth()/3;int[]cx={sp/2,sp+sp/2,sp*2+sp/2};int cy=getHeight()/2+10,r=68;
            drawGauge(g2,cx[0],cy,r,aBags,-1,new Color(52,152,219),"80lb Bags",String.valueOf(bags),"max "+maxBags);
            drawGauge(g2,cx[1],cy,r,aTrucksMin,aTrucksMax,new Color(155,89,182),"Trucks",trucksMin+"-"+trucksMax,"max "+maxTrucks);
            drawGauge(g2,cx[2],cy,r,aCost,-1,new Color(230,126,34),"Est. Cost",String.format("$%.0f",cost),"max $"+(int)maxCost);
        }
        private void drawGauge(Graphics2D g2,int cx,int cy,int r,float sweepMin,float sweepMax,Color col,String label,String value,String maxLbl){
            int sa=195,ta=210;
            g2.setStroke(new BasicStroke(10f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g2.setColor(new Color(150,150,155,80));g2.drawArc(cx-r,cy-r,r*2,r*2,sa,-ta);
            if(sweepMin>0){g2.setColor(col);g2.drawArc(cx-r,cy-r,r*2,r*2,sa,-(int)(ta*sweepMin));}
            g2.setStroke(new BasicStroke(1.2f));for(int t2=0;t2<=10;t2++){double a=Math.toRadians(sa-(ta*t2/10.0));int in=(t2%5==0)?r-14:r-9;g2.setColor(t2%5==0?theme().text:theme().subtext);g2.drawLine((int)(cx+Math.cos(a)*r),(int)(cy-Math.sin(a)*r),(int)(cx+Math.cos(a)*in),(int)(cy-Math.sin(a)*in));}
            if(sweepMax>=0){double aM=Math.toRadians(sa-ta*sweepMax);int nx2=(int)(cx+Math.cos(aM)*(r-22)),ny2=(int)(cy-Math.sin(aM)*(r-22));g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),140));g2.drawLine(cx,cy,nx2,ny2);int[]dx={nx2,nx2+4,nx2,nx2-4},dy={ny2-4,ny2,ny2+4,ny2};g2.fillPolygon(dx,dy,4);}
            double aN=Math.toRadians(sa-ta*sweepMin);int nx=(int)(cx+Math.cos(aN)*(r-18)),ny=(int)(cy-Math.sin(aN)*(r-18));g2.setStroke(new BasicStroke(2.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g2.setColor(col.brighter());g2.drawLine(cx,cy,nx,ny);g2.setColor(theme().text);g2.fillOval(cx-5,cy-5,10,10);g2.setColor(col);g2.fillOval(cx-3,cy-3,6,6);
            g2.setFont(new Font("SansSerif",Font.BOLD,16));g2.setColor(col);FontMetrics fm=g2.getFontMetrics();g2.drawString(value,cx-fm.stringWidth(value)/2,cy+20);
            g2.setFont(new Font("SansSerif",Font.BOLD,11));g2.setColor(theme().text);fm=g2.getFontMetrics();g2.drawString(label,cx-fm.stringWidth(label)/2,cy+34);
            if(sweepMax>=0){g2.setFont(new Font("SansSerif",Font.PLAIN,9));g2.setColor(col.brighter());g2.drawString("min",cx-18,cy+46);g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),160));g2.drawString("max",cx+4,cy+46);}
            g2.setFont(new Font("SansSerif",Font.PLAIN,9));g2.setColor(theme().subtext);fm=g2.getFontMetrics();g2.drawString(maxLbl,cx-fm.stringWidth(maxLbl)/2,cy+r-2);
        }
    }

    // ── Truck Panel ───────────────────────────────────────────────────
    static class TruckPanel extends JPanel {
        private float truckX=-160f,progress=0f;private int wheelAngle=0;private boolean running=false;private Timer animTimer;
        TruckPanel(){setPreferredSize(new Dimension(560,80));setOpaque(false);}
        void start(Runnable onComplete){truckX=-160f;progress=0f;wheelAngle=0;running=true;if(animTimer!=null&&animTimer.isRunning())animTimer.stop();animTimer=new Timer(16,null);animTimer.addActionListener(new ActionListener(){int step=0;final int total=90;public void actionPerformed(ActionEvent e){step++;progress=(float)step/total;float eased=progress<0.5f?2*progress*progress:(float)(1-Math.pow(-2*progress+2,2)/2);int tw=getWidth()>0?getWidth():560;truckX=-160f+eased*(tw+160f);wheelAngle=(step*8)%360;repaint();if(step>=total){animTimer.stop();running=false;onComplete.run();}}});animTimer.start();}
        void reset(){if(animTimer!=null)animTimer.stop();running=false;truckX=-160f;progress=0f;repaint();}
        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);Graphics2D g2=(Graphics2D)g;g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            int w=getWidth(),h=getHeight(),ground=h-14;g2.setColor(new Color(80,80,85));g2.fillRoundRect(0,ground,w,12,4,4);g2.setColor(new Color(255,220,60,180));g2.setStroke(new BasicStroke(2f,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,10f,new float[]{14f,10f},(float)(progress*24)));g2.drawLine(0,ground+6,w,ground+6);g2.setStroke(new BasicStroke(1f));
            if(!running&&progress==0f)return;
            int tx=(int)truckX,ty=ground-52;int dX=tx+55,dY=ty+4,dW=68,dH=40;g2.setPaint(new GradientPaint(dX,dY,new Color(220,100,30),dX+dW,dY+dH,new Color(160,60,10)));g2.fillRoundRect(dX,dY,dW,dH,14,14);g2.setColor(new Color(255,255,255,60));g2.setStroke(new BasicStroke(3f));for(int s=0;s<4;s++){int sx=dX+8+s*14+(wheelAngle/20);if(sx<dX+dW-4)g2.drawLine(sx,dY+4,sx-8,dY+dH-4);}g2.setStroke(new BasicStroke(1.5f));g2.setColor(new Color(120,50,10));g2.drawRoundRect(dX,dY,dW,dH,14,14);int[]cX2={tx+118,tx+130,tx+125,tx+112},cY2={ty+35,ty+33,ty+50,ty+50};g2.setColor(new Color(140,70,20));g2.fillPolygon(cX2,cY2,4);
            int cabX=tx+2,cabY=ty+14,cabW=58,cabH=38;g2.setPaint(new GradientPaint(cabX,cabY,new Color(60,130,200),cabX+cabW,cabY+cabH,new Color(30,80,140)));g2.fillRoundRect(cabX,cabY,cabW,cabH,10,10);g2.setPaint(new GradientPaint(cabX+6,cabY-12,new Color(70,150,220),cabX+6,cabY+4,new Color(50,110,180)));g2.fillRoundRect(cabX+6,cabY-12,cabW-12,22,10,10);g2.setColor(new Color(180,230,255,200));g2.fillRoundRect(cabX+8,cabY-8,cabW-22,18,6,6);g2.setColor(new Color(100,180,230,80));g2.fillRect(cabX+14,cabY-6,6,14);g2.setColor(new Color(20,60,110));g2.setStroke(new BasicStroke(1.5f));g2.drawRoundRect(cabX,cabY,cabW,cabH,10,10);g2.drawRoundRect(cabX+6,cabY-12,cabW-12,22,10,10);g2.setColor(new Color(40,100,170));g2.setStroke(new BasicStroke(1f));g2.drawLine(cabX+30,cabY+4,cabX+30,cabY+cabH-4);g2.setColor(new Color(50,50,60));g2.fillRoundRect(cabX,cabY+cabH-6,cabW,10,4,4);g2.setColor(new Color(255,240,150));g2.fillOval(cabX+4,cabY+cabH-14,10,8);g2.setColor(new Color(60,60,65));g2.fillRect(cabX+42,cabY-20,5,16);
            if(running){int pa=60+(int)(40*Math.sin(wheelAngle*0.1));for(int p=0;p<3;p++){int ps=7+p*4,py2=cabY-22-p*9,px2=cabX+42+p*2;g2.setColor(new Color(180,180,180,Math.max(0,pa-p*15)));g2.fillOval(px2,py2,ps,ps);}}
            g2.setColor(new Color(40,40,45));g2.fillRect(tx+2,ty+50,130,6);drawWheel(g2,tx+18,ground-12,13,wheelAngle);drawWheel(g2,tx+90,ground-12,13,wheelAngle);drawWheel(g2,tx+112,ground-12,13,wheelAngle);g2.setColor(new Color(255,255,255,180));g2.setFont(new Font("SansSerif",Font.BOLD,7));g2.drawString("CONCRETE",dX+8,dY+dH/2+3);
        }
        private void drawWheel(Graphics2D g2,int cx,int cy,int r,int angle){g2.setColor(new Color(30,30,30));g2.fillOval(cx-r,cy-r,r*2,r*2);g2.setColor(new Color(190,190,200));g2.fillOval(cx-r+3,cy-r+3,(r-3)*2,(r-3)*2);g2.setColor(new Color(80,80,90));g2.fillOval(cx-4,cy-4,8,8);g2.setColor(new Color(130,130,140));g2.setStroke(new BasicStroke(1.5f));for(int s=0;s<5;s++){double a=Math.toRadians(angle+s*72);g2.drawLine(cx,cy,(int)(cx+Math.cos(a)*(r-5)),(int)(cy+Math.sin(a)*(r-5)));}g2.setColor(new Color(20,20,20));g2.setStroke(new BasicStroke(1.5f));g2.drawOval(cx-r,cy-r,r*2,r*2);}
    }

    // ── Progress bar ──────────────────────────────────────────────────
    static JProgressBar makeProgressBar(){JProgressBar pb=new JProgressBar(0,100);pb.setStringPainted(true);pb.setString("Ready");pb.setValue(0);pb.setForeground(new Color(39,174,96));pb.setBackground(new Color(220,220,220));pb.setFont(new Font("SansSerif",Font.BOLD,11));pb.setPreferredSize(new Dimension(520,22));return pb;}
    static void animateProgressBar(JProgressBar pb,TruckPanel truck,Runnable onComplete){pb.setValue(0);pb.setString("Calculating...");pb.setForeground(new Color(52,152,219));Timer t=new Timer(15,null);t.addActionListener(new ActionListener(){int p=0;public void actionPerformed(ActionEvent e){p+=4;pb.setValue(Math.min(p,100));if(p>=100){t.stop();pb.setString("Done!");pb.setForeground(new Color(39,174,96));}}});t.start();truck.start(onComplete);}

    // ── Count-up animators ────────────────────────────────────────────
    static void animateCount(JLabel l,double target,String pre,String suf,int dec){Timer t=new Timer(16,null);t.addActionListener(new ActionListener(){int s=0;final int tot=30;public void actionPerformed(ActionEvent e){s++;l.setText(pre+String.format("%."+dec+"f",target*((double)s/tot))+suf);if(s>=tot){t.stop();l.setText(pre+String.format("%."+dec+"f",target)+suf);}}}); t.start();}
    static void animateInt(JLabel l,int target,String suf){Timer t=new Timer(16,null);t.addActionListener(new ActionListener(){int s=0;final int tot=30;public void actionPerformed(ActionEvent e){s++;l.setText((int)(target*((double)s/tot))+suf);if(s>=tot){t.stop();l.setText(target+suf);}}}); t.start();}
    static void slideIn(JComponent c,int delayMs){c.setVisible(false);Timer t=new Timer(delayMs,null);t.setRepeats(false);t.addActionListener(ev->{c.setVisible(true);Timer f=new Timer(16,null);f.addActionListener(new ActionListener(){int s=0;final int tot=18;public void actionPerformed(ActionEvent e2){s++;c.putClientProperty("slideAlpha",(float)s/tot);c.repaint();if(s>=tot){f.stop();c.putClientProperty("slideAlpha",1f);}}});f.start();});t.start();}

    // ── Helpers ───────────────────────────────────────────────────────
    static Color jobColor(double v){return v<SMALL_JOB?new Color(39,174,96):v<MEDIUM_JOB?new Color(241,196,15):new Color(192,57,43);}
    static String jobLabel(double v){return v<SMALL_JOB?"Small Job":v<MEDIUM_JOB?"Medium Job":"Large Job";}
    static void styleBtn(JButton b,Color bg,Color fg){b.setBackground(bg);b.setForeground(fg);b.setFont(new Font("SansSerif",Font.BOLD,13));b.setFocusPainted(false);b.setBorderPainted(false);b.setOpaque(true);b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.setBorder(BorderFactory.createEmptyBorder(8,18,8,18));}

    // ── Globals ───────────────────────────────────────────────────────
    static JFrame mainFrame;
    static ConfettiPanel confettiLayer;

    public static void main(String[] args){SwingUtilities.invokeLater(()->createAndShowGUI());}
    static void createAndShowGUI(){mainFrame=new JFrame("Jim's Concrete Calculator");mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);mainFrame.setSize(640,1100);mainFrame.setLocationRelativeTo(null);confettiLayer=new ConfettiPanel();confettiLayer.setBounds(0,0,640,1100);mainFrame.setGlassPane(confettiLayer);confettiLayer.setVisible(true);buildUI();mainFrame.setVisible(true);}


    // ── Print report ──────────────────────────────────────────────────
    static void printReport(){
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Concrete Calculator Report");
        job.setPrintable((graphics, pf, page) -> {
            if (page > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double x = pf.getImageableX(), y = pf.getImageableY(), w = pf.getImageableWidth();
            g2.translate(x, y);
            int cx = (int)(w / 2);
            int iy = 30;

            // Header bar
            g2.setColor(new Color(52, 73, 94));
            g2.fillRoundRect(0, 0, (int)w, 44, 10, 10);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            FontMetrics fm = g2.getFontMetrics();
            String heading = "Jim's Concrete Calculator - Job Report";
            g2.drawString(heading, cx - fm.stringWidth(heading) / 2, 28);
            iy = 70;

            // Detail rows
            g2.setColor(new Color(52, 73, 94));
            String[][] rows = {
                {"Mix Type:",         MIX_NAMES[lastMix]},
                {"Waste Factor:",     String.format("%.0f%%", lastWaste * 100)},
                {"Area:",             String.format("%.2f sq ft", lastArea)},
                {"Volume:",           String.format("%.2f cu ft  /  %.2f cu yd", lastVolFt, lastVolYd)},
                {"Estimated Cost:",   String.format("$%.2f", lastCost)},
                {"80lb Bags Needed:", String.valueOf(lastBags)},
                {"Trucks Needed:",    lastTMin + " - " + lastTMax + " truck(s)"}
            };
            for (String[] row : rows) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                fm = g2.getFontMetrics();
                g2.drawString(row[0], (int)(w * 0.1), iy);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2.drawString(row[1], (int)(w * 0.45), iy);
                iy += 22;
                g2.setColor(new Color(200, 200, 210));
                g2.drawLine((int)(w * 0.1), iy - 6, (int)(w * 0.9), iy - 6);
                g2.setColor(new Color(52, 73, 94));
            }

            // Footer
            g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
            g2.setColor(Color.GRAY);
            g2.drawString("Generated by Jim's Concrete Calculator", 10, (int)(pf.getImageableHeight() - 20));
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(mainFrame, "Print failed: " + ex.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    static void buildUI(){
        mainFrame.getContentPane().removeAll();

        // ── Theme bar ─────────────────────────────────────────────────
        JPanel themeBar=new JPanel(new FlowLayout(FlowLayout.RIGHT,8,6));themeBar.setBackground(theme().headerBg);
        JLabel tL=new JLabel("Theme: ");tL.setForeground(theme().headerText);tL.setFont(new Font("SansSerif",Font.BOLD,11));themeBar.add(tL);
        String[]tN={"Light","Dark","Blueprint"};
        for(int i=0;i<tN.length;i++){final int idx=i;JButton tb=new JButton(tN[i]);tb.setFont(new Font("SansSerif",Font.BOLD,11));tb.setFocusPainted(false);tb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));tb.setBackground(idx==currentTheme?theme().accent:theme().panelBg);tb.setForeground(idx==currentTheme?Color.WHITE:theme().text);tb.setBorder(BorderFactory.createEmptyBorder(4,10,4,10));tb.addActionListener(e->{currentTheme=idx;buildUI();});themeBar.add(tb);}

        // ── Header ────────────────────────────────────────────────────
        JPanel header=new JPanel(new FlowLayout(FlowLayout.CENTER,16,10));header.setBackground(theme().headerBg);
        JLabel title=new JLabel("Jim's Concrete Calculator");title.setFont(new Font("SansSerif",Font.BOLD,20));title.setForeground(theme().headerText);
        header.add(new WarehouseIcon());header.add(title);
        JPanel topPanel=new JPanel(new BorderLayout());topPanel.setBackground(theme().headerBg);topPanel.add(themeBar,BorderLayout.NORTH);topPanel.add(header,BorderLayout.CENTER);

        // ── Unit toggle ───────────────────────────────────────────────
        CardPanel unitCard=new CardPanel(16);unitCard.setLayout(new FlowLayout(FlowLayout.LEFT,12,8));
        JLabel uL=new JLabel("Units:");uL.setFont(new Font("SansSerif",Font.BOLD,12));uL.setForeground(theme().text);
        JRadioButton impBtn=new JRadioButton("Imperial (ft / in)",true);JRadioButton metBtn=new JRadioButton("Metric (m / cm)");
        for(JRadioButton rb:new JRadioButton[]{impBtn,metBtn}){rb.setOpaque(false);rb.setForeground(theme().text);rb.setFont(new Font("SansSerif",Font.PLAIN,12));}
        ButtonGroup ug=new ButtonGroup();ug.add(impBtn);ug.add(metBtn);
        unitCard.add(uL);unitCard.add(impBtn);unitCard.add(metBtn);

        // ── Mix selector ──────────────────────────────────────────────
        CardPanel mixCard=new CardPanel(16);mixCard.setLayout(new GridBagLayout());
        GridBagConstraints mc=new GridBagConstraints();mc.insets=new Insets(5,12,5,12);mc.fill=GridBagConstraints.HORIZONTAL;
        JLabel mT=new JLabel("Concrete Mix");mT.setFont(new Font("SansSerif",Font.BOLD,13));mT.setForeground(theme().text);
        mc.gridx=0;mc.gridy=0;mc.gridwidth=2;mixCard.add(mT,mc);mc.gridwidth=1;
        JComboBox<String> mixCombo=new JComboBox<>(MIX_NAMES);mixCombo.setFont(new Font("SansSerif",Font.PLAIN,12));
        JLabel mD=new JLabel(MIX_DESCS[0]);mD.setFont(new Font("SansSerif",Font.ITALIC,11));mD.setForeground(theme().subtext);
        JLabel mC=new JLabel("Cost: $"+(int)MIX_COSTS[0]+" / cu yd");mC.setFont(new Font("SansSerif",Font.BOLD,12));mC.setForeground(theme().accent);
        mc.gridx=0;mc.gridy=1;mc.weightx=0.35;JLabel mL=new JLabel("Mix Type:");mL.setForeground(theme().text);mixCard.add(mL,mc);
        mc.gridx=1;mc.weightx=0.65;mixCard.add(mixCombo,mc);mc.gridx=0;mc.gridy=2;mc.gridwidth=2;mixCard.add(mD,mc);mc.gridy=3;mixCard.add(mC,mc);
        mixCombo.addActionListener(e->{int i=mixCombo.getSelectedIndex();mD.setText(MIX_DESCS[i]);mC.setText("Cost: $"+(int)MIX_COSTS[i]+" / cu yd");});

        // ── Input card ────────────────────────────────────────────────
        CardPanel inputCard=new CardPanel(16);inputCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc=new GridBagConstraints();gbc.insets=new Insets(6,12,6,12);gbc.fill=GridBagConstraints.HORIZONTAL;
        JLabel iT=new JLabel("Foundation Dimensions");iT.setFont(new Font("SansSerif",Font.BOLD,13));iT.setForeground(theme().text);
        gbc.gridx=0;gbc.gridy=0;gbc.gridwidth=2;inputCard.add(iT,gbc);gbc.gridwidth=1;
        JLabel[]dL={new JLabel("Length (ft):"),new JLabel("Width (ft):"),new JLabel("Depth (in):")};
        JTextField[]fields=new JTextField[3];
        for(int i=0;i<3;i++){dL[i].setForeground(theme().text);dL[i].setFont(new Font("SansSerif",Font.PLAIN,12));gbc.gridx=0;gbc.gridy=i+1;gbc.weightx=0.4;inputCard.add(dL[i],gbc);fields[i]=new JTextField("",10);fields[i].setBackground(theme().bg);fields[i].setForeground(theme().text);fields[i].setCaretColor(theme().text);fields[i].setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(theme().border,1,true),BorderFactory.createEmptyBorder(4,6,4,6)));gbc.gridx=1;gbc.weightx=0.6;inputCard.add(fields[i],gbc);}
        impBtn.addActionListener(e->{dL[0].setText("Length (ft):");dL[1].setText("Width (ft):");dL[2].setText("Depth (in):");});
        metBtn.addActionListener(e->{dL[0].setText("Length (m):");dL[1].setText("Width (m):");dL[2].setText("Depth (cm):");});

        // ── Waste factor slider ───────────────────────────────────────
        CardPanel wasteCard=new CardPanel(16);wasteCard.setLayout(new GridBagLayout());
        GridBagConstraints wc=new GridBagConstraints();wc.insets=new Insets(5,12,5,12);wc.fill=GridBagConstraints.HORIZONTAL;
        JLabel wasteTitle=new JLabel("Waste Factor");wasteTitle.setFont(new Font("SansSerif",Font.BOLD,13));wasteTitle.setForeground(theme().text);
        wc.gridx=0;wc.gridy=0;wc.gridwidth=3;wasteCard.add(wasteTitle,wc);wc.gridwidth=1;
        JSlider wasteSlider=new JSlider(0,15,5); // 0-15% in whole %
        wasteSlider.setOpaque(false);wasteSlider.setMajorTickSpacing(5);wasteSlider.setMinorTickSpacing(1);wasteSlider.setPaintTicks(true);wasteSlider.setPaintLabels(true);wasteSlider.setForeground(theme().text);
        JLabel wasteLbl=new JLabel("Extra: 5%");wasteLbl.setFont(new Font("SansSerif",Font.BOLD,12));wasteLbl.setForeground(theme().accent);
        JLabel wasteDesc=new JLabel("Accounts for spillage, overfill & error.");wasteDesc.setFont(new Font("SansSerif",Font.ITALIC,11));wasteDesc.setForeground(theme().subtext);
        wasteSlider.addChangeListener(e->wasteLbl.setText("Extra: "+wasteSlider.getValue()+"%"));
        wc.gridx=0;wc.gridy=1;wc.weightx=0.2;wasteCard.add(wasteLbl,wc);
        wc.gridx=1;wc.weightx=0.6;wasteCard.add(wasteSlider,wc);
        wc.gridx=0;wc.gridy=2;wc.gridwidth=2;wasteCard.add(wasteDesc,wc);
        // ── What-if comparison card ───────────────────────────────────
        CardPanel compareCard=new CardPanel(16);compareCard.setLayout(new GridBagLayout());
        GridBagConstraints cc=new GridBagConstraints();cc.insets=new Insets(5,10,5,10);cc.fill=GridBagConstraints.HORIZONTAL;
        JLabel cTitle=new JLabel("What-If Comparison");cTitle.setFont(new Font("SansSerif",Font.BOLD,13));cTitle.setForeground(theme().text);
        cc.gridx=0;cc.gridy=0;cc.gridwidth=4;compareCard.add(cTitle,cc);cc.gridwidth=1;
        // Headers
        String[]cH={"","Slab A (current)","Slab B (compare)"};Color[]cC={theme().text,new Color(52,152,219),new Color(155,89,182)};
        for(int i=0;i<3;i++){JLabel h=new JLabel(cH[i],SwingConstants.CENTER);h.setFont(new Font("SansSerif",Font.BOLD,11));h.setForeground(cC[i]);cc.gridx=i;cc.gridy=1;compareCard.add(h,cc);}
        // Compare input fields for slab B
        String[]cRowLbls={"Length:","Width:","Depth:"};
        JTextField[]bFields=new JTextField[3];
        JLabel[]aLabels=new JLabel[3];
        for(int i=0;i<3;i++){JLabel rl=new JLabel(cRowLbls[i]);rl.setForeground(theme().text);rl.setFont(new Font("SansSerif",Font.PLAIN,11));cc.gridx=0;cc.gridy=i+2;compareCard.add(rl,cc);aLabels[i]=new JLabel("--",SwingConstants.CENTER);aLabels[i].setForeground(new Color(52,152,219));aLabels[i].setFont(new Font("SansSerif",Font.PLAIN,11));cc.gridx=1;compareCard.add(aLabels[i],cc);bFields[i]=new JTextField("",7);bFields[i].setBackground(theme().bg);bFields[i].setForeground(theme().text);bFields[i].setCaretColor(theme().text);bFields[i].setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(theme().border,1,true),BorderFactory.createEmptyBorder(3,5,3,5)));cc.gridx=2;compareCard.add(bFields[i],cc);}
        // Compare result rows
        String[]cResultLbls={"Volume (cu yd):","Est. Cost:","Bags:"};
        JLabel[]aRes=new JLabel[3],bRes=new JLabel[3];
        for(int i=0;i<3;i++){JLabel rl=new JLabel(cResultLbls[i]);rl.setForeground(theme().text);rl.setFont(new Font("SansSerif",Font.BOLD,11));cc.gridx=0;cc.gridy=i+5;compareCard.add(rl,cc);aRes[i]=new JLabel("--",SwingConstants.CENTER);aRes[i].setForeground(new Color(52,152,219));aRes[i].setFont(new Font("SansSerif",Font.BOLD,11));cc.gridx=1;compareCard.add(aRes[i],cc);bRes[i]=new JLabel("--",SwingConstants.CENTER);bRes[i].setForeground(new Color(155,89,182));bRes[i].setFont(new Font("SansSerif",Font.BOLD,11));cc.gridx=2;compareCard.add(bRes[i],cc);}
        JButton compareBtn=new JButton("Compare");styleBtn(compareBtn,new Color(155,89,182),Color.WHITE);
        cc.gridx=0;cc.gridy=8;cc.gridwidth=3;compareCard.add(compareBtn,cc);
        compareBtn.addActionListener(e->{
            // Update Slab A labels from last calculation
            if(lastArea==0){JOptionPane.showMessageDialog(mainFrame,"Run a calculation first to populate Slab A.","No Data",JOptionPane.WARNING_MESSAGE);return;}
            aLabels[0].setText(String.format("%.1f ft",Math.sqrt(lastArea)));
            aLabels[1].setText(String.format("%.1f ft",Math.sqrt(lastArea)));
            aLabels[2].setText(String.format("%.0f in",(lastVolFt/lastArea)*12));
            aRes[0].setText(String.format("%.2f yd3",lastVolYd));
            aRes[1].setText(String.format("$%.2f",lastCost));
            aRes[2].setText(String.valueOf(lastBags));
            try{
                double bL=Double.parseDouble(bFields[0].getText().trim());
                double bW=Double.parseDouble(bFields[1].getText().trim());
                double bD=Double.parseDouble(bFields[2].getText().trim());
                double bDepthFt=bD/12.0,bArea=bL*bW,bVolFt=bArea*bDepthFt,bVolYd=bVolFt/27.0;
                double bCost=bVolYd*MIX_COSTS[lastMix]*(1+lastWaste);
                int bBags=(int)Math.ceil(bVolFt/BAG_COVERAGE);
                bRes[0].setText(String.format("%.2f yd3",bVolYd));
                bRes[1].setText(String.format("$%.2f",bCost));
                bRes[2].setText(String.valueOf(bBags));
                // Highlight cheaper option
                Color win=new Color(39,174,96),lose=new Color(192,57,43);
                aRes[1].setForeground(lastCost<=bCost?win:lose);
                bRes[1].setForeground(bCost<=lastCost?win:lose);
            }catch(NumberFormatException ex){JOptionPane.showMessageDialog(mainFrame,"Enter valid numbers for Slab B.","Input Error",JOptionPane.ERROR_MESSAGE);}
        });

        // ── Truck + progress ──────────────────────────────────────────
        TruckPanel truckPanel=new TruckPanel();truckPanel.setAlignmentX(Component.CENTER_ALIGNMENT);truckPanel.setMaximumSize(new Dimension(560,80));
        JProgressBar progressBar=makeProgressBar();JPanel pbWrap=new JPanel(new FlowLayout(FlowLayout.CENTER,0,2));pbWrap.setOpaque(false);pbWrap.add(progressBar);

        // ── Results card ──────────────────────────────────────────────
        ConcreteResultsCard resultsCard=new ConcreteResultsCard();resultsCard.setLayout(new GridBagLayout());
        GridBagConstraints rgbc=new GridBagConstraints();rgbc.insets=new Insets(5,14,5,14);
        JLabel rTitle=new JLabel("Results");rTitle.setFont(new Font("SansSerif",Font.BOLD,13));rTitle.setForeground(theme().text);
        rgbc.gridx=0;rgbc.gridy=0;rgbc.gridwidth=2;rgbc.anchor=GridBagConstraints.CENTER;resultsCard.add(rTitle,rgbc);
        JLabel jobSizeLbl=new JLabel(" ");jobSizeLbl.setFont(new Font("SansSerif",Font.BOLD,13));rgbc.gridy=1;resultsCard.add(jobSizeLbl,rgbc);rgbc.gridwidth=1;
        String[]icons={"[]","[V]","[V]","[$]","[B]","[T]"};
        String[]rLabels={"Area:","Volume (cu ft):","Volume (cu yd):","Estimated Cost:","80lb Bags Needed:","Trucks (min-max):"};
        JLabel[]rValues=new JLabel[rLabels.length];
        for(int i=0;i<rLabels.length;i++){rgbc.gridy=i+2;rgbc.gridx=0;rgbc.anchor=GridBagConstraints.EAST;rgbc.weightx=0.5;JLabel nl=new JLabel(icons[i]+"  "+rLabels[i]);nl.setFont(new Font("SansSerif",Font.BOLD,12));nl.setForeground(theme().text);resultsCard.add(nl,rgbc);rgbc.gridx=1;rgbc.anchor=GridBagConstraints.WEST;rgbc.weightx=0.5;rValues[i]=new JLabel("--");rValues[i].setFont(new Font("SansSerif",Font.PLAIN,12));rValues[i].setForeground(theme().accent);resultsCard.add(rValues[i],rgbc);}

        JLabel tipLbl=new JLabel(" ");tipLbl.setFont(new Font("SansSerif",Font.ITALIC,12));tipLbl.setForeground(theme().subtext);tipLbl.setHorizontalAlignment(SwingConstants.CENTER);
        GaugePanel gaugePanel=new GaugePanel();

        // ── History ───────────────────────────────────────────────────
        DefaultListModel<String> histModel=new DefaultListModel<>();
        JList<String> histList=new JList<>(histModel);histList.setFont(new Font("Monospaced",Font.PLAIN,11));histList.setBackground(theme().histBg);histList.setForeground(theme().histText);histList.setSelectionBackground(theme().border);
        JScrollPane histScroll=new JScrollPane(histList);histScroll.setPreferredSize(new Dimension(520,100));histScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(theme().border,2),"Calculation History",TitledBorder.DEFAULT_JUSTIFICATION,TitledBorder.DEFAULT_POSITION,new Font("SansSerif",Font.BOLD,12),theme().text));

        // ── Buttons ───────────────────────────────────────────────────
        JButton calcBtn=new JButton("Calculate");styleBtn(calcBtn,theme().accent,Color.WHITE);
        JButton resetBtn=new JButton("Reset");styleBtn(resetBtn,new Color(192,57,43),Color.WHITE);
        JButton clearBtn=new JButton("Clear History");styleBtn(clearBtn,new Color(100,100,110),Color.WHITE);
        JButton printBtn=new JButton("Print Report");styleBtn(printBtn,new Color(52,73,94),Color.WHITE);

        for(JTextField f:fields){f.addKeyListener(new KeyAdapter(){public void keyPressed(KeyEvent e){if(e.getKeyCode()==KeyEvent.VK_ENTER){boolean ok=true;for(JTextField fx:fields)if(fx.getText().trim().isEmpty()){ok=false;break;}if(!ok)JOptionPane.showMessageDialog(mainFrame,"Please fill in all fields before pressing Enter.","Missing Input",JOptionPane.WARNING_MESSAGE);else calcBtn.doClick();}}});}

        JPanel btnPanel=new JPanel(new FlowLayout(FlowLayout.CENTER,10,6));btnPanel.setOpaque(false);
        btnPanel.add(calcBtn);btnPanel.add(resetBtn);btnPanel.add(clearBtn);btnPanel.add(printBtn);

        // ── Center ────────────────────────────────────────────────────
        JPanel center=new JPanel();center.setLayout(new BoxLayout(center,BoxLayout.Y_AXIS));center.setBackground(theme().bg);center.setBorder(BorderFactory.createEmptyBorder(12,18,12,18));
        for(JComponent c:new JComponent[]{unitCard,mixCard,inputCard,wasteCard,compareCard})
            {c.setAlignmentX(Component.CENTER_ALIGNMENT);center.add(c);center.add(Box.createVerticalStrut(10));}
        for(JComponent c:new JComponent[]{btnPanel,truckPanel,pbWrap,resultsCard,tipLbl,gaugePanel,histScroll})
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(btnPanel);center.add(Box.createVerticalStrut(8));
        center.add(truckPanel);center.add(Box.createVerticalStrut(4));
        center.add(pbWrap);center.add(Box.createVerticalStrut(10));
        center.add(resultsCard);center.add(Box.createVerticalStrut(4));
        center.add(tipLbl);center.add(Box.createVerticalStrut(10));
        center.add(gaugePanel);center.add(Box.createVerticalStrut(10));
        center.add(histScroll);center.add(Box.createVerticalStrut(8));
        JScrollPane scroll=new JScrollPane(center);scroll.setBorder(null);scroll.getVerticalScrollBar().setUnitIncrement(16);scroll.getViewport().setBackground(theme().bg);

        // ── Calculate ─────────────────────────────────────────────────
        calcBtn.addActionListener(e->{
            try{
                boolean metric=metBtn.isSelected();
                double rL=Double.parseDouble(fields[0].getText().trim());
                double rW=Double.parseDouble(fields[1].getText().trim());
                double rD=Double.parseDouble(fields[2].getText().trim());
                double length=metric?rL*3.28084:rL,width=metric?rW*3.28084:rW,depthIn=metric?rD/2.54:rD;
                if(length<=0||width<=0||depthIn<=0){JOptionPane.showMessageDialog(mainFrame,"Please enter positive values.","Input Error",JOptionPane.ERROR_MESSAGE);return;}
                double wasteFactor=1.0+wasteSlider.getValue()/100.0;
                int mix=mixCombo.getSelectedIndex();double costPerYd=MIX_COSTS[mix];
                double depthFt=depthIn/12.0,area=length*width,volFt=area*depthFt*wasteFactor,volYd=volFt/27.0,cost=volYd*costPerYd;
                int bags=(int)Math.ceil(volFt/BAG_COVERAGE),tMin=(int)Math.ceil(volYd/10.0),tMax=(int)Math.ceil(volYd/8.0);
                // Save for print/compare
                lastArea=area;lastVolFt=volFt;lastVolYd=volYd;lastCost=cost;lastBags=bags;lastTMin=tMin;lastTMax=tMax;lastMix=mix;lastWaste=wasteFactor-1.0;
                // Update compare Slab A labels live
                aLabels[0].setText(String.format("%.1f",rL)+(metric?" m":" ft"));
                aLabels[1].setText(String.format("%.1f",rW)+(metric?" m":" ft"));
                aLabels[2].setText(String.format("%.0f",rD)+(metric?" cm":" in"));
                String tip=volYd<1?"Tip: Small pour - bagged concrete may be more practical.":tMax==tMin?"Tip: Order "+(tMin+1)+" trucks as a buffer.":"Tip: Budget for "+tMax+" trucks; fewer if loads are full.";
                String uStr=metric?"m":"ft";
                String hist=String.format("#%d | %s | +%.0f%% waste | %.1f%sx%.1f%sx%.0f%s | %.2fyd3 | %d bags | %d-%d trucks | $%.2f",histModel.size()+1,MIX_NAMES[mix],lastWaste*100,rL,uStr,rW,uStr,rD,metric?"cm":"in",volYd,bags,tMin,tMax,cost);
                calcBtn.setEnabled(false);
                animateProgressBar(progressBar,truckPanel,()->{
                    Color jc=jobColor(volYd);
                    jobSizeLbl.setText("\u25cf "+jobLabel(volYd)+" ("+MIX_NAMES[mix]+" Mix)");
                    jobSizeLbl.setForeground(jc);for(JLabel lv:rValues)lv.setForeground(jc);
                    resultsCard.animateFill();
                    for(int i=0;i<rValues.length;i++)slideIn(rValues[i],i*80);
                    new Timer(0,ev->{animateCount(rValues[0],area,"","  sq ft",2);((Timer)ev.getSource()).stop();}).start();
                    new Timer(80,ev->{animateCount(rValues[1],volFt,"","  cu ft",2);((Timer)ev.getSource()).stop();}).start();
                    new Timer(160,ev->{animateCount(rValues[2],volYd,"","  cu yd",2);((Timer)ev.getSource()).stop();}).start();
                    new Timer(240,ev->{animateCount(rValues[3],cost,"$","",2);((Timer)ev.getSource()).stop();}).start();
                    new Timer(320,ev->{animateInt(rValues[4],bags,"  bags");((Timer)ev.getSource()).stop();}).start();
                    new Timer(400,ev->{rValues[5].setText(tMin+"-"+tMax+" truck(s)");((Timer)ev.getSource()).stop();}).start();
                    tipLbl.setText(tip);gaugePanel.update(bags,tMin,tMax,cost);histModel.add(0,hist);
                    int cx=mainFrame.getWidth()/2,cy=mainFrame.getHeight()/3;
                    confettiLayer.setBounds(0,0,mainFrame.getWidth(),mainFrame.getHeight());
                    confettiLayer.burst(cx,cy);
                    playHorn();
                    calcBtn.setEnabled(true);
                });
            }catch(NumberFormatException ex){JOptionPane.showMessageDialog(mainFrame,"Please fill in all fields with valid numbers.","Input Error",JOptionPane.ERROR_MESSAGE);}
        });

        resetBtn.addActionListener(e->{for(JTextField f:fields)f.setText("");for(JLabel lv:rValues){lv.setText("--");lv.setForeground(theme().accent);lv.setVisible(true);}jobSizeLbl.setText(" ");tipLbl.setText(" ");progressBar.setValue(0);progressBar.setString("Ready");progressBar.setForeground(new Color(39,174,96));gaugePanel.reset();truckPanel.reset();resultsCard.reset();});
        clearBtn.addActionListener(e->histModel.clear());
        printBtn.addActionListener(e->{if(lastArea==0){JOptionPane.showMessageDialog(mainFrame,"Run a calculation first before printing.","No Data",JOptionPane.WARNING_MESSAGE);return;}printReport();});

        mainFrame.add(topPanel,BorderLayout.NORTH);mainFrame.add(scroll,BorderLayout.CENTER);
        mainFrame.revalidate();mainFrame.repaint();
    }
}

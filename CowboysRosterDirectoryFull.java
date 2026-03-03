import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.AbstractMap;
import java.util.function.*;

/**
 * CowboysApp – Enhanced Edition
 *
 * Changes from original:
 *  ── UI / Visual Polish ───────────────────────────────────────────────────────
 *  • Position badges colour-coded by unit (offense / defense / ST)
 *  • Hover-highlight on list items and table rows
 *  • Gradient header panel
 *  • Smooth card transitions (fade via Timer)
 *  • Consistent Cowboys palette used everywhere
 *  • Status bar shows filtered count AND total
 *
 *  ── New Features ─────────────────────────────────────────────────────────────
 *  • Sortable JTable view (toggle with "Table / Card" button)
 *  • Statistics tab: bar charts for positions, draft rounds, Pro Bowls
 *  • Favourites system: ★ button on each card; persisted to disk
 *  • Sidebar "⭐ Favourites" quick-access entry
 *
 *  ── Bug Fixes ────────────────────────────────────────────────────────────────
 *  • updateNames() now respects active filters (calls applyFilters)
 *  • "Years with Dallas" uses Calendar.getInstance().get(Calendar.YEAR)
 *  • CSV now uses RFC-4180 quoting (no comma→semicolon corruption)
 *  • applyFilters no longer auto-selects when count == 1 (was jarring UX)
 *  • positionFilter correctly re-populates from live data on reset
 */
public class CowboysApp extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color NAVY        = new Color(0,  34, 68);
    private static final Color NAVY_DARK   = new Color(0,  20, 45);
    private static final Color BLUE        = new Color(0,  53,148);
    private static final Color SILVER      = new Color(134,147,151);
    private static final Color SILVER_LITE = new Color(220,225,228);
    private static final Color WHITE       = Color.WHITE;
    private static final Color BG_GRAY     = new Color(242,244,248);
    private static final Color GOLD        = new Color(255,182, 18);
    private static final Color GOLD_LITE   = new Color(255,248,200);
    private static final Color GREEN       = new Color( 34,139, 34);
    private static final Color RED_SOFT    = new Color(180, 50, 50);
    private static final Color PINK        = new Color(200, 60,120);
    private static final Color TEAL        = new Color(  0,130,130);

    // position-group colours
    private static final Color OFF_COLOR   = new Color( 30,100,190);
    private static final Color DEF_COLOR   = new Color(180, 40, 40);
    private static final Color ST_COLOR    = new Color(140, 90,  0);
    private static final Color OL_COLOR    = new Color( 60,130, 60);


    // ── Football terms (for non-fans) ────────────────────────────────────────
    // Display full position names + simple explanations via tooltips.
    private static final Map<String,String> POS_FULL = new LinkedHashMap<>();
    private static final Map<String,String> POS_DESC = new LinkedHashMap<>();
    static {
        addPos("QB", "Quarterback", "Leads the offense; usually throws passes and calls plays.");
        addPos("RB", "Running Back", "Main ball carrier; runs the ball and can catch short passes.");
        addPos("FB", "Fullback", "Blocks for runners; sometimes runs short-yardage plays.");
        addPos("WR", "Wide Receiver", "Catches passes downfield and creates separation from defenders.");
        addPos("TE", "Tight End", "Hybrid role: blocks like a lineman and catches passes like a receiver.");
        addPos("OL", "Offensive Lineman", "Blocks to protect the quarterback and open running lanes.");
        addPos("OT", "Offensive Tackle", "Edge blocker; protects the quarterback from outside rushers.");
        addPos("OG", "Offensive Guard", "Interior blocker; helps protect and opens lanes inside.");
        addPos("C",  "Center", "Snaps the ball to start each play and blocks in the middle.");
        addPos("DE", "Defensive End", "Pass-rusher/edge defender; pressures the quarterback and stops runs outside.");
        addPos("DT", "Defensive Tackle", "Interior defender; stops runs up the middle and collapses the pocket.");
        addPos("NT", "Nose Tackle", "Central run-stopper lined up over the center.");
        addPos("LB", "Linebacker", "Versatile defender; tackles runners, covers short passes, and can blitz.");
        addPos("MLB","Middle Linebacker", "Linebacker in the middle; often calls the defense and reads plays.");
        addPos("OLB","Outside Linebacker", "Linebacker on the edge; sets the edge vs run and can rush the passer.");
        addPos("CB", "Cornerback", "Covers wide receivers and defends passes near the sidelines.");
        addPos("S",  "Safety", "Deep defender; helps prevent big passes and supports the run.");
        addPos("DB", "Defensive Back", "Generic term for pass defenders (corners and safeties).");
        addPos("K",  "Kicker", "Kicks field goals and kickoffs.");
        addPos("P",  "Punter", "Kicks on 4th down to flip field position.");
        addPos("LS", "Long Snapper", "Specialist who snaps the ball on punts and field goal attempts.");
        addPos("FS", "Free Safety", "Typically the deepest defender; reads the quarterback and covers deep passes.");
        addPos("SS", "Strong Safety", "Often closer to the line; supports run defense and covers tight ends.");
        addPos("ILB","Inside Linebacker", "Linebacker aligned inside; key tackler in the middle of the field.");
    }

    private static void addPos(String code, String full, String desc) {
        POS_FULL.put(code, full);
        POS_DESC.put(code, desc);
    }

    // ── Components ────────────────────────────────────────────────────────────
    private JTabbedPane tabbedPane;
    private JComboBox<String> categoryBox;
    private JComboBox<String> nameBox;
    private JTextField   searchField;
    private JPanel       infoDisplayPanel;
    private JLabel       statusLabel;
    private JLabel       countLabel;
    private JLabel       dirtyLabel;
    private JCheckBox    activeOnlyCheckbox;
    private JComboBox<String> positionFilter;
    private JComboBox<String> yearFilter;
    private JList<String>     quickAccessList;
    private DefaultListModel<String> quickAccessModel;
    private JButton      viewToggleButton;

    // Undo / Dirty state / Autosave
    private final UndoManager undoManager = new UndoManager(200);
    private JMenuItem undoMenuItem;
    private JMenuItem redoMenuItem;
    private boolean isDirty = false;
    private javax.swing.Timer autosaveTimer;

    // Image loading
    private final ImageCache imageCache = new ImageCache(160);

    // Table view
    private JPanel       tableViewPanel;
    private JPanel       cardViewPanel;
    private boolean      tableViewActive = false;
    private JTable       mainTable;
    private DefaultTableModel tableModel;
    private String       sortColumn  = "Name";
    private boolean      sortAsc     = true;

    // Data
    private List<Player> players;
    private List<Coach>  coaches;
    private Object       currentSelection;

    // New category data
    private List<Cheerleader> cheerleaders;
    private List<Staff>       trainers;
    private List<Staff>       otherStaff;

    // Per-player career totals (keyed by Player name)
    private Map<String, PlayerStats> playerStats = new LinkedHashMap<>();

    // Global search
    private JTextField   globalSearchField;
    private JPanel       globalResultsPanel;

    // Comparison
    private JComboBox<String> compareBox1, compareBox2;
    private JPanel            comparePanel;

    // Current filtered list (used by table)
    private List<Player> filteredPlayers = new ArrayList<>();
    private List<Coach>  filteredCoaches = new ArrayList<>();
    private List<Cheerleader> filteredCheerleaders = new ArrayList<>();
    private List<Staff>       filteredTrainers     = new ArrayList<>();
    private List<Staff>       filteredOtherStaff   = new ArrayList<>();

    private static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

    /** Data directory resolved by CowboysDirectory (single source of truth). */
    private static final String DATA_DIR = CowboysDirectory.DATA_DIR;

    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================
    public CowboysApp() {
        installUIDefaults();
        setTitle("Dallas Cowboys Directory – Full Roster Edition");
        setSize(1280, 920);
        setMinimumSize(new Dimension(1000, 700));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(BG_GRAY);

        System.out.println("Working dir: " + System.getProperty("user.dir"));
        players = CowboysDirectory.loadPlayers(DATA_DIR + "players.csv");
        coaches = CowboysDirectory.loadCoaches(DATA_DIR + "coaches.csv");
        cheerleaders = CowboysDirectory.loadCheerleaders(DATA_DIR + "cheerleaders.csv");
        trainers     = CowboysDirectory.loadStaff(DATA_DIR + "trainers.csv");
        otherStaff   = CowboysDirectory.loadStaff(DATA_DIR + "other_staff.csv");

        // Load stats (or create defaults for all players).
        playerStats = CowboysDirectory.loadPlayerStats(DATA_DIR + "player_stats.csv");
        boolean statsChanged = ensureStatsForAllPlayers();
        if (statsChanged || playerStats.isEmpty()) {
            CowboysDirectory.savePlayerStats(playerStats, DATA_DIR + "player_stats.csv");
        }
        System.out.printf("Loaded: %d players, %d coaches, %d cheerleaders, %d trainers, %d staff%n",
            players.size(), coaches.size(), cheerleaders.size(), trainers.size(), otherStaff.size());

        setJMenuBar(buildMenuBar());
        add(buildHeader(),          BorderLayout.NORTH);
        add(buildTabbedInterface(), BorderLayout.CENTER);
        add(buildStatusBar(),       BorderLayout.SOUTH);

        // Close handling (prompt on unsaved changes)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApp();
            }
        });

        setupKeyBindings();
        setupAutosave();
        updateUndoMenuState();

        setVisible(true);
        applyFilters(); // populate immediately with correct filters
    }

    /** Ensures every loaded player has a stats row (prefilled with zero totals). */
    private boolean ensureStatsForAllPlayers() {
        boolean changed = false;
        for (Player p : players) {
            String key = p.getName();
            if (key == null) continue;
            key = key.trim();
            if (key.isEmpty()) continue;
            if (!playerStats.containsKey(key)) {
                playerStats.put(key, new PlayerStats(key));
                changed = true;
            }
        }
        // Remove orphaned stats entries (players no longer present)
        Set<String> names = players.stream().map(Player::getName).filter(Objects::nonNull)
            .map(String::trim).collect(java.util.stream.Collectors.toSet());
        Iterator<Map.Entry<String, PlayerStats>> it = playerStats.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, PlayerStats> e = it.next();
            if (!names.contains(e.getKey())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    // =========================================================================
    // MENU BAR
    // =========================================================================
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(NAVY);
        bar.setOpaque(true);
        bar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        // ── File ──
        JMenu file = styledMenu("File");
        addItem(file, "💾 Save All Changes",       e -> saveAllData());
        addItem(file, "📦 Backup Data",             e -> backupData());
        addItem(file, "♻️ Restore from Backup",    e -> restoreData());
        file.addSeparator();
        addItem(file, "❌ Exit",                    e -> exitApp());
        bar.add(file);

        // ── Edit ──
        JMenu edit = styledMenu("Edit");
        undoMenuItem = new JMenuItem("Undo");
        redoMenuItem = new JMenuItem("Redo");
        undoMenuItem.setFont(new Font("Arial", Font.PLAIN, 13));
        redoMenuItem.setFont(new Font("Arial", Font.PLAIN, 13));
        undoMenuItem.setOpaque(true);
        redoMenuItem.setOpaque(true);
        undoMenuItem.setBackground(WHITE);
        redoMenuItem.setBackground(WHITE);
        undoMenuItem.setForeground(NAVY);
        redoMenuItem.setForeground(NAVY);
        undoMenuItem.addActionListener(e -> doUndo());
        redoMenuItem.addActionListener(e -> doRedo());
        edit.add(undoMenuItem);
        edit.add(redoMenuItem);
        bar.add(edit);

        // ── Players ──
        JMenu playerMenu = styledMenu("Players");
        addItem(playerMenu, "➕ Add New Player",    e -> showAddPlayerDialog());
        addItem(playerMenu, "✏️ Edit Player",       e -> showEditPlayerDialog());
        addItem(playerMenu, "📊 Edit Player Stats", e -> showEditPlayerStatsDialog());
        addItem(playerMenu, "🗑️ Delete Player",    e -> deleteCurrentPlayer());
        bar.add(playerMenu);

        // ── Coaches ──
        JMenu coachMenu = styledMenu("Coaches");
        addItem(coachMenu, "➕ Add New Coach",      e -> showAddCoachDialog());
        addItem(coachMenu, "✏️ Edit Coach",         e -> showEditCoachDialog());
        addItem(coachMenu, "🗑️ Delete Coach",      e -> deleteCurrentCoach());
        bar.add(coachMenu);

        // ── View ──
        JMenu viewMenu = styledMenu("View");
        addItem(viewMenu, "🔄 Refresh Data",        e -> refreshAllData());
        addItem(viewMenu, "📊 Statistics",          e -> tabbedPane.setSelectedIndex(1));
        addItem(viewMenu, "🔍 Global Search",      e -> tabbedPane.setSelectedIndex(1));
        addItem(viewMenu, "⚖️ Compare Players",   e -> tabbedPane.setSelectedIndex(4));
        addItem(viewMenu, "⭐ Favourites",          e -> showFavouritesQuickAccess());
        bar.add(viewMenu);

        return bar;
    }

    private JMenu styledMenu(String title) {
        JMenu m = new JMenu(title);
        m.setForeground(WHITE);
        m.setBackground(NAVY);
        m.setOpaque(true);
        m.setFont(new Font("Arial", Font.BOLD, 13));
        return m;
    }

    private void addItem(JMenu menu, String label, ActionListener al) {
        JMenuItem item = new JMenuItem(label);
        item.setFont(new Font("Arial", Font.PLAIN, 13));
        // Ensure menu items stay readable across Look & Feel variants.
        item.setOpaque(true);
        item.setBackground(WHITE);
        item.setForeground(NAVY);
        item.addActionListener(al);
        menu.add(item);
    }

    // =========================================================================
    // GRADIENT HEADER
    // =========================================================================
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, NAVY_DARK, getWidth(), getHeight(), NAVY);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 25, 18, 25));

        // Star icon with gold glow
        JLabel star = new JLabel("★") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // subtle glow
                g2.setColor(new Color(255,182,18,60));
                g2.setFont(getFont().deriveFont(74f));
                g2.drawString("★", 2, 66);
                g2.setColor(GOLD);
                g2.setFont(getFont());
                g2.drawString("★", 0, 64);
                g2.dispose();
            }
        };
        star.setFont(new Font("Dialog", Font.BOLD, 64));
        star.setPreferredSize(new Dimension(75, 70));

        JLabel title = new JLabel("DALLAS COWBOYS DIRECTORY");
        title.setFont(new Font("Arial Black", Font.BOLD, 30));
        title.setForeground(SILVER);

        JLabel subtitle = new JLabel("Full Roster Edition  –  Players · Coaches · Cheerleaders · Staff  |  " + CURRENT_YEAR);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setForeground(new Color(200, 210, 220));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 4));
        text.setOpaque(false);
        text.add(title);
        text.add(subtitle);

        header.add(star, BorderLayout.WEST);
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    // =========================================================================
    // TABBED INTERFACE
    // =========================================================================
    private JTabbedPane buildTabbedInterface() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 13));
        tabbedPane.setBackground(WHITE);
        tabbedPane.addTab("📁 Directory",    buildDirectoryTab());
        tabbedPane.addTab("🔍 Global Search",buildGlobalSearchTab());
        tabbedPane.addTab("📊 Statistics",   buildStatisticsTab());
        tabbedPane.addTab("📋 Depth Chart",  buildDepthChartTab());
        tabbedPane.addTab("⚖️ Compare",     buildCompareTab());
        tabbedPane.addTab("ℹ️ About",        buildAboutTab());
        return tabbedPane;
    }

    // =========================================================================
    // DIRECTORY TAB
    // =========================================================================
    private JPanel buildDirectoryTab() {
        JPanel dir = new JPanel(new BorderLayout());
        dir.setBackground(BG_GRAY);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(220);
        split.setDividerSize(6);
        split.setResizeWeight(0.0);
        split.setLeftComponent(buildSidebar());

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setBackground(BG_GRAY);
        right.setBorder(new EmptyBorder(10, 10, 10, 10));
        right.add(buildControlPanel(),    BorderLayout.NORTH);
        right.add(buildCenterContent(),   BorderLayout.CENTER);
        right.add(buildActionPanel(),     BorderLayout.SOUTH);

        split.setRightComponent(right);
        dir.add(split, BorderLayout.CENTER);
        return dir;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sb = new JPanel(new BorderLayout(0, 8));
        sb.setBackground(WHITE);
        sb.setBorder(new CompoundBorder(
            new LineBorder(SILVER_LITE, 1),
            new EmptyBorder(12, 10, 12, 10)
        ));

        JLabel lbl = new JLabel("Quick Access");
        lbl.setFont(new Font("Arial", Font.BOLD, 15));
        lbl.setForeground(NAVY);
        lbl.setBorder(new EmptyBorder(0,2,8,0));

        quickAccessModel = new DefaultListModel<>();
        quickAccessModel.addElement("🟢 All Active Players");
        quickAccessModel.addElement("🟢 All Active Coaches");
        quickAccessModel.addElement("💃 Cheerleaders");
        quickAccessModel.addElement("⚕️ Trainers");
        quickAccessModel.addElement("🏢 Other Staff");
        quickAccessModel.addElement("🏈 Quarterbacks");
        quickAccessModel.addElement("⚡ Wide Receivers");
        quickAccessModel.addElement("🛡️ Defensive Players");
        quickAccessModel.addElement("🏆 Pro Bowl Players");
        quickAccessModel.addElement("📜 Hall of Famers");
        quickAccessModel.addElement("⭐ Favourites");
        quickAccessModel.addElement("📅 Joined This Decade");

        quickAccessList = new JList<>(quickAccessModel);
        quickAccessList.setFont(new Font("Arial", Font.PLAIN, 13));
        quickAccessList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        quickAccessList.setBackground(WHITE);
        quickAccessList.setFixedCellHeight(34);
        quickAccessList.setCellRenderer(new SidebarCellRenderer());
        quickAccessList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) handleQuickAccess();
        });

        JScrollPane scroll = new JScrollPane(quickAccessList);
        scroll.setBorder(new LineBorder(SILVER_LITE, 1));

        sb.add(lbl,    BorderLayout.NORTH);
        sb.add(scroll, BorderLayout.CENTER);
        return sb;
    }

    private void handleQuickAccess() {
        String sel = quickAccessList.getSelectedValue();
        if (sel == null) return;
        // reset filters first
        activeOnlyCheckbox.setSelected(false);
        positionFilter.setSelectedIndex(0);
        yearFilter.setSelectedIndex(0);
        searchField.setText("");

        switch (sel) {
            case "🟢 All Active Players":
                categoryBox.setSelectedItem("Players");
                activeOnlyCheckbox.setSelected(true);
                break;
            case "🟢 All Active Coaches":
                categoryBox.setSelectedItem("Coaches");
                activeOnlyCheckbox.setSelected(true);
                break;
            case "💃 Cheerleaders":        categoryBox.setSelectedItem("Cheerleaders"); break;
            case "⚕️ Trainers":           categoryBox.setSelectedItem("Trainers"); break;
            case "🏢 Other Staff":         categoryBox.setSelectedItem("Other Staff"); break;
            case "🏈 Quarterbacks":
                categoryBox.setSelectedItem("Players");
                selectPositionByCode("QB");
                break;
            case "⚡ Wide Receivers":
                categoryBox.setSelectedItem("Players");
                selectPositionByCode("WR");
                break;
            case "🛡️ Defensive Players":
                categoryBox.setSelectedItem("Players");
                // filter applied via applyFilters; we also filter by def positions below
                break;
            case "🏆 Pro Bowl Players":
                categoryBox.setSelectedItem("Players");
                break;
            case "📜 Hall of Famers":
                categoryBox.setSelectedItem("Players");
                searchField.setText("Hall of Fame");
                break;
            case "⭐ Favourites":
                showFavouritesQuickAccess();
                return;
            case "📅 Joined This Decade":
                categoryBox.setSelectedItem("Players");
                break;
        }
        applyFilters();

        // Post-filter refinements
        if (sel.equals("🛡️ Defensive Players")) {
            List<String> defPos = Arrays.asList("DE","DT","LB","CB","S","MLB","OLB","DB","NT");
            // IMPORTANT: table view uses filteredPlayers; so we must actually filter the data,
            // not just the name dropdown.
            filteredPlayers = filteredPlayers.stream()
                    .filter(p -> defPos.contains(p.getPosition()))
                    .collect(Collectors.toList());

            nameBox.removeAllItems();
            for (Player p : filteredPlayers) nameBox.addItem(p.getName());

            if (tableViewActive) refreshTableData();
        }
        if (sel.equals("🏆 Pro Bowl Players")) {
            nameBox.removeAllItems();
            filteredPlayers.stream()
                .filter(p -> p.getProBowls() > 0)
                .sorted(Comparator.comparingInt(Player::getProBowls).reversed())
                .forEach(p -> nameBox.addItem(p.getName()));
        }
        if (sel.equals("📅 Joined This Decade")) {
            int decade = (CURRENT_YEAR / 10) * 10;
            nameBox.removeAllItems();
            filteredPlayers.stream()
                .filter(p -> p.getJoined() >= decade)
                .forEach(p -> nameBox.addItem(p.getName()));
        }
        updateStatusLabel();
    }

    private void showFavouritesQuickAccess() {
        nameBox.removeAllItems();
        if (categoryBox.getSelectedItem().equals("Players")) {
            CowboysDirectory.filterPlayersFavorites(players)
                .forEach(p -> nameBox.addItem(p.getName()));
        } else {
            CowboysDirectory.filterCoachesFavorites(coaches)
                .forEach(c -> nameBox.addItem(c.getName()));
        }
        updateStatusLabel();
    }

    // ── Control panel ─────────────────────────────────────────────────────────
    private JPanel buildControlPanel() {
        JPanel cc = new JPanel(new BorderLayout(0, 6));
        cc.setBackground(BG_GRAY);
        cc.add(buildSelectionPanel(), BorderLayout.NORTH);
        cc.add(buildFilterPanel(),    BorderLayout.CENTER);
        return cc;
    }

    private JPanel buildSelectionPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(SILVER_LITE, 1),
            new EmptyBorder(12, 16, 12, 16)
        ));

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.fill   = GridBagConstraints.HORIZONTAL;

        // Row 0
        g.gridx=0; g.gridy=0; g.weightx=0;
        p.add(label("Category:"), g);
        g.gridx=1; g.weightx=0.2;
        categoryBox = comboBox("Players","Coaches","Cheerleaders","Trainers","Other Staff");
        p.add(categoryBox, g);

        g.gridx=2; g.weightx=0;
        p.add(label("Name:"), g);
        g.gridx=3; g.weightx=0.5;
        nameBox = new JComboBox<>();
        nameBox.setFont(new Font("Arial", Font.PLAIN, 13));
        nameBox.setPreferredSize(new Dimension(260, 34));
        p.add(nameBox, g);

        g.gridx=4; g.weightx=0;
        viewToggleButton = styledBtn("🗒️ Table View");
        viewToggleButton.setPreferredSize(new Dimension(130, 34));
        viewToggleButton.addActionListener(e -> toggleView());
        p.add(viewToggleButton, g);

        // Row 1 – search
        g.gridx=0; g.gridy=1; g.weightx=0;
        p.add(label("🔍 Search:"), g);
        g.gridx=1; g.gridwidth=3; g.weightx=0.7;
        searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(300, 34));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(SILVER, 1),
            new EmptyBorder(4, 8, 4, 8)
        ));
        p.add(searchField, g);

        g.gridx=4; g.gridwidth=1; g.weightx=0;
        JButton clear = styledBtn("Clear");
        clear.setPreferredSize(new Dimension(90, 34));
        clear.addActionListener(e -> { searchField.setText(""); resetFilters(); applyFilters(); });
        p.add(clear, g);

        // Listeners
        categoryBox.addActionListener(e -> { resetFilters(); applyFilters(); });
        nameBox.addActionListener(e -> showInfo());
        searchField.getDocument().addDocumentListener(simpleDocListener(this::applyFilters));

        return p;
    }


    private String[] buildPositionFilterItems() {
        // Keep a stable, friendly ordering.
        java.util.List<String> items = new java.util.ArrayList<>();
        items.add("All Positions");
        String[] order = {"QB","RB","FB","WR","TE","OT","OG","C","OL","DE","DT","NT","LB","MLB","OLB","CB","S","FS","SS","DB","K","P","LS"};
        for (String code : order) items.add(positionFilterLabel(code));
        return items.toArray(String[]::new);
    }

    private JPanel buildFilterPanel() {
        JPanel fp = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        fp.setBackground(WHITE);
        fp.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(SILVER_LITE, 1),
            BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(),
                "Filters",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Arial", Font.BOLD, 12), NAVY
            )
        ));

        activeOnlyCheckbox = new JCheckBox("Active Only");
        activeOnlyCheckbox.setFont(new Font("Arial", Font.PLAIN, 13));
        activeOnlyCheckbox.setBackground(WHITE);
        activeOnlyCheckbox.setSelected(true);
        activeOnlyCheckbox.addActionListener(e -> applyFilters());

        String[] posItems = buildPositionFilterItems();
        positionFilter = new JComboBox<>(posItems);
        positionFilter.setFont(new Font("Arial", Font.PLAIN, 13));
        positionFilter.setPreferredSize(new Dimension(150, 32));
        positionFilter.addActionListener(e -> applyFilters());

        int thisYear = CURRENT_YEAR;
        String[] years = new String[thisYear - 1988 + 1];
        years[0] = "All Years";
        for (int i = 1; i < years.length; i++) years[i] = String.valueOf(1988 + i);
        yearFilter = new JComboBox<>(years);
        yearFilter.setFont(new Font("Arial", Font.PLAIN, 13));
        yearFilter.setPreferredSize(new Dimension(120, 32));
        yearFilter.addActionListener(e -> applyFilters());

        JButton reset = styledBtn("↺ Reset");
        reset.setPreferredSize(new Dimension(90, 32));
        reset.addActionListener(e -> { resetFilters(); applyFilters(); });

        fp.add(new JLabel("Status:")); fp.add(activeOnlyCheckbox);
        fp.add(Box.createHorizontalStrut(6));
        fp.add(label("Position:")); fp.add(positionFilter);
        fp.add(Box.createHorizontalStrut(6));
        fp.add(label("Year Joined:")); fp.add(yearFilter);
        fp.add(Box.createHorizontalStrut(6));
        fp.add(reset);

        return fp;
    }

    // ── Center content (card / table) ─────────────────────────────────────────
    private JPanel buildCenterContent() {
        JPanel center = new JPanel(new CardLayout());
        center.setBackground(BG_GRAY);

        cardViewPanel = buildCardViewPanel();
        tableViewPanel = buildTableViewPanel();

        center.add(cardViewPanel,  "CARD");
        center.add(tableViewPanel, "TABLE");

        // Keep reference so we can flip
        center.setName("CONTENT");
        return center;
    }

    private JPanel buildCardViewPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_GRAY);

        infoDisplayPanel = new JPanel(new BorderLayout());
        infoDisplayPanel.setBackground(WHITE);
        infoDisplayPanel.setBorder(new CompoundBorder(
            new LineBorder(SILVER_LITE, 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel welcome = new JLabel(
            "<html><div style='text-align:center;padding:40px'>" +
            "<h2 style='color:#002244'>Welcome to the Cowboys Directory</h2>" +
            "<p style='color:#666'>Select a player or coach from the list above to view their profile</p>" +
            "<p style='color:#999;font-size:11px'>Use Quick Access shortcuts on the left for fast filtering</p>" +
            "</div></html>", SwingConstants.CENTER);
        infoDisplayPanel.add(welcome, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(infoDisplayPanel);
        scroll.setBorder(null);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildTableViewPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(BG_GRAY);

        String[] playerCols = {"★","Name","Position","#","Status","Joined","Pro Bowls","College"};
        tableModel = new DefaultTableModel(playerCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return String.class;
            }
        };

        mainTable = new JTable(tableModel);
        mainTable.setRowHeight(28);
        mainTable.setFont(new Font("Arial", Font.PLAIN, 13));
        mainTable.setSelectionBackground(new Color(0, 53, 148, 40));
        mainTable.setSelectionForeground(NAVY);
        mainTable.setGridColor(SILVER_LITE);
        mainTable.setShowVerticalLines(false);
        mainTable.setIntercellSpacing(new Dimension(0, 1));
        mainTable.setFillsViewportHeight(true);

        // Header styling (force readable header colors on all Look & Feels)
        JTableHeader th = mainTable.getTableHeader();
        th.setReorderingAllowed(false);
        th.setPreferredSize(new Dimension(0, 34));
        applyTableTheme(mainTable);

        // Column widths
        int[] widths = {30,180,80,50,80,70,80,150};
        for (int i = 0; i < widths.length; i++) {
            mainTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Sort on header click
        th.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int col = th.columnAtPoint(e.getPoint());
                String[] cols = {"★","Name","Position","Number","Status","Joined","Pro Bowls","College"};
                if (col < 1) return;
                String clicked = cols[col];
                if (clicked.equals(sortColumn)) sortAsc = !sortAsc;
                else { sortColumn = clicked; sortAsc = true; }
                refreshTableData();
            }
        });

        // Click row → show card
        mainTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && mainTable.getSelectedRow() >= 0) {
                String name = (String) tableModel.getValueAt(mainTable.getSelectedRow(), 1);
                selectByName(name);
            }
        });

        // Row colouring
        mainTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? WHITE : new Color(247,249,252));
                    // highlight active vs former
                    Object status = tableModel.getValueAt(row, 4);
                    if ("Former".equals(status)) c.setForeground(SILVER);
                    else                         c.setForeground(NAVY);
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return c;
            }
        });

        JScrollPane scroll = new JScrollPane(mainTable);
        scroll.setBorder(new LineBorder(SILVER_LITE, 1));
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    private void toggleView() {
        tableViewActive = !tableViewActive;
        JPanel content = findContentPanel();
        if (content != null) {
            CardLayout cl = (CardLayout) content.getLayout();
            cl.show(content, tableViewActive ? "TABLE" : "CARD");
        }
        viewToggleButton.setText(tableViewActive ? "🃏 Card View" : "🗒️ Table View");
        if (tableViewActive) refreshTableData();
    }

    private JPanel findContentPanel() {
        // Walk up from infoDisplayPanel
        Container c = infoDisplayPanel.getParent();
        while (c != null) {
            if (c instanceof JPanel && "CONTENT".equals(((JPanel) c).getName())) return (JPanel) c;
            c = c.getParent();
        }
        return null;
    }

    private void refreshTableData() {
        tableModel.setRowCount(0);
        String cat = String.valueOf(categoryBox.getSelectedItem());
        if ("Players".equals(cat)) {
            List<Player> sorted = CowboysDirectory.sortPlayers(filteredPlayers, sortColumn, sortAsc);
            String[] coachCols = {"★","Name","Position","#","Status","Joined","Pro Bowls","College"};
            updateTableColumns(coachCols, new int[]{30,180,150,50,80,70,80,150});
            for (Player p : sorted) {
                tableModel.addRow(new Object[]{
                    p.isFavorite() ? "★" : "",
                    p.getName(),
                    positionCell(p.getPosition()),
                    p.getNumber(),
                    p.getStatus(),
                    p.getJoined(),
                    p.getProBowls(),
                    p.getCollege()
                });
            }
        } else if ("Coaches".equals(cat)) {
            List<Coach> sorted = CowboysDirectory.sortCoaches(filteredCoaches, sortColumn, sortAsc);
            String[] coachCols = {"★","Name","Role","Status","Joined","Championships","Experience","Previous Team"};
            updateTableColumns(coachCols, new int[]{30,200,160,80,70,100,120,160});
            for (Coach c : sorted) {
                tableModel.addRow(new Object[]{
                    c.isFavorite() ? "★" : "",
                    c.getName(),
                    c.getRole(),
                    c.getStatus(),
                    c.getJoined(),
                    c.getChampionships(),
                    c.getExperience(),
                    c.getPreviousTeam()
                });
            }
        } else if ("Cheerleaders".equals(cat)) {
            // Cheerleaders aren't sortable via CowboysDirectory; keep UI consistent.
            String[] cols = {"Name","Role","Age","Height","Weight","College","Experience","Status"};
            updateTableColumns(cols, new int[]{200,140,50,70,60,180,90,80});
            for (Cheerleader c : filteredCheerleaders) {
                tableModel.addRow(new Object[]{
                    c.getName(),
                    c.getRole(),
                    c.getAge(),
                    c.getHeight(),
                    c.getWeight(),
                    c.getCollege(),
                    c.getExperience(),
                    c.getStatus()
                });
            }
        } else if ("Trainers".equals(cat) || "Other Staff".equals(cat)) {
            List<Staff> list = "Trainers".equals(cat) ? filteredTrainers : filteredOtherStaff;
            String[] cols = {"Name","Role","Age","Height","Weight","College","Experience","Status"};
            updateTableColumns(cols, new int[]{200,160,50,70,60,180,90,80});
            for (Staff s : list) {
                tableModel.addRow(new Object[]{
                    s.getName(),
                    s.getRole(),
                    s.getAge(),
                    s.getHeight(),
                    s.getWeight(),
                    s.getCollege(),
                    s.getExperience(),
                    s.getStatus()
                });
            }
        }
    }

    private void updateTableColumns(String[] cols, int[] widths) {
        tableModel.setColumnIdentifiers(cols);
        for (int i = 0; i < widths.length && i < mainTable.getColumnCount(); i++) {
            mainTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    // ── Action panel ──────────────────────────────────────────────────────────
    private JPanel buildActionPanel() {
        JPanel ap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        ap.setBackground(BG_GRAY);

        JButton fav = styledBtn("⭐ Toggle Favourite");
        fav.addActionListener(e -> toggleFavourite());

        JButton exp = styledBtn("💾 Export Profile");
        exp.addActionListener(e -> exportCurrentProfile());

        JButton prt = styledBtn("🖨️ Print");
        prt.addActionListener(e -> printCurrentProfile());

        ap.add(fav); ap.add(exp); ap.add(prt);
        return ap;
    }

    // =========================================================================
    // STATISTICS TAB
    // =========================================================================
    private JPanel buildStatisticsTab() {
        JPanel stats = new JPanel(new BorderLayout(10, 10));
        stats.setBackground(BG_GRAY);
        stats.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("📊 Directory Statistics", SwingConstants.CENTER);
        title.setFont(new Font("Arial Black", Font.BOLD, 22));
        title.setForeground(NAVY);
        title.setBorder(new EmptyBorder(0, 0, 10, 0));

        // Stat cards row
        JPanel cards = new JPanel(new GridLayout(2, 4, 12, 12));
        cards.setBackground(BG_GRAY);

        long activePl  = CowboysDirectory.countActivePlayers(players);
        long activeCo  = CowboysDirectory.countActiveCoaches(coaches);
        int  totPB     = CowboysDirectory.getTotalProBowls(players);
        long pbPlayers = players.stream().filter(p -> p.getProBowls() > 0).count();
        int  totChamp  = CowboysDirectory.getTotalChampionships(coaches);
        long hallFame  = players.stream().filter(p -> p.getAchievements() != null && p.getAchievements().toLowerCase().contains("hall of fame")).count();

        cards.add(statCard("Total Players",   String.valueOf(players.size()), BLUE));
        cards.add(statCard("Active Players",  String.valueOf(activePl),       GREEN));
        cards.add(statCard("Total Coaches",   String.valueOf(coaches.size()),  BLUE));
        cards.add(statCard("Active Coaches",  String.valueOf(activeCo),        GREEN));
        cards.add(statCard("Total Pro Bowls", String.valueOf(totPB),           GOLD));
        cards.add(statCard("Pro Bowl Players",String.valueOf(pbPlayers),        GOLD));
        cards.add(statCard("Championships",   String.valueOf(totChamp),         RED_SOFT));
        cards.add(statCard("Hall of Famers",  String.valueOf(hallFame),         new Color(120,0,180)));

        // Row 1 charts (original 3)
        JPanel charts1 = new JPanel(new GridLayout(1, 3, 12, 0));
        charts1.setBackground(BG_GRAY);
        charts1.add(buildPositionChart());
        charts1.add(buildProBowlsChart());
        charts1.add(buildDraftRoundChart());

        // Row 2 charts (new)
        JPanel charts2 = new JPanel(new GridLayout(1, 2, 12, 0));
        charts2.setBackground(BG_GRAY);
        charts2.add(buildActiveVsFormerPie());
        charts2.add(buildYearsJoinedTimeline());

        JPanel chartsBlock = new JPanel(new GridLayout(2, 1, 0, 12));
        chartsBlock.setBackground(BG_GRAY);
        chartsBlock.add(charts1);
        chartsBlock.add(charts2);

        JPanel bottom = new JPanel(new GridLayout(1, 2, 12, 0));
        bottom.setBackground(BG_GRAY);
        bottom.add(chartsBlock);
        bottom.add(buildProBowlLeaderboard());

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(BG_GRAY);
        body.add(cards,  BorderLayout.NORTH);
        body.add(bottom, BorderLayout.CENTER);

        stats.add(title, BorderLayout.NORTH);
        stats.add(body,  BorderLayout.CENTER);
        return stats;
    }

    // ── Active vs Former pie chart ─────────────────────────────────────────────
    private JPanel buildActiveVsFormerPie() {
        long active = CowboysDirectory.countActivePlayers(players);
        long former = players.size() - active;
        JPanel pie = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setFont(new Font("Arial", Font.BOLD, 12)); g2.setColor(NAVY);
                String t = "Active vs Former Players";
                g2.drawString(t, (w - g2.getFontMetrics().stringWidth(t)) / 2, 18);
                int size = Math.max(60, Math.min(w - 140, h - 50));
                int px = 20, py = (h - size) / 2 + 10;
                long total = active + former;
                if (total == 0) return;
                double sw = 360.0 * active / total;
                g2.setColor(GREEN);  g2.fillArc(px, py, size, size, 90, -(int) sw);
                g2.setColor(SILVER); g2.fillArc(px, py, size, size, (int)(90 - sw), -(int)(360 - sw));
                g2.setColor(WHITE); g2.setStroke(new BasicStroke(2)); g2.drawArc(px, py, size, size, 0, 360);
                int lx = px + size + 16, ly = py + size / 2 - 20;
                g2.setFont(new Font("Arial", Font.BOLD, 11));
                g2.setColor(GREEN);  g2.fillRect(lx, ly, 12, 12);
                g2.setColor(NAVY);   g2.drawString("Active: " + active + " (" + Math.round(100.0*active/total) + "%)", lx+16, ly+11);
                g2.setColor(SILVER); g2.fillRect(lx, ly+22, 12, 12);
                g2.setColor(NAVY);   g2.drawString("Former: " + former + " (" + Math.round(100.0*former/total) + "%)", lx+16, ly+33);
            }
        };
        pie.setBackground(WHITE);
        pie.setBorder(new CompoundBorder(new LineBorder(SILVER_LITE, 1), new EmptyBorder(10, 10, 10, 10)));
        return pie;
    }

    // ── Years-joined timeline ──────────────────────────────────────────────────
    private JPanel buildYearsJoinedTimeline() {
        Map<String, Long> buckets = new LinkedHashMap<>();

        // Use actual data range (avoids dropping older players and odd labels like "2000-4").
        int minJoined = players.stream().mapToInt(Player::getJoined).min().orElse(CURRENT_YEAR);
        int start = (minJoined / 5) * 5;

        for (int y = start; y <= CURRENT_YEAR; y += 5) {
            int from = y;
            int to = Math.min(y + 4, CURRENT_YEAR);
            long cnt = players.stream().filter(p -> p.getJoined() >= from && p.getJoined() <= to).count();
            String label = from + "-" + String.format("%02d", (to % 100));
            buckets.put(label, cnt);
        }

        return new BarChartPanel("Players by Join Year (5-yr)", new ArrayList<>(buckets.entrySet()), BLUE);
    }

    // ── Top Pro Bowlers leaderboard ────────────────────────────────────────────
    private JPanel buildProBowlLeaderboard() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBackground(WHITE);
        panel.setBorder(new CompoundBorder(new LineBorder(SILVER_LITE, 1), new EmptyBorder(12, 14, 12, 14)));
        JLabel title = new JLabel("🏆 Top Pro Bowlers");
        title.setFont(new Font("Arial Black", Font.BOLD, 14)); title.setForeground(NAVY);
        title.setBorder(new MatteBorder(0, 0, 1, 0, SILVER_LITE));
        panel.add(title, BorderLayout.NORTH);
        JPanel list = new JPanel(); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setBackground(WHITE);
        List<Player> top = players.stream().filter(p -> p.getProBowls() > 0)
            .sorted(Comparator.comparingInt(Player::getProBowls).reversed()).limit(10).collect(Collectors.toList());
        for (int i = 0; i < top.size(); i++) {
            Player p = top.get(i);
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setBackground(i % 2 == 0 ? WHITE : new Color(248, 250, 253));
            row.setBorder(new EmptyBorder(6, 8, 6, 8));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel rank = new JLabel("#" + (i + 1));
            rank.setFont(new Font("Arial", Font.BOLD, 12));
            rank.setForeground(i == 0 ? GOLD : (i == 1 ? SILVER : (i == 2 ? new Color(180, 100, 30) : SILVER)));
            rank.setPreferredSize(new Dimension(30, 20));
            JLabel nm = new JLabel(p.getName()); nm.setFont(new Font("Arial", Font.PLAIN, 13)); nm.setForeground(NAVY);
            JLabel pos = new JLabel(p.getPosition()); pos.setFont(new Font("Arial", Font.PLAIN, 11)); pos.setForeground(SILVER);
            JLabel pb = new JLabel(p.getProBowls() + " × Pro Bowl"); pb.setFont(new Font("Arial", Font.BOLD, 12)); pb.setForeground(GOLD);
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0)); left.setBackground(row.getBackground());
            left.add(rank); left.add(nm); left.add(pos);
            row.add(left, BorderLayout.CENTER); row.add(pb, BorderLayout.EAST);
            list.add(row);
        }
        JScrollPane sc = new JScrollPane(list); sc.setBorder(null);
        panel.add(sc, BorderLayout.CENTER);
        return panel;
    }

    private JPanel statCard(String label, String value, Color accent) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(accent, 2), new EmptyBorder(14, 14, 14, 14)));

        JLabel val = new JLabel(value, SwingConstants.CENTER);
        val.setFont(new Font("Arial Black", Font.BOLD, 32));
        val.setForeground(accent);

        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font("Arial", Font.PLAIN, 12));
        lbl.setForeground(NAVY);

        card.add(val, BorderLayout.CENTER);
        card.add(lbl, BorderLayout.SOUTH);
        return card;
    }

    // ── Charts ─────────────────────────────────────────────────────────────────
    private JPanel buildPositionChart() {
        Map<String, Long> data = CowboysDirectory.getPlayerCountByPosition(players);
        // Only top 8 positions
        List<Map.Entry<String, Long>> entries = data.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(8).collect(Collectors.toList());
        return new BarChartPanel("Players by Position", entries, BLUE);
    }

    private JPanel buildProBowlsChart() {
        // Bin players by pro bowl count 0,1,2,3,4,5+
        Map<String, Long> data = new LinkedHashMap<>();
        long[] bins = new long[6];
        for (Player p : players) {
            int pb = Math.min(p.getProBowls(), 5);
            bins[pb]++;
        }
        String[] labels = {"0","1","2","3","4","5+"};
        List<Map.Entry<String, Long>> entries = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            entries.add(new AbstractMap.SimpleEntry<>(labels[idx], bins[idx]));
        }
        return new BarChartPanel("Pro Bowl Distribution", entries, GOLD);
    }

    private JPanel buildDraftRoundChart() {
        Map<Integer, Long> raw = CowboysDirectory.getDraftRoundDistribution(players);
        List<Map.Entry<String, Long>> entries = new ArrayList<>();
        for (int r = 1; r <= 7; r++) {
            long v = raw.getOrDefault(r, 0L);
            final int rr = r; final long vv = v;
            entries.add(new AbstractMap.SimpleEntry<>("Rd " + rr, vv));
        }
        return new BarChartPanel("Draft Round Distribution", entries, GREEN);
    }

    /** Simple vertical bar chart drawn with Java2D. */
    private static class BarChartPanel extends JPanel {
        private final String title;
        private final List<Map.Entry<String, Long>> data;
        private final Color barColor;

        BarChartPanel(String title, List<Map.Entry<String, Long>> data, Color barColor) {
            this.title    = title;
            this.data     = data;
            this.barColor = barColor;
            setBackground(WHITE);
            setBorder(new CompoundBorder(new LineBorder(SILVER_LITE, 1), new EmptyBorder(10,10,10,10)));
            setPreferredSize(new Dimension(0, 240));
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int padL=40, padR=10, padT=30, padB=44;
            int chartW = w-padL-padR, chartH = h-padT-padB;

            // Title
            g2.setFont(new Font("Arial", Font.BOLD, 12));
            g2.setColor(NAVY);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(title, (w - fm.stringWidth(title))/2, 18);

            if (chartW <= 0 || chartH <= 0) return;

            long maxVal = data.stream().mapToLong(Map.Entry::getValue).max().orElse(1);
            if (maxVal == 0) maxVal = 1;

            int barW = Math.max(4, (chartW - data.size()*4) / data.size());
            int gap  = (chartW - data.size()*barW) / (data.size()+1);

            // Axis
            g2.setColor(SILVER_LITE);
            g2.drawLine(padL, padT, padL, padT+chartH);
            g2.drawLine(padL, padT+chartH, padL+chartW, padT+chartH);

            // Y grid lines
            g2.setFont(new Font("Arial", Font.PLAIN, 10));
            g2.setColor(new Color(220,220,220));
            for (int i = 1; i <= 4; i++) {
                int yy = padT + chartH - (int)(chartH * i / 4.0);
                g2.drawLine(padL, yy, padL+chartW, yy);
                g2.setColor(SILVER);
                g2.drawString(String.valueOf((long)(maxVal * i / 4)), 2, yy+4);
                g2.setColor(new Color(220,220,220));
            }

            // Bars
            for (int i = 0; i < data.size(); i++) {
                long val  = data.get(i).getValue();
                String lbl = data.get(i).getKey();
                int barH  = (int)(chartH * val / maxVal);
                int x     = padL + gap + i*(barW+gap);
                int y     = padT + chartH - barH;

                // Bar with gradient
                GradientPaint gp = new GradientPaint(x, y, barColor.brighter(), x, y+barH, barColor.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(x, y, barW, barH, 4, 4);

                // Value label above bar
                g2.setColor(NAVY);
                g2.setFont(new Font("Arial", Font.BOLD, 10));
                String valStr = String.valueOf(val);
                int vw = g2.getFontMetrics().stringWidth(valStr);
                if (barH > 14) g2.drawString(valStr, x+(barW-vw)/2, y+12);
                else           g2.drawString(valStr, x+(barW-vw)/2, y-2);

                // X label
                g2.setColor(NAVY);
                g2.setFont(new Font("Arial", Font.PLAIN, 10));
                int lw = g2.getFontMetrics().stringWidth(lbl);
                g2.drawString(lbl, x+(barW-lw)/2, padT+chartH+14);
            }
        }
    }

    // =========================================================================
    // ABOUT TAB
    // =========================================================================
    private JPanel buildAboutTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(WHITE);
        p.setBorder(new EmptyBorder(30, 40, 30, 40));

        JTextArea ta = new JTextArea(
            "DALLAS COWBOYS DIRECTORY – Enhanced Edition\n" +
            "Professional Player & Coach Management System\n\n" +
            "VERSION HISTORY\n" +
            "─────────────────────────────────────────────\n" +
            "v8.0 – Enhanced Edition (Current)\n" +
            "  • Sortable JTable view (click column headers)\n" +
            "  • Bar charts in Statistics tab (positions, Pro Bowls, draft rounds)\n" +
            "  • Favourites system with ★ toggle (persisted to disk)\n" +
            "  • Gradient header with improved Cowboys palette\n" +
            "  • Position badges colour-coded by unit\n" +
            "  • RFC-4180 CSV parsing: no more comma→semicolon corruption\n" +
            "  • 'Years with Dallas' now uses the real current year\n" +
            "  • Filter resets no longer wipe nameBox before populating\n" +
            "  • Proper coaches.csv with actual coaching staff data\n\n" +
            "v7.0 – Original\n" +
            "  • CRUD, save/backup/restore, export/print, split-pane layout\n\n" +
            "DATA FILES\n" +
            "─────────────────────────────────────────────\n" +
            "  data/players.csv          – Player roster\n" +
            "  data/coaches.csv          – Coaching staff\n" +
            "  data/favorites_players.txt – Persisted favourites\n" +
            "  data/favorites_coaches.txt – Persisted favourites\n\n" +
            "USAGE TIPS\n" +
            "─────────────────────────────────────────────\n" +
            "  • Click '🗒️ Table View' to switch to sortable table\n" +
            "  • Click any column header in table view to sort\n" +
            "  • ⭐ Favourite button on each card saves across sessions\n" +
            "  • Search finds by name, position, AND college\n" +
            "  • File > Save All Changes writes edits back to CSV\n\n" +
            "© " + CURRENT_YEAR + " Dallas Cowboys Directory System"
        );
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
        ta.setBackground(WHITE);
        ta.setForeground(NAVY);

        p.add(new JScrollPane(ta), BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // STATUS BAR
    // =========================================================================
    private JPanel buildStatusBar() {
        JPanel sb = new JPanel(new BorderLayout());
        sb.setBackground(NAVY_DARK);
        sb.setBorder(new EmptyBorder(5, 12, 5, 12));

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(WHITE);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        countLabel = new JLabel();
        countLabel.setForeground(SILVER);
        countLabel.setFont(new Font("Arial", Font.PLAIN, 12));

        dirtyLabel = new JLabel(" ");
        dirtyLabel.setForeground(GOLD);
        dirtyLabel.setFont(new Font("Arial", Font.BOLD, 12));
        dirtyLabel.setHorizontalAlignment(SwingConstants.CENTER);

        sb.add(statusLabel, BorderLayout.WEST);
        sb.add(dirtyLabel,  BorderLayout.CENTER);
        sb.add(countLabel,  BorderLayout.EAST);
        return sb;
    }

    private void updateStatusLabel() {
        String cat = String.valueOf(categoryBox.getSelectedItem());
        int filtered = nameBox.getItemCount();
        int total;
        if ("Players".equals(cat)) total = players.size();
        else if ("Coaches".equals(cat)) total = coaches.size();
        else if ("Cheerleaders".equals(cat)) total = cheerleaders.size();
        else if ("Trainers".equals(cat)) total = trainers.size();
        else if ("Other Staff".equals(cat)) total = otherStaff.size();
        else total = filtered;

        statusLabel.setText("Players: " + players.size() + "  |  Coaches: " + coaches.size()
            + "  |  Cheerleaders: " + cheerleaders.size() + "  |  Staff: " + (trainers.size() + otherStaff.size()));
        countLabel.setText("Showing " + filtered + " / " + total + "  ");
        dirtyLabel.setText(isDirty ? "● Unsaved changes" : " ");
    }

    // =========================================================================
    // UNDO / REDO + AUTOSAVE
    // =========================================================================
    private void setupKeyBindings() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "redo");

        am.put("undo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { doUndo(); }
        });
        am.put("redo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { doRedo(); }
        });
    }

    private void setupAutosave() {
        autosaveTimer = new javax.swing.Timer(90_000, e -> {
            if (isDirty) {
                saveAllData(false);
                statusLabel.setText("💾 Autosaved");
            }
        });
        autosaveTimer.setRepeats(true);
        autosaveTimer.start();
    }

    private void updateUndoMenuState() {
        if (undoMenuItem == null || redoMenuItem == null) return;
        if (undoManager.canUndo()) {
            undoMenuItem.setEnabled(true);
            Command c = undoManager.peekUndo();
            undoMenuItem.setText("Undo " + (c != null ? c.label() : ""));
        } else {
            undoMenuItem.setEnabled(false);
            undoMenuItem.setText("Undo");
        }

        if (undoManager.canRedo()) {
            redoMenuItem.setEnabled(true);
            Command c = undoManager.peekRedo();
            redoMenuItem.setText("Redo " + (c != null ? c.label() : ""));
        } else {
            redoMenuItem.setEnabled(false);
            redoMenuItem.setText("Redo");
        }
    }

    private void doUndo() {
        if (!undoManager.canUndo()) return;
        undoManager.undo();
        applyFilters();
        showInfo();
        updateUndoMenuState();
    }

    private void doRedo() {
        if (!undoManager.canRedo()) return;
        undoManager.redo();
        applyFilters();
        showInfo();
        updateUndoMenuState();
    }

    private void markDirty() {
        isDirty = true;
        updateStatusLabel();
    }

    private void runCommand(Command c) {
        undoManager.doCommand(c);
        updateUndoMenuState();
    }

    // =========================================================================
    // FILTER LOGIC  (central, authoritative)
    // =========================================================================
    private void resetFilters() {
        activeOnlyCheckbox.setSelected(true);
        positionFilter.setSelectedIndex(0);
        yearFilter.setSelectedIndex(0);
        searchField.setText("");
    }

    /** Applies all active filters and repopulates nameBox. */
    private void applyFilters() {
        // Disconnect listener while rebuilding to avoid spurious showInfo calls
        nameBox.removeAllItems();

        String  search    = searchField.getText().trim();
        boolean activeOnly= activeOnlyCheckbox.isSelected();
        String  year      = (String) yearFilter.getSelectedItem();
        String  positionSel  = (String) positionFilter.getSelectedItem();
        String  position  = extractPositionCode(positionSel);

        String cat = (String) categoryBox.getSelectedItem();
        if ("Players".equals(cat)) {
            // clear non-player filtered lists to avoid accidental stale use
            filteredCheerleaders = new ArrayList<>();
            filteredTrainers     = new ArrayList<>();
            filteredOtherStaff   = new ArrayList<>();
            filteredPlayers = new ArrayList<>(players);
            if (activeOnly) filteredPlayers = CowboysDirectory.filterPlayersByStatus(filteredPlayers, true);
            if (position != null && !position.equals("All Positions"))
                filteredPlayers = CowboysDirectory.filterPlayersByPosition(filteredPlayers, position);
            if (year != null && !year.equals("All Years")) {
                try { filteredPlayers = CowboysDirectory.filterPlayersByYear(filteredPlayers, Integer.parseInt(year)); }
                catch (NumberFormatException ignored) {}
            }
            if (!search.isEmpty()) filteredPlayers = CowboysDirectory.searchPlayers(filteredPlayers, search);
            for (Player p : filteredPlayers) nameBox.addItem(p.getName());
        } else if ("Coaches".equals(cat)) {
            filteredCheerleaders = new ArrayList<>();
            filteredTrainers     = new ArrayList<>();
            filteredOtherStaff   = new ArrayList<>();
            filteredCoaches = new ArrayList<>(coaches);
            if (activeOnly) filteredCoaches = CowboysDirectory.filterCoachesByStatus(filteredCoaches, true);
            if (year != null && !year.equals("All Years")) {
                try { filteredCoaches = CowboysDirectory.filterCoachesByYear(filteredCoaches, Integer.parseInt(year)); }
                catch (NumberFormatException ignored) {}
            }
            if (!search.isEmpty()) filteredCoaches = CowboysDirectory.searchCoaches(filteredCoaches, search);
            for (Coach c : filteredCoaches) nameBox.addItem(c.getName());
        } else if ("Cheerleaders".equals(cat)) {
            filteredCheerleaders = cheerleaders.stream()
                .filter(c -> !activeOnly || c.isActive())
                .filter(c -> search.isEmpty() || c.getName().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
            filteredCheerleaders.forEach(c -> nameBox.addItem(c.getName()));
        } else if ("Trainers".equals(cat)) {
            filteredTrainers = trainers.stream()
                .filter(s -> !activeOnly || s.isActive())
                .filter(s -> search.isEmpty() || s.getName().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
            filteredTrainers.forEach(s -> nameBox.addItem(s.getName()));
        } else if ("Other Staff".equals(cat)) {
            filteredOtherStaff = otherStaff.stream()
                .filter(s -> !activeOnly || s.isActive())
                .filter(s -> search.isEmpty() || s.getName().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
            filteredOtherStaff.forEach(s -> nameBox.addItem(s.getName()));
        }

        updateStatusLabel();
        if (tableViewActive) refreshTableData();
    }

    private void selectByName(String name) {
        for (int i = 0; i < nameBox.getItemCount(); i++) {
            if (nameBox.getItemAt(i).equals(name)) {
                nameBox.setSelectedIndex(i);
                return;
            }
        }
    }

    // =========================================================================
    // SHOW INFO (card view)
    // =========================================================================
    private void showInfo() {
        String name = (String) nameBox.getSelectedItem();
        if (name == null) return;
        infoDisplayPanel.removeAll();
        infoDisplayPanel.setLayout(new BorderLayout(15, 15));
        String showCat = (String) categoryBox.getSelectedItem();
        switch (showCat) {
            case "Players":
                players.stream().filter(p -> p.getName().equals(name)).findFirst().ifPresent(p -> {
                    currentSelection = p; infoDisplayPanel.add(buildPlayerCard(p), BorderLayout.CENTER); });
                break;
            case "Coaches":
                coaches.stream().filter(c -> c.getName().equals(name)).findFirst().ifPresent(c -> {
                    currentSelection = c; infoDisplayPanel.add(buildCoachCard(c), BorderLayout.CENTER); });
                break;
            case "Cheerleaders":
                cheerleaders.stream().filter(c -> c.getName().equals(name)).findFirst().ifPresent(c -> {
                    currentSelection = c; infoDisplayPanel.add(buildCheerleaderCard(c), BorderLayout.CENTER); });
                break;
            case "Trainers":
                trainers.stream().filter(s -> s.getName().equals(name)).findFirst().ifPresent(s -> {
                    currentSelection = s; infoDisplayPanel.add(buildStaffCard(s), BorderLayout.CENTER); });
                break;
            case "Other Staff":
                otherStaff.stream().filter(s -> s.getName().equals(name)).findFirst().ifPresent(s -> {
                    currentSelection = s; infoDisplayPanel.add(buildStaffCard(s), BorderLayout.CENTER); });
                break;
        }
        infoDisplayPanel.revalidate();
        infoDisplayPanel.repaint();
    }

    // =========================================================================
    // PLAYER CARD
    // =========================================================================
    private JPanel buildPlayerCard(Player p) {
        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(WHITE);

        // ── Photo placeholder ──
        JPanel photo = buildPhotoPanel(p.getImageURL(), "📷");
        card.add(photo, BorderLayout.WEST);

        // ── Info panel ──
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(WHITE);

        // Name + status badge
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setBackground(WHITE);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel nameLbl = new JLabel(p.getName());
        nameLbl.setFont(new Font("Arial Black", Font.BOLD, 26));
        nameLbl.setForeground(NAVY);
        header.add(nameLbl);
        header.add(statusBadge(p.getStatus(), p.isActive()));
        if (p.isFavorite()) header.add(favBadge());

        JButton editStatsBtn = new JButton("📊 Edit Stats");
        styleSmallActionButton(editStatsBtn);
        editStatsBtn.addActionListener(e -> {
            currentSelection = p;
            showEditPlayerStatsDialog();
        });
        header.add(editStatsBtn);

        // Position badge (colour-coded)
        JLabel posBadge = buildPositionBadge(p.getPosition());
        String posFull = positionFullName(p.getPosition());
        String posDesc = positionDescriptionShort(p.getPosition());
        JLabel posHelp = new JLabel(posFull.isEmpty() ? "" : (" " + posFull + "  •  " + posDesc));
        posHelp.setFont(new Font("Arial", Font.PLAIN, 12));
        posHelp.setForeground(new Color(90, 98, 105));
        posHelp.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (!posFull.isEmpty()) {
            posHelp.setToolTipText(posBadge.getToolTipText());
        }

        // Stats grid
        JPanel statsGrid = new JPanel(new GridLayout(4, 2, 8, 6));
        statsGrid.setBackground(new Color(248,250,253));
        statsGrid.setBorder(new CompoundBorder(
            new LineBorder(SILVER_LITE, 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        statsGrid.add(kv("Height",    p.getHeight()));
        statsGrid.add(kv("Weight",    p.getWeight() + " lbs"));
        statsGrid.add(kv("College",   p.getCollege()));
        statsGrid.add(kv("Joined",    String.valueOf(p.getJoined())));
        statsGrid.add(kv("Draft",     p.getDraftDisplay()));
        statsGrid.add(kv("Years w/ Dallas", String.valueOf(p.yearsWithDallas(CURRENT_YEAR))));
        statsGrid.add(kv("Pro Bowls", String.valueOf(p.getProBowls())));
        statsGrid.add(kv("Jersey #",  "#" + p.getNumber()));

        info.add(header);
        info.add(Box.createVerticalStrut(4));
        info.add(posBadge);
        if (!posHelp.getText().trim().isEmpty()) info.add(posHelp);
        info.add(Box.createVerticalStrut(12));
        info.add(statsGrid);
        info.add(Box.createVerticalStrut(12));

        // Career stats (position-specific)
        PlayerStats ps = getPlayerStats(p);
        info.add(sectionHeader("📊 Career Stats"));
        info.add(Box.createVerticalStrut(4));
        info.add(buildPlayerStatsCard(p, ps));
        info.add(Box.createVerticalStrut(12));

        if (!p.getAchievements().isEmpty()) {
            info.add(sectionHeader("🏆 Career Achievements"));
            info.add(Box.createVerticalStrut(4));
            info.add(achievementBox(p.getAchievements()));
            info.add(Box.createVerticalStrut(12));
        }
        info.add(sectionHeader("Biography"));
        info.add(Box.createVerticalStrut(4));
        info.add(bioArea(p.getInfo()));

        JScrollPane infoScroll = new JScrollPane(info);
        infoScroll.setBorder(null);
        infoScroll.getVerticalScrollBar().setUnitIncrement(12);
        card.add(infoScroll, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // PLAYER STATS (per-player career totals)
    // =========================================================================

    private PlayerStats getPlayerStats(Player p) {
        if (p == null) return new PlayerStats("");
        String key = p.getName() == null ? "" : p.getName().trim();
        if (key.isEmpty()) return new PlayerStats("");
        PlayerStats s = playerStats.get(key);
        if (s == null) {
            s = new PlayerStats(key);
            playerStats.put(key, s);
            CowboysDirectory.savePlayerStats(playerStats, DATA_DIR + "player_stats.csv");
        }
        return s;
    }

    /**
     * Renders a position-specific stats grid. Under the hood we store a superset.
     */
    private JPanel buildPlayerStatsCard(Player p, PlayerStats s) {
        List<AbstractMap.SimpleEntry<String,String>> rows = new ArrayList<>();
        String pos = p.getPosition() == null ? "" : p.getPosition().trim().toUpperCase();
        String group = positionGroup(pos);

        // Always show games.
        rows.add(new AbstractMap.SimpleEntry<>("Games", String.valueOf(s.games)));
        rows.add(new AbstractMap.SimpleEntry<>("Starts", String.valueOf(s.gamesStarted)));

        if ("QB".equals(group)) {
            rows.add(new AbstractMap.SimpleEntry<>("Comp/Att", s.passCompletions + "/" + s.passAttempts));
            rows.add(new AbstractMap.SimpleEntry<>("Comp %", String.format("%.1f%%", s.completionPct())));
            rows.add(new AbstractMap.SimpleEntry<>("Pass Yds", String.valueOf(s.passYards)));
            rows.add(new AbstractMap.SimpleEntry<>("Pass TD", String.valueOf(s.passTD)));
            rows.add(new AbstractMap.SimpleEntry<>("INT", String.valueOf(s.passINT)));
            rows.add(new AbstractMap.SimpleEntry<>("Sacks", String.valueOf(s.sacksTaken)));
            rows.add(new AbstractMap.SimpleEntry<>("Rush Yds", String.valueOf(s.rushYards)));
            rows.add(new AbstractMap.SimpleEntry<>("Rush TD", String.valueOf(s.rushTD)));
            rows.add(new AbstractMap.SimpleEntry<>("Fumbles", String.valueOf(s.fumbles)));
            rows.add(new AbstractMap.SimpleEntry<>("Fum Lost", String.valueOf(s.fumblesLost)));
        } else if ("RB".equals(group)) {
            rows.add(new AbstractMap.SimpleEntry<>("Rush Att", String.valueOf(s.rushAttempts)));
            rows.add(new AbstractMap.SimpleEntry<>("Rush Yds", String.valueOf(s.rushYards)));
            rows.add(new AbstractMap.SimpleEntry<>("Yds/Carry", String.format("%.1f", s.rushYardsPerCarry())));
            rows.add(new AbstractMap.SimpleEntry<>("Rush TD", String.valueOf(s.rushTD)));
            rows.add(new AbstractMap.SimpleEntry<>("Rec", String.valueOf(s.receptions)));
            rows.add(new AbstractMap.SimpleEntry<>("Rec Yds", String.valueOf(s.recYards)));
            rows.add(new AbstractMap.SimpleEntry<>("Rec TD", String.valueOf(s.recTD)));
            rows.add(new AbstractMap.SimpleEntry<>("Fumbles", String.valueOf(s.fumbles)));
            rows.add(new AbstractMap.SimpleEntry<>("Fum Lost", String.valueOf(s.fumblesLost)));
        } else if ("WRTE".equals(group)) {
            rows.add(new AbstractMap.SimpleEntry<>("Targets", String.valueOf(s.targets)));
            rows.add(new AbstractMap.SimpleEntry<>("Rec", String.valueOf(s.receptions)));
            rows.add(new AbstractMap.SimpleEntry<>("Rec Yds", String.valueOf(s.recYards)));
            rows.add(new AbstractMap.SimpleEntry<>("Yds/Catch", String.format("%.1f", s.recYardsPerCatch())));
            rows.add(new AbstractMap.SimpleEntry<>("Rec TD", String.valueOf(s.recTD)));
            rows.add(new AbstractMap.SimpleEntry<>("Rush Yds", String.valueOf(s.rushYards)));
            rows.add(new AbstractMap.SimpleEntry<>("Fumbles", String.valueOf(s.fumbles)));
            rows.add(new AbstractMap.SimpleEntry<>("Fum Lost", String.valueOf(s.fumblesLost)));
        } else if ("OL".equals(group)) {
            rows.add(new AbstractMap.SimpleEntry<>("Penalties", String.valueOf(s.penalties)));
            rows.add(new AbstractMap.SimpleEntry<>("Sacks Allowed", String.valueOf(s.sacksAllowed)));
        } else if ("DEF".equals(group)) {
            rows.add(new AbstractMap.SimpleEntry<>("Tackles", String.valueOf(s.tackles)));
            rows.add(new AbstractMap.SimpleEntry<>("TFL", String.valueOf(s.tacklesForLoss)));
            rows.add(new AbstractMap.SimpleEntry<>("Sacks", String.valueOf(s.sacks)));
            rows.add(new AbstractMap.SimpleEntry<>("QB Hits", String.valueOf(s.qbHits)));
            rows.add(new AbstractMap.SimpleEntry<>("INT", String.valueOf(s.interceptions)));
            rows.add(new AbstractMap.SimpleEntry<>("Passes Def", String.valueOf(s.passesDefended)));
            rows.add(new AbstractMap.SimpleEntry<>("FF", String.valueOf(s.forcedFumbles)));
            rows.add(new AbstractMap.SimpleEntry<>("FR", String.valueOf(s.fumbleRecoveries)));
            rows.add(new AbstractMap.SimpleEntry<>("Def TD", String.valueOf(s.defensiveTD)));
        } else if ("K".equals(group)) {
            rows.add(new AbstractMap.SimpleEntry<>("FG", s.fieldGoalsMade + "/" + s.fieldGoalsAttempted));
            rows.add(new AbstractMap.SimpleEntry<>("XP", s.extraPointsMade + "/" + s.extraPointsAttempted));
            rows.add(new AbstractMap.SimpleEntry<>("Points", String.valueOf(s.fieldGoalsMade*3 + s.extraPointsMade)));
        } else if ("P".equals(group)) {
            rows.add(new AbstractMap.SimpleEntry<>("Punts", String.valueOf(s.punts)));
            rows.add(new AbstractMap.SimpleEntry<>("Punt Yds", String.valueOf(s.puntYards)));
            rows.add(new AbstractMap.SimpleEntry<>("Inside 20", String.valueOf(s.puntsInside20)));
        } else {
            // fallback: show a few common fields
            rows.add(new AbstractMap.SimpleEntry<>("Fumbles", String.valueOf(s.fumbles)));
            rows.add(new AbstractMap.SimpleEntry<>("Fum Lost", String.valueOf(s.fumblesLost)));
        }

        int cols = 2;
        int n = rows.size();
        int rCount = (int) Math.ceil(n / (double) cols);
        JPanel grid = new JPanel(new GridLayout(Math.max(1, rCount), cols, 8, 6));
        grid.setBackground(new Color(248,250,253));
        grid.setBorder(new CompoundBorder(new LineBorder(SILVER_LITE, 1), new EmptyBorder(10, 12, 10, 12)));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (AbstractMap.SimpleEntry<String,String> e : rows) {
            grid.add(kv(e.getKey(), e.getValue()));
        }
        // If odd number of entries, pad last cell so GridLayout doesn't stretch weirdly.
        if (n % cols != 0) grid.add(new JLabel(""));
        return grid;
    }

    /** Coarse grouping so we can choose stat templates. */
    private String positionGroup(String pos) {
        if (pos == null) return "";
        pos = pos.toUpperCase();
        if (pos.startsWith("QB")) return "QB";
        if (pos.startsWith("RB") || pos.startsWith("FB")) return "RB";
        if (pos.startsWith("WR") || pos.startsWith("TE")) return "WRTE";
        if (pos.equals("C") || pos.equals("G") || pos.equals("T") || pos.contains("OL")) return "OL";
        if (pos.startsWith("DT") || pos.startsWith("DE") || pos.startsWith("NT") || pos.startsWith("EDGE")
                || pos.startsWith("LB") || pos.startsWith("ILB") || pos.startsWith("OLB")
                || pos.startsWith("CB") || pos.startsWith("S") || pos.startsWith("FS") || pos.startsWith("SS")) {
            return "DEF";
        }
        if (pos.equals("K")) return "K";
        if (pos.equals("P")) return "P";
        return "";
    }

    // =========================================================================
    // COACH CARD
    // =========================================================================
    private JPanel buildCoachCard(Coach c) {
        JPanel card = new JPanel(new BorderLayout(16, 0));
        card.setBackground(WHITE);

        JPanel photo = buildPhotoPanel(c.getImageURL(), "👔");
        card.add(photo, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBackground(WHITE);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setBackground(WHITE);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel nameLbl = new JLabel(c.getName());
        nameLbl.setFont(new Font("Arial Black", Font.BOLD, 26));
        nameLbl.setForeground(NAVY);
        header.add(nameLbl);
        header.add(statusBadge(c.getStatus(), c.isActive()));
        if (c.isFavorite()) header.add(favBadge());

        JLabel roleLbl = new JLabel(c.getRole());
        roleLbl.setFont(new Font("Arial", Font.BOLD, 16));
        roleLbl.setForeground(BLUE);
        roleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel statsGrid = new JPanel(new GridLayout(3, 2, 8, 6));
        statsGrid.setBackground(new Color(248,250,253));
        statsGrid.setBorder(new CompoundBorder(
            new LineBorder(SILVER_LITE, 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        statsGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        statsGrid.add(kv("Joined Cowboys",   String.valueOf(c.getJoined())));
        statsGrid.add(kv("Years with Dallas",String.valueOf(c.yearsWithDallas(CURRENT_YEAR))));
        statsGrid.add(kv("Experience",        c.getExperience()));
        statsGrid.add(kv("Previous Team",     c.getPreviousTeam()));
        statsGrid.add(kv("Championships",     String.valueOf(c.getChampionships())));

        info.add(header);
        info.add(Box.createVerticalStrut(4));
        info.add(roleLbl);
        info.add(Box.createVerticalStrut(12));
        info.add(statsGrid);
        info.add(Box.createVerticalStrut(12));

        if (!c.getAchievements().isEmpty()) {
            info.add(sectionHeader("🏆 Career Achievements"));
            info.add(Box.createVerticalStrut(4));
            info.add(achievementBox(c.getAchievements()));
            info.add(Box.createVerticalStrut(12));
        }
        info.add(sectionHeader("Background"));
        info.add(Box.createVerticalStrut(4));
        info.add(bioArea(c.getInfo()));

        JScrollPane infoScroll = new JScrollPane(info);
        infoScroll.setBorder(null);
        infoScroll.getVerticalScrollBar().setUnitIncrement(12);
        card.add(infoScroll, BorderLayout.CENTER);

        return card;
    }

    // =========================================================================
    // CARD HELPERS
    // =========================================================================
    private JPanel buildPhotoPanel(String url, String fallbackEmoji) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setPreferredSize(new Dimension(190, 240));

        JLabel img = new JLabel();
        img.setHorizontalAlignment(SwingConstants.CENTER);
        img.setVerticalAlignment(SwingConstants.CENTER);
        img.setBorder(new CompoundBorder(
            new LineBorder(SILVER_LITE, 2),
            new EmptyBorder(4,4,4,4)
        ));

        // Always start with a fallback to avoid UI hitching on network loads.
        setFallback(img, fallbackEmoji);

        if (url != null && !url.isEmpty()) {
            img.putClientProperty("photoUrl", url);

            ImageIcon cached = imageCache.get(url);
            if (cached != null) {
                img.setText(null);
                img.setOpaque(false);
                img.setIcon(cached);
            } else {
                // Load off the EDT
                new SwingWorker<ImageIcon, Void>() {
                    @Override
                    protected ImageIcon doInBackground() throws Exception {
                        return imageCache.loadAndScale(url, 176, 228);
                    }

                    @Override
                    protected void done() {
                        try {
                            ImageIcon icon = get();
                            imageCache.put(url, icon);
                            Object current = img.getClientProperty("photoUrl");
                            if (url.equals(current)) {
                                img.setText(null);
                                img.setOpaque(false);
                                img.setIcon(icon);
                            }
                        } catch (Exception ignored) {
                            // keep fallback
                        }
                    }
                }.execute();
            }
        }
        panel.add(img, BorderLayout.CENTER);
        return panel;
    }

    private void setFallback(JLabel lbl, String emoji) {
        lbl.setText(emoji);
        lbl.setFont(new Font("Dialog", Font.PLAIN, 72));
        lbl.setForeground(SILVER_LITE);
        lbl.setBackground(new Color(248,250,253));
        lbl.setOpaque(true);
    }

    private JLabel statusBadge(String text, boolean active) {
        JLabel badge = new JLabel(text);
        badge.setFont(new Font("Arial", Font.BOLD, 11));
        badge.setForeground(WHITE);
        badge.setOpaque(true);
        badge.setBackground(active ? GREEN : SILVER);
        badge.setBorder(new EmptyBorder(3, 9, 3, 9));
        return badge;
    }

    private JLabel favBadge() {
        JLabel badge = new JLabel("★ Favourite");
        badge.setFont(new Font("Arial", Font.BOLD, 11));
        badge.setForeground(NAVY_DARK);
        badge.setOpaque(true);
        badge.setBackground(GOLD);
        badge.setBorder(new EmptyBorder(3, 9, 3, 9));
        return badge;
    }


    private String positionFullName(String code) {
        if (code == null) return "";
        String k = code.trim().toUpperCase();
        return POS_FULL.getOrDefault(k, k);
    }

    private String positionDescription(String code) {
        if (code == null) return "";
        String k = code.trim().toUpperCase();
        return POS_DESC.getOrDefault(k, "");
    }

private String positionCell(String code) {
        if (code == null) return "";
        String c = code.trim().toUpperCase();
        String full = positionFullName(c);
        if (full.isEmpty() || full.equals(c)) return c;
        return c + " (" + full + ")";
    }

    private String positionDescriptionShort(String code) {
        String d = positionDescription(code);
        if (d == null) return "";
        // Keep short for inline labels
        return d.length() > 80 ? d.substring(0, 77) + "…" : d;
    }

    /** Builds a friendly dropdown label like "QB — Quarterback". */
    private String positionFilterLabel(String code) {
        String full = positionFullName(code);
        return code + " — " + full;
    }

    /** Extracts the short code from dropdown labels like "QB — Quarterback". */
    private String extractPositionCode(Object selected) {
        if (selected == null) return "";
        String s = String.valueOf(selected).trim();
        if (s.equals("All Positions")) return "All Positions";
        int dash = s.indexOf("—");
        if (dash > 0) return s.substring(0, dash).trim().toUpperCase();
        // fallback: first token
        int sp = s.indexOf(' ');
        return (sp > 0 ? s.substring(0, sp) : s).trim().toUpperCase();
    }

    /** Select a position in the dropdown by code even if items are "QB — Quarterback". */
    private void selectPositionByCode(String code) {
        if (code == null || positionFilter == null) return;
        String target = code.trim().toUpperCase();
        for (int i = 0; i < positionFilter.getItemCount(); i++) {
            String item = positionFilter.getItemAt(i);
            if (target.equals(extractPositionCode(item))) {
                positionFilter.setSelectedIndex(i);
                return;
            }
        }
    }

    private JLabel buildPositionBadge(String position) {
        Color bg = positionColor(position);
        String code = position == null ? "" : position.trim().toUpperCase();
        JLabel lbl = new JLabel("  " + code + "  ");
        lbl.setFont(new Font("Arial", Font.BOLD, 14));
        lbl.setForeground(WHITE);
        lbl.setOpaque(true);
        lbl.setBackground(bg);
        lbl.setBorder(new EmptyBorder(4, 10, 4, 10));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        String full = positionFullName(code);
        String desc = positionDescription(code);
        if (!full.isEmpty() && !full.equals(code)) {
            lbl.setToolTipText("<html><b>" + code + " — " + full + "</b><br/>" + desc + "</html>");
        } else if (!desc.isEmpty()) {
            lbl.setToolTipText("<html><b>" + code + "</b><br/>" + desc + "</html>");
        }
        return lbl;
    }

    private Color positionColor(String pos) {
        if (pos == null) return SILVER;
        switch (pos.toUpperCase()) {
            case "QB": case "RB": case "FB": case "WR": case "TE": return OFF_COLOR;
            case "OL": case "OT": case "OG": case "C":             return OL_COLOR;
            case "DE": case "DT": case "NT": case "LB":
            case "MLB": case "OLB": case "ILB":                    return DEF_COLOR;
            case "CB": case "S": case "DB": case "FS": case "SS":  return new Color(140,40,140);
            case "K": case "P": case "LS":                         return ST_COLOR;
            default: return SILVER;
        }
    }

    private JPanel kv(String key, String value) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        p.setBackground(new Color(248,250,253));
        JLabel k = new JLabel(key + ":");
        k.setFont(new Font("Arial", Font.BOLD, 12));
        k.setForeground(NAVY);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Arial", Font.PLAIN, 12));
        v.setForeground(Color.DARK_GRAY);
        p.add(k); p.add(v);
        return p;
    }

    private JLabel sectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.BOLD, 15));
        lbl.setForeground(NAVY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextArea achievementBox(String text) {
        JTextArea ta = new JTextArea(text.replace(";", "\n• ").trim());
        if (!ta.getText().startsWith("•")) ta.setText("• " + ta.getText());
        ta.setFont(new Font("Arial", Font.ITALIC, 13));
        ta.setLineWrap(true); ta.setWrapStyleWord(true); ta.setEditable(false);
        ta.setBackground(GOLD_LITE); ta.setForeground(NAVY_DARK);
        ta.setBorder(new CompoundBorder(new LineBorder(GOLD,1), new EmptyBorder(8,10,8,10)));
        ta.setAlignmentX(Component.LEFT_ALIGNMENT);
        ta.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return ta;
    }

    private JTextArea bioArea(String text) {
        JTextArea ta = new JTextArea(text);
        ta.setFont(new Font("Arial", Font.PLAIN, 13));
        ta.setLineWrap(true); ta.setWrapStyleWord(true); ta.setEditable(false);
        ta.setBackground(WHITE); ta.setForeground(Color.DARK_GRAY);
        ta.setAlignmentX(Component.LEFT_ALIGNMENT);
        ta.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return ta;
    }

    // =========================================================================
    // FAVOURITES
    // =========================================================================
    private void toggleFavourite() {
        if (currentSelection instanceof Player) {
            Player p = (Player) currentSelection;
            final Player target = p;
            runCommand(new Command() {
                @Override public void execute() {
                    target.toggleFavorite();
                    CowboysDirectory.savePlayerFavorites(players);
                    showInfo();
                    statusLabel.setText("★ " + target.getName() + (target.isFavorite() ? " added to" : " removed from") + " favourites");
                }
                @Override public void undo() {
                    target.toggleFavorite();
                    CowboysDirectory.savePlayerFavorites(players);
                    showInfo();
                    statusLabel.setText("★ " + target.getName() + (target.isFavorite() ? " added to" : " removed from") + " favourites");
                }
                @Override public String label() { return "Toggle Favourite"; }
            });
        } else if (currentSelection instanceof Coach) {
            Coach c = (Coach) currentSelection;
            final Coach target = c;
            runCommand(new Command() {
                @Override public void execute() {
                    target.toggleFavorite();
                    CowboysDirectory.saveCoachFavorites(coaches);
                    showInfo();
                    statusLabel.setText("★ " + target.getName() + (target.isFavorite() ? " added to" : " removed from") + " favourites");
                }
                @Override public void undo() {
                    target.toggleFavorite();
                    CowboysDirectory.saveCoachFavorites(coaches);
                    showInfo();
                    statusLabel.setText("★ " + target.getName() + (target.isFavorite() ? " added to" : " removed from") + " favourites");
                }
                @Override public String label() { return "Toggle Favourite"; }
            });
        } else {
            JOptionPane.showMessageDialog(this, "Please select a player or coach first.");
        }
    }

    // =========================================================================
    // SAVE / BACKUP / RESTORE
    // =========================================================================
    private void saveAllData() {
        saveAllData(true);
    }

    private boolean saveAllData(boolean showDialogs) {
        boolean ok = CowboysDirectory.savePlayers(players, DATA_DIR + "players.csv")
                  && CowboysDirectory.saveCoaches(coaches, DATA_DIR + "coaches.csv")
                  && CowboysDirectory.savePlayerStats(playerStats, DATA_DIR + "player_stats.csv");
        CowboysDirectory.savePlayerFavorites(players);
        CowboysDirectory.saveCoachFavorites(coaches);
        if (ok) {
            isDirty = false;
            updateStatusLabel();
            statusLabel.setText("✅ All data saved.");
            if (showDialogs) {
                JOptionPane.showMessageDialog(this, "✅ All data saved!", "Saved", JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            if (showDialogs) {
                JOptionPane.showMessageDialog(this, "⚠️ Error saving – check console.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return ok;
    }

    private void backupData() {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        boolean ok = CowboysDirectory.backupData(DATA_DIR + "players.csv", DATA_DIR + "players_backup_"+ts+".csv")
                  && CowboysDirectory.backupData(DATA_DIR + "coaches.csv", DATA_DIR + "coaches_backup_"+ts+".csv");
        JOptionPane.showMessageDialog(this, ok
            ? "✅ Backup created:\nplayers_backup_"+ts+".csv\ncoaches_backup_"+ts+".csv"
            : "⚠️ Backup failed!",
            ok ? "Backup Complete" : "Backup Error",
            ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
    }

    private void restoreData() {
        JFileChooser fc = new JFileChooser("data");
        fc.setDialogTitle("Select Backup File");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            String target = f.getName().contains("player") ? DATA_DIR + "players.csv" : DATA_DIR + "coaches.csv";
            if (JOptionPane.showConfirmDialog(this, "⚠️ This will overwrite current data. Continue?",
                    "Confirm Restore", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                if (CowboysDirectory.restoreData(f.getPath(), target)) {
                    JOptionPane.showMessageDialog(this, "✅ Restored successfully!");
                    refreshAllData();
                } else {
                    JOptionPane.showMessageDialog(this, "⚠️ Restore failed!");
                }
            }
        }
    }

    private void refreshAllData() {
        players = CowboysDirectory.loadPlayers(DATA_DIR + "players.csv");
        coaches = CowboysDirectory.loadCoaches(DATA_DIR + "coaches.csv");
        cheerleaders = CowboysDirectory.loadCheerleaders(DATA_DIR + "cheerleaders.csv");
        trainers     = CowboysDirectory.loadStaff(DATA_DIR + "trainers.csv");
        otherStaff   = CowboysDirectory.loadStaff(DATA_DIR + "other_staff.csv");

        playerStats = CowboysDirectory.loadPlayerStats(DATA_DIR + "player_stats.csv");
        if (ensureStatsForAllPlayers()) {
            CowboysDirectory.savePlayerStats(playerStats, DATA_DIR + "player_stats.csv");
        }
        isDirty = false;
        undoManager.clear();
        updateUndoMenuState();
        applyFilters();
        statusLabel.setText("✅ Data refreshed.");
    }

    private void exitApp() {
        if (isDirty) {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "You have unsaved changes. Save before exiting?",
                "Unsaved changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return;
            if (choice == JOptionPane.YES_OPTION) {
                if (!saveAllData(false)) {
                    JOptionPane.showMessageDialog(this, "⚠️ Save failed. Exit cancelled.");
                    return;
                }
            }
        }
        CowboysDirectory.savePlayerFavorites(players);
        CowboysDirectory.saveCoachFavorites(coaches);
        System.exit(0);
    }

    // =========================================================================
    // ADD / EDIT / DELETE – PLAYERS
    // =========================================================================
    private void showAddPlayerDialog() {
        JDialog d = playerDialog("Add New Player", null);
        d.setVisible(true);
    }

    private void showEditPlayerDialog() {
        if (!(currentSelection instanceof Player)) {
            JOptionPane.showMessageDialog(this, "Please select a player first."); return;
        }
        JDialog d = playerDialog("Edit Player", (Player) currentSelection);
        d.setVisible(true);
    }

    // =========================================================================
    // EDIT – PLAYER STATS
    // =========================================================================

    private static class StatBinding {
        final String label;
        final int max;
        final IntSupplier getter;
        final IntConsumer setter;
        StatBinding(String label, int max, IntSupplier getter, IntConsumer setter) {
            this.label = label; this.max = max; this.getter = getter; this.setter = setter;
        }
    }

    private void showEditPlayerStatsDialog() {
        if (!(currentSelection instanceof Player)) {
            JOptionPane.showMessageDialog(this, "Please select a player first.");
            return;
        }
        Player p = (Player) currentSelection;
        PlayerStats s = getPlayerStats(p);

        String pos = p.getPosition() == null ? "" : p.getPosition().trim().toUpperCase();
        String group = positionGroup(pos);

        List<StatBinding> fields = new ArrayList<>();
        // Always
        fields.add(new StatBinding("Games Played",  500, () -> s.games,        v -> s.games = v));
        fields.add(new StatBinding("Games Started", 500, () -> s.gamesStarted, v -> s.gamesStarted = v));

        if ("QB".equals(group)) {
            fields.add(new StatBinding("Pass Attempts",     50000, () -> s.passAttempts,    v -> s.passAttempts = v));
            fields.add(new StatBinding("Pass Completions",  50000, () -> s.passCompletions, v -> s.passCompletions = v));
            fields.add(new StatBinding("Passing Yards",     300000, () -> s.passYards,      v -> s.passYards = v));
            fields.add(new StatBinding("Passing TD",        2000, () -> s.passTD,          v -> s.passTD = v));
            fields.add(new StatBinding("Interceptions",     2000, () -> s.passINT,         v -> s.passINT = v));
            fields.add(new StatBinding("Sacks Taken",       2000, () -> s.sacksTaken,      v -> s.sacksTaken = v));
            fields.add(new StatBinding("Rush Attempts",     20000, () -> s.rushAttempts,   v -> s.rushAttempts = v));
            fields.add(new StatBinding("Rushing Yards",     50000, () -> s.rushYards,      v -> s.rushYards = v));
            fields.add(new StatBinding("Rushing TD",        1000, () -> s.rushTD,          v -> s.rushTD = v));
            fields.add(new StatBinding("Fumbles",           1000, () -> s.fumbles,         v -> s.fumbles = v));
            fields.add(new StatBinding("Fumbles Lost",      1000, () -> s.fumblesLost,     v -> s.fumblesLost = v));
        } else if ("RB".equals(group)) {
            fields.add(new StatBinding("Rush Attempts",     50000, () -> s.rushAttempts,   v -> s.rushAttempts = v));
            fields.add(new StatBinding("Rushing Yards",     200000, () -> s.rushYards,     v -> s.rushYards = v));
            fields.add(new StatBinding("Rushing TD",        2000, () -> s.rushTD,          v -> s.rushTD = v));
            fields.add(new StatBinding("Targets",           20000, () -> s.targets,        v -> s.targets = v));
            fields.add(new StatBinding("Receptions",        20000, () -> s.receptions,     v -> s.receptions = v));
            fields.add(new StatBinding("Receiving Yards",   200000, () -> s.recYards,      v -> s.recYards = v));
            fields.add(new StatBinding("Receiving TD",      2000, () -> s.recTD,          v -> s.recTD = v));
            fields.add(new StatBinding("Fumbles",           1000, () -> s.fumbles,         v -> s.fumbles = v));
            fields.add(new StatBinding("Fumbles Lost",      1000, () -> s.fumblesLost,     v -> s.fumblesLost = v));
        } else if ("WRTE".equals(group)) {
            fields.add(new StatBinding("Targets",           20000, () -> s.targets,        v -> s.targets = v));
            fields.add(new StatBinding("Receptions",        20000, () -> s.receptions,     v -> s.receptions = v));
            fields.add(new StatBinding("Receiving Yards",   250000, () -> s.recYards,      v -> s.recYards = v));
            fields.add(new StatBinding("Receiving TD",      2000, () -> s.recTD,          v -> s.recTD = v));
            fields.add(new StatBinding("Rush Attempts",     5000, () -> s.rushAttempts,    v -> s.rushAttempts = v));
            fields.add(new StatBinding("Rushing Yards",     50000, () -> s.rushYards,      v -> s.rushYards = v));
            fields.add(new StatBinding("Rushing TD",        500, () -> s.rushTD,           v -> s.rushTD = v));
            fields.add(new StatBinding("Fumbles",           1000, () -> s.fumbles,         v -> s.fumbles = v));
            fields.add(new StatBinding("Fumbles Lost",      1000, () -> s.fumblesLost,     v -> s.fumblesLost = v));
        } else if ("OL".equals(group)) {
            fields.add(new StatBinding("Penalties",         2000, () -> s.penalties,       v -> s.penalties = v));
            fields.add(new StatBinding("Sacks Allowed",     2000, () -> s.sacksAllowed,    v -> s.sacksAllowed = v));
        } else if ("DEF".equals(group)) {
            fields.add(new StatBinding("Tackles",           20000, () -> s.tackles,        v -> s.tackles = v));
            fields.add(new StatBinding("Tackles for Loss",  5000, () -> s.tacklesForLoss,  v -> s.tacklesForLoss = v));
            fields.add(new StatBinding("Sacks",             3000, () -> s.sacks,          v -> s.sacks = v));
            fields.add(new StatBinding("QB Hits",           5000, () -> s.qbHits,         v -> s.qbHits = v));
            fields.add(new StatBinding("Interceptions",     2000, () -> s.interceptions,  v -> s.interceptions = v));
            fields.add(new StatBinding("Passes Defended",   5000, () -> s.passesDefended, v -> s.passesDefended = v));
            fields.add(new StatBinding("Forced Fumbles",    2000, () -> s.forcedFumbles,  v -> s.forcedFumbles = v));
            fields.add(new StatBinding("Fumble Recoveries", 2000, () -> s.fumbleRecoveries,v -> s.fumbleRecoveries = v));
            fields.add(new StatBinding("Defensive TD",      500, () -> s.defensiveTD,     v -> s.defensiveTD = v));
        } else if ("K".equals(group)) {
            fields.add(new StatBinding("FG Made",           1000, () -> s.fieldGoalsMade,       v -> s.fieldGoalsMade = v));
            fields.add(new StatBinding("FG Attempted",      1200, () -> s.fieldGoalsAttempted,  v -> s.fieldGoalsAttempted = v));
            fields.add(new StatBinding("XP Made",           2000, () -> s.extraPointsMade,      v -> s.extraPointsMade = v));
            fields.add(new StatBinding("XP Attempted",      2200, () -> s.extraPointsAttempted, v -> s.extraPointsAttempted = v));
        } else if ("P".equals(group)) {
            fields.add(new StatBinding("Punts",             8000, () -> s.punts,          v -> s.punts = v));
            fields.add(new StatBinding("Punt Yards",        400000, () -> s.puntYards,    v -> s.puntYards = v));
            fields.add(new StatBinding("Punts Inside 20",   8000, () -> s.puntsInside20, v -> s.puntsInside20 = v));
        } else {
            // generic
            fields.add(new StatBinding("Fumbles",           1000, () -> s.fumbles,     v -> s.fumbles = v));
            fields.add(new StatBinding("Fumbles Lost",      1000, () -> s.fumblesLost, v -> s.fumblesLost = v));
        }

        JDialog d = new JDialog(this, "Edit Player Stats – " + p.getName(), true);
        d.setSize(520, 680);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(10,10));

        JPanel form = new JPanel(new GridLayout(0, 2, 10, 10));
        form.setBorder(new EmptyBorder(16,16,10,16));

        Map<StatBinding, JSpinner> inputs = new LinkedHashMap<>();
        for (StatBinding b : fields) {
            form.add(new JLabel(b.label + ":"));
            JSpinner sp = new JSpinner(new SpinnerNumberModel(b.getter.getAsInt(), 0, b.max, 1));
            ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setColumns(10);
            form.add(sp);
            inputs.put(b, sp);
        }

        // Helpful derived display (QB)
        JPanel derived = new JPanel(new GridLayout(0, 1, 6, 6));
        derived.setBorder(new EmptyBorder(0,16,0,16));
        derived.setBackground(WHITE);
        JLabel hint = new JLabel("Derived stats update when you save.");
        hint.setFont(new Font("Arial", Font.ITALIC, 12));
        hint.setForeground(new Color(90, 98, 105));
        derived.add(hint);

        JScrollPane sc = new JScrollPane(form);
        sc.setBorder(null);
        sc.getVerticalScrollBar().setUnitIncrement(12);
        d.add(sc, BorderLayout.CENTER);
        d.add(derived, BorderLayout.NORTH);

        JButton save = styledBtn("💾 Save Stats");
        JButton cancel = styledBtn("Cancel");
        cancel.setBackground(new Color(120, 130, 140));
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btns.setBackground(BG_GRAY);
        btns.add(cancel);
        btns.add(save);
        d.add(btns, BorderLayout.SOUTH);

        cancel.addActionListener(e -> d.dispose());
        save.addActionListener(e -> {
            for (Map.Entry<StatBinding, JSpinner> entry : inputs.entrySet()) {
                int v = ((Number) entry.getValue().getValue()).intValue();
                entry.getKey().setter.accept(v);
            }
            playerStats.put(p.getName().trim(), s);
            CowboysDirectory.savePlayerStats(playerStats, DATA_DIR + "player_stats.csv");
            isDirty = true;
            updateStatusLabel();

            // Refresh card if visible
            if (currentSelection instanceof Player) {
                infoDisplayPanel.removeAll();
                infoDisplayPanel.add(buildPlayerCard((Player) currentSelection), BorderLayout.CENTER);
                infoDisplayPanel.revalidate();
                infoDisplayPanel.repaint();
            }
            d.dispose();
        });

        d.setVisible(true);
    }

    private JDialog playerDialog(String dialogTitle, Player existing) {
        JDialog d = new JDialog(this, dialogTitle, true);
        d.setSize(520, 720);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(10,10));

        JPanel form = new JPanel(new GridLayout(15, 2, 8, 8));
        form.setBorder(new EmptyBorder(20,20,10,20));

        JTextField[] fields = new JTextField[12];
        for (int i = 0; i < fields.length; i++) fields[i] = new JTextField();
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"Active","Former"});

        String[] labels = {"Name","Position","Joined Year","Height (e.g. 6-2)","Weight (lbs)",
                           "College","Jersey #","Draft Year","Draft Round","Draft Pick","Pro Bowls","Achievements"};
        for (int i = 0; i < labels.length; i++) {
            form.add(new JLabel(labels[i]+":")); form.add(fields[i]);
        }
        form.add(new JLabel("Status:")); form.add(statusBox);

        JLabel bioLbl = new JLabel("Biography:");
        JTextArea bio = new JTextArea(3, 20);
        bio.setLineWrap(true); bio.setWrapStyleWord(true);

        if (existing != null) {
            fields[0].setText(existing.getName());
            fields[1].setText(existing.getPosition());
            fields[2].setText(String.valueOf(existing.getJoined()));
            fields[3].setText(existing.getHeight());
            fields[4].setText(existing.getWeight());
            fields[5].setText(existing.getCollege());
            fields[6].setText(existing.getNumber());
            fields[7].setText(String.valueOf(existing.getDraftYear()));
            fields[8].setText(String.valueOf(existing.getDraftRound()));
            fields[9].setText(String.valueOf(existing.getDraftPick()));
            fields[10].setText(String.valueOf(existing.getProBowls()));
            fields[11].setText(existing.getAchievements());
            statusBox.setSelectedItem(existing.getStatus());
            bio.setText(existing.getInfo());
        }

        form.add(bioLbl); form.add(new JScrollPane(bio));

        JButton save   = styledBtn("💾 Save");
        JButton cancel = styledBtn("Cancel");
        cancel.setBackground(SILVER);

        save.addActionListener(e -> {
            // Validation
            String err;
            if ((err = Validators.requireNonBlank("Name", fields[0].getText())) != null
                    || (err = Validators.requireNonBlank("Position", fields[1].getText())) != null
                    || (err = Validators.requireIntRange("Joined Year", fields[2].getText(), 1960, CURRENT_YEAR)) != null
                    || (err = Validators.height(fields[3].getText())) != null
                    || (err = Validators.digits("Weight", fields[4].getText())) != null
                    || (err = Validators.digits("Jersey #", fields[6].getText())) != null
                    || (err = Validators.intRange("Draft Year", fields[7].getText(), 1936, CURRENT_YEAR)) != null
                    || (err = Validators.intRange("Draft Round", fields[8].getText(), 0, 10)) != null
                    || (err = Validators.intRange("Draft Pick", fields[9].getText(), 0, 400)) != null
                    || (err = Validators.intRange("Pro Bowls", fields[10].getText(), 0, 30)) != null) {
                JOptionPane.showMessageDialog(d, "⚠️ " + err, "Invalid input", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Cross-field draft rules
            int dYear = parseIntField(fields[7]);
            int dRound = parseIntField(fields[8]);
            int dPick = parseIntField(fields[9]);
            if (dRound > 0 && (dYear <= 0 || dPick <= 0)) {
                JOptionPane.showMessageDialog(d, "⚠️ If Draft Round is set, Draft Year and Draft Pick must also be set.",
                        "Invalid draft info", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Player after = new Player(
                fields[0].getText(), fields[1].getText(),
                Integer.parseInt(fields[2].getText().trim()),
                fields[3].getText(), fields[4].getText(), fields[5].getText(),
                fields[6].getText(), (String)statusBox.getSelectedItem(),
                dYear, dRound, dPick, parseIntField(fields[10]),
                fields[11].getText(), bio.getText(),
                existing != null ? existing.getImageURL() : ""
            );
            if (existing != null) after.setFavorite(existing.isFavorite());

            final boolean isEdit = existing != null;
            final int originalIndex = isEdit ? players.indexOf(existing) : -1;
            final Player beforeSnapshot = isEdit ? Player.copyOf(existing) : null;
            final Player[] ref = new Player[] { existing };

            runCommand(new Command() {
                @Override public void execute() {
                    if (isEdit) {
                        int idx = players.indexOf(ref[0]);
                        if (idx < 0) idx = originalIndex;
                        if (idx >= 0 && idx < players.size()) {
                            players.set(idx, after);
                        } else {
                            players.add(after);
                        }
                        ref[0] = after;
                    } else {
                        players.add(after);
                        ref[0] = after;
                    }
                    currentSelection = after;
                    markDirty();
                }

                @Override public void undo() {
                    if (isEdit) {
                        int idx = players.indexOf(ref[0]);
                        if (idx < 0) idx = originalIndex;
                        if (idx >= 0 && idx < players.size()) {
                            players.set(idx, beforeSnapshot);
                        } else {
                            players.add(beforeSnapshot);
                        }
                        ref[0] = beforeSnapshot;
                        currentSelection = beforeSnapshot;
                    } else {
                        players.remove(ref[0]);
                        currentSelection = null;
                    }
                    markDirty();
                }

                @Override public String label() {
                    return (isEdit ? "Edit Player" : "Add Player");
                }
            });

            applyFilters();
            selectByName(after.getName());
            d.dispose();
        });
        cancel.addActionListener(e -> d.dispose());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(save); btns.add(cancel);

        d.add(new JScrollPane(form), BorderLayout.CENTER);
        d.add(btns, BorderLayout.SOUTH);
        return d;
    }

    private void deleteCurrentPlayer() {
        if (!(currentSelection instanceof Player)) {
            JOptionPane.showMessageDialog(this, "Please select a player first."); return;
        }
        Player p = (Player) currentSelection;
        if (confirm("Delete " + p.getName() + "?")) {
            final Player snapshot = Player.copyOf(p);
            final int idx0 = players.indexOf(p);
            final Player[] ref = new Player[] { p };
            runCommand(new Command() {
                @Override public void execute() {
                    players.remove(ref[0]);
                    currentSelection = null;
                    markDirty();
                }
                @Override public void undo() {
                    int idx = idx0;
                    if (idx < 0 || idx > players.size()) idx = players.size();
                    players.add(idx, snapshot);
                    ref[0] = snapshot;
                    currentSelection = snapshot;
                    markDirty();
                }
                @Override public String label() { return "Delete Player"; }
            });
            applyFilters();
            clearInfoPanel("Player deleted.");
        }
    }

    // =========================================================================
    // ADD / EDIT / DELETE – COACHES
    // =========================================================================
    private void showAddCoachDialog() {
        JDialog d = coachDialog("Add New Coach", null); d.setVisible(true);
    }

    private void showEditCoachDialog() {
        if (!(currentSelection instanceof Coach)) {
            JOptionPane.showMessageDialog(this, "Please select a coach first."); return;
        }
        JDialog d = coachDialog("Edit Coach", (Coach) currentSelection); d.setVisible(true);
    }

    private JDialog coachDialog(String dialogTitle, Coach existing) {
        JDialog d = new JDialog(this, dialogTitle, true);
        d.setSize(520, 560);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(10,10));

        JPanel form = new JPanel(new GridLayout(10, 2, 8, 8));
        form.setBorder(new EmptyBorder(20,20,10,20));

        JTextField[] fields = new JTextField[7];
        for (int i = 0; i < fields.length; i++) fields[i] = new JTextField();
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"Active","Former"});

        String[] labels = {"Name","Role","Joined Year","Experience","Previous Team","Championships","Achievements"};
        for (int i = 0; i < labels.length; i++) {
            form.add(new JLabel(labels[i]+":")); form.add(fields[i]);
        }
        form.add(new JLabel("Status:")); form.add(statusBox);

        JTextArea bio = new JTextArea(3,20);
        bio.setLineWrap(true); bio.setWrapStyleWord(true);

        if (existing != null) {
            fields[0].setText(existing.getName());
            fields[1].setText(existing.getRole());
            fields[2].setText(String.valueOf(existing.getJoined()));
            fields[3].setText(existing.getExperience());
            fields[4].setText(existing.getPreviousTeam());
            fields[5].setText(String.valueOf(existing.getChampionships()));
            fields[6].setText(existing.getAchievements());
            statusBox.setSelectedItem(existing.getStatus());
            bio.setText(existing.getInfo());
        }
        form.add(new JLabel("Background:")); form.add(new JScrollPane(bio));

        JButton save   = styledBtn("💾 Save");
        JButton cancel = styledBtn("Cancel");
        cancel.setBackground(SILVER);

        save.addActionListener(e -> {
            String err;
            if ((err = Validators.requireNonBlank("Name", fields[0].getText())) != null
                    || (err = Validators.requireNonBlank("Role", fields[1].getText())) != null
                    || (err = Validators.requireIntRange("Joined Year", fields[2].getText(), 1960, CURRENT_YEAR)) != null
                    || (err = Validators.intRange("Championships", fields[5].getText(), 0, 30)) != null) {
                JOptionPane.showMessageDialog(d, "⚠️ " + err, "Invalid input", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Coach after = new Coach(
                fields[0].getText(), fields[1].getText(),
                Integer.parseInt(fields[2].getText().trim()),
                fields[3].getText(), fields[4].getText(),
                (String) statusBox.getSelectedItem(),
                parseIntField(fields[5]),
                fields[6].getText(), bio.getText(),
                existing != null ? existing.getImageURL() : ""
            );
            if (existing != null) after.setFavorite(existing.isFavorite());

            final boolean isEdit = existing != null;
            final int originalIndex = isEdit ? coaches.indexOf(existing) : -1;
            final Coach beforeSnapshot = isEdit ? Coach.copyOf(existing) : null;
            final Coach[] ref = new Coach[] { existing };

            runCommand(new Command() {
                @Override public void execute() {
                    if (isEdit) {
                        int idx = coaches.indexOf(ref[0]);
                        if (idx < 0) idx = originalIndex;
                        if (idx >= 0 && idx < coaches.size()) {
                            coaches.set(idx, after);
                        } else {
                            coaches.add(after);
                        }
                        ref[0] = after;
                    } else {
                        coaches.add(after);
                        ref[0] = after;
                    }
                    currentSelection = after;
                    markDirty();
                }

                @Override public void undo() {
                    if (isEdit) {
                        int idx = coaches.indexOf(ref[0]);
                        if (idx < 0) idx = originalIndex;
                        if (idx >= 0 && idx < coaches.size()) {
                            coaches.set(idx, beforeSnapshot);
                        } else {
                            coaches.add(beforeSnapshot);
                        }
                        ref[0] = beforeSnapshot;
                        currentSelection = beforeSnapshot;
                    } else {
                        coaches.remove(ref[0]);
                        currentSelection = null;
                    }
                    markDirty();
                }

                @Override public String label() {
                    return (isEdit ? "Edit Coach" : "Add Coach");
                }
            });

            applyFilters();
            selectByName(after.getName());
            d.dispose();
        });
        cancel.addActionListener(e -> d.dispose());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(save); btns.add(cancel);
        d.add(new JScrollPane(form), BorderLayout.CENTER);
        d.add(btns, BorderLayout.SOUTH);
        return d;
    }

    private void deleteCurrentCoach() {
        if (!(currentSelection instanceof Coach)) {
            JOptionPane.showMessageDialog(this, "Please select a coach first."); return;
        }
        Coach c = (Coach) currentSelection;
        if (confirm("Delete " + c.getName() + "?")) {
            final Coach snapshot = Coach.copyOf(c);
            final int idx0 = coaches.indexOf(c);
            final Coach[] ref = new Coach[] { c };
            runCommand(new Command() {
                @Override public void execute() {
                    coaches.remove(ref[0]);
                    currentSelection = null;
                    markDirty();
                }
                @Override public void undo() {
                    int idx = idx0;
                    if (idx < 0 || idx > coaches.size()) idx = coaches.size();
                    coaches.add(idx, snapshot);
                    ref[0] = snapshot;
                    currentSelection = snapshot;
                    markDirty();
                }
                @Override public String label() { return "Delete Coach"; }
            });
            applyFilters();
            clearInfoPanel("Coach deleted.");
        }
    }

    // =========================================================================
    // EXPORT / PRINT
    // =========================================================================
    private void exportCurrentProfile() {
        if (currentSelection == null) { JOptionPane.showMessageDialog(this,"Select a player or coach first."); return; }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("profile.txt"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter w = new PrintWriter(fc.getSelectedFile())) {
                w.println(buildProfileText());
                JOptionPane.showMessageDialog(this, "✅ Exported successfully!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "⚠️ Error: " + ex.getMessage());
            }
        }
    }

    private String buildProfileText() {
        StringBuilder sb = new StringBuilder();
        String line = "=".repeat(52);
        if (currentSelection instanceof Player) {
            Player p = (Player) currentSelection;
            PlayerStats s = getPlayerStats(p);
            String pos = p.getPosition() == null ? "" : p.getPosition().trim().toUpperCase();
            String group = positionGroup(pos);
            sb.append("DALLAS COWBOYS PLAYER PROFILE\n").append(line).append("\n");
            sb.append("Name:      ").append(p.getName()).append("\n");
            sb.append("Position:  ").append(p.getPosition()).append("  #").append(p.getNumber()).append("\n");
            sb.append("Status:    ").append(p.getStatus()).append("\n");
            sb.append("Height:    ").append(p.getHeight()).append("\n");
            sb.append("Weight:    ").append(p.getWeight()).append(" lbs\n");
            sb.append("College:   ").append(p.getCollege()).append("\n");
            sb.append("Joined:    ").append(p.getJoined()).append("\n");
            sb.append("Draft:     ").append(p.getDraftDisplay()).append("\n");
            sb.append("Pro Bowls: ").append(p.getProBowls()).append("\n\n");

            sb.append("Career Stats:\n");
            sb.append("Games: ").append(s.games).append("  Starts: ").append(s.gamesStarted).append("\n");
            if ("QB".equals(group)) {
                sb.append("Passing: ")
                  .append(s.passCompletions).append("/").append(s.passAttempts)
                  .append(" (").append(String.format("%.1f", s.completionPct())).append("%)")
                  .append(", ").append(s.passYards).append(" yds, ")
                  .append(s.passTD).append(" TD, ")
                  .append(s.passINT).append(" INT, ")
                  .append(s.sacksTaken).append(" sacks\n");
                sb.append("Rushing: ")
                  .append(s.rushAttempts).append(" att, ")
                  .append(s.rushYards).append(" yds, ")
                  .append(s.rushTD).append(" TD\n");
                sb.append("Fumbles: ").append(s.fumbles).append("  Lost: ").append(s.fumblesLost).append("\n\n");
            } else if ("RB".equals(group)) {
                sb.append("Rushing: ")
                  .append(s.rushAttempts).append(" att, ")
                  .append(s.rushYards).append(" yds, ")
                  .append(s.rushTD).append(" TD\n");
                sb.append("Receiving: ")
                  .append(s.receptions).append(" rec, ")
                  .append(s.recYards).append(" yds, ")
                  .append(s.recTD).append(" TD\n");
                sb.append("Fumbles: ").append(s.fumbles).append("  Lost: ").append(s.fumblesLost).append("\n\n");
            } else if ("WRTE".equals(group)) {
                sb.append("Receiving: ")
                  .append(s.receptions).append(" rec on ").append(s.targets).append(" targets, ")
                  .append(s.recYards).append(" yds, ")
                  .append(s.recTD).append(" TD\n");
                sb.append("Rushing: ")
                  .append(s.rushAttempts).append(" att, ")
                  .append(s.rushYards).append(" yds, ")
                  .append(s.rushTD).append(" TD\n");
                sb.append("Fumbles: ").append(s.fumbles).append("  Lost: ").append(s.fumblesLost).append("\n\n");
            } else if ("OL".equals(group)) {
                sb.append("Penalties: ").append(s.penalties)
                  .append("  Sacks Allowed: ").append(s.sacksAllowed).append("\n\n");
            } else if ("DEF".equals(group)) {
                sb.append("Defense: ")
                  .append(s.tackles).append(" tackles, ")
                  .append(s.tacklesForLoss).append(" TFL, ")
                  .append(s.sacks).append(" sacks, ")
                  .append(s.interceptions).append(" INT\n");
                sb.append("Pass Def: ").append(s.passesDefended)
                  .append("  FF: ").append(s.forcedFumbles)
                  .append("  FR: ").append(s.fumbleRecoveries)
                  .append("  Def TD: ").append(s.defensiveTD).append("\n\n");
            } else if ("K".equals(group)) {
                sb.append("Kicking: FG ").append(s.fieldGoalsMade).append("/").append(s.fieldGoalsAttempted)
                  .append("  XP ").append(s.extraPointsMade).append("/").append(s.extraPointsAttempted)
                  .append("\n\n");
            } else if ("P".equals(group)) {
                sb.append("Punting: ").append(s.punts).append(" punts, ")
                  .append(s.puntYards).append(" yds, ")
                  .append(s.puntsInside20).append(" inside 20\n\n");
            } else {
                sb.append("Fumbles: ").append(s.fumbles).append("  Lost: ").append(s.fumblesLost).append("\n\n");
            }

            sb.append("Achievements:\n").append(p.getAchievements()).append("\n\n");
            sb.append("Biography:\n").append(p.getInfo()).append("\n");
        } else if (currentSelection instanceof Coach) {
            Coach c = (Coach) currentSelection;
            sb.append("DALLAS COWBOYS COACH PROFILE\n").append(line).append("\n");
            sb.append("Name:           ").append(c.getName()).append("\n");
            sb.append("Role:           ").append(c.getRole()).append("\n");
            sb.append("Status:         ").append(c.getStatus()).append("\n");
            sb.append("Joined:         ").append(c.getJoined()).append("\n");
            sb.append("Experience:     ").append(c.getExperience()).append("\n");
            sb.append("Previous Team:  ").append(c.getPreviousTeam()).append("\n");
            sb.append("Championships:  ").append(c.getChampionships()).append("\n\n");
            sb.append("Achievements:\n").append(c.getAchievements()).append("\n\n");
            sb.append("Background:\n").append(c.getInfo()).append("\n");
        }
        return sb.toString();
    }

    private void printCurrentProfile() {
        if (currentSelection == null) { JOptionPane.showMessageDialog(this,"Select a profile first."); return; }
        String text = buildProfileText();
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable((g, pf, pi) -> {
            if (pi > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
            int y = 20;
            for (String line : text.split("\n")) {
                g2.drawString(line, 20, y);
                y += 16;
                if (y > pf.getImageableHeight() - 20) break;
            }
            return Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) { JOptionPane.showMessageDialog(this,"⚠️ Print error: "+ex.getMessage()); }
        }
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private void installUIDefaults() {
        // Make default (non-styled) buttons readable even without hover.
        Font btnFont = new Font("Arial", Font.BOLD, 13);

        // Force a UI delegate that respects background/foreground reliably.
        // Some Look & Feel implementations can ignore initial colors and only
        // repaint them on rollover, which makes buttons appear "invisible".
        UIManager.put("ButtonUI", "javax.swing.plaf.basic.BasicButtonUI");
        UIManager.put("ToggleButtonUI", "javax.swing.plaf.basic.BasicToggleButtonUI");
        UIManager.put("Button.font", btnFont);
        UIManager.put("ToggleButton.font", btnFont);
        UIManager.put("Button.background", new Color(245, 247, 250));
        UIManager.put("Button.foreground", NAVY);
        UIManager.put("Button.disabledText", new Color(120, 130, 145));
        Border b = new CompoundBorder(new LineBorder(SILVER, 1, true), new EmptyBorder(8, 14, 8, 14));
        UIManager.put("Button.border", b);
        UIManager.put("ToggleButton.border", b);
        UIManager.put("ComboBox.font", new Font("Arial", Font.PLAIN, 13));
        UIManager.put("TextField.font", new Font("Arial", Font.PLAIN, 13));
        UIManager.put("Label.font", new Font("Arial", Font.PLAIN, 13));

        // Menu readability (some L&Fs can render menu text nearly invisible).
        UIManager.put("MenuBar.background", NAVY);
        UIManager.put("MenuBar.foreground", WHITE);
        UIManager.put("Menu.background", NAVY);
        UIManager.put("Menu.foreground", WHITE);
        UIManager.put("Menu.selectionBackground", NAVY_DARK);
        UIManager.put("Menu.selectionForeground", WHITE);
        UIManager.put("MenuItem.background", WHITE);
        UIManager.put("MenuItem.foreground", NAVY);
        UIManager.put("MenuItem.selectionBackground", NAVY);
        UIManager.put("MenuItem.selectionForeground", WHITE);
        UIManager.put("PopupMenu.border", new LineBorder(SILVER, 1, true));

        // Table header readability.
        UIManager.put("TableHeader.font", new Font("Arial", Font.BOLD, 12));
        UIManager.put("TableHeader.background", new Color(235, 240, 246));
        UIManager.put("TableHeader.foreground", NAVY);
        UIManager.put("TableHeader.cellBorder", new LineBorder(SILVER, 1));
    }


    private void applyTableTheme(JTable table) {
        // Ensure table + header remain readable across Look & Feel themes.
        table.setBackground(WHITE);
        table.setForeground(NAVY);
        table.setSelectionBackground(new Color(209, 229, 255)); // readable highlight
        table.setSelectionForeground(NAVY);
        table.setGridColor(SILVER_LITE);
        table.setShowVerticalLines(false);

        JTableHeader header = table.getTableHeader();
        header.setOpaque(true);
        header.setBackground(new Color(235, 240, 246));
        header.setForeground(NAVY);
        header.setFont(new Font("Arial", Font.BOLD, 13));
        header.setDefaultRenderer(new HeaderCellRenderer());

        // Make the viewport background consistent too.
        if (table.getParent() instanceof JViewport) {
            ((JViewport) table.getParent()).setBackground(WHITE);
        }
    }

    private static class HeaderCellRenderer extends DefaultTableCellRenderer {
        private final Border border = new MatteBorder(0, 0, 1, 1, new Color(210, 218, 230));

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, false, false, row, column);
            setOpaque(true);
            setBackground(new Color(235, 240, 246));
            setForeground(NAVY);
            setFont(new Font("Arial", Font.BOLD, 13));
            setBorder(border);
            setHorizontalAlignment(LEFT);
            return this;
        }
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.BOLD, 13));
        l.setForeground(NAVY);
        return l;
    }

    private JComboBox<String> comboBox(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(new Font("Arial", Font.PLAIN, 13));
        cb.setPreferredSize(new Dimension(160, 34));
        return cb;
    }

    private JButton styledBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 13));
        btn.setForeground(WHITE);
        btn.setBackground(BLUE);

        // Ensure a consistent painter (prevents "text-only until hover" effect).
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());

        // Ensure buttons remain visible across Look & Feel settings.
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(true);

        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(NAVY_DARK, 1, true), new EmptyBorder(8, 18, 8, 18)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            final Color orig = btn.getBackground();
            public void mouseEntered(MouseEvent e) { btn.setBackground(NAVY); }
            public void mouseExited (MouseEvent e) { btn.setBackground(orig); }
        });
        return btn;
    }

    /** A compact action button used inside cards/headers. */
    private void styleSmallActionButton(JButton btn) {
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setForeground(WHITE);
        btn.setBackground(BLUE);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(true);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(NAVY_DARK, 1, true), new EmptyBorder(6, 10, 6, 10)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(this, "⚠️ " + msg + "\nThis cannot be undone without a restore.",
            "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void clearInfoPanel(String msg) {
        infoDisplayPanel.removeAll();
        infoDisplayPanel.add(new JLabel(msg, SwingConstants.CENTER), BorderLayout.CENTER);
        infoDisplayPanel.revalidate(); infoDisplayPanel.repaint();
    }

    private int parseIntField(JTextField field) {
        String t = field.getText().trim();
        if (t.isEmpty()) return 0;
        return Integer.parseInt(t);
    }

    private DocumentListener simpleDocListener(Runnable r) {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e){ r.run(); }
        };
    }

    /** Custom sidebar cell renderer with subtle hover effect. */
    private class SidebarCellRenderer extends DefaultListCellRenderer {
        @Override public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            lbl.setBorder(new EmptyBorder(6, 10, 6, 10));
            if (isSelected) {
                lbl.setBackground(new Color(0,53,148,200));
                lbl.setForeground(WHITE);
            } else {
                lbl.setBackground(index % 2 == 0 ? WHITE : new Color(247,249,252));
                lbl.setForeground(NAVY);
            }
            return lbl;
        }
    }

    // =========================================================================
    // GLOBAL SEARCH TAB
    // =========================================================================
    private JPanel buildGlobalSearchTab() {
        JPanel outer = new JPanel(new BorderLayout(0, 10));
        outer.setBackground(BG_GRAY);
        outer.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel searchBar = new JPanel(new BorderLayout(10, 0));
        searchBar.setBackground(WHITE);
        searchBar.setBorder(new CompoundBorder(new LineBorder(SILVER_LITE, 1), new EmptyBorder(12, 14, 12, 14)));
        JLabel lbl = new JLabel("🔍 Search All Categories:");
        lbl.setFont(new Font("Arial", Font.BOLD, 15)); lbl.setForeground(NAVY);
        globalSearchField = new JTextField();
        globalSearchField.setFont(new Font("Arial", Font.PLAIN, 15));
        globalSearchField.setBorder(new CompoundBorder(new LineBorder(SILVER, 1), new EmptyBorder(6, 10, 6, 10)));
        globalSearchField.getDocument().addDocumentListener(simpleDocListener(this::runGlobalSearch));
        JButton clrBtn = styledBtn("Clear");
        clrBtn.addActionListener(e -> globalSearchField.setText(""));
        searchBar.add(lbl, BorderLayout.WEST);
        searchBar.add(globalSearchField, BorderLayout.CENTER);
        searchBar.add(clrBtn, BorderLayout.EAST);

        globalResultsPanel = new JPanel();
        globalResultsPanel.setLayout(new BoxLayout(globalResultsPanel, BoxLayout.Y_AXIS));
        globalResultsPanel.setBackground(BG_GRAY);
        JLabel hint = new JLabel("Type above to search across Players, Coaches, Cheerleaders, and Staff simultaneously", SwingConstants.CENTER);
        hint.setFont(new Font("Arial", Font.ITALIC, 14)); hint.setForeground(SILVER);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        globalResultsPanel.add(Box.createVerticalStrut(30));
        globalResultsPanel.add(hint);

        JScrollPane scroll = new JScrollPane(globalResultsPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        outer.add(searchBar, BorderLayout.NORTH);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    private void runGlobalSearch() {
        String q = globalSearchField.getText().trim().toLowerCase();
        globalResultsPanel.removeAll();
        if (q.isEmpty()) {
            JLabel hint = new JLabel("Type above to search all categories", SwingConstants.CENTER);
            hint.setFont(new Font("Arial", Font.ITALIC, 14)); hint.setForeground(SILVER);
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);
            globalResultsPanel.add(Box.createVerticalStrut(30)); globalResultsPanel.add(hint);
        } else {
            List<Player>      pR  = players.stream().filter(p -> p.getName().toLowerCase().contains(q) || p.getPosition().toLowerCase().contains(q) || p.getCollege().toLowerCase().contains(q)).collect(Collectors.toList());
            List<Coach>       cR  = coaches.stream().filter(c -> c.getName().toLowerCase().contains(q) || c.getRole().toLowerCase().contains(q)).collect(Collectors.toList());
            List<Cheerleader> chR = cheerleaders.stream().filter(c -> c.getName().toLowerCase().contains(q) || c.getRole().toLowerCase().contains(q)).collect(Collectors.toList());
            List<Staff> stR = new ArrayList<>(trainers); stR.addAll(otherStaff);
            stR = stR.stream().filter(s -> s.getName().toLowerCase().contains(q) || s.getRole().toLowerCase().contains(q)).collect(Collectors.toList());
            int total = pR.size() + cR.size() + chR.size() + stR.size();
            JLabel sum = new JLabel("  Found " + total + " result" + (total != 1 ? "s" : "") + " for \"" + q + "\"");
            sum.setFont(new Font("Arial", Font.BOLD, 14)); sum.setForeground(NAVY); sum.setAlignmentX(Component.LEFT_ALIGNMENT);
            globalResultsPanel.add(Box.createVerticalStrut(8)); globalResultsPanel.add(sum); globalResultsPanel.add(Box.createVerticalStrut(10));
            if (!pR.isEmpty())  addSearchSection("👟 Players ("       + pR.size()  + ")", pR,  BLUE);
            if (!cR.isEmpty())  addSearchSection("👔 Coaches ("       + cR.size()  + ")", cR,  NAVY);
            if (!chR.isEmpty()) addSearchSection("💃 Cheerleaders (" + chR.size() + ")", chR, PINK);
            if (!stR.isEmpty()) addSearchSection("🏢 Staff ("         + stR.size() + ")", stR, TEAL);
            if (total == 0) {
                JLabel none = new JLabel("No results found.", SwingConstants.CENTER);
                none.setFont(new Font("Arial", Font.ITALIC, 14)); none.setForeground(SILVER); none.setAlignmentX(Component.CENTER_ALIGNMENT);
                globalResultsPanel.add(none);
            }
        }
        globalResultsPanel.revalidate(); globalResultsPanel.repaint();
    }

    private void addSearchSection(String title, List<?> items, Color accent) {
        JLabel sl = new JLabel("  " + title);
        sl.setFont(new Font("Arial", Font.BOLD, 13)); sl.setForeground(WHITE); sl.setOpaque(true);
        sl.setBackground(accent); sl.setBorder(new EmptyBorder(6, 10, 6, 10));
        sl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34)); sl.setAlignmentX(Component.LEFT_ALIGNMENT);
        globalResultsPanel.add(sl);
        for (Object item : items) {
            String nm = gName(item), rl = gRole(item), ex = gExtra(item);
            JPanel row = new JPanel(new BorderLayout(10, 0)); row.setBackground(WHITE);
            row.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, SILVER_LITE), new EmptyBorder(8, 16, 8, 16)));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50)); row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel nl = new JLabel(nm); nl.setFont(new Font("Arial", Font.BOLD, 13)); nl.setForeground(NAVY);
            JLabel rl2 = new JLabel(rl); rl2.setFont(new Font("Arial", Font.PLAIN, 12)); rl2.setForeground(SILVER);
            JLabel el = new JLabel(ex); el.setFont(new Font("Arial", Font.PLAIN, 12)); el.setForeground(accent);
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); left.setBackground(WHITE);
            left.add(nl); left.add(rl2);
            row.add(left, BorderLayout.CENTER); row.add(el, BorderLayout.EAST);
            row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            Object cap = item;
            row.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { jumpToProfile(cap); }
                @Override public void mouseEntered(MouseEvent e) { row.setBackground(new Color(240, 245, 255)); left.setBackground(new Color(240, 245, 255)); }
                @Override public void mouseExited(MouseEvent e)  { row.setBackground(WHITE); left.setBackground(WHITE); }
            });
            globalResultsPanel.add(row);
        }
        globalResultsPanel.add(Box.createVerticalStrut(12));
    }

    private void jumpToProfile(Object item) {
        tabbedPane.setSelectedIndex(0);
        if (item instanceof Player) {
            categoryBox.setSelectedItem("Players"); activeOnlyCheckbox.setSelected(false); applyFilters(); selectByName(((Player) item).getName());
        } else if (item instanceof Coach) {
            categoryBox.setSelectedItem("Coaches"); activeOnlyCheckbox.setSelected(false); applyFilters(); selectByName(((Coach) item).getName());
        } else if (item instanceof Cheerleader) {
            categoryBox.setSelectedItem("Cheerleaders"); applyFilters(); selectByName(((Cheerleader) item).getName());
        } else if (item instanceof Staff) {
            Staff s = (Staff) item;
            boolean inT = trainers.stream().anyMatch(t -> t.getName().equals(s.getName()));
            categoryBox.setSelectedItem(inT ? "Trainers" : "Other Staff"); applyFilters(); selectByName(s.getName());
        }
    }

    private String gName(Object o) {
        if (o instanceof Player)      return ((Player) o).getName();
        if (o instanceof Coach)       return ((Coach) o).getName();
        if (o instanceof Cheerleader) return ((Cheerleader) o).getName();
        if (o instanceof Staff)       return ((Staff) o).getName();
        return "";
    }
    private String gRole(Object o) {
        if (o instanceof Player)      return ((Player) o).getPosition();
        if (o instanceof Coach)       return ((Coach) o).getRole();
        if (o instanceof Cheerleader) return ((Cheerleader) o).getRole();
        if (o instanceof Staff)       return ((Staff) o).getRole();
        return "";
    }
    private String gExtra(Object o) {
        if (o instanceof Player)      return "#" + ((Player) o).getNumber() + " · " + ((Player) o).getStatus();
        if (o instanceof Coach)       return ((Coach) o).getStatus();
        if (o instanceof Cheerleader) return ((Cheerleader) o).getStatus();
        if (o instanceof Staff)       return ((Staff) o).getStatus();
        return "";
    }

    // =========================================================================
    // DEPTH CHART TAB
    // =========================================================================
    private JPanel buildDepthChartTab() {
        JPanel outer = new JPanel(new BorderLayout(0, 10));
        outer.setBackground(BG_GRAY); outer.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("📋 Offensive & Defensive Depth Charts");
        title.setFont(new Font("Arial Black", Font.BOLD, 20)); title.setForeground(NAVY); title.setBorder(new EmptyBorder(0, 0, 10, 0));

        String[][] off  = {{"QB"}, {"RB","FB"}, {"WR"}, {"TE"}, {"OL","OT","OG","C"}};
        String[][] def  = {{"DE","DT","NT"}, {"LB","MLB","OLB","ILB"}, {"CB","DB"}, {"S","FS","SS"}};
        String[]   offL = {"QB", "RB / FB", "WR", "TE", "O-Line"};
        String[]   defL = {"D-Line", "Linebackers", "Cornerbacks", "Safeties"};

        JPanel offPanel = buildDepthColumn("⚡ Offense",  off, offL, OFF_COLOR);
        JPanel defPanel = buildDepthColumn("🛡️ Defense", def, defL, DEF_COLOR);
        JPanel stPanel  = buildSpecialTeamsCol(new String[]{"K","P","LS"});

        JSplitPane s1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, offPanel, defPanel);
        s1.setResizeWeight(0.5); s1.setDividerSize(6);
        JSplitPane s2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, s1, stPanel);
        s2.setResizeWeight(0.75); s2.setDividerSize(6);

        outer.add(title, BorderLayout.NORTH); outer.add(s2, BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildDepthColumn(String heading, String[][] posGroups, String[] groupLabels, Color accent) {
        JPanel panel = new JPanel(new BorderLayout(0, 6)); panel.setBackground(BG_GRAY);
        JLabel hdr = new JLabel(heading, SwingConstants.CENTER);
        hdr.setFont(new Font("Arial Black", Font.BOLD, 14)); hdr.setForeground(WHITE);
        hdr.setOpaque(true); hdr.setBackground(accent); hdr.setBorder(new EmptyBorder(8, 10, 8, 10));
        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS)); body.setBackground(BG_GRAY);

        for (int g = 0; g < posGroups.length; g++) {
            String[] positions = posGroups[g]; String gl = groupLabels[g];
            List<Player> grp = new ArrayList<>();
            for (String pos : positions)
                grp.addAll(players.stream().filter(p -> p.getPosition().equalsIgnoreCase(pos) && p.isActive())
                    .sorted(Comparator.comparingInt(Player::getProBowls).reversed()).collect(Collectors.toList()));

            JLabel glbl = new JLabel("  " + gl + " (" + grp.size() + ")");
            glbl.setFont(new Font("Arial", Font.BOLD, 12)); glbl.setForeground(WHITE); glbl.setOpaque(true);
            glbl.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200));
            glbl.setBorder(new EmptyBorder(4, 6, 4, 6)); glbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            glbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(glbl);

            for (int i = 0; i < grp.size(); i++) {
                Player p = grp.get(i); final int fi = i;
                JPanel row = new JPanel(new BorderLayout(6, 0));
                row.setBackground(i % 2 == 0 ? WHITE : new Color(248, 250, 253));
                row.setBorder(new EmptyBorder(5, 10, 5, 10)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel dl = new JLabel(i == 0 ? "STARTER" : "  #" + (i + 1));
                dl.setFont(new Font("Arial", Font.BOLD, 10)); dl.setForeground(i == 0 ? GREEN : SILVER);
                dl.setPreferredSize(new Dimension(52, 18));
                JLabel nl = new JLabel(p.getName()); nl.setFont(new Font("Arial", Font.PLAIN, 13)); nl.setForeground(NAVY);
                JLabel nml = new JLabel("#" + p.getNumber()); nml.setFont(new Font("Arial", Font.BOLD, 11)); nml.setForeground(SILVER);
                JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)); left.setBackground(row.getBackground());
                left.add(dl); left.add(nl);
                row.add(left, BorderLayout.CENTER); row.add(nml, BorderLayout.EAST);
                final Player fp = p;
                row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                row.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { jumpToProfile(fp); }
                    @Override public void mouseEntered(MouseEvent e) { row.setBackground(new Color(232, 240, 255)); left.setBackground(new Color(232, 240, 255)); }
                    @Override public void mouseExited(MouseEvent e)  { Color bg = fi % 2 == 0 ? WHITE : new Color(248, 250, 253); row.setBackground(bg); left.setBackground(bg); }
                });
                body.add(row);
            }
            body.add(Box.createVerticalStrut(8));
        }
        JScrollPane sc = new JScrollPane(body); sc.setBorder(new LineBorder(SILVER_LITE, 1));
        panel.add(hdr, BorderLayout.NORTH); panel.add(sc, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSpecialTeamsCol(String[] positions) {
        JPanel panel = new JPanel(new BorderLayout(0, 6)); panel.setBackground(BG_GRAY);
        JLabel hdr = new JLabel("⚽ Special Teams", SwingConstants.CENTER);
        hdr.setFont(new Font("Arial Black", Font.BOLD, 14)); hdr.setForeground(WHITE);
        hdr.setOpaque(true); hdr.setBackground(ST_COLOR); hdr.setBorder(new EmptyBorder(8, 10, 8, 10));
        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS)); body.setBackground(BG_GRAY);
        for (String pos : positions) {
            List<Player> grp = players.stream().filter(p -> p.getPosition().equalsIgnoreCase(pos) && p.isActive()).collect(Collectors.toList());
            JLabel pl = new JLabel("  " + pos + " (" + grp.size() + ")");
            pl.setFont(new Font("Arial", Font.BOLD, 12)); pl.setForeground(WHITE); pl.setOpaque(true);
            pl.setBackground(new Color(ST_COLOR.getRed(), ST_COLOR.getGreen(), ST_COLOR.getBlue(), 200));
            pl.setBorder(new EmptyBorder(4, 6, 4, 6)); pl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            pl.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(pl);
            for (int i = 0; i < grp.size(); i++) {
                JPanel row = new JPanel(new BorderLayout()); row.setBackground(i % 2 == 0 ? WHITE : new Color(248, 250, 253));
                row.setBorder(new EmptyBorder(5, 10, 5, 10)); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                JLabel nl = new JLabel(grp.get(i).getName()); nl.setFont(new Font("Arial", Font.PLAIN, 13)); nl.setForeground(NAVY);
                row.add(nl, BorderLayout.CENTER); body.add(row);
            }
            body.add(Box.createVerticalStrut(8));
        }
        JScrollPane sc = new JScrollPane(body); sc.setBorder(new LineBorder(SILVER_LITE, 1));
        panel.add(hdr, BorderLayout.NORTH); panel.add(sc, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================================
    // COMPARE TAB
    // =========================================================================
    private JPanel buildCompareTab() {
        JPanel fullPanel = new JPanel(new BorderLayout(0, 10));
        fullPanel.setBackground(BG_GRAY); fullPanel.setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel title = new JLabel("⚖️ Player Comparison", SwingConstants.CENTER);
        title.setFont(new Font("Arial Black", Font.BOLD, 20)); title.setForeground(NAVY);

        JPanel selBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10)); selBar.setBackground(WHITE);
        selBar.setBorder(new CompoundBorder(new LineBorder(SILVER_LITE, 1), new EmptyBorder(8, 16, 8, 16)));
        compareBox1 = new JComboBox<>(); compareBox2 = new JComboBox<>();
        compareBox1.setFont(new Font("Arial", Font.PLAIN, 13)); compareBox2.setFont(new Font("Arial", Font.PLAIN, 13));
        compareBox1.setPreferredSize(new Dimension(240, 34)); compareBox2.setPreferredSize(new Dimension(240, 34));
        players.stream().sorted(Comparator.comparing(Player::getName)).forEach(p -> {
            compareBox1.addItem(p.getName()); compareBox2.addItem(p.getName());
        });
        if (compareBox2.getItemCount() > 1) compareBox2.setSelectedIndex(1);
        JButton btn = styledBtn("⚖️ Compare"); btn.addActionListener(e -> runComparison());
        selBar.add(new JLabel("Player 1:")); selBar.add(compareBox1);
        selBar.add(new JLabel("vs")); selBar.add(new JLabel("Player 2:")); selBar.add(compareBox2); selBar.add(btn);

        comparePanel = new JPanel(new GridLayout(1, 2, 20, 0)); comparePanel.setBackground(BG_GRAY);
        JLabel hint = new JLabel("Select two players above and click Compare", SwingConstants.CENTER);
        hint.setFont(new Font("Arial", Font.ITALIC, 15)); hint.setForeground(SILVER); comparePanel.add(hint);

        JScrollPane sc = new JScrollPane(comparePanel); sc.setBorder(null); sc.getVerticalScrollBar().setUnitIncrement(16);
        JPanel top = new JPanel(new BorderLayout(0, 8)); top.setBackground(BG_GRAY);
        top.add(title, BorderLayout.NORTH); top.add(selBar, BorderLayout.CENTER);
        fullPanel.add(top, BorderLayout.NORTH); fullPanel.add(sc, BorderLayout.CENTER);
        return fullPanel;
    }

    private void runComparison() {
        String n1 = (String) compareBox1.getSelectedItem(), n2 = (String) compareBox2.getSelectedItem();
        if (n1 == null || n2 == null || n1.equals(n2)) { JOptionPane.showMessageDialog(this, "Please select two different players."); return; }
        Player p1 = players.stream().filter(p -> p.getName().equals(n1)).findFirst().orElse(null);
        Player p2 = players.stream().filter(p -> p.getName().equals(n2)).findFirst().orElse(null);
        if (p1 == null || p2 == null) return;
        comparePanel.removeAll();
        comparePanel.add(buildCompareCard(p1, p2, true));
        comparePanel.add(buildCompareCard(p2, p1, false));
        comparePanel.revalidate(); comparePanel.repaint();
    }

    private JPanel buildCompareCard(Player p, Player other, boolean isLeft) {
        JPanel card = new JPanel(); card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(isLeft ? BLUE : RED_SOFT, 2), new EmptyBorder(20, 20, 20, 20)));

        JLabel nl = new JLabel(p.getName()); nl.setFont(new Font("Arial Black", Font.BOLD, 22)); nl.setForeground(NAVY); nl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel pl = new JLabel(p.getPosition() + "  •  #" + p.getNumber()); pl.setFont(new Font("Arial", Font.BOLD, 14));
        pl.setForeground(isLeft ? BLUE : RED_SOFT); pl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nl); card.add(Box.createVerticalStrut(4)); card.add(pl); card.add(Box.createVerticalStrut(14));

        String[][] rows = {{"Status", p.getStatus()}, {"Height", p.getHeight()}, {"Weight", p.getWeight() + " lbs"},
            {"College", p.getCollege()}, {"Joined", String.valueOf(p.getJoined())},
            {"Years w/ DAL", String.valueOf(p.yearsWithDallas(CURRENT_YEAR))},
            {"Pro Bowls", String.valueOf(p.getProBowls())}, {"Draft", p.getDraftDisplay()}};

        for (String[] row : rows) {
            JPanel rp = new JPanel(new BorderLayout(12, 0)); rp.setBackground(WHITE);
            rp.setBorder(new MatteBorder(0, 0, 1, 0, new Color(240, 243, 248)));
            rp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32)); rp.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel k = new JLabel(row[0]); k.setFont(new Font("Arial", Font.BOLD, 12)); k.setForeground(SILVER);
            JLabel v = new JLabel(row[1]); v.setFont(new Font("Arial", Font.BOLD, 13)); v.setForeground(NAVY);
            if (row[0].equals("Pro Bowls")) { int my = p.getProBowls(), ov = other.getProBowls(); if (my > ov) v.setForeground(GREEN); else if (my < ov) v.setForeground(RED_SOFT); }
            if (row[0].equals("Years w/ DAL")) { int my = p.yearsWithDallas(CURRENT_YEAR), ov = other.yearsWithDallas(CURRENT_YEAR); if (my > ov) v.setForeground(GREEN); else if (my < ov) v.setForeground(RED_SOFT); }
            rp.add(k, BorderLayout.WEST); rp.add(v, BorderLayout.EAST);
            card.add(rp);
        }
        if (!p.getAchievements().isEmpty()) {
            card.add(Box.createVerticalStrut(12));
            JLabel ah = new JLabel("🏆 Achievements"); ah.setFont(new Font("Arial", Font.BOLD, 13)); ah.setForeground(NAVY); ah.setAlignmentX(Component.LEFT_ALIGNMENT);
            JTextArea ach = new JTextArea(p.getAchievements().replace(";", "\n• ").trim());
            if (!ach.getText().startsWith("•")) ach.setText("• " + ach.getText());
            ach.setFont(new Font("Arial", Font.ITALIC, 12)); ach.setEditable(false); ach.setLineWrap(true); ach.setWrapStyleWord(true);
            ach.setBackground(GOLD_LITE); ach.setForeground(NAVY_DARK);
            ach.setBorder(new CompoundBorder(new LineBorder(GOLD, 1), new EmptyBorder(6, 8, 6, 8)));
            ach.setAlignmentX(Component.LEFT_ALIGNMENT); ach.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
            card.add(ah); card.add(Box.createVerticalStrut(4)); card.add(ach);
        }
        return card;
    }

    // =========================================================================
    // CHEERLEADER + STAFF CARDS
    // =========================================================================
    private JPanel buildCheerleaderCard(Cheerleader c) {
        JPanel card = new JPanel(new BorderLayout(16, 0)); card.setBackground(WHITE);
        card.add(buildPhotoPanel(null, "💃"), BorderLayout.WEST);
        JPanel info = new JPanel(); info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS)); info.setBackground(WHITE);
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); hdr.setBackground(WHITE); hdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel nl = new JLabel(c.getName()); nl.setFont(new Font("Arial Black", Font.BOLD, 26)); nl.setForeground(NAVY);
        hdr.add(nl); hdr.add(statusBadge(c.getStatus(), c.isActive())); if (c.isFavorite()) hdr.add(favBadge());
        JLabel rl = new JLabel(c.getRole()); rl.setFont(new Font("Arial", Font.BOLD, 16)); rl.setForeground(PINK); rl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel sg = new JPanel(new GridLayout(3, 2, 8, 6)); sg.setBackground(new Color(248, 250, 253));
        sg.setBorder(new CompoundBorder(new LineBorder(SILVER_LITE, 1), new EmptyBorder(10, 12, 10, 12)));
        sg.setAlignmentX(Component.LEFT_ALIGNMENT); sg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        sg.add(kv("Age", String.valueOf(c.getAge()))); sg.add(kv("Height", c.getHeight()));
        sg.add(kv("College", c.getCollege())); sg.add(kv("Experience", c.getExperience() + " years")); sg.add(kv("Status", c.getStatus()));
        info.add(hdr); info.add(Box.createVerticalStrut(4)); info.add(rl); info.add(Box.createVerticalStrut(12)); info.add(sg); info.add(Box.createVerticalStrut(12));
        if (!c.getAchievements().isEmpty()) { info.add(sectionHeader("🏆 Achievements")); info.add(Box.createVerticalStrut(4)); info.add(achievementBox(c.getAchievements())); info.add(Box.createVerticalStrut(12)); }
        if (!c.getInfo().isEmpty()) { info.add(sectionHeader("About")); info.add(Box.createVerticalStrut(4)); info.add(bioArea(c.getInfo())); }
        JScrollPane sc = new JScrollPane(info); sc.setBorder(null); sc.getVerticalScrollBar().setUnitIncrement(12);
        card.add(sc, BorderLayout.CENTER); return card;
    }

    private JPanel buildStaffCard(Staff s) {
        JPanel card = new JPanel(new BorderLayout(16, 0)); card.setBackground(WHITE);
        card.add(buildPhotoPanel(null, "🏢"), BorderLayout.WEST);
        JPanel info = new JPanel(); info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS)); info.setBackground(WHITE);
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); hdr.setBackground(WHITE); hdr.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel nl = new JLabel(s.getName()); nl.setFont(new Font("Arial Black", Font.BOLD, 26)); nl.setForeground(NAVY);
        hdr.add(nl); hdr.add(statusBadge(s.getStatus(), s.isActive())); if (s.isFavorite()) hdr.add(favBadge());
        JLabel rl = new JLabel(s.getRole()); rl.setFont(new Font("Arial", Font.BOLD, 16)); rl.setForeground(TEAL); rl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel sg = new JPanel(new GridLayout(2, 2, 8, 6)); sg.setBackground(new Color(248, 250, 253));
        sg.setBorder(new CompoundBorder(new LineBorder(SILVER_LITE, 1), new EmptyBorder(10, 12, 10, 12)));
        sg.setAlignmentX(Component.LEFT_ALIGNMENT); sg.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        sg.add(kv("Experience", s.getExperience() + " years")); sg.add(kv("College", s.getCollege())); sg.add(kv("Status", s.getStatus()));
        info.add(hdr); info.add(Box.createVerticalStrut(4)); info.add(rl); info.add(Box.createVerticalStrut(12)); info.add(sg); info.add(Box.createVerticalStrut(12));
        if (!s.getAchievements().isEmpty()) { info.add(sectionHeader("🏆 Achievements")); info.add(Box.createVerticalStrut(4)); info.add(achievementBox(s.getAchievements())); info.add(Box.createVerticalStrut(12)); }
        if (!s.getInfo().isEmpty()) { info.add(sectionHeader("About")); info.add(Box.createVerticalStrut(4)); info.add(bioArea(s.getInfo())); }
        JScrollPane sc = new JScrollPane(info); sc.setBorder(null); sc.getVerticalScrollBar().setUnitIncrement(12);
        card.add(sc, BorderLayout.CENTER); return card;
    }

    // =========================================================================
    // MAIN
    // =========================================================================
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(CowboysApp::new);
    }
}

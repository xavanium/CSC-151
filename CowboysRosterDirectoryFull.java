import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
public class CowboysRosterDirectoryFull extends JFrame {
private JComboBox<String> searchTypeCombo;
private JComboBox<String> positionCombo;
private JComboBox<String> statusCombo;
private JComboBox<String> viewTypeCombo;
private JTextField nameField;
private JButton searchBtn;
private JButton randomBtn;
private JTextArea resultArea;
private JTable rosterTable;
private DefaultTableModel tableModel;
private List<Cheerleader> cheerleaders;
private List<Staff> trainers;
private List<Staff> otherStaff;
private List<Player> roster;
private List<Coach> coaches;
private Random random;
public CowboysRosterDirectoryFull() {
setTitle("Dallas Cowboys 2025 Roster Directory");
setSize(1200, 750);
setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
setLayout(new BorderLayout(10, 10));
random = new Random();
initializeRoster();
initializeCoaches();
initializeCheerleaders();
initializeTrainers();
initializeOtherStaff();
createUI();
setLocationRelativeTo(null);
setVisible(true);
}
private void initializeRoster() {
roster = new ArrayList<>();
// Offense - QB
roster.add(new Player("Dak Prescott", 4, "QB", 32, "6'2\"", 238, 10,
"Mississippi State", "Active"));
roster.add(new Player("Joe Milton III", 20, "QB", 25, "6'5\"", 246, 2,
"Tennessee", "Active"));
// RB
roster.add(new Player("Israel Abanikanda", 46, "RB", 23, "5'10\"", 217, 3,
"Pittsburgh", "Active"));
roster.add(new Player("Jaydon Blue", 23, "RB", 22, "5'9\"", 196, 0,
"Texas", "Active"));
roster.add(new Player("Hunter Luepke", 42, "RB", 25, "6'1\"", 238, 3,
"North Dakota State", "Active"));
roster.add(new Player("Phil Mafah", 24, "RB", 23, "6'0\"", 234, 0,
"Clemson", "Active"));
// WR
roster.add(new Player("CeeDee Lamb", 88, "WR", 26, "6'2\"", 200, 6,
"Oklahoma", "Active"));
roster.add(new Player("George Pickens", 14, "WR", 24, "6'3\"", 200, 4,
"Georgia", "Active"));
roster.add(new Player("Jonathan Mingo", 15, "WR", 24, "6'2\"", 220, 3, "Ole
Miss", "Active"));
roster.add(new Player("Jalen Tolbert", 1, "WR", 26, "6'1\"", 195, 4, "South
Alabama", "Active"));
roster.add(new Player("KaVontae Turpin", 9, "WR", 29, "5'9\"", 153, 4,
"TCU", "Active"));
roster.add(new Player("Parris Campbell", 11, "WR", 28, "6'0\"", 208, 7,
"Ohio State", "Active"));
roster.add(new Player("Ryan Flournoy", 0, "WR", 26, "6'1\"", 200, 2, "SE
Missouri State", "Active"));
roster.add(new Player("Traeshon Holden", 12, "WR", 24, "6'3\"", 205, 0,
"Oregon", "Active"));
// TE
roster.add(new Player("Jake Ferguson", 87, "TE", 27, "6'5\"", 244, 4,
"Wisconsin", "Active"));
roster.add(new Player("Luke Schoonmaker", 86, "TE", 27, "6'5\"", 250, 3,
"Michigan", "Active"));
roster.add(new Player("Brevyn Spann-Ford", 89, "TE", 26, "6'7\"", 270, 2,
"Minnesota", "Active"));
// Offensive Line
roster.add(new Player("Tyler Smith", 73, "G", 24, "6'6\"", 332, 4, "Tulsa",
"Active"));
roster.add(new Player("Cooper Beebe", 68, "C", 24, "6'4\"", 335, 2, "Kansas
State", "Active"));
roster.add(new Player("Brock Hoffman", 56, "C", 26, "6'4\"", 302, 3,
"Virginia Tech", "Active"));
roster.add(new Player("Tyler Booker", 75, "G", 21, "6'4\"", 321, 0,
"Alabama", "Active"));
roster.add(new Player("Nick Leverett", 61, "G", 29, "6'4\"", 310, 5,
"Rice", "Active"));
roster.add(new Player("Terence Steele", 78, "OT", 28, "6'6\"", 310, 6,
"Texas Tech", "Active"));
roster.add(new Player("Hakeem Adeniji", 76, "OT", 28, "6'4\"", 302, 6,
"Kansas", "Active"));
roster.add(new Player("Marcellus Johnson", 71, "OT", 25, "6'4\"", 307, 1,
"Missouri", "Active"));
roster.add(new Player("Nate Thomas", 64, "OT", 24, "6'4\"", 331, 2,
"Louisiana", "Active"));
// Defense - DE
roster.add(new Player("Jadeveon Clowney", 90, "DE", 32, "6'5\"", 266, 12,
"South Carolina", "Active"));
roster.add(new Player("Dante Fowler Jr.", 56, "DE", 31, "6'3\"", 261, 11,
"Florida", "Active"));
roster.add(new Player("Sam Williams", 54, "DE", 26, "6'4\"", 261, 4, "Ole
Miss", "Active"));
roster.add(new Player("Donovan Ezeiruaku", 92, "DE", 22, "6'2\"", 248, 0,
"Boston College", "Active"));
roster.add(new Player("James Houston", 41, "DE", 27, "6'1\"", 241, 4,
"Jackson State", "Active"));
roster.add(new Player("Adedayo Odeleye", 55, "DE", 28, "6'5\"", 283, 1,
"—", "Active"));
// DT
roster.add(new Player("Quinnen Williams", 95, "DT", 28, "6'3\"", 303, 7,
"Alabama", "Active"));
roster.add(new Player("Kenny Clark", 97, "DT", 30, "6'3\"", 314, 10,
"UCLA", "Active"));
roster.add(new Player("Osa Odighizuwa", 94, "DT", 27, "6'2\"", 280, 5,
"UCLA", "Active"));
roster.add(new Player("Solomon Thomas", 98, "DT", 30, "6'2\"", 285, 9,
"Stanford", "Active"));
roster.add(new Player("Perrion Winfrey", 99, "DT", 25, "6'4\"", 290, 2,
"Oklahoma", "Active"));
roster.add(new Player("Jay Toia", 93, "DT", 22, "6'2\"", 342, 0, "UCLA",
"Active"));
// LB
roster.add(new Player("DeMarvion Overshown", 0, "LB", 25, "6'2\"", 220, 3,
"Texas", "Active"));
roster.add(new Player("Logan Wilson", 55, "LB", 29, "6'2\"", 245, 6,
"Wyoming", "Active"));
roster.add(new Player("Kenneth Murray Jr.", 9, "LB", 27, "6'2\"", 241, 6,
"Oklahoma", "Active"));
roster.add(new Player("Marist Liufau", 44, "LB", 24, "6'2\"", 239, 2,
"Notre Dame", "Active"));
roster.add(new Player("Justin Barron", 48, "LB", 24, "6'4\"", 235, 0,
"Syracuse", "Active"));
roster.add(new Player("Shemar James", 52, "LB", 21, "6'1\"", 222, 0,
"Florida", "Active"));
roster.add(new Player("Isaiah Land", 53, "LB", 25, "6'4\"", 225, 3,
"Florida A&M", "Active"));
// CB
roster.add(new Player("Caelen Carson", 30, "CB", 23, "6'0\"", 195, 2, "Wake
Forest", "Active"));
roster.add(new Player("Trikweze Bridges", 7, "CB", 25, "6'2\"", 200, 0,
"Florida", "Active"));
roster.add(new Player("Shavon Revel Jr.", 2, "CB", 24, "6'1\"", 194, 0,
"East Carolina", "Active"));
roster.add(new Player("Corey Ballentine", 27, "CB", 29, "6'0\"", 191, 7,
"Washburn", "Active"));
roster.add(new Player("Josh Butler", 34, "CB", 29, "6'0\"", 182, 2,
"Michigan State", "Active"));
roster.add(new Player("C.J. Goodwin", 29, "CB", 35, "6'3\"", 190, 10,
"California (PA)", "Active"));
roster.add(new Player("Zion Childress", 22, "CB", 23, "5'11\"", 203, 0,
"Kentucky", "Active"));
roster.add(new Player("Reddy Steward", 25, "CB", 24, "5'11\"", 178, 1,
"Troy", "Active"));
// S
roster.add(new Player("Malik Hooker", 28, "S", 29, "6'2\"", 212, 9, "Ohio
State", "Active"));
roster.add(new Player("Donovan Wilson", 6, "S", 30, "6'0\"", 204, 7, "Texas
A&M", "Active"));
roster.add(new Player("Markquese Bell", 23, "S", 27, "6'3\"", 205, 4,
"Florida A&M", "Active"));
roster.add(new Player("Julius Wood", 26, "S", 24, "6'1\"", 193, 2, "East
Carolina", "Active"));
roster.add(new Player("Alijah Clark", 33, "S", 22, "6'1\"", 187, 0,
"Syracuse", "Active"));
// Special Teams
roster.add(new Player("Brandon Aubrey", 17, "PK", 30, "6'3\"", 218, 3,
"Notre Dame", "Active"));
roster.add(new Player("Bryan Anger", 5, "P", 37, "6'3\"", 202, 14,
"California", "Active"));
roster.add(new Player("Trent Sieg", 49, "LS", 30, "6'3\"", 240, 8,
"Colorado State", "Active"));
// Injured Reserve
roster.add(new Player("Malik Davis", 28, "RB", 27, "5'11\"", 202, 2,
"Florida", "Injured"));
roster.add(new Player("Miles Sanders", 25, "RB", 28, "5'11\"", 211, 7,
"Penn State", "Injured"));
roster.add(new Player("Javonte Williams", 35, "RB", 25, "5'10\"", 220, 5,
"North Carolina", "Injured"));
roster.add(new Player("T.J. Bass", 62, "G", 26, "6'4\"", 325, 3, "Oregon",
"Injured"));
roster.add(new Player("Rob Jones", 74, "G", 26, "6'4\"", 322, 5, "Middle
Tennessee", "Injured"));
roster.add(new Player("Trevor Keegan", 67, "G", 25, "6'5\"", 306, 2,
"Michigan", "Injured"));
roster.add(new Player("Ajani Cornelius", 77, "OT", 23, "6'4\"", 310, 0,
"Oregon", "Injured"));
roster.add(new Player("Tyler Guyton", 72, "OT", 24, "6'7\"", 322, 2,
"Oklahoma", "Injured"));
roster.add(new Player("Payton Turner", 91, "DE", 27, "6'6\"", 270, 5,
"Houston", "Injured"));
roster.add(new Player("Jack Sanborn", 47, "LB", 25, "6'2\"", 234, 4,
"Wisconsin", "Injured"));
roster.add(new Player("DaRon Bland", 26, "CB", 26, "6'0\"", 197, 4, "Fresno
State", "Injured"));
roster.add(new Player("Juanyeh Thomas", 21, "S", 25, "6'3\"", 217, 3,
"Georgia Tech", "Injured"));
}
private void initializeCoaches() {
coaches = new ArrayList<>();
coaches.add(new Coach("Brian Schottenheimer", "Head Coach", 51, "5'10\"", 190,
20, "Kansas"));
coaches.add(new Coach("Klayton Adams", "Offensive Coordinator", 41, "5'11\"",
195, 15, "Boise State"));
coaches.add(new Coach("Matt Eberflus", "Defensive Coordinator", 55, "6'1\"",
210, 25, "Toledo"));
coaches.add(new Coach("Nick Sorensen", "Special Teams Coach", 46, "6'3\"", 210,
12, "Virginia Tech"));
coaches.add(new Coach("Steve Shimko", "Quarterbacks Coach", 35, "6'2\"", 215,
8, "Rutgers"));
coaches.add(new Coach("Derek Foster", "Running Backs Coach", 40, "5'9\"", 185,
10, "South Carolina"));
coaches.add(new Coach("Junior Adams", "Wide Receivers Coach", 44, "5'10\"",
190, 15, "Boise State"));
coaches.add(new Coach("Connor Riley", "Offensive Line Coach", 39, "6'3\"", 260,
12, "Nebraska"));
coaches.add(new Coach("Lunda Wells", "Tight Ends Coach", 41, "6'1\"", 245, 14,
"Southern"));
coaches.add(new Coach("Aaron Whitecotton", "Defensive Line Coach", 42, "6'2\"",
250, 13, "South Carolina"));
coaches.add(new Coach("Dave Borgonzi", "Linebackers Coach", 42, "5'11\"", 200,
14, "Amherst"));
coaches.add(new Coach("David Overstreet II", "Secondary/Cornerbacks Coach", 36,
"5'11\"", 195, 9, "Missouri"));
}
private void initializeCheerleaders() {
cheerleaders = new ArrayList<>();
cheerleaders.add(new Cheerleader("Alyssa", 24, "Veteran Cheerleader", "5'6\"",
125, 4, "Texas A&M"));
cheerleaders.add(new Cheerleader("Brianna", 22, "Rookie Cheerleader", "5'5\"",
120, 1, "Oklahoma State"));
cheerleaders.add(new Cheerleader("Kelsey", 25, "Squad Leader", "5'7\"", 130, 5,
"University of Texas"));
}
private void initializeTrainers() {
trainers = new ArrayList<>();
trainers.add(new Staff("Jim Maurer", "Head Athletic Trainer", 58, "5'10\"",
185, 32, "Texas A&M"));
trainers.add(new Staff("Greg Gaither", "Assistant Trainer", 45, "5'9\"", 178,
18, "Oklahoma State"));
trainers.add(new Staff("Britt Brown", "Rehab Director", 50, "6'0\"", 190, 25,
"Texas Tech"));
}
private void initializeOtherStaff() {
otherStaff = new ArrayList<>();
otherStaff.add(new Staff("Jerry Jones", "Owner / GM", 82, "6'0\"", 200, 35,
"Arkansas"));
otherStaff.add(new Staff("Stephen Jones", "Executive VP", 60, "6'2\"", 210, 30,
"Arkansas"));
otherStaff.add(new Staff("Charlotte Jones", "Chief Brand Officer", 58, "5'8\"",
140, 28, "Stanford"));
}
private void createUI() {
// Top Panel - Search Controls
JPanel topPanel = new JPanel(new GridBagLayout());
topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
topPanel.setBackground(new Color(0, 34, 68)); // Cowboys blue
GridBagConstraints gbc = new GridBagConstraints();
gbc.insets = new Insets(5, 5, 5, 5);
gbc.fill = GridBagConstraints.HORIZONTAL;
// Title
JLabel titleLabel = new JLabel("Dallas Cowboys 2025 Roster Directory");
titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
titleLabel.setForeground(Color.WHITE);
gbc.gridx = 0;
gbc.gridy = 0;
gbc.gridwidth = 5;
topPanel.add(titleLabel, gbc);
// View Type (Players or Coaches)
gbc.gridwidth = 1;
gbc.gridy = 1;
gbc.gridx = 0;
JLabel viewTypeLabel = new JLabel("View:");
viewTypeLabel.setForeground(Color.WHITE);
viewTypeLabel.setFont(new Font("Arial", Font.BOLD, 12));
topPanel.add(viewTypeLabel, gbc);
gbc.gridx = 1;
viewTypeCombo = new JComboBox<>(new String[]{
"Players", "Coaches", "Cheerleaders", "Trainers", "Other Staff"
});
viewTypeCombo.addActionListener(e -> updateViewType());
topPanel.add(viewTypeCombo, gbc);
// Random Button
gbc.gridx = 2;
randomBtn = new JButton("🎲 Random");
randomBtn.setBackground(new Color(255, 215, 0)); // Gold
randomBtn.setForeground(Color.BLACK);
randomBtn.setFont(new Font("Arial", Font.BOLD, 13));
randomBtn.addActionListener(e -> showRandom());
topPanel.add(randomBtn, gbc);
// Search Type
gbc.gridy = 2;
gbc.gridx = 0;
JLabel searchTypeLabel = new JLabel("Search By:");
searchTypeLabel.setForeground(Color.WHITE);
topPanel.add(searchTypeLabel, gbc);
gbc.gridx = 1;
searchTypeCombo = new JComboBox<>(new String[]{"Name", "Position",
"Status", "All Players"});
searchTypeCombo.addActionListener(e -> updateSearchFields());
topPanel.add(searchTypeCombo, gbc);
// Name Field
gbc.gridx = 0;
gbc.gridy = 3;
JLabel nameLabel = new JLabel("Name:");
nameLabel.setForeground(Color.WHITE);
topPanel.add(nameLabel, gbc);
gbc.gridx = 1;
nameField = new JTextField(20);
topPanel.add(nameField, gbc);
// Position Dropdown
gbc.gridx = 2;
gbc.gridy = 3;
JLabel posLabel = new JLabel("Position:");
posLabel.setForeground(Color.WHITE);
topPanel.add(posLabel, gbc);
gbc.gridx = 3;
positionCombo = new JComboBox<>(new String[]{"All", "QB", "RB", "WR", "TE",
"C", "G", "OT",
"DE", "DT", "LB", "CB", "S", "PK", "P", "LS"});
positionCombo.setEnabled(false);
topPanel.add(positionCombo, gbc);
// Status Dropdown
gbc.gridx = 0;
gbc.gridy = 4;
JLabel statusLabel = new JLabel("Status:");
statusLabel.setForeground(Color.WHITE);
topPanel.add(statusLabel, gbc);
gbc.gridx = 1;
statusCombo = new JComboBox<>(new String[]{"All", "Active", "Injured"});
statusCombo.setEnabled(false);
topPanel.add(statusCombo, gbc);
// Search Button
gbc.gridx = 2;
gbc.gridwidth = 2;
searchBtn = new JButton("Search");
searchBtn.setBackground(new Color(134, 147, 151)); // Cowboys silver
searchBtn.setFont(new Font("Arial", Font.BOLD, 14));
searchBtn.addActionListener(e -> performSearch());
topPanel.add(searchBtn, gbc);
add(topPanel, BorderLayout.NORTH);
// Center Panel - Results Table
String[] columnNames = {"#", "Name", "Position/Role", "Age", "Height",
"Weight", "Exp", "College", "Status"};
tableModel = new DefaultTableModel(columnNames, 0) {
@Override
public boolean isCellEditable(int row, int column) {
return false;
}
};
rosterTable = new JTable(tableModel);
rosterTable.setFont(new Font("Arial", Font.PLAIN, 12));
rosterTable.setRowHeight(25);
rosterTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
rosterTable.getTableHeader().setBackground(new Color(0, 34, 68));
rosterTable.getTableHeader().setForeground(Color.WHITE);
JScrollPane scrollPane = new JScrollPane(rosterTable);
scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
add(scrollPane, BorderLayout.CENTER);
// Bottom Panel - Summary
resultArea = new JTextArea(3, 50);
resultArea.setEditable(false);
resultArea.setFont(new Font("Arial", Font.PLAIN, 12));
resultArea.setBackground(new Color(240, 240, 240));
resultArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
add(new JScrollPane(resultArea), BorderLayout.SOUTH);
// Load all players initially
performSearch();
}
private void updateViewType() {
String viewType = (String) viewTypeCombo.getSelectedItem();
boolean isPlayers = "Players".equals(viewType);
searchTypeCombo.setEnabled(isPlayers);
nameField.setEnabled(isPlayers);
positionCombo.setEnabled(false);
statusCombo.setEnabled(false);
switch (viewType) {
case "Players":
performSearch();
break;
case "Coaches":
displayCoaches();
break;
case "Cheerleaders":
displayCheerleaders();
break;
case "Trainers":
displayTrainers();
break;
case "Other Staff":
displayOtherStaff();
break;
}
}
private void updateSearchFields() {
String searchType = (String) searchTypeCombo.getSelectedItem();
nameField.setEnabled("Name".equals(searchType));
positionCombo.setEnabled("Position".equals(searchType));
statusCombo.setEnabled("Status".equals(searchType));
}
private void showRandom() {
String viewType = (String) viewTypeCombo.getSelectedItem();
if ("Coaches".equals(viewType)) {
// Random coach
Coach randomCoach = coaches.get(random.nextInt(coaches.size()));
tableModel.setRowCount(0);
tableModel.addRow(new Object[]{
"—", randomCoach.name, randomCoach.role, "—", "—", "—", "—", "—",
"—"
});
resultArea.setText("🎲 Random Coach: " + randomCoach.name + " (" +
randomCoach.role + ")");
} else {
// Random player based on current filters
String searchType = (String) searchTypeCombo.getSelectedItem();
List<Player> pool = new ArrayList<>();
switch (searchType) {
case "Position":
String position = (String) positionCombo.getSelectedItem();
if ("All".equals(position)) {
pool = new ArrayList<>(roster);
} else {
pool = roster.stream()
.filter(p -> p.position.equals(position))
.collect(java.util.stream.Collectors.toList());
}
break;
case "Status":
String status = (String) statusCombo.getSelectedItem();
if ("All".equals(status)) {
pool = new ArrayList<>(roster);
} else {
pool = roster.stream()
.filter(p -> p.status.equals(status))
.collect(java.util.stream.Collectors.toList());
}
break;
default:
pool = new ArrayList<>(roster);
break;
}
if (pool.isEmpty()) {
resultArea.setText("No players match the current filters!");
return;
}
Player randomPlayer = pool.get(random.nextInt(pool.size()));
tableModel.setRowCount(0);
tableModel.addRow(new Object[]{
randomPlayer.number, randomPlayer.name, randomPlayer.position,
randomPlayer.age,
randomPlayer.height, randomPlayer.weight, randomPlayer.experience,
randomPlayer.college, randomPlayer.status
});
resultArea.setText("🎲 Random Player: #" + randomPlayer.number + " " +
randomPlayer.name +
" - " + randomPlayer.position + " (" + randomPlayer.status + ")");
}
}
private void displayCoaches() {
tableModel.setRowCount(0);
for (Coach c : coaches) {
tableModel.addRow(new Object[]{
"—", c.name, c.role, c.age, c.height, c.weight, c.experience,
c.college, "—"
});
}
resultArea.setText(String.format("Coaching Staff: %d coaches",
coaches.size()));
}
private void displayCheerleaders() {
tableModel.setRowCount(0);
for (Cheerleader c : cheerleaders) {
tableModel.addRow(new Object[]{
"—", c.name, c.role, c.age, c.height, c.weight, c.experience, c.college, "—"
});
}
resultArea.setText("Cheerleaders: " + cheerleaders.size());
}
private void displayTrainers() {
tableModel.setRowCount(0);
for (Staff s : trainers) {
tableModel.addRow(new Object[]{
"—", s.name, s.role, s.age, s.height, s.weight, s.experience,
s.college, "—"
});
}
resultArea.setText("Trainers: " + trainers.size());
}
private void displayOtherStaff() {
tableModel.setRowCount(0);
for (Staff s : otherStaff) {
tableModel.addRow(new Object[]{
"—", s.name, s.role, s.age, s.height, s.weight, s.experience,
s.college, "—"
});
}
resultArea.setText("Other Staff: " + otherStaff.size());
}
private void performSearch() {
String viewType = (String) viewTypeCombo.getSelectedItem();
if ("Coaches".equals(viewType)) {
displayCoaches();
return;
}
tableModel.setRowCount(0);
String searchType = (String) searchTypeCombo.getSelectedItem();
List<Player> results = new ArrayList<>();
switch (searchType) {
case "Name":
String searchName = nameField.getText().trim().toLowerCase();
if (searchName.isEmpty()) {
resultArea.setText("Please enter a player name to search.");
return;
}
results = roster.stream()
.filter(p -> p.name.toLowerCase().contains(searchName))
.collect(java.util.stream.Collectors.toList());
break;
case "Position":
String position = (String) positionCombo.getSelectedItem();
if ("All".equals(position)) {
results = new ArrayList<>(roster);
} else {
results = roster.stream()
.filter(p -> p.position.equals(position))
.collect(java.util.stream.Collectors.toList());
}
break;
case "Status":
String status = (String) statusCombo.getSelectedItem();
if ("All".equals(status)) {
results = new ArrayList<>(roster);
} else {
results = roster.stream()
.filter(p -> p.status.equals(status))
.collect(java.util.stream.Collectors.toList());
}
break;
case "All Players":
results = new ArrayList<>(roster);
break;
}
// Populate table
for (Player p : results) {
tableModel.addRow(new Object[]{
p.number, p.name, p.position, p.age, p.height, p.weight,
p.experience, p.college, p.status
});
}
// Update summary
int active = (int) results.stream().filter(p ->
"Active".equals(p.status)).count();
int injured = (int) results.stream().filter(p ->
"Injured".equals(p.status)).count();
resultArea.setText(String.format("Found %d player(s) | Active: %d |
Injured: %d",
results.size(), active, injured));
}
// Player class
static class Player {
String name, position, height, college, status;
int number, age, weight, experience;
Player(String name, int number, String position, int age, String height,
int weight,
int experience, String college, String status) {
this.name = name;
this.number = number;
this.position = position;
this.age = age;
this.height = height;
this.weight = weight;
this.experience = experience;
this.college = college;
this.status = status;
}
}
// Coach class
// Coach class
static class Coach {
String name, role, height, college;
int age, weight, experience;
Coach(String name, String role, int age, String height, int weight, int
experience, String college) {
this.name = name;
this.role = role;
this.age = age;
this.height = height;
this.weight = weight;
this.experience = experience;
this.college = college;
}
}
// Cheerleader class
static class Cheerleader {
String name, role, height, college;
int age, weight, experience;
Cheerleader(String name, int age, String role, String height, int weight, int
experience, String college) {
this.name = name;
this.age = age;
this.role = role;
this.height = height;
this.weight = weight;
this.experience = experience;
this.college = college;
}
}
// Staff class (trainers, medical, equipment, etc.)
static class Staff {
String name, role, height, college;
int age, weight, experience;
Staff(String name, String role, int age, String height, int weight, int
experience, String college) {
this.name = name;
this.role = role;
this.age = age;
this.height = height;
this.weight = weight;
this.experience = experience;
this.college = college;
}
}
public static void main(String[] args) {
SwingUtilities.invokeLater(() -> new CowboysRosterDirectoryFull());
}
}
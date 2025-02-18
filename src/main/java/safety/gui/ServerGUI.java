package safety.gui;

import safety.server.FirewallServer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerGUI extends JFrame {
    private JTextArea logArea;
    private JLabel statusLabel;
    private ConcurrentLinkedQueue<String> logBuffer;
    private int activeConnections = 0;
    private int blockedClients = 0;

    private JPanel ddosPanel;
    private JPanel mitmPanel;
    private JLabel ddosStatusLabel;
    private JLabel mitmStatusLabel;
    private Timer ddosVisualTimer;
    private Timer mitmVisualTimer;
    private Color defaultBackground;
    private JProgressBar securityBar;
    private JLabel threatLabel;
    private Timer blinkTimer;
    private JPanel securityPanel;
    private JPanel attackPanel;
    private JLabel securityStatus;

    public ServerGUI() {
        setTitle("Firewall Server Monitor - Socket Fire");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Main layout
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        defaultBackground = new Color(32, 33, 36);
        getContentPane().setBackground(defaultBackground);

        // Security Status Panel
        createSecurityPanel();

        // Log Panel
        createLogPanel();

        // Status Bar
        createStatusBar();

        logBuffer = new ConcurrentLinkedQueue<>();
        blinkTimer = new Timer(500, e -> blinkSecurityAlert(false));

        startLogUpdates();
    }

    private void createSecurityPanel() {
        securityPanel = new JPanel();
        securityPanel.setLayout(new GridLayout(3, 1, 5, 5));
        securityPanel.setBackground(defaultBackground);
        securityPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GREEN),
                "Security Status",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.GREEN
        ));

        // Security Status
        securityStatus = new JLabel("🟢 System Active - Monitoring Network", SwingConstants.CENTER);
        securityStatus.setForeground(Color.GREEN);
        securityStatus.setFont(new Font("Arial", Font.BOLD, 14));

        // Threat Level
        threatLabel = new JLabel("Current Threat Level: Normal", SwingConstants.CENTER);
        threatLabel.setForeground(Color.GREEN);

        // Security Bar
        securityBar = new JProgressBar(0, 100);
        securityBar.setValue(100);
        securityBar.setStringPainted(true);
        securityBar.setString("Security Status: Normal");
        securityBar.setForeground(Color.GREEN);

        securityPanel.add(securityStatus);
        securityPanel.add(threatLabel);
        securityPanel.add(securityBar);

        add(securityPanel, BorderLayout.NORTH);
    }

    private void createAttackPanel() {
        attackPanel = new JPanel();
        attackPanel.setLayout(new GridLayout(5, 1, 5, 5));
        attackPanel.setBackground(defaultBackground);
        attackPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GREEN),
                "Attack Monitoring",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.GREEN
        ));

        // Add attack type labels
        JLabel ddosLabel = createAttackLabel("DDoS Protection: Active");
        JLabel httpFloodLabel = createAttackLabel("HTTP Flood Protection: Active");
        JLabel slowlorisLabel = createAttackLabel("Slowloris Protection: Active");
        JLabel mitmLabel = createAttackLabel("MITM Protection: Active");
        JLabel podLabel = createAttackLabel("Ping of Death Protection: Active");

        attackPanel.add(ddosLabel);
        attackPanel.add(httpFloodLabel);
        attackPanel.add(slowlorisLabel);
        attackPanel.add(mitmLabel);
        attackPanel.add(podLabel);

        add(attackPanel, BorderLayout.EAST);
    }

    private JLabel createAttackLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.GREEN);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }


    private void createLogPanel() {
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(18, 18, 18));
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createStatusBar() {
        statusLabel = new JLabel("🔒 Firewall Active | Connections: 0 | Blocked: 0");
        statusLabel.setForeground(Color.GREEN);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void updateSecurityStatus(String status, Color color) {
        SwingUtilities.invokeLater(() -> {
            securityStatus.setText(status);
            securityStatus.setForeground(color);
            securityBar.setString(status);
            securityBar.setForeground(color);
        });
    }

    private Timer createVisualTimer(JPanel panel) {
        return new Timer(500, e -> {
            if (panel.getBackground().equals(Color.RED)) {
                panel.setBackground(Color.YELLOW);
            } else {
                panel.setBackground(Color.RED);
            }
        });
    }

    public void log(String message) {
        String timestamp = String.format("[%tT] ", System.currentTimeMillis());
        logBuffer.add(timestamp + message);
    }

    public void logSecurityEvent(String message, boolean isWarning) {
        String prefix = isWarning ? "⚠️ WARNING: " : "ℹ️ INFO: ";
        log(prefix + message);
        if (isWarning) {
            blinkSecurityAlert(true);
        }
    }

    private void blinkSecurityAlert(boolean isActive) {
        SwingUtilities.invokeLater(() -> {
            if (isActive) {
                securityPanel.setBackground(Color.RED);
                threatLabel.setText("⚠️ THREAT DETECTED - Active Response");
                threatLabel.setForeground(Color.RED);
                securityBar.setValue(25);
                securityBar.setString("THREAT DETECTED");
                securityBar.setForeground(Color.RED);
            } else {
                securityPanel.setBackground(defaultBackground);
                threatLabel.setText("Current Threat Level: Normal");
                threatLabel.setForeground(Color.GREEN);
                securityBar.setValue(100);
                securityBar.setString("Security Status: Normal");
                securityBar.setForeground(Color.GREEN);
            }
        });
    }

    public void updateDDoSStatus(boolean underAttack, String attackerIP) {
        SwingUtilities.invokeLater(() -> {
            if (underAttack) {
                // Update security panel
                securityStatus.setText("🚨 DDoS ATTACK IN PROGRESS - Blocking " + attackerIP);
                securityStatus.setForeground(Color.RED);

                // Update threat level
                threatLabel.setText("⚠️ CRITICAL - DDoS Attack in Progress");
                threatLabel.setForeground(Color.RED);

                // Update security bar
                securityBar.setValue(0);
                securityBar.setString("DDoS ATTACK DETECTED");
                securityBar.setForeground(Color.RED);

                // Start blinking effect
                blinkSecurityAlert(true);

                // Log the event
                logSecurityEvent("DDoS Attack detected from " + attackerIP, true);

                // Schedule reset after 10 seconds if we don't get another attack notification
                Timer resetTimer = new Timer(10000, e -> {
                    if (!FirewallServer.getDdosProtector().isAttacking(attackerIP)) {
                        blinkSecurityAlert(false);
                    }
                });
                resetTimer.setRepeats(false);
                resetTimer.start();
            } else {
                // Only reset visuals if there are no other attacks happening
                if (!FirewallServer.getHttpFloodProtector().isAttacking(attackerIP)) {
                    securityStatus.setText("🟢 System Active - Monitoring Network");
                    securityStatus.setForeground(Color.GREEN);
                    threatLabel.setText("Current Threat Level: Normal");
                    threatLabel.setForeground(Color.GREEN);
                    securityBar.setValue(100);
                    securityBar.setString("Security Status: Normal");
                    securityBar.setForeground(Color.GREEN);
                    blinkSecurityAlert(false);
                }
                logSecurityEvent("DDoS Attack from " + attackerIP + " has stopped", false);
            }
        });
    }

    public void updateHTTPFloodStatus(boolean detected, String attackerIP) {
        SwingUtilities.invokeLater(() -> {
            if (detected) {
                // Update security panel
                securityStatus.setText("🚨 HTTP FLOOD ATTACK DETECTED - Blocking " + attackerIP);
                securityStatus.setForeground(Color.RED);

                // Update threat level
                threatLabel.setText("⚠️ CRITICAL - HTTP Flood Attack in Progress");
                threatLabel.setForeground(Color.RED);

                // Update security bar
                securityBar.setValue(0);
                securityBar.setString("HTTP FLOOD ATTACK DETECTED");
                securityBar.setForeground(Color.RED);

                // Start blinking effect
                blinkSecurityAlert(true);

                // Log the event
                logSecurityEvent("HTTP Flood Attack detected from " + attackerIP, true);

                // Schedule reset after 10 seconds
                Timer resetTimer = new Timer(10000, e -> blinkSecurityAlert(false));
                resetTimer.setRepeats(false);
                resetTimer.start();
            } else {
                // Reset the visual indicators if the attack is over
                securityStatus.setText("🟢 System Active - Monitoring Network");
                securityStatus.setForeground(Color.GREEN);
                threatLabel.setText("Current Threat Level: Normal");
                threatLabel.setForeground(Color.GREEN);
                securityBar.setValue(100);
                securityBar.setString("Security Status: Normal");
                securityBar.setForeground(Color.GREEN);
                blinkSecurityAlert(false);
                logSecurityEvent("HTTP Flood Attack from " + attackerIP + " has stopped", false);
            }
        });
    }


    public void updatePoDStatus(boolean detected, String attackerIP) {
        SwingUtilities.invokeLater(() -> {
            if (detected) {
                // Update security panel
                securityStatus.setText("🚨 PING OF DEATH ATTACK DETECTED - Blocking " + attackerIP);
                securityStatus.setForeground(Color.RED);

                // Update threat level
                threatLabel.setText("⚠️ CRITICAL - Ping of Death Attack in Progress");
                threatLabel.setForeground(Color.RED);

                // Update security bar
                securityBar.setValue(0);
                securityBar.setString("PING OF DEATH ATTACK DETECTED");
                securityBar.setForeground(Color.RED);

                // Start blinking effect
                blinkSecurityAlert(true);

                // Log the event
                logSecurityEvent("Ping of Death Attack detected from " + attackerIP, true);

                // Schedule reset after 10 seconds
                Timer resetTimer = new Timer(10000, e -> {
                    if (!FirewallServer.getPodProtector().isAttacking(attackerIP)) {
                        blinkSecurityAlert(false);
                    }
                });
                resetTimer.setRepeats(false);
                resetTimer.start();
            } else {
                // Reset visual indicators if no other attacks are happening
                if (!FirewallServer.getDdosProtector().isAttacking(attackerIP) &&
                        !FirewallServer.getHttpFloodProtector().isAttacking(attackerIP)) {
                    securityStatus.setText("🟢 System Active - Monitoring Network");
                    securityStatus.setForeground(Color.GREEN);
                    threatLabel.setText("Current Threat Level: Normal");
                    threatLabel.setForeground(Color.GREEN);
                    securityBar.setValue(100);
                    securityBar.setString("Security Status: Normal");
                    securityBar.setForeground(Color.GREEN);
                    blinkSecurityAlert(false);
                }
                logSecurityEvent("Ping of Death Attack from " + attackerIP + " has stopped", false);
            }
        });
    }


    private void startLogUpdates() {
        Thread logThread = new Thread(() -> {
            while (true) {
                while (!logBuffer.isEmpty()) {
                    String log = logBuffer.poll();
                    SwingUtilities.invokeLater(() -> {
                        logArea.append(log + "\n");
                        logArea.setCaretPosition(logArea.getDocument().getLength());
                    });
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        logThread.setDaemon(true);
        logThread.start();
    }

    public void updateMITMStatus(boolean detected, String attackerIP) {
        SwingUtilities.invokeLater(() -> {
            if (detected) {
                // Update security panel
                securityStatus.setText("🚨 MITM ATTACK DETECTED - Blocking " + attackerIP);
                securityStatus.setForeground(Color.RED);

                // Update threat level
                threatLabel.setText("⚠️ CRITICAL - MITM Attack in Progress");
                threatLabel.setForeground(Color.RED);

                // Update security bar
                securityBar.setValue(0);
                securityBar.setString("MITM ATTACK DETECTED");
                securityBar.setForeground(Color.RED);

                // Start blinking effect
                blinkSecurityAlert(true);

                // Log the event
                logSecurityEvent("MITM Attack detected from " + attackerIP, true);

                // Schedule reset after 10 seconds
                Timer resetTimer = new Timer(10000, e -> blinkSecurityAlert(false));
                resetTimer.setRepeats(false);
                resetTimer.start();
            }
        });
    }

    private void updateSlowlorisPanel() {
        JPanel slowlorisPanel = new JPanel();
        slowlorisPanel.setLayout(new BorderLayout());
        slowlorisPanel.setBackground(defaultBackground);
        slowlorisPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GREEN),
                "Slowloris Protection",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                null,
                Color.GREEN
        ));
    }

    public void updateSlowlorisStatus(boolean underAttack, int connectionCount) {
        SwingUtilities.invokeLater(() -> {
            if (underAttack) {
                securityStatus.setText("🚨 SLOWLORIS ATTACK IN PROGRESS - " + connectionCount + " slow connections");
                securityStatus.setForeground(Color.RED);
                threatLabel.setText("⚠️ CRITICAL - Slowloris Attack Detected");
                threatLabel.setForeground(Color.RED);
                securityBar.setValue(0);
                securityBar.setString("SLOWLORIS ATTACK DETECTED");
                securityBar.setForeground(Color.RED);
                blinkSecurityAlert(true);
            }
        });
    }


    public synchronized void incrementBlockedClients() {
        blockedClients++;
        updateStatusLabel();
    }

    public synchronized void updateConnectionStatus(int connections) {
        this.activeConnections = connections;
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(String.format("🔒 Firewall Active | Connections: %d | Blocked: %d",
                    activeConnections, blockedClients));
        });
    }

    private void updateAttackStatus(JPanel panel, JLabel label, Timer timer,
                                    String attackMsg, String clearMsg, boolean underAttack) {
        SwingUtilities.invokeLater(() -> {
            if (underAttack) {
                label.setText("⚠️ " + attackMsg);
                timer.start();

                Timer clearTimer = new Timer(10000, e -> {
                    label.setText(clearMsg);
                    panel.setBackground(defaultBackground);
                    timer.stop();
                });
                clearTimer.setRepeats(false);
                clearTimer.start();
            } else {
                label.setText(clearMsg);
                panel.setBackground(defaultBackground);
                timer.stop();
            }
        });
    }
}
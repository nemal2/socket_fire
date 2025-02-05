package safety.gui;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerGUI extends JFrame {
    private JTextArea logArea;
    private JLabel statusLabel;
    private ConcurrentLinkedQueue<String> logBuffer;
    private int activeConnections = 0;
    private int blockedClients = 0;

    public ServerGUI() {
        setTitle("Firewall Server Monitor - Hacker Mode");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set hacker mode theme
        getContentPane().setBackground(Color.BLACK);
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBackground(Color.BLACK);
        add(scrollPane, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Server Status: Running | Active Connections: 0 | Blocked Clients: 0");
        statusLabel.setForeground(Color.GREEN);
        statusLabel.setBackground(Color.BLACK);
        statusLabel.setOpaque(true);
        add(statusLabel, BorderLayout.SOUTH);

        logBuffer = new ConcurrentLinkedQueue<>();

        // Start real-time log updates
        startLogUpdates();
    }

    private void startLogUpdates() {
        Thread logThread = new Thread(() -> {
            while (true) {
                if (!logBuffer.isEmpty()) {
                    String log = logBuffer.poll();
                    SwingUtilities.invokeLater(() -> {
                        logArea.append(log + "\n");
                        updateStatus(log);
                    });
                }
                try {
                    Thread.sleep(500); // Update every 500ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        logThread.start();
    }

    private void updateStatus(String log) {
        if (log.contains("Accepted connection")) {
            activeConnections++;
        } else if (log.contains("Blocked blacklisted client") || log.contains("Rate limit exceeded")) {
            blockedClients++;
            activeConnections--;
        } else if (log.contains("Error handling client")) {
            activeConnections--;
        }
        statusLabel.setText("Server Status: Running | Active Connections: " + activeConnections + " | Blocked Clients: " + blockedClients);
    }

    public void log(String message) {
        logBuffer.add(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI gui = new ServerGUI();
            gui.setVisible(true);
        });
    }
}
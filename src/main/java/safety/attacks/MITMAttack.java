// MITMAttack.java
package safety.attacks;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

public class MITMAttack {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        System.out.println("🚀 Starting MITM Attack Simulation");
        System.out.println("=================================");

        try {
            System.out.println("📡 Connecting to server...");
            Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Initial connection
            System.out.println("🤝 Establishing initial connection...");
            out.println("HELLO_SERVER");
            String response = in.readLine();
            System.out.println("📥 Server response: " + response);

            // MITM attack patterns
            String[] attackPatterns = {
                    "MITM:CERT_INTERCEPT:" + Base64.getEncoder().encodeToString("FAKE_CERT".getBytes()),
                    "PROXY:SSL_STRIP:" + Base64.getEncoder().encodeToString("STRIP_SSL".getBytes()),
                    "CERT:FORGE:" + Base64.getEncoder().encodeToString("FORGE_CERT".getBytes())
            };

            System.out.println("\n🔄 Starting attack sequence...");
            for (int i = 0; i < attackPatterns.length; i++) {
                System.out.println("\n📤 Sending attack pattern " + (i + 1) + "/" + attackPatterns.length);
                System.out.println("Pattern: " + attackPatterns[i]);
                out.println(attackPatterns[i]);

                try {
                    response = in.readLine();
                    if (response != null) {
                        System.out.println("📥 Server response: " + response);
                        if (response.contains("BLOCKED")) {
                            System.out.println("\n❌ Attack detected and blocked by server!");
                            System.out.println("Attack simulation completed - Server successfully detected MITM attempt");
                            break;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("\n❌ Connection terminated by server - Attack detected!");
                    System.out.println("Attack simulation completed - Server successfully blocked MITM attempt");
                    break;
                }

                Thread.sleep(1000);
            }

            socket.close();

        } catch (IOException | InterruptedException e) {
            System.out.println("\n❌ Attack simulation failed: " + e.getMessage());
        }
    }
}
package firewall;

import java.io.*;
import java.net.*;

public class GoodClient {
    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 12345;

        try (Socket socket = new Socket(hostname, port)) {
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));

            String text;
            do {
                System.out.print("Enter message: ");
                text = consoleReader.readLine();
                writer.println(text);

                String response = reader.readLine();
                System.out.println("Server response: " + response);
            } while (!text.equals("exit"));
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
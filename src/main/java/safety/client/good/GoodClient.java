package safety.client.good;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GoodClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("127.0.0.1", 5000);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("Hello from Good Client!");
            System.out.println("Server response: " + in.readLine());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
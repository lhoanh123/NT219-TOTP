import java.io.*;
import java.net.Socket;

import TOTP.TOTP;

import java.net.ServerSocket;

public class server {
    public static void main(String argv[]) throws Exception {
        // Seed for HMAC-SHA256 - 32 bytes
        String seed = "3132333435363738393031323334353637383930" + "313233343536373839303132";

        // Tạo socket server, chờ tại cổng '8080'
        ServerSocket welcomeSocket = new ServerSocket(8080);
        System.out.println("Server listening on 8080");

        int id = 0;

        while (true) {

            String sentence_from_client;
            String sentence_to_client;

            // chờ yêu cầu từ client
            Socket connectionSocket = welcomeSocket.accept();

            // Tạo input stream, nối tới Socket
            // đọc dữ liệu
            BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));

            // Tạo outputStream, nối tới socket
            // ghi dữ liệu
            DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());

            // Đọc thông tin từ socket
            sentence_from_client = inFromClient.readLine();

            // tạo mã TOTP
            String totp = TOTP.TOTP_now(seed, "6", "HmacSHA256");
            System.out.println(totp + "\n");

            while (!sentence_from_client.equals(totp)) {
                if (!sentence_from_client.equals("timeout")) {
                    sentence_to_client = "false, " + id + "\n";
                    id++;
                    outToClient.writeBytes(sentence_to_client);
                    // Đọc thông tin từ socket
                    sentence_from_client = inFromClient.readLine();
                } else {
                    totp = TOTP.TOTP_now(seed, "6", "HmacSHA1");
                    System.out.println(totp + "\n");
                    sentence_from_client = inFromClient.readLine();
                }
            }

            sentence_to_client = "true, " + id + "\n";
            System.out.println("client disconnect");
            outToClient.writeBytes(sentence_to_client);
        }
    }
}
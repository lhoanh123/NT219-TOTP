import javax.swing.*;

import TOTP.TOTP;

import java.awt.*;
import java.awt.event.*;
import java.net.Socket;
import java.io.*;

import java.util.ArrayList;
import java.util.List;

public class client implements ActionListener {

    private static JLabel label;
    private static JLabel result;
    private static JTextField textField;
    private static JLabel count_label;

    // hàm khởi tạo của lớp client. Trong hàm này, giao diện đồ họa (GUI) của client được thiết lập 
    public client() {
        JFrame frame = new JFrame();

        Font font = new Font("Bitter", Font.PLAIN ,16);   

        JButton button = new JButton("Send");
        button.setBounds(80, 230, 80, 30);

        JLabel OTPlabel = new JLabel("TOTP: ");
        OTPlabel.setBounds(50, 30, 60, 30);
        OTPlabel.setForeground(Color.BLACK);
        
        label = new JLabel();
        label.setBounds(100, 30, 60, 30);
        label.setForeground(Color.BLUE);

        result = new JLabel();
        result.setBounds(50, 190, 160, 30);

        button.addActionListener(this);

        count_label = new JLabel();
        count_label.setBounds(50, 80, 100, 30);
        count_label.setForeground(Color.MAGENTA);

        textField = new JTextField();
        textField.setBounds(50, 150, 150, 20);

        result.setFont(font);
        label.setFont(font);
        button.setFont(font);
        OTPlabel.setFont(font);
        textField.setFont(font);
        count_label.setFont(font);

        frame.add(result);
        frame.add(label);
        frame.add(button);
        frame.add(OTPlabel);
        frame.add(textField);
        frame.add(count_label);
        frame.setSize(300, 350);
        frame.setLayout(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }


    // secret key
    private static final String seed = "3132333435363738393031323334353637383930" + "313233343536373839303132";
    private static String currentTOTP;
    private static boolean isTimeout = false; // Biến flag để kiểm tra time out
    private static boolean isSuccess = false; // Biến flag để dừng đếm ngược

    /* Đây là một lớp con được định nghĩa bên trong lớp client. 
    Lớp này kế thừa từ lớp Thread và được sử dụng để thực hiện đếm ngược. 
    Trong phương thức run(), một vòng lặp vô hạn được thực hiện để đếm thời gian. 
    Khi thời gian đạt đến 30 giây, biến isTimeout được đặt là true, và một tin nhắn "timeout" được gửi tới server thông qua socket. */
    private static class CountdownThread extends Thread {
        @Override
        public void run() {
            while (true) {
                long startTime = System.currentTimeMillis(); // Thời gian bắt đầu

                while (!isTimeout && !isSuccess) {
                    // dừng 1 giây
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // lấy thời gian hiện tại
                    long currentTime = System.currentTimeMillis();

                    // hiển thị thời gian lên giao diện người dùng
                    count_label.setText(String.valueOf(30 - (currentTime - startTime) / 1000));

                    // kiểm tra xem đã đủ 30 giây chưa
                    if (currentTime - startTime >= 30000) {
                        isTimeout = true; // Đánh dấu là đã đến thời gian timeout
                        startTime = currentTime; // Cập nhật thời gian bắt đầu mới

                        // Tạo mã TOTP mới
                        currentTOTP = TOTP.TOTP_now(seed, "6", "HmacSHA256");

                        // hiển thị mã TOTP mới lên giao diện của người dùng
                        label.setText(currentTOTP);

                        // Gửi tin nhắn "timeout" tới server
                        try {
                            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
                            outToServer.writeBytes("timeout\n");
                            outToServer.flush(); // Đảm bảo gửi từ buffer ngay lập tức
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // xóa nội dung của textField và gọi hàm sendButton để tiếp tục gửi mã TOTP
                        textField.setText(null);
                        sendButton();
                    }
                }

                isTimeout = false; // Đánh dấu là đã gửi tin nhắn "timeout", chuẩn bị bắt đầu lại đếm ngược
            }
        }
    }

    /* Đây là một lớp con được định nghĩa bên trong lớp client. 
    Lớp này kế thừa từ lớp Thread và được sử dụng để xử lý phản hồi từ server. 
    Trong phương thức run(), một vòng lặp được thực hiện để đọc các phản hồi từ server thông qua BufferedReader. 
    Nếu phản hồi là "true", tức là thành công, một thông báo "Success!" được hiển thị và chương trình kết thúc. 
    Nếu phản hồi là "false", tức là thất bại, một thông báo "Failed!" được hiển thị và gọi hàm sendButton(). */
    private static class ResponseThread extends Thread {
        private final Socket clientSocket;

        // Đây là hàm khởi tạo của lớp ResponseThread. 
        // Nó nhận một tham số là clientSocket, là một đối tượng Socket đại diện cho kết nối giữa client và server.
        public ResponseThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                // danh sách để lưu trữ các ID nhận được từ server.
                List<String> ids = new ArrayList<>();

                // đọc dữ liệu từ server thông qua luồng đầu vào của clientSocket.
                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String response;

                // Vòng lặp này đọc từng dòng dữ liệu từ server thông qua BufferedReader cho đến khi không còn dữ liệu nào được đọc.
                while ((response = inFromServer.readLine()) != null) {
                    //  Dòng này tách chuỗi response thành một mảng các chuỗi bằng cách sử dụng dấu phẩy (,) làm dấu phân cách.
                    String[] data = response.split(",");

                    /* Nếu phần tử đầu tiên trong mảng data là chuỗi "true", có nghĩa là phản hồi từ server là thành công, 
                    trong trường hợp này, một thông báo "Success!" được hiển thị trên giao diện, sau đó thoát khỏi chương trình */
                    if (data[0].equals("true")) {
                        isSuccess = true;
                        result.setText("Success!");
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException i) {
                            i.printStackTrace();
                        }
                        System.exit(0);
                        break;
                    } 
                    
                    /* Nếu phần tử đầu tiên trong mảng data là chuỗi "false" và ids không chứa phần tử thứ hai trong mảng data, 
                    có nghĩa là phản hồi từ server là thất bại. Trong trường hợp này, một thông báo "Failed!" được hiển thị trên giao diện,
                    hàm sendButton() được gọi để gửi lại dữ liệu từ textField, 
                    và dòng tiếp theo (response = inFromServer.readLine()) sẽ đọc một dòng dữ liệu mới từ server. */
                    else if (data[0].equals("false") && !(ids.contains(data[1]))) {
                        result.setText("Failed!");
                        textField.setText(null);
                        sendButton();
                        response = inFromServer.readLine();
                    }
                    ids.add(data[1]);
                }

                // Khi kết thúc vòng lặp, đóng kết nối socket
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Socket clientSocket;

    // một đối tượng client được tạo, kết nối với server thông qua socket, và hai luồng con (CountdownThread và ResponseThread) được khởi động.
    public static void main(String argv[]) throws Exception {
        new client();

        // Tạo socket để client kết nối đến server qua địa chỉ IP và số cổng
        clientSocket = new Socket("127.0.0.1", 8080);

        currentTOTP = TOTP.TOTP_now(seed, "6", "HmacSHA256");

        label.setText(currentTOTP);

        // Tạo và bắt đầu luồng phản hồi từ server
        ResponseThread responseThread = new ResponseThread(clientSocket);
        responseThread.start();

        // Tạo và bắt đầu luồng đếm ngược
        CountdownThread countdownThread = new CountdownThread();
        countdownThread.start();

    }

    /* Đây là một phương thức tĩnh (static) được sử dụng để gửi dữ liệu từ client tới server. 
    Phương thức này đọc chuỗi ký tự từ textField và gửi chuỗi đó tới server thông qua DataOutputStream đã được nối với socket. */
    public static void sendButton() {
        try {
            try {
                Thread.sleep(100);
            } catch (InterruptedException i) {
                i.printStackTrace();
            }

            if (!isTimeout) {
                String sentenceToServer = textField.getText();

                // Tạo OutputStream nối với Socket (gửi dữ liệu tới server)
                DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

                // Gửi chuỗi ký tự tới Server thông qua OutputStream đã nối với Socket
                outToServer.writeBytes(sentenceToServer + '\n');
                outToServer.flush(); // Đảm bảo gửi từ buffer ngay lập tức
            }
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /* Đây là phương thức được triệu gọi khi sự kiện ActionEvent xảy ra. 
    Trong trường hợp này, phương thức này được gọi khi nút button được nhấn. Phương thức này */
    public void actionPerformed(ActionEvent e) {
        try {
            sendButton();
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }
}

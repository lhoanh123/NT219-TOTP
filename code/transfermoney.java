import javax.swing.*;
import java.awt.*;

public class transfermoney {

    private static JTextField textField;

    public void display() {
        JFrame frame = new JFrame();

        Font font = new Font("Bitter", Font.PLAIN, 16);

        JButton button = new JButton("Send");
        button.setBounds(80, 230, 80, 30);

        JLabel OTPlabel = new JLabel("Nhập số lượng tiền muốn chuyển: ");
        OTPlabel.setBounds(20, 30, 270, 30);
        OTPlabel.setForeground(Color.BLACK);

        textField = new JTextField();
        textField.setBounds(50, 150, 150, 20);

        button.setFont(font);
        OTPlabel.setFont(font);
        textField.setFont(font);

        frame.add(button);
        frame.add(OTPlabel);
        frame.add(textField);
        frame.setSize(300, 350);
        frame.setLayout(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        // Xử lý sự kiện khi button được nhấn
        button.addActionListener(e -> {
            try {
                // Tạo và hiển thị client
                client.main(null);
            } catch (Exception ex) {
                System.out.println(ex);
            }
            frame.dispose();
        });

    }

    public static void main(String[] args) {
        transfermoney transfermoney = new transfermoney();
        transfermoney.display();
    }
}

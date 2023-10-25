import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Random;

public class Client extends JFrame {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    private JProgressBar progressBar;
    private JButton button;
    private JLabel infoLabel;
    private boolean waitingForStart = false;

    public static void main(String[] args) throws IOException {
        String team = JOptionPane.showInputDialog("Enter team (L / R):");
        Random rand = new Random();
        int id = rand.nextInt(1000000000);
        new Client(team, Integer.toString(id));
    }

    public Client(String team, String id) throws IOException {
        try {
            socket = new Socket("localhost", 1245);
            dis = new DataInputStream(socket.getInputStream());
            dos = new DataOutputStream(socket.getOutputStream());
            dos.writeUTF(team + id);

            initGUI(team);
            startListening();
        } catch (NullPointerException e) {
            // Send a special message to the server to indicate that this client is cancelling
            try {
                if (dos != null) {
                    dos.writeUTF("cancel");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            // Close the socket
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    private void initGUI(String team) {
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(50);
        button = new JButton("Pull!");
        infoLabel = new JLabel("Dajesz malina!");

        button.addActionListener(e -> {
            try {
                if (!waitingForStart) {
                    dos.writeBoolean(true);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        this.setLayout(new FlowLayout());
        this.add(progressBar);
        this.add(button);
        this.add(infoLabel);

        this.setTitle(team);
        this.setSize(400, 200);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    private void startListening() {
        new Thread(() -> {
            try {
                while (true) {
                    String message = dis.readUTF();
                    System.out.println(message);
                    if (message.equals("You are in the waiting queue")) {
                        button.setEnabled(false);
                        infoLabel.setText("Waiting for the second player...");
                    } else if (message.equals("Game starting")) {
                        waitingForStart = false;
                        button.setEnabled(true);
                        infoLabel.setText("");
                    } else if (message.startsWith("Game Over! ")) {
                        button.setEnabled(false);
                        infoLabel.setText(message);

                    } else if (message.equals("New game started!")){
                        button.setEnabled(true);
                        infoLabel.setText("");
                    }
                    else {
                        String[] parts = message.split(" ");
                        if (parts.length == 3 && parts[0].equals("Current") && parts[1].equals("counter:")) {
                            int counter = Integer.parseInt(parts[2]);
                            progressBar.setValue(counter);
                            if (counter == 0 || counter == 100) {
                                JOptionPane.showMessageDialog(null, "Game Over! " + (counter == 0 ? "Left Team" : "Right Team") + " wins!");
                                button.setEnabled(false);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 1245; // example value
    private int counter = 50; // example value
    private final ConcurrentHashMap<String, DataOutputStream> clients = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    private boolean gameInProgress = false;

    public static void main(String[] args) throws IOException {
        new Server().startServer();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void startServer() throws IOException {

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

        while(true)
            {
                Socket socket = serverSocket.accept();
                new ClientHandler(this, socket).start();
            }
        }
    }


    public void broadcast(String message) throws IOException {
        for (DataOutputStream dos : clients.values()) {
            dos.writeUTF(message);
        }
    }

    public void modifyCounter(String id) throws IOException, InterruptedException {
        if (id.startsWith("L")) {
            counter--;
        } else if (id.startsWith("R")) {
            counter++;
        }
        if (counter <= 0) {
            broadcast("Game Over! Left Team wins!");
            broadcast("New game starts in 3...");
            TimeUnit.SECONDS.sleep(1);
            broadcast("New game starts in 2...");
            TimeUnit.SECONDS.sleep(1);
            broadcast("New game starts in 1...");
            TimeUnit.SECONDS.sleep(1);
            broadcast("New game started!");
            resetGame();

        } else if (counter >= 100) {
            broadcast("Game Over! Right Team wins!");
            broadcast("New game starts in 3...");
            TimeUnit.SECONDS.sleep(1);
            broadcast("New game starts in 2...");
            TimeUnit.SECONDS.sleep(1);
            broadcast("New game starts in 1...");
            TimeUnit.SECONDS.sleep(1);
            broadcast("New game started!");
            resetGame();
        } else {
            broadcast("Current counter: " + counter);
        }
    }

    public int getCounter() {
        return counter;
    }

    public void addClient(String id, DataOutputStream dos) throws IOException {
            clients.put(id, dos);
            System.out.println(clients);
            dos.writeUTF("You are in the game");
            System.out.println("Client " + id + " connected");

            if (!gameInProgress) {
                startGame();
            }

    }

    public void removeClient(String id) {
        clients.remove(id);
        if (!queue.isEmpty()) {
            try {
                String nextPlayer = queue.poll();
                clients.get(nextPlayer).writeUTF("You are in the game");
                System.out.println("Client " + nextPlayer + " connected");
                if (clients.size() == 2 && !gameInProgress) {
                    startGame();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (clients.isEmpty()) {
            resetCounter();
        }
    }

    public void resetCounter() {
        counter = 50;
    }

    private void startGame() {
        gameInProgress = true;

        new Thread(() -> {
        }).start();
    }


    public void resetGame() {
        counter = 50;
    }

}

class ClientHandler extends Thread {
    private final Socket socket;
    private final Server server;
    private String id;

    public ClientHandler(Server server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try {
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            id = dis.readUTF();
            System.out.println(id);

            // Check if the client is sending a cancel message
            if ("cancel".equals(id)) {
                System.out.println("Client has cancelled");
                socket.close();
                return;
            }

            server.addClient(id, dos);

            while (true) {
                if (dis.readBoolean()) {
                    System.out.println(id);
                    server.modifyCounter(id);
                    server.broadcast("Current counter: " + server.getCounter());
                    System.out.println(id);
                    System.out.println(server.getCounter());
                }

            }
        } catch (IOException e) {
            System.out.println("Client " + id + " has cancelled");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                server.removeClient(id);
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

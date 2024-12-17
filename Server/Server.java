import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 1234;
    private static final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, Socket> clientDictionary = Collections.synchronizedMap(new HashMap<>());
    private static final Map<String, String> credentials = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        loadCredentials();
        new Thread(Server::handleBroadcastMessages).start();
        new Thread(Server::handleClientConnections).start();
        new Thread(Server::handleLogin).start();
    }
    private static void handleLogin() {
        try (ServerSocket serverSocket = new ServerSocket(9874)) {
            System.out.println("Server is running and waiting for login connections...");

            while (true) {
                // Accept new client connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                // Handle the client in a separate thread to allow concurrent clients
                new Thread(() -> {
                    try {
                        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                        String username;
                        String password;
                        String line;


                            // Loop to read multiple login attempts from the same client
                        while ((line = in.readLine()) != null) {
                            switch (line) {
                                case "***LOGIN***":
                                    username = in.readLine();
                                    password = in.readLine();

                                    if (username != null && password != null) {
                                        System.out.println("Received login: " + username + ", " + password);
                                        if (credentials.containsKey(username) && credentials.get(username).equals(password)) {
                                            out.println("Login successful");
                                        } else {
                                            out.println("Login failed");
                                        }
                                    }
                                    break;

                                case "***REGISTER***":
                                    username = in.readLine();
                                    password = in.readLine();

                                    if (username != null && password != null) {
                                        System.out.println("Received register: " + username + ", " + password);
                                        if (credentials.containsKey(username)) {
                                            out.println("User already exists");
                                        } else {
                                            credentials.put(username, password);
                                            saveCredentials();
                                            out.println("Registration successful");
                                        }
                                    }
                                    break;

                                default:
                                    System.out.println("Unknown command received: " + line);
                                    out.println("Invalid command");
                                    break;
                            }
                        }

                        System.out.println("Client disconnected: " + clientSocket);
                    } catch (IOException e) {
                        System.err.println("Error handling client: " + e.getMessage());
                    } finally {
                        try {
                            clientSocket.close(); // Clean up connection
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadCredentials() {
        try (BufferedReader reader = new BufferedReader(new FileReader("credentials.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length == 2) {
                    credentials.put(parts[0], parts[1]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveCredentials() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("credentials.txt"))) {
            for (Map.Entry<String, String> entry : credentials.entrySet()) {
                writer.println(entry.getKey() + ":" + entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void handleBroadcastMessages() {
        try (DatagramSocket socket = new DatagramSocket(12345)) {
            System.out.println("Server is listening for broadcast messages...");

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String response = "Server IP: " + InetAddress.getLocalHost().getHostAddress();
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();
                DatagramPacket responsePacket = new DatagramPacket(
                        response.getBytes(), response.length(), clientAddress, clientPort
                );
                socket.send(responsePacket);
                System.out.println("Response sent to: " + clientAddress.getHostAddress());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClientConnections() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running and waiting for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        synchronized (clientDictionary) {
            for (Map.Entry<String, Socket> entry : clientDictionary.entrySet()) {
                if (!entry.getValue().equals(sender.clientSocket)) {
                    try {
                        PrintWriter out = new PrintWriter(entry.getValue().getOutputStream(), true);
                        out.println(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                username = getUsername();
                synchronized (clientDictionary) {
                    clientDictionary.put(username, clientSocket);
                }
                System.out.println("User " + username + " connected.");
                System.out.println(clientDictionary);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(username + ": " + inputLine);
                    handleClientMessage(inputLine);
                }

                synchronized (clientDictionary) {
                    clientDictionary.remove(username);
                }
                clients.remove(this);
                in.close();
                out.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleClientMessage(String inputLine) throws IOException {
            switch (inputLine) {
                case "***ADD***":
                    String newUsername = in.readLine();
                    if (credentials.containsKey(newUsername)) {
                        out.println("Exists");
                    } else {
                        out.println("Error");
                    }
                    break;
                case "***PRV0***":
                    sendPrivateMessageHistory();
                    break;
                case "***PRV***":
                    sendPrivateMessage();
                    break;
                case "***GRP***":
                    String groupMessage = in.readLine();
                    broadcast(username + ": " + groupMessage, this);
                    break;
                case "***PRIVATE***":
                    System.out.println("Private message received");
                    String receiver = in.readLine();
                    if (receiver != null && clientDictionary.containsKey(receiver)) {
                        out.println("ONLINE");
                    } else {
                        out.println("OFFLINE");
                    }
                    break;
                default:
                    break;
            }
        }

        private void sendPrivateMessageHistory() throws IOException {
            String receiver = in.readLine();
            String fileName = getFileName(receiver);
            File file = new File(fileName);
            if (file.exists()) {
                try (Scanner fileReader = new Scanner(file)) {
                    while (fileReader.hasNextLine()) {
                        out.println(fileReader.nextLine());
                    }
                }
            }
        }

        private void sendPrivateMessage() throws IOException {
            String receiver = in.readLine();
            String fileName = getFileName(receiver);
            try (FileWriter fileWriter = new FileWriter(fileName, true)) {
                if (clientDictionary.containsKey(receiver)) {
                    PrintWriter receiverOut = new PrintWriter(clientDictionary.get(receiver).getOutputStream(), true);
                    String privateMessage = in.readLine();
                    receiverOut.println(username + ": " + privateMessage);
                    fileWriter.write(username + ": " + privateMessage + "\n");
                } else {
                    out.println("Error");
                }
            }
        }

        private String getFileName(String receiver) {
            return (username.compareTo(receiver) > 0) ? receiver + username + ".txt" : username + receiver + ".txt";
        }

        private String getUsername() throws IOException {
            return in.readLine();
        }
    }
}
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class Main {
    private static final List<String> IP_POOL = Arrays.asList("127.0.0.2", "127.0.0.3", "127.0.0.4");
    private static final String RESERVATION_FILE = "used_ips.txt";
    private static String reservedIp = null;

    public static void main(String[] args) throws Exception {
        reservedIp = reserveAvailableIP();
        if (reservedIp == null) {
            System.err.println("No available IPs to bind.");
            return;
        }

        int port = 8000 + new Random().nextInt(1000);
        InetAddress ip = InetAddress.getByName(reservedIp);

        // Start server
        HttpServer server = HttpServer.create(new InetSocketAddress(ip, port), 0);
        server.createContext("/", exchange -> {
            String response = "Hello from " + reservedIp + ":" + port;
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.getResponseBody().close();
        });

        server.start();
        System.out.println("Server running on " + reservedIp + ":" + port);
        //Add new Node to kademlia


        String json = "{\"id\":\"\",\"ip\":\""+reservedIp+"\",\"port\":\""+port+"\"}";

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8060/kademlia/addNode"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());


        // Register shutdown hook to clean up IP
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            releaseIP(reservedIp);
            System.out.println("Released IP: " + reservedIp);
        }));

        // Keep alive
        AtomicBoolean running = new AtomicBoolean(true);
        while (running.get()) {
            Thread.sleep(1000);
        }
    }

    private static synchronized String reserveAvailableIP() throws IOException {
        Set<String> used = new HashSet<>();
        File file = new File(RESERVATION_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    used.add(line.trim());
                }
            }
        }

        for (String ip : IP_POOL) {
            if (!used.contains(ip)) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                    writer.write(ip);
                    writer.newLine();
                }
                return ip;
            }
        }

        return null; // All taken
    }

    private static synchronized void releaseIP(String ip) {
        try {
            File file = new File(RESERVATION_FILE);
            if (!file.exists()) return;

            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().equals(ip)) {
                        lines.add(line.trim());
                    }
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to release IP: " + e.getMessage());
        }
    }
}

/*
Windows command prompt to create fake ip address
netsh interface ipv4 add address "Loopback Pseudo-Interface 1" 127.0.0.2 255.0.0.0
netsh interface ipv4 add address "Loopback Pseudo-Interface 1" 127.0.0.3 255.0.0.0
netsh interface ipv4 add address "Loopback Pseudo-Interface 1" 127.0.0.4 255.0.0.0

Windows command prompt to delete fake ip address
netsh interface ipv4 delete address "Loopback Pseudo-Interface 1" 127.0.0.2
 */

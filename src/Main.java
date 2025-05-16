import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public class Main {
    private static final List<String> IP_POOL = Arrays.asList("127.0.0.2", "127.0.0.3", "127.0.0.4");
    private static final String RESERVATION_FILE = "used_ips.txt";
    private static String reservedIp = null;

    public static void main(String[] args) throws Exception {
        Scanner input = new Scanner(System.in);
        String url = "jdbc:mysql://localhost:3306/ssd";
        String username = "root";
//        String password = "qwerty";
        String password = "QwertY1234!";

        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            System.out.println("Connection successful!");





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

            server.createContext("/newNode", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String message = null;
                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=");
                        if (pair.length == 2 && pair[0].equals("node")) {
                            message = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                            break;
                        }
                    }
                }
                System.out.println("New node received " + message + " on your area");

                String response = "Received";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            });

            server.createContext("/removedNode", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String message = null;
                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=");
                        if (pair.length == 2 && pair[0].equals("node")) {
                            message = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                            break;
                        }
                    }
                }
                System.out.println("Node " + message + " was removed from your area");

                String response = "Received";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            });


            //Quando é adcionado um novo no receber notificação apenas ao no da mesma area ***********
            //Quando um no é removido receber notificação na mesma area ***********

    //        1-Adcionar um produto
    //        Propiedades
    //        Nome
    //        Preço Inicial
    //        //Adcionar na bd mas nao na adicao do produto
    //        Preço Final
    //
    //        2-Criar Um Leilao
    //        Tempo
    //        Lista de produtos
    //        //NA BD
    //        Lances-leilao, produto, preço
    //
    //
    //        3-Leilao
    //        3.1-Ver Todos os leiloes
    //                Selecionar Leilao
    //                        Apanha o valor final atual e na medida qu ese da lances aumenta o valor final
    //                        Selecioanr produto e dar lance e associava ao ip e porta
    //        3.2-Ver Todos os leiloes que ele participa
    //                Selecionar Leilao
    //                Apanha o valor final atual e na medida qu ese da lances aumenta o valor final
    //                Selecioanr produto e dar lance e associava ao ip e porta
    //
    //        Enviar uma notificação para todos os participantes quando um leilao terminar e o preço final para todos os produtos
    //        E para quem ganhou em cada produto mostrar um parabens por ter vencido



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
                System.out.println("Selecione a opção pretendida");
                System.out.println("1-Adcionar um produto");
                System.out.println("2-Criar um leilão");
                System.out.println("3-Visualizar leilões");
                int option = input.nextInt();
                if(option==1){
                    System.out.println("Introduza o nome do produto");
                    String nome = input.next();
                    System.out.println("Introduza o preço inicial do produto");
                    Float precoInicial = input.nextFloat();

                    String insertSql = "INSERT INTO produto (nome, preco_inicial) VALUES (?, ?)";
                    PreparedStatement stmt = connection.prepareStatement(insertSql);
                    stmt.setString(1, nome);
                    stmt.setString(2, String.valueOf(precoInicial));
                    stmt.executeUpdate();
                }
                else if(option==2){
                    System.out.println("Introduza a duração do leilao em horas");
                    int tempo = input.nextInt();
                    System.out.println("Selecione os produtos que queres comprar");
                    String query = "SELECT * FROM produto";

                    PreparedStatement stmt = connection.prepareStatement(query);
                    ResultSet rs = stmt.executeQuery();
                    List<Produto> produtos = new ArrayList<>();
                    int i=1;
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String nome = rs.getString("nome");
                        Float preco_inicial = rs.getFloat("preco_inicial");
                        produtos.add(new Produto(id,nome,preco_inicial));
                        System.out.println(i + "-" + nome);
                        i++;
                    }

                    List<Produto> produtosLeilao=new ArrayList<>();
                    System.out.println("Selecione o produto a adcionar no leilao ou 0 para terminar");
                    while(true){
                        System.out.println("Produto:");
                        int choice=input.nextInt();
                        if(choice==0)
                            break;
                        System.out.println("Lance");
                        float lance = input.nextFloat();
                        Produto produto = produtos.get(choice-1);
                        produto.setPreco_final(lance);
                        produtosLeilao.add(produto);
                    }


                    String insertSql = "INSERT INTO leilao (tempo,username) VALUES (?,?)";
                    stmt = connection.prepareStatement(insertSql,Statement.RETURN_GENERATED_KEYS);
                    stmt.setString(1, String.valueOf(tempo));
                    stmt.setString(2, reservedIp + ":" + port);
                    int rowsAffected = stmt.executeUpdate();

                    if (rowsAffected > 0) {
                        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                long id = generatedKeys.getLong(1);
                                for(Produto produto:produtosLeilao){
                                    insertSql = "INSERT INTO lance (id_produto,id_leilao,valor) VALUES (?,?,?)";
                                    stmt = connection.prepareStatement(insertSql);
                                    stmt.setString(1, String.valueOf(produto.getId()));
                                    stmt.setString(2, String.valueOf(id));
                                    stmt.setString(3, String.valueOf(produto.getPreco_final()));
                                    stmt.executeUpdate();
                                }
                            }
                        }
                    }
                }
                else if(option==3){
                    System.out.println("Selecione a opção pretendida");
                    System.out.println("1-Ver todos os leiloes");
                    System.out.println("2-Ver todos os leiloes em que participas");
                    int choice= input.nextInt();
                    if(choice==1){
                        String query = "SELECT p.nome produto,l.id_leilao leilao,l.valor valor FROM lance l INNER JOIN produto p";

                        PreparedStatement stmt = connection.prepareStatement(query);
                        ResultSet rs = stmt.executeQuery();
                        List<LanceDTO> lances=new ArrayList<>();
                        int leilaoID=0;
                        while (rs.next()) {
                            int leilao = rs.getInt("leilao");
                            String produto = rs.getString("produto");
                            Float valor = rs.getFloat("valor");

                            LanceDTO lanceDTO = new LanceDTO(produto,leilao,valor);
                            lances.add(lanceDTO);
                            if(leilao!=leilaoID){
                                System.out.println("Leilão "+leilao);
                                leilaoID=leilao;
                            }
                            System.out.println("Produto:" +produto+ "-" + valor);

                        }
                        if(leilaoID>0) {
                            System.out.println("Selecione o Leilao que pretende dar lance ou 0 para sair");
                            while (true) {
                                int choice2 = input.nextInt();
                                if (choice2 == 0)
                                    break;

                                List<LanceDTO> filteredLances = lances.stream()
                                        .filter(l -> l.getLeilao() == choice2)
                                        .collect(Collectors.toList());

                                List<LanceDTO> novosLances = new ArrayList<>();
                                for (LanceDTO lanceDTO : filteredLances) {
                                    System.out.println("Produto:" + lanceDTO.getProduto() + ", lance atual " + lanceDTO.getValor());
                                    System.out.println("Introduza o valor do teu lance");
                                    Float valor = input.nextFloat();
                                    if (valor > lanceDTO.getValor()) {
                                        LanceDTO newLance = new LanceDTO(lanceDTO.getProduto(), lanceDTO.getLeilao(), valor);
                                        novosLances.add(newLance);
                                    } else {
                                        System.out.println("Valor do lance tem de ser maior do que o lance atual");
                                    }
                                }

                                for (LanceDTO lanceDTO : novosLances) {
                                    String updateSql = "UPDATE lance SET valor = ? WHERE id_leilao = ?";
                                    stmt = connection.prepareStatement(updateSql);
                                    stmt.setFloat(1, lanceDTO.getValor());
                                    stmt.setInt(2, lanceDTO.getLeilao());
                                    stmt.executeUpdate();
                                }
                                break;
                            }
                        }
                    }
                    else if(choice==2){
                        String query = "SELECT p.nome produto,l.id_leilao leilao,l.valor valor FROM lance l INNER JOIN produto p INNER JOIN leilao ll WHERE ll.username=?";

                        PreparedStatement stmt = connection.prepareStatement(query);
                        stmt.setString(1, reservedIp + ":" + port);
                        ResultSet rs = stmt.executeQuery();
                        List<LanceDTO> lances=new ArrayList<>();
                        int leilaoID=0;

                        while (rs.next()) {
                            int leilao = rs.getInt("leilao");
                            String produto = rs.getString("produto");
                            Float valor = rs.getFloat("valor");

                            LanceDTO lanceDTO = new LanceDTO(produto,leilao,valor);
                            lances.add(lanceDTO);
                            if(leilao!=leilaoID){
                                System.out.println("Leilão "+leilao);
                                leilaoID=leilao;
                            }
                            System.out.println("Produto:" +produto+ "-" + valor);

                        }
                        if(leilaoID>0) {
                            System.out.println("Selecione o Leilao que pretende dar lance ou 0 para sair");
                            while (true) {
                                int choice2 = input.nextInt();
                                if (choice2 == 0)
                                    break;

                                List<LanceDTO> filteredLances = lances.stream()
                                        .filter(l -> l.getLeilao() == choice2)
                                        .collect(Collectors.toList());

                                List<LanceDTO> novosLances = new ArrayList<>();
                                for (LanceDTO lanceDTO : filteredLances) {
                                    System.out.println("Produto:" + lanceDTO.getProduto() + ", lance atual " + lanceDTO.getValor());
                                    System.out.println("Introduza o valor do teu lance");
                                    Float valor = input.nextFloat();
                                    if (valor > lanceDTO.getValor()) {
                                        LanceDTO newLance = new LanceDTO(lanceDTO.getProduto(), lanceDTO.getLeilao(), valor);
                                        novosLances.add(newLance);
                                    } else {
                                        System.out.println("Valor do lance tem de ser maior do que o lance atual");
                                    }
                                }

                                for (LanceDTO lanceDTO : novosLances) {
                                    String updateSql = "UPDATE lance SET valor = ? WHERE id_leilao = ?";
                                    stmt = connection.prepareStatement(updateSql);
                                    stmt.setFloat(1, lanceDTO.getValor());
                                    stmt.setInt(2, lanceDTO.getLeilao());
                                    stmt.executeUpdate();
                                }
                                break;
                            }
                        }
                    }
                }
                else{
                    System.out.println("Opção não encontrada");
                }
            }
        } catch (SQLException e) {
            System.out.println("Connection failed!");
            e.printStackTrace();
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

//Adcionar mais uma interface

/*
Windows command prompt to create fake ip address
netsh interface ipv4 add address "Loopback Pseudo-Interface 1" 127.0.0.2 255.0.0.0
netsh interface ipv4 add address "Loopback Pseudo-Interface 1" 127.0.0.3 255.0.0.0
netsh interface ipv4 add address "Loopback Pseudo-Interface 1" 127.0.0.4 255.0.0.0

Windows command prompt to delete fake ip address
netsh interface ipv4 delete address "Loopback Pseudo-Interface 1" 127.0.0.2
 */

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Java 8 TCP text server, without external dependencies. */
public final class EchoServer {
    private final Set<Socket> clients = Collections.newSetFromMap(new ConcurrentHashMap<Socket, Boolean>());
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private volatile boolean stopping;
    private ServerSocket listener;

    public static void main(String[] args) {
        try {
            Config config = Config.parse(args);
            if (config.help) { printHelp(); return; }
            new EchoServer().run(config);
        } catch (IllegalArgumentException | IOException ex) {
            System.err.println("Fehler: " + ex.getMessage());
            System.err.println("Hilfe: java -jar EchoServer.jar --help");
            System.exit(1);
        }
    }

    private void run(final Config config) throws IOException {
        listener = new ServerSocket();
        listener.bind(new InetSocketAddress(config.host, config.port));
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "shutdown"));
        System.out.println("TCP " + listener.getLocalSocketAddress() + " | " + config.mode
                + " | " + config.seconds + " s | Zeitstempel: " + config.timestamp);
        System.out.println("Warte auf Clients. Beenden: Strg+C.");
        try {
            while (!stopping) {
                final Socket client = listener.accept();
                clients.add(client);
                if (stopping) { close(client); break; }
                try { workers.execute(() -> handle(client, config)); }
                catch (RejectedExecutionException ex) { close(client); if (!stopping) throw ex; }
            }
        } catch (SocketException ex) {
            if (!stopping) throw ex;
        } finally { stop(); }
    }

    private void handle(Socket client, Config config) {
        String peer = String.valueOf(client.getRemoteSocketAddress());
        System.out.println("Verbunden: " + peer);
        try (Socket connection = client;
             BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8))) {
            while (!stopping) {
                String message = config.mode.equals("echo") ? reader.readLine() : config.text;
                if (message == null) break;
                TimeUnit.NANOSECONDS.sleep(config.delayNanos);
                if (config.timestamp) message = "[" + Instant.now().toString() + "] " + message;
                writer.write(message);
                writer.write('\n');
                writer.flush();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (IOException ex) {
            if (!stopping) System.out.println("Verbindung beendet (" + peer + "): " + ex.getMessage());
        } finally {
            clients.remove(client);
            System.out.println("Getrennt: " + peer);
        }
    }

    private synchronized void stop() {
        stopping = true;
        if (listener != null) try { listener.close(); } catch (IOException ignored) { }
        for (Socket client : clients) close(client);
        workers.shutdownNow();
    }

    private static void close(Socket client) {
        try { client.close(); } catch (IOException ignored) { }
    }

    private static void printHelp() {
        System.out.println("TCP-Testserver: UTF-8, eine Nachricht pro Zeile\n"
                + "--mode echo|periodic  Betriebsart (Standard: echo)\n"
                + "--host ADRESSE        Bind-Adresse (Standard: 127.0.0.1)\n"
                + "--port 5000           Port (1-65535)\n"
                + "--seconds 1           Echo-Verzoegerung / Sendeintervall\n"
                + "--text \"Hallo\"       Text fuer periodisches Senden\n"
                + "--timestamp           UTC-Sendezeit voranstellen\n"
                + "Dezimalzeiten mit Punkt oder Komma. Beenden: Strg+C.");
    }

    private static final class Config {
        String host = "127.0.0.1", mode = "echo", text = "Hallo";
        int port = 5000;
        double seconds = 1;
        long delayNanos;
        boolean timestamp, help;

        static Config parse(String[] args) {
            Config config = new Config();
            for (int i = 0; i < args.length; i++) {
                String option = args[i];
                if (option.equals("--help") || option.equals("-h")) { config.help = true; return config; }
                if (option.equals("--timestamp")) { config.timestamp = true; continue; }
                if (!Arrays.asList("--mode", "--host", "--port", "--seconds", "--text").contains(option))
                    throw new IllegalArgumentException("Unbekannte Option: " + option);
                if (++i == args.length) throw new IllegalArgumentException("Wert fuer " + option + " fehlt.");
                String value = args[i];
                switch (option) {
                    case "--mode": config.mode = value; break;
                    case "--host": config.host = value; break;
                    case "--text": config.text = value; break;
                    case "--port": config.port = Integer.parseInt(value); break;
                    case "--seconds": config.seconds = Double.parseDouble(value.replace(',', '.')); break;
                    default: throw new IllegalArgumentException(option);
                }
            }
            if (!config.mode.equals("echo") && !config.mode.equals("periodic"))
                throw new IllegalArgumentException("Modus muss echo oder periodic sein.");
            if (config.host.trim().isEmpty()) throw new IllegalArgumentException("Bind-Adresse fehlt.");
            if (config.port < 1 || config.port > 65535) throw new IllegalArgumentException("Port muss zwischen 1 und 65535 liegen.");
            if (!Double.isFinite(config.seconds) || config.seconds < 0 || config.seconds > 86400
                    || (config.mode.equals("periodic") && config.seconds < 0.001))
                throw new IllegalArgumentException("Zeit: 0 bis 86400 Sekunden; periodisch mindestens 0,001 Sekunden.");
            if (config.text.contains("\n") || config.text.contains("\r"))
                throw new IllegalArgumentException("Sendetext muss eine einzelne Zeile sein.");
            config.delayNanos = Math.round(config.seconds * 1_000_000_000L);
            return config;
        }
    }
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Echo_Server_II extends JFrame {

    private JTextField portField;
    private JTextField delayField;
    private JButton startButton;
    private JButton stopButton;
    private JTextArea logArea;
    private JLabel statusLabel;

    private ServerSocket serverSocket;
    private volatile boolean running = false;
    private Thread acceptThread;
    private int delayMs = 0;

    public Echo_Server_II() {
        super("TCP Echo Server");
        initGui();
    }

    private void initGui() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 450);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        // --- Obere Leiste: Konfiguration ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        topPanel.add(new JLabel("Port:"));
        portField = new JTextField("5000", 6);
        topPanel.add(portField);

        topPanel.add(new JLabel("Verzoegerung (ms):"));
        delayField = new JTextField("1000", 6);
        topPanel.add(delayField);

        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        topPanel.add(startButton);
        topPanel.add(stopButton);

        add(topPanel, BorderLayout.NORTH);

        // --- Mitte: Log-Ausgabe ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);
        add(scroll, BorderLayout.CENTER);

        // --- Unten: Status ---
        statusLabel = new JLabel("Gestoppt");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
        add(statusLabel, BorderLayout.SOUTH);

        // --- Events ---
        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { startServer(); }
        });
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { stopServer(); }
        });

        // Beim Schliessen sauber aufraeumen
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { stopServer(); }
        });
    }

    private void startServer() {
        int port;
        int delay;
        try {
            port = Integer.parseInt(portField.getText().trim());
            delay = Integer.parseInt(delayField.getText().trim());
            if (port < 1 || port > 65535) throw new NumberFormatException("Port out of range");
            if (delay < 0) delay = 0;
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Bitte gueltige Zahlen eingeben.\nPort: 1-65535, Verzoegerung >= 0",
                "Ungueltige Eingabe", JOptionPane.ERROR_MESSAGE);
            return;
        }

        delayMs = delay;

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            log("FEHLER beim Binden an Port " + port + ": " + ex.getMessage());
            JOptionPane.showMessageDialog(this,
                "Port " + port + " konnte nicht geoeffnet werden.\n" + ex.getMessage(),
                "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        running = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);
        portField.setEnabled(false);
        delayField.setEnabled(false);
        statusLabel.setText("Laeuft auf Port " + port + " (Verzoegerung " + delayMs + " ms)");
        log("Server gestartet auf Port " + port + " (Verzoegerung " + delayMs + " ms)");

        acceptThread = new Thread(new Runnable() {
            public void run() { acceptLoop(); }
        }, "Accept-Thread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                final Socket client = serverSocket.accept();
                log("Verbindung von " + client.getInetAddress().getHostAddress()
                        + ":" + client.getPort());

                Thread t = new Thread(new Runnable() {
                    public void run() { handleClient(client); }
                }, "Client-" + client.getPort());
                t.setDaemon(true);
                t.start();

            } catch (IOException ex) {
                if (running) {
                    log("Accept-Fehler: " + ex.getMessage());
                }
                // Wenn running==false, wurde serverSocket.close() aufgerufen -> normal beenden
            }
        }
    }

    private void handleClient(Socket client) {
        BufferedReader in = null;
        PrintWriter out = null;
        try {
            // Wir nutzen bewusst eine Zeilen-basierte Kommunikation.
            in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), "UTF-8"), true);

            String line;
            while ((line = in.readLine()) != null) {
                log("Empfangen: " + line);

                if (delayMs > 0) {
                    try { Thread.sleep(delayMs); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }

                out.println(line); // echo zurueck
                log("Gesendet:  " + line);

                if ("exit".equalsIgnoreCase(line.trim())) {
                    log("Client hat 'exit' gesendet - Verbindung wird geschlossen.");
                    break;
                }
            }
        } catch (IOException ex) {
            log("Client-Fehler: " + ex.getMessage());
        } finally {
            try { if (in != null) in.close(); } catch (IOException ignored) {}
            if (out != null) out.close();
            try { client.close(); } catch (IOException ignored) {}
            log("Verbindung zu " + client.getInetAddress().getHostAddress()
                    + ":" + client.getPort() + " geschlossen.");
        }
    }

    private void stopServer() {
        if (!running) return;
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ex) {
            log("Fehler beim Schliessen des ServerSockets: " + ex.getMessage());
        }

        startButton.setEnabled(true);
        stopButton.setEnabled(false);
        portField.setEnabled(true);
        delayField.setEnabled(true);
        statusLabel.setText("Gestoppt");
        log("Server gestoppt.");
    }

    private void log(final String msg) {
        // Swing-Komponenten nur im EDT aktualisieren
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
                logArea.append("[" + ts + "] " + msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    public static void main(String[] args) {
        // Auf alten Systemen kann Nimbus fehlen -> robust bleiben
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new Echo_Server_II().setVisible(true);
            }
        });
    }
}

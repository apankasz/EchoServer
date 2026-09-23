# EchoServer mit GUI und Mini-Testclient

Alternative Variante im Branch `alternative-EchoServer-With-GUI`. Der Server verwendet Java Swing, benötigt keine zusätzlichen Bibliotheken und verarbeitet mehrere TCP-Clients parallel.

## Kompilieren (JDK 8 oder neuer)

Im Ordner `gui`:

```text
mkdir build
javac -encoding UTF-8 -d build Echo_Server_II.java Mini_Testclient.java
```

## Server starten

```text
java -cp build Echo_Server_II
```

Im Fenster den Port (Standard: **5000**) und die Antwortverzögerung in Millisekunden (Standard: **1000**) einstellen und **Start** drücken. **Stop** beendet die Annahme neuer Verbindungen; bereits verbundene Clients können weiterarbeiten. Das Schließen des Fensters beendet das Programm. Für einen vollständigen Neustart mit neuen Einstellungen das Fenster schließen und neu öffnen.

Der Server lauscht auf allen Netzwerkschnittstellen. Für Tests im Netzwerk muss der gewählte Port erreichbar sein. Das Programm bietet keine Authentifizierung oder Verschlüsselung und ist für lokale bzw. vertrauenswürdige Testnetze gedacht.

## Mini-Testclient starten

Bei laufendem Server auf Port 5000 in einem zweiten Terminal, ebenfalls im Ordner `gui`:

```text
java -cp build Mini_Testclient
```

Der Client verbindet sich mit `localhost:5000`, sendet `Hallo Server!`, gibt die Echo-Antwort aus und sendet anschließend `exit`. Host und Port sind im Client-Quellcode festgelegt. Nachrichten sind UTF-8-kodiert und zeilenweise abgeschlossen; der Server gibt sie nach der eingestellten Verzögerung zurück.

Die Dateinamen wurden beibehalten. Die öffentlichen Klassen heißen passend dazu `Echo_Server_II` und `Mini_Testclient`, damit die Dateien direkt mit `javac` kompilierbar sind.

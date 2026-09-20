# TCP-Testserver für Java 8

Eigenständiges Konsolenprogramm ohne Zusatzbibliotheken. Verzögertes Echo oder periodisches Senden an verbundene TCP-Clients. Port, Zeit, Text und Zeitstempel sind beim Start wählbar. Mehrere Clients werden unabhängig bedient.

## Bauen und starten

Zum Bauen wird ein JDK benötigt. Im Ordner `EchoServer` unter Windows `build.cmd`, unter Linux `sh build.sh` ausführen. Es entsteht `EchoServer.jar`. Code und Bytecode sind auf Java 8 ausgelegt.

Zum Ausführen genügt eine für das Betriebssystem geeignete Java-Laufzeit ab Java 8. Für **Windows 7 muss die konkrete Java-Distribution Windows 7 unterstützen**. Dieselbe JAR-Datei kann unter Linux verwendet werden.

```text
java -jar EchoServer.jar --mode echo --port 5000 --seconds 2
java -jar EchoServer.jar --mode echo --port 6000 --seconds 0.5 --timestamp
java -jar EchoServer.jar --mode periodic --port 5000 --seconds 3 --text "Testnachricht" --timestamp
```

Standard: `127.0.0.1:5000`, Echo, 1 Sekunde, kein Zeitstempel. Für Netzwerkzugriff `--host 0.0.0.0` ergänzen und gegebenenfalls den Port in der Firewall freigeben. Clients verwenden dann die IP-Adresse des Servers. Einstellungen gelten bis zum Neustart. `--help` zeigt alle Optionen. Strg+C beendet den Server.

## Nachrichtenformat und Zeitverhalten

TCP, UTF-8 ohne BOM, eine Nachricht pro Zeile. Nachrichten mit LF (`\n`) oder CRLF abschließen; Antworten enden mit LF. Ohne Zeilenende wartet der Server auf die vollständige Nachricht (ein Ende des Client-Sendestroms schließt auch die letzte Zeile ab). Leere Zeilen sind gültig. Text bleibt erhalten, Zeilenenden werden vereinheitlicht.

Mit `--timestamp` wird die UTC-Sendezeit vorangestellt, beispielsweise `[2026-09-20T12:34:56.123Z] Testnachricht`.

Echo: Jede Nachricht wartet die eingestellte Zeit; Nachrichten desselben Clients werden der Reihe nach verarbeitet. Andere Clients werden unabhängig bedient.

Periodisch: Der Client verbindet sich, dann kommt die erste Nachricht nach dem eingestellten Intervall. Nach jeder Sendung beginnt die nächste Wartezeit. Der Client muss keinen Text senden. Der Server ist kein Echtzeit-Taktgeber.

## Client-Test in PowerShell (auch Windows 7)

Server im Echo-Modus starten, dann in einem zweiten Fenster:

```powershell
$client = New-Object System.Net.Sockets.TcpClient('127.0.0.1', 5000)
$stream = $client.GetStream()
$stream.ReadTimeout = 10000
$reader = New-Object System.IO.StreamReader($stream)
$utf8 = New-Object System.Text.UTF8Encoding($false)
$writer = New-Object System.IO.StreamWriter($stream, $utf8)
$writer.AutoFlush = $true
$writer.WriteLine('Hallo vom Client')
$reader.ReadLine()
$client.Close()
```

Im periodischen Modus entfällt `WriteLine`; jedes `ReadLine()` liest die nächste Nachricht. Unter Linux kann beispielsweise `nc 127.0.0.1 5000` als Client dienen, sofern Netcat installiert ist.

Für lokale und vertrauenswürdige Testnetze: keine Authentifizierung, Verschlüsselung oder Begrenzung der Nachrichtenlänge.

## Prüfung

Mit einem Java-8-JDK gebaut und unter Windows mit echten TCP-Verbindungen getestet: Echo, UTF-8, Zeilenrahmung, leere Nachrichten, mehrere Clients, Reihenfolge, Verzögerung, UTC-Zeitstempel, periodisches Senden und ungültige Eingaben. Wiederholbar mit `./Verify.ps1` in PowerShell (Java im PATH) oder `./Verify.ps1 -Java 'Pfad\zu\java.exe'`. Direkte Betriebssystemtests unter Windows 7 und Linux stehen noch aus.

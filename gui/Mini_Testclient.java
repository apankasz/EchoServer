import java.io.*;
import java.net.*;

public class Mini_Testclient {
    public static void main(String[] args) throws Exception {
        Socket s = new Socket("localhost", 5000);
        BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"));
        PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
        out.println("Hallo Server!");
        System.out.println("Antwort: " + in.readLine());
        out.println("exit");
        s.close();
    }
}

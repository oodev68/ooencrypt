package oo.dinnoo;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println( "                 /$$$$$$ \n" +
                                "                /$$__  $$\n" +
                                "  /$$$$$$      | $$  \\ $$\n" +
                                " /$$__  $$     | $$  | $$\n" +
                                "| $$  \\ $$     | $$  | $$\n" +
                                "| $$  | $$     | $$  | $$\n" +
                                "|  $$$$$$/     |  $$$$$$/\n" +
                                " \\______//$$$$$$\\______/ \n" +
                                "        |______/         ");
            System.out.println("------------------------------------------------------------------");
            System.out.println("Użycie: java oo.dinnoo.Main encrypt|decrypt ŚCIEŻKA [-p HASŁO] [-f]");
            System.out.println("-p > hasło do szyfrowania");
            System.out.println("-f > szyfrowanie folderu");
            return;
        }

        String mode = args[0].toLowerCase();
        Path path = Paths.get(args[1]);
        String password = null;
        boolean folder = false;

        for (int i = 2; i < args.length; i++) {
            switch (args[i]) {
                case "-p":
                    if (i + 1 < args.length) password = args[++i];
                    break;
                case "-f":
                    folder = true;
                    break;
            }
        }

        if (password == null || password.isBlank()) {
            password = "nadusia432523454";
            System.out.println("Ostrzeżenie: Użyto hasła domyślnego. Zalecane podanie własnego przez -p");
        }

        System.out.println("Wątki CPU: " + Runtime.getRuntime().availableProcessors());

        if (mode.equals("encrypt")) {
            if (folder) Encryptor.encryptFolder(path, password);
            else        Encryptor.encryptFile(path, password);
        } else if (mode.equals("decrypt")) {
            if (folder) Encryptor.decryptFolder(path, password);
            else        Encryptor.decryptFile(path, password);
        } else {
            System.out.println("Nieznany tryb: " + mode + ". Użyj encrypt lub decrypt.");
        }
    }
}
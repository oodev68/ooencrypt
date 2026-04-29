package oo.dinnoo;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Encryptor {

    private static final int THREADS = Runtime.getRuntime().availableProcessors();
    private static final int CHUNK_SIZE = 1024 * 1024; // 1mb

    // haslo na 32 bajty
    private static SecretKeySpec deriveKey(String password) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(password.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }

    // szyfrowanie pojedynczego pliku
    public static void encryptFile(Path path, String password) throws Exception {
        SecretKeySpec key = deriveKey(password);
        byte[] plaintext = Files.readAllBytes(path);
        int originalSize = plaintext.length;
        byte[] ciphertext = parallelProcess(plaintext, key, Cipher.ENCRYPT_MODE);

        Path outPath = path.resolveSibling(path.getFileName() + ".oo");
        try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(outPath))) {
            out.writeInt(originalSize);
            out.write(ciphertext);
        }
        System.out.println("Zaszyfrowano: " + outPath);
    }
    // deszyfrowanie pojedynczego pliku
    public static void decryptFile(Path path, String password) throws Exception {
        SecretKeySpec key = deriveKey(password);

        int originalSize;
        byte[] ciphertext;
        try (DataInputStream in = new DataInputStream(Files.newInputStream(path))) {
            originalSize = in.readInt();
            ciphertext = in.readAllBytes();
        }

        byte[] plaintext = parallelProcess(ciphertext, key, Cipher.DECRYPT_MODE);
        plaintext = Arrays.copyOf(plaintext, originalSize); // odcina to gówno NULL na koncu

        String fileName = path.getFileName().toString();
        if (fileName.endsWith(".oo")) fileName = fileName.substring(0, fileName.length() - 3);
        Path outPath = path.resolveSibling(fileName);
        Files.write(outPath, plaintext);
        System.out.println("Odszyfrowano: " + outPath);
    }

    // folder
    public static void encryptFolder(Path folder, String password) throws Exception {
        processFolder(folder, password, true);
    }

    public static void decryptFolder(Path folder, String password) throws Exception {
        processFolder(folder, password, false);
    }

    private static void processFolder(Path folder, String password, boolean encrypt) throws Exception {
        List<Path> files = Files.walk(folder)
                .filter(Files::isRegularFile)
                .filter(p -> !encrypt || !p.toString().endsWith(".oo"))
                .filter(p ->  encrypt || p.toString().endsWith(".oo"))
                .collect(Collectors.toList());

        System.out.println("Znalezione pliki: " + files.size() + ", przetwarzanie na " + THREADS + " watkach...");

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<?>> futures = new java.util.ArrayList<>();

        for (Path file : files) {
            futures.add(pool.submit(() -> {
                try {
                    if (encrypt) encryptFile(file, password);
                    else         decryptFile(file, password);
                } catch (Exception e) {
                    System.err.println("Blad pliku " + file + ": " + e.getMessage());
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(); }
            catch (ExecutionException e) {
                System.err.println("Watek zgłosil blad: " + e.getCause().getMessage());
            }
        }

        pool.shutdown();
        System.out.println("gotowe.");
    }

    static byte[] parallelProcess(byte[] data, SecretKeySpec key, int mode) throws Exception {
        int blockSize = 16;
        int paddedLen = (data.length + blockSize - 1) / blockSize * blockSize;
        if (paddedLen != data.length) {
            data = Arrays.copyOf(data, paddedLen);
        }

        int chunkSize = (CHUNK_SIZE / blockSize) * blockSize;
        int numChunks = (data.length + chunkSize - 1) / chunkSize;
        byte[] result  = new byte[data.length];

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<?>> futures = new java.util.ArrayList<>(numChunks);
        final byte[] src = data;

        for (int i = 0; i < numChunks; i++) {
            final int start = i * chunkSize;
            final int end   = Math.min(start + chunkSize, src.length);

            futures.add(pool.submit(() -> {
                try {
                    Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
                    cipher.init(mode, key);
                    byte[] out = cipher.doFinal(src, start, end - start);
                    System.arraycopy(out, 0, result, start, out.length);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        for (Future<?> f : futures) {
            try { f.get(); }
            catch (ExecutionException e) {
                throw new Exception("Blad watku szyfrujacego: " + e.getCause().getMessage(), e.getCause());
            }
        }

        pool.shutdown();
        return result;
    }
}
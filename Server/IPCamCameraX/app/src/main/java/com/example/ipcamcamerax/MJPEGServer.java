package com.example.ipcamcamerax;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MJPEGServer extends Thread {
    private volatile Bitmap latestBitmap;
    private boolean running = true;
    private ServerSocket serverSocket;
    private final Object lock = new Object();

    // Frame rate control (frames per second)
    private static final int FRAME_INTERVAL_MS = 100; // 10 FPS (50ms per frame)

    public void updateFrame(Bitmap bitmap) {
        synchronized (lock) {
            if (latestBitmap != null && !latestBitmap.isRecycled()) {
                latestBitmap.recycle(); // Free old bitmap memory
            }
            latestBitmap = bitmap;
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(8989);
            while (running) {
                Socket client = serverSocket.accept();
                new Thread(() -> streamToClient(client)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void streamToClient(Socket client) {
        try (OutputStream outputStream = client.getOutputStream()) {
            String boundary = "MJPEGBOUNDARY";
            outputStream.write((
                    "HTTP/1.0 200 OK\r\n" +
                            "Server: IPCamServer\r\n" +
                            "Connection: close\r\n" +
                            "Max-Age: 0\r\n" +
                            "Expires: 0\r\n" +
                            "Cache-Control: no-cache, private\r\n" +
                            "Pragma: no-cache\r\n" +
                            "Content-Type: multipart/x-mixed-replace; boundary=" + boundary + "\r\n" +
                            "\r\n"
            ).getBytes());

            while (running && !client.isClosed()) {
                Bitmap bitmapToSend = null;
                synchronized (lock) {
                    if (latestBitmap != null && !latestBitmap.isRecycled()) {
                        bitmapToSend = latestBitmap.copy(latestBitmap.getConfig(), false);
                    }
                }

                if (bitmapToSend != null) {
                    ByteArrayOutputStream jpegOutputStream = new ByteArrayOutputStream();
                    bitmapToSend.compress(Bitmap.CompressFormat.JPEG, 50, jpegOutputStream);
                    byte[] jpegData = jpegOutputStream.toByteArray();

                    outputStream.write(("--" + boundary + "\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Content-Length: " + jpegData.length + "\r\n" +
                            "\r\n").getBytes());
                    outputStream.write(jpegData);
                    outputStream.write("\r\n".getBytes());
                    outputStream.flush();

                    bitmapToSend.recycle(); // Very important! Free memory
                }

                Thread.sleep(FRAME_INTERVAL_MS); // Control frame rate (reduce CPU and network usage)
            }

            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception ignored) {}
    }
}
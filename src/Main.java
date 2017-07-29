import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException {
        Serverinfo("198.24.164.162", 33399);
    }

    private static void Serverinfo(String IP, int Port) throws IOException {
        InetSocketAddress host = new InetSocketAddress(IP, Port);
        Socket socket = new Socket();
        socket.connect(host, 3000);
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());
        DataInputStream input = new DataInputStream(socket.getInputStream());
        byte[] handshakeMessage = createHandshakeMessage(IP, Port);
        writeVarInt(output, handshakeMessage.length);
        output.write(handshakeMessage);
        output.writeByte(0x01);
        output.writeByte(0x00);
        @SuppressWarnings("UnusedAssignment") int size = readVarInt(input);
        int packetId = readVarInt(input);

        if (packetId == -1) {
            throw new IOException("Premature end of stream.");
        }

        if (packetId != 0x00) {
            throw new IOException("Invalid packetID");
        }
        int length = readVarInt(input);

        if (length == -1) {
            throw new IOException("Premature end of stream.");
        } else if (length == 0) {
            throw new IOException("Invalid string length.");
        }

        byte[] in = new byte[length];
        input.readFully(in);
        String json = new String(in);

        long now = System.currentTimeMillis();
        output.writeByte(0x09);
        output.writeByte(0x01);
        output.writeLong(now);

        readVarInt(input);
        packetId = readVarInt(input);
        if (packetId == -1) {
            throw new IOException("Premature end of stream.");
        }

        if (packetId != 0x01) {
            throw new IOException("Invalid packetID");
        }
        long pingtime = input.readLong();

        JsonElement jelement = new JsonParser().parse(json);
        JsonObject  jobject = jelement.getAsJsonObject();

        JsonElement version = jobject.get("version");
        JsonElement name = version.getAsJsonObject().get("name");
        JsonElement protocol = version.getAsJsonObject().get("protocol");

        JsonElement players = jobject.get("players");
        JsonElement playersonline = players.getAsJsonObject().get("online");
        JsonElement maxonline = players.getAsJsonObject().get("max");
        JsonElement desc = jobject.get("description");

        System.out.print("Server address: " + IP + ":" + Port + "\n");
        System.out.print("Description: " + desc.toString() + "\n");
        System.out.print("Server Type: " + name + "\n");
        System.out.print("Players Online: " + playersonline + "/" + maxonline + "\n");
        System.out.print("Server Protocol: " + protocol + "\n");
        System.out.print("---------------------------------------------------------" + "\n");
    }
    private static byte[] createHandshakeMessage(String host, int port) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        DataOutputStream handshake = new DataOutputStream(buffer);
        handshake.writeByte(0x00);
        writeVarInt(handshake, 4);
        writeString(handshake, host);
        handshake.writeShort(port);
        writeVarInt(handshake, 1);

        return buffer.toByteArray();
    }
    private static void writeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }
    private static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }
            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }
    private static int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }
}
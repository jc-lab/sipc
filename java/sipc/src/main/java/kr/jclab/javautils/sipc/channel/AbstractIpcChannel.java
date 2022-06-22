package kr.jclab.javautils.sipc.channel;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import kr.jclab.javautils.sipc.ProtoMessageHouse;
import kr.jclab.javautils.sipc.SecurityProviderHolder;
import kr.jclab.javautils.sipc.crypto.CryptoException;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;
import kr.jclab.sipc.common.proto.Frames;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class AbstractIpcChannel implements IpcChannel {
    protected boolean serverMode;
    private byte[] serverPublicKey = null;

    protected byte[] serverNonce = null;
    protected byte[] clientNonce = null;
    protected final String channelId;
    protected final IpcChannelListener ipcChannelListener;
    protected final EphemeralKeyPair myKey;
    protected ClientContext clientContext = null;
    protected long myCounter = 0;
    protected long peerCounter = 0;

    private byte[] sharedMasterSecret = null;

    private IpcChannelStatus ipcChannelStatus = IpcChannelStatus.Idle;

    private final List<CleanupHandler> cleanupHandlers = new ArrayList<>();
    private final Map<String, WrappedDataReceiverHolder<?>> wrappedDataReceivers = new HashMap<>();

    protected AbstractIpcChannel(boolean serverMode, String channelId, IpcChannelListener ipcChannelListener, EphemeralKeyPair myKey) {
        this.serverMode = serverMode;
        this.channelId = channelId;
        this.ipcChannelListener = ipcChannelListener;
        this.myKey = myKey;

        if (!serverMode) {
            this.clientNonce = new byte[32];
            SecurityProviderHolder.SECURE_RANDOM.nextBytes(this.clientNonce);
        }
    }

    public void attachClientContext(ClientContext clientContext) {
        this.clientContext = clientContext;
        this.myCounter = 0;
        this.peerCounter = 0;
    }

    public void setServerPublicKey(byte[] serverPublicKey) {
        this.serverPublicKey = serverPublicKey;
    }

    @Override
    public IpcChannelStatus getChannelStatus() {
        return this.ipcChannelStatus;
    }

    public void sendClientHello() throws IOException {
        byte[] publicKey = this.myKey.getPublicKey();
        Frames.ClientHelloFrame frame = Frames.ClientHelloFrame.newBuilder()
                .setVersion(1)
                .setChannelId(this.getChannelId())
                .setEphemeralPublicKey(ByteString.copyFrom(publicKey))
                .setClientNonce(ByteString.copyFrom(this.clientNonce))
                .build();
        this.sendFrame(frame);
    }

    private void updateChannelStatus(IpcChannelStatus status) {
        this.ipcChannelStatus = status;
        this.ipcChannelListener.onChangeChannelStatus(status);
    }

    @Override
    public String getChannelId() {
        return this.channelId;
    }

    @Override
    public void sendFrame(GeneratedMessageV3 frame) throws IOException {
        if (this.clientContext == null) {
            throw new IOException("Not Ready ClientContext");
        }
        this.clientContext.sendFrame(frame);
    }

    @Override
    public void onAlertFrame(Frames.AlertFrame frame) throws IOException {
        System.out.println("onAlertFrame : " + frame);
    }

    @Override
    public void onServerHello(Frames.ServerHelloFrame frame) throws IOException {
        if (this.serverMode) {
            throw new IOException("Illegal Frame");
        }

        this.serverNonce = frame.getServerNonce().toByteArray();
        this.keyExchange(this.serverPublicKey);

        try {
            Frames.WrappedData wrappedData = this.unwrapDataToRecv(frame.getEncryptedData());
        } catch (RuntimeException e) {
            this.updateChannelStatus(IpcChannelStatus.Error);
            throw e;
        }

        this.updateChannelStatus(IpcChannelStatus.Established);
    }

    @Override
    public void onClientHello(Frames.ClientHelloFrame frame) throws IOException {
        if (!this.serverMode) {
            throw new IOException("Illegal Frame");
        }

        this.serverNonce = new byte[32];
        this.clientNonce = frame.getClientNonce().toByteArray();
        SecurityProviderHolder.SECURE_RANDOM.nextBytes(this.serverNonce);
        this.keyExchange(frame.getEphemeralPublicKey().toByteArray());

        Frames.ServerHelloEncrpytedData encryptedData = Frames.ServerHelloEncrpytedData.newBuilder()
                .setVersion(1)
                .build();

        Frames.ServerHelloFrame serverHelloFrame = Frames.ServerHelloFrame.newBuilder()
                .setVersion(1)
                .setServerNonce(ByteString.copyFrom(this.serverNonce))
                .setEncryptedData(wrapDataToSend(encryptedData.toByteArray()))
                .build();
        this.sendFrame(serverHelloFrame);

        this.updateChannelStatus(IpcChannelStatus.Established);
    }

    private void keyExchange(byte[] remotePublicKey) throws IOException {
        try {
            byte[] ecdhDerivedSecret = this.myKey.derive(remotePublicKey);

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(ecdhDerivedSecret, "HMAC"));
            mac.update(this.serverNonce);
            mac.update(this.clientNonce);
            this.sharedMasterSecret = mac.doFinal();
        } catch (CryptoException | NoSuchAlgorithmException | InvalidKeyException e) {
            this.updateChannelStatus(IpcChannelStatus.Error);
            throw new IOException(e);
        }
    }

    @Override
    public void onWrappedData(Frames.EncryptedWrappedData frame) throws IOException {
        Frames.WrappedData wrappedData = unwrapDataToRecv(frame);
        WrappedDataReceiverHolder<GeneratedMessageV3> receiver = (WrappedDataReceiverHolder<GeneratedMessageV3>) this.wrappedDataReceivers.get(wrappedData.getMessage().getTypeUrl());
        if (receiver == null) {
            System.err.println("receiver is null : " + wrappedData.getMessage().getTypeUrl());
            return ;
        }
        GeneratedMessageV3 decodedMessage = (GeneratedMessageV3)receiver.messageDefaultInstance.newBuilderForType()
                .mergeFrom(wrappedData.getMessage().getValue())
                .build();
        receiver.receiver.onMessage(wrappedData, decodedMessage);
    }

    @Override
    public void onCleanup() {
        boolean hasException = false;
        RuntimeException exception = new RuntimeException("exceptions");
        for (CleanupHandler handler : this.cleanupHandlers) {
            try {
                handler.cleanup();
            } catch (Throwable e) {
                hasException = true;
                exception.addSuppressed(e);
            }
        }
        if (hasException) {
            throw exception;
        }
    }

    @Override
    public synchronized void sendWrappedData(Frames.WrappedData wrappedData) throws IOException {
        Frames.EncryptedWrappedData encryptedWrappedData = wrapDataToSend(wrappedData.toByteArray());
        this.sendFrame(encryptedWrappedData);
    }

    public byte[] generateSecret(String type, long counter, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(type.length() + 8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(type.getBytes(StandardCharsets.UTF_8))
                .putLong(counter);
        ((Buffer)buffer).flip();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(this.sharedMasterSecret, "HMAC"));
            mac.update(buffer);
            byte[] output = mac.doFinal();
            if (output.length > size) {
                return Arrays.copyOfRange(output, 0, size);
            }
            return output;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private Frames.EncryptedWrappedData wrapDataToSend(byte[] data) {
        long counter = this.myCounter++;
        byte[] iv = generateSecret(this.serverMode ? "siv" : "civ", counter, 16);
        byte[] key = generateSecret(this.serverMode ? "sky" : "cky", counter, 32);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    gcmParameterSpec
            );
            byte[] cipherText = cipher.doFinal(data);
            int authTagPos = cipherText.length - (gcmParameterSpec.getTLen() / Byte.SIZE);
            byte[] authTag = Arrays.copyOfRange(cipherText, authTagPos, cipherText.length);

            return Frames.EncryptedWrappedData.newBuilder()
                    .setVersion(1)
                    .setCipherText(ByteString.copyFrom(cipherText, 0, authTagPos))
                    .setAuthTag(ByteString.copyFrom(authTag))
                    .build();
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void safeOutputStreamWrite(OutputStream outputStream, byte[] data) throws IOException {
        if (data != null) {
            outputStream.write(data);
        }
    }

    public Frames.WrappedData unwrapDataToRecv(Frames.EncryptedWrappedData wrappedData) {
        long counter = this.peerCounter++;
        byte[] iv = generateSecret(this.serverMode ? "civ" : "siv", counter, 16);
        byte[] key = generateSecret(this.serverMode ? "cky" : "sky", counter, 32);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, "AES"),
                    gcmParameterSpec
            );
            ByteArrayOutputStream bos = new ByteArrayOutputStream(wrappedData.getCipherText().size());
            safeOutputStreamWrite(bos, cipher.update(wrappedData.getCipherText().toByteArray()));
            safeOutputStreamWrite(bos, cipher.doFinal(wrappedData.getAuthTag().toByteArray()));

            return Frames.WrappedData.parseFrom(bos.toByteArray());
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T extends GeneratedMessageV3> void registerWrappedData(T messageDefaultInstance, WrappedDataReceiver<T> receiver) {
        String typeUrl = ProtoMessageHouse.getTypeUrl(messageDefaultInstance);
        this.wrappedDataReceivers.compute(typeUrl, (k, old) -> {
            if (old != null) {
                throw new IllegalArgumentException("Duplicated type url");
            }
            return new WrappedDataReceiverHolder<T>(messageDefaultInstance, receiver);
        });
    }

    @Override
    public void addCleanupHandler(CleanupHandler cleanupHandler) {
        this.cleanupHandlers.add(cleanupHandler);
    }

    @VisibleForTesting
    private static class WrappedDataReceiverHolder<T extends GeneratedMessageV3> {
        public final GeneratedMessageV3 messageDefaultInstance;
        public final WrappedDataReceiver<T> receiver;

        public WrappedDataReceiverHolder(GeneratedMessageV3 messageDefaultInstance, WrappedDataReceiver<T> receiver) {
            this.messageDefaultInstance = messageDefaultInstance;
            this.receiver = receiver;
        }
    }
}

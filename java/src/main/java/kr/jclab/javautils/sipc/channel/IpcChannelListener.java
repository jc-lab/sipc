package kr.jclab.javautils.sipc.channel;

public interface IpcChannelListener {
    void onChangeChannelStatus(IpcChannelStatus channelStatus);
    void onError(Throwable e);
}

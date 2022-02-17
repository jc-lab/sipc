package kr.jclab.javautils.sipc;

public enum DefaultChannelType {
    Stdio("stdio"),
    Tcp4("tcp4");

    private final String value;
    DefaultChannelType(String value) {
        this.value = value;
    }
    public String value() {
        return this.value;
    }
}

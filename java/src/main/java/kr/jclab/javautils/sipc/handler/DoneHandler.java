package kr.jclab.javautils.sipc.handler;

@FunctionalInterface
public interface DoneHandler {
    /**
     * done
     *
     * @param exception No error if exception is null
     */
    void done(Throwable exception);
}

package mes.app.sse.Transaction;

public interface SseObserver {

    void send(String message);

}

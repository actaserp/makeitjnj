package mes.sse.Transaction;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class SseSubject {

    private final Map<String, List<SseClient>> observers = new ConcurrentHashMap<>();

    public void addObservers(String key, SseClient client){
        observers.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(client);
    }

    public void removeObserver(String key, SseClient client) {
         List<SseClient> list = observers.get(key);

        if(list != null){
            list.remove(client);
            if(list.isEmpty()) observers.remove(key);
        }
    }

    public void notify(String key, String message){
        List<SseClient> list = observers.get(key);

        if(list != null){
            for(SseClient client : list){
                client.send(message);
            }
        }
    }
}

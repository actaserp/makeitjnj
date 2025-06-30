package mes.sse;

import mes.domain.entity.User;
import mes.sse.Transaction.SseClient;
import mes.sse.Transaction.SseSubject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/sse")
public class SseController {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private final SseSubject subject = new SseSubject();
    /*SseController(){
        startBroadcastTestMessages();
    }*/

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam String spjangcd){
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1시간 타임아웃
        SseClient client = new SseClient(emitter);

        System.out.println(spjangcd);

        subject.addObservers(spjangcd, client);

        emitter.onCompletion(() -> subject.removeObserver(spjangcd, client));
        emitter.onTimeout(() -> subject.removeObserver(spjangcd, client));
        emitter.onError((e) -> subject.removeObserver(spjangcd, client));

        try {
            emitter.send(SseEmitter.event().name("연결").data("연결"));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    public void SendingToClientMessage(String spjangcd, String accnum) {
        subject.notify(spjangcd, accnum);
    }

    /*private void startBroadcastTestMessages() {
        final String spjangcd = "ZZ";
        final String accnum = "94160200188218";

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        // 3분 간격으로 ZZ에게만 메시지 전송
        scheduler.scheduleAtFixedRate(() -> {
            SendingToClientMessage(spjangcd, accnum + " (" + spjangcd + ")");
        }, 0, 120, TimeUnit.SECONDS); // 바로 시작, 3분마다 반복
    }*/

    private String getDefaultSpjangcdForUser(String userid) {
        String sql = "SELECT TOP 1 tx.spjangcd " +
            "FROM tb_xa012 tx " +
            "LEFT JOIN auth_user au ON tx.spjangcd = au.spjangcd " +
            "WHERE au.username = ?";

        List<String> result = jdbcTemplate.queryForList(sql, String.class, userid);
        return result.isEmpty() ? "ZZ" : result.get(0); // fallback: ZZ
    }

}

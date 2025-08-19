package mes.domain.model;

import java.util.List;
import java.util.Map;

public record CopyResult(
    int copied,
    int skipped,
    List<String> newReqnums,
    List<Map<String, Object>> failures
) {}
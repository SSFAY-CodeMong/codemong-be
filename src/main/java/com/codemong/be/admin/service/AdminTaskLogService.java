package com.codemong.be.admin.service;

import com.codemong.be.admin.dto.AdminTaskLogResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AdminTaskLogService {

    private static final int MAX_LOG_SIZE = 500;
    private static final Path LOG_FILE_PATH = Path.of(".logs", "admin-log.xml");
    private final Map<String, MutableTaskLog> logs = new ConcurrentHashMap<>();
    private final LinkedList<String> order = new LinkedList<>();

    @PostConstruct
    void loadLogs() {
        if (!Files.exists(LOG_FILE_PATH)) {
            return;
        }
        synchronized (order) {
            try {
                Document document = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(LOG_FILE_PATH.toFile());
                NodeList nodes = document.getElementsByTagName("log");
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element element = (Element) nodes.item(i);
                    MutableTaskLog log = MutableTaskLog.fromElement(element);
                    logs.put(log.id, log);
                    order.add(log.id);
                }
                order.sort((left, right) -> logs.get(right).startedAt.compareTo(logs.get(left).startedAt));
                trimLogs();
                log.info("Admin logs loaded from {}", LOG_FILE_PATH);
            } catch (Exception e) {
                log.warn("Failed to load admin logs from {}", LOG_FILE_PATH, e);
            }
        }
    }

    public String start(String type, String message, Long userId, Long repositoryId, Long branchId, Long step) {
        String id = UUID.randomUUID().toString();
        MutableTaskLog log = new MutableTaskLog(id, type, message, userId, repositoryId, branchId, step);
        synchronized (order) {
            logs.put(id, log);
            order.addFirst(id);
            trimLogs();
            persistLogs();
        }
        return id;
    }

    public void complete(String id, boolean success, String response) {
        MutableTaskLog log = logs.get(id);
        if (log == null) {
            return;
        }
        log.complete(success, response);
        synchronized (order) {
            persistLogs();
        }
    }

    public List<AdminTaskLogResponse> latest() {
        return logs.values().stream()
                .sorted(Comparator.comparing(MutableTaskLog::startedAt).reversed())
                .map(MutableTaskLog::toResponse)
                .toList();
    }

    public long runningCount() {
        return logs.values().stream().filter(log -> "처리중".equals(log.status)).count();
    }

    public long completedCount() {
        return logs.values().stream().filter(log -> "처리완료".equals(log.status)).count();
    }

    public long successfulCount() {
        return logs.values().stream().filter(log -> Boolean.TRUE.equals(log.success)).count();
    }

    public long failedCount() {
        return logs.values().stream().filter(log -> Boolean.FALSE.equals(log.success)).count();
    }

    private void persistLogs() {
        try {
            Files.createDirectories(LOG_FILE_PATH.getParent());
            Files.writeString(LOG_FILE_PATH, toXml(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to persist admin logs to {}", LOG_FILE_PATH, e);
        }
    }

    private void trimLogs() {
        while (order.size() > MAX_LOG_SIZE) {
            String removed = order.removeLast();
            logs.remove(removed);
        }
    }

    private String toXml() {
        StringWriter writer = new StringWriter();
        writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.append("<logs>\n");
        for (String id : order) {
            MutableTaskLog log = logs.get(id);
            if (log == null) {
                continue;
            }
            writer.append("  <log")
                    .append(attr("id", log.id))
                    .append(attr("type", log.type))
                    .append(attr("status", log.status))
                    .append(attr("success", log.success))
                    .append(attr("durationMs", log.durationMs))
                    .append(attr("userId", log.userId))
                    .append(attr("repositoryId", log.repositoryId))
                    .append(attr("branchId", log.branchId))
                    .append(attr("step", log.step))
                    .append(attr("startedAt", log.startedAt))
                    .append(attr("completedAt", log.completedAt))
                    .append(">\n");
            writer.append("    <message>").append(escape(log.message)).append("</message>\n");
            writer.append("    <response>").append(escape(log.response)).append("</response>\n");
            writer.append("  </log>\n");
        }
        writer.append("</logs>\n");
        return writer.toString();
    }

    private String attr(String name, Object value) {
        if (value == null) {
            return "";
        }
        return " " + name + "=\"" + escape(String.valueOf(value)) + "\"";
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static final class MutableTaskLog {
        private final String id;
        private final String type;
        private final String message;
        private final Long userId;
        private final Long repositoryId;
        private final Long branchId;
        private final Long step;
        private final LocalDateTime startedAt;
        private volatile String status;
        private volatile Boolean success;
        private volatile Long durationMs;
        private volatile String response;
        private volatile LocalDateTime completedAt;

        private MutableTaskLog(String id, String type, String message, Long userId, Long repositoryId, Long branchId, Long step) {
            this(id, type, message, userId, repositoryId, branchId, step, LocalDateTime.now(), "처리중", null, null, null, null);
        }

        private MutableTaskLog(
                String id,
                String type,
                String message,
                Long userId,
                Long repositoryId,
                Long branchId,
                Long step,
                LocalDateTime startedAt,
                String status,
                Boolean success,
                Long durationMs,
                String response,
                LocalDateTime completedAt
        ) {
            this.id = id;
            this.type = type;
            this.message = message;
            this.userId = userId;
            this.repositoryId = repositoryId;
            this.branchId = branchId;
            this.step = step;
            this.startedAt = startedAt;
            this.status = status;
            this.success = success;
            this.durationMs = durationMs;
            this.response = response;
            this.completedAt = completedAt;
        }

        private LocalDateTime startedAt() {
            return startedAt;
        }

        private void complete(boolean success, String response) {
            this.success = success;
            this.response = response;
            this.completedAt = LocalDateTime.now();
            this.durationMs = Duration.between(startedAt, completedAt).toMillis();
            this.status = "처리완료";
        }

        private AdminTaskLogResponse toResponse() {
            return new AdminTaskLogResponse(
                    id,
                    type,
                    type,
                    message,
                    status,
                    success,
                    durationMs,
                    userId,
                    repositoryId,
                    branchId,
                    step,
                    response,
                    startedAt,
                    completedAt
            );
        }

        private static MutableTaskLog fromElement(Element element) {
            return new MutableTaskLog(
                    element.getAttribute("id"),
                    value(element, "type", value(element, "action", "UNKNOWN")),
                    text(element, "message"),
                    longValue(element.getAttribute("userId")),
                    longValue(element.getAttribute("repositoryId")),
                    longValue(element.getAttribute("branchId")),
                    longValue(element.getAttribute("step")),
                    dateTimeValue(element.getAttribute("startedAt"), LocalDateTime.now()),
                    value(element, "status", "처리완료"),
                    booleanValue(element.getAttribute("success")),
                    longValue(element.getAttribute("durationMs")),
                    text(element, "response"),
                    dateTimeValue(element.getAttribute("completedAt"), null)
            );
        }

        private static String value(Element element, String name, String defaultValue) {
            String value = element.getAttribute(name);
            return value == null || value.isBlank() ? defaultValue : value;
        }

        private static String text(Element element, String tagName) {
            NodeList nodes = element.getElementsByTagName(tagName);
            if (nodes.getLength() == 0) {
                return "";
            }
            return nodes.item(0).getTextContent();
        }

        private static Long longValue(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Long.valueOf(value);
        }

        private static Boolean booleanValue(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Boolean.valueOf(value);
        }

        private static LocalDateTime dateTimeValue(String value, LocalDateTime defaultValue) {
            if (value == null || value.isBlank()) {
                return defaultValue;
            }
            return LocalDateTime.parse(value);
        }
    }
}

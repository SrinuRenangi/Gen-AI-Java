package code;

import java.time.Instant;

/**
 * Entity modeling an AI Chat Message for Spring Data repository queries.
 *
 * Fields:
 * - id: Primary key
 * - sessionId: Conversation session identifier
 * - role: USER, ASSISTANT, or SYSTEM
 * - content: The textual message content
 * - tokenCount: Number of tokens consumed by this message
 * - createdAt: Timestamp used for chronology and context window slicing
 */
public class ChatMessageEntity {

    private Long id;
    private String sessionId;
    private String role; // "USER", "ASSISTANT", "SYSTEM"
    private String content;
    private int tokenCount;
    private Instant createdAt;

    public ChatMessageEntity() {}

    public ChatMessageEntity(Long id, String sessionId, String role, String content, int tokenCount, Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.tokenCount = tokenCount;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "ChatMessageEntity[id=" + id + ", session='" + sessionId + "', role=" + role 
            + ", tokens=" + tokenCount + ", time=" + createdAt + ", text='" 
            + (content.length() > 30 ? content.substring(0, 27) + "..." : content) + "']";
    }
}

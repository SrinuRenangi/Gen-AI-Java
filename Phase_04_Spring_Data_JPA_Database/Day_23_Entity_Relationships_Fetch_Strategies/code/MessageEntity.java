package code;

/**
 * Child entity: ChatMessage in a conversation.
 *
 * Models the @ManyToOne side of the relationship.
 * The 'chat_messages' table holds the 'session_id' Foreign Key column.
 */
public class MessageEntity {

    private Long id;
    private String role; // "USER", "ASSISTANT", "SYSTEM"
    private String content;
    private int tokenCount;
    private SessionEntity session; // Foreign key back-reference

    public MessageEntity() {}

    public MessageEntity(Long id, String role, String content, int tokenCount) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.tokenCount = tokenCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getTokenCount() { return tokenCount; }
    public void setTokenCount(int tokenCount) { this.tokenCount = tokenCount; }

    public SessionEntity getSession() { return session; }
    public void setSession(SessionEntity session) { this.session = session; }

    @Override
    public String toString() {
        return "MessageEntity[id=" + id + ", role=" + role + ", tokens=" + tokenCount 
            + ", text='" + (content.length() > 25 ? content.substring(0, 22) + "..." : content) + "']";
    }
}

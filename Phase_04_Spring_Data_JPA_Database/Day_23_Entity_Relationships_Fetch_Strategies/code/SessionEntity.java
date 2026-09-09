package code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parent entity: ConversationSession.
 *
 * Models the @OneToMany side with:
 * - mappedBy = "session" (inverse side; MessageEntity owns the foreign key)
 * - cascade = CascadeType.ALL
 * - orphanRemoval = true
 * - Helper methods addMessage() / removeMessage() to maintain bidirectional link consistency.
 */
public class SessionEntity {

    private Long id;
    private String title;
    private String userId;
    private final List<MessageEntity> messages = new ArrayList<>();

    public SessionEntity() {}

    public SessionEntity(Long id, String title, String userId) {
        this.id = id;
        this.title = title;
        this.userId = userId;
    }

    /**
     * Bidirectional helper method:
     * Adds message to parent list AND sets foreign key reference on child!
     */
    public void addMessage(MessageEntity message) {
        messages.add(message);
        message.setSession(this);
    }

    /**
     * Bidirectional helper method:
     * Removes message from parent list AND clears foreign key reference on child!
     */
    public void removeMessage(MessageEntity message) {
        messages.remove(message);
        message.setSession(null);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<MessageEntity> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    @Override
    public String toString() {
        return "SessionEntity[id=" + id + ", title='" + title + "', user='" + userId 
            + "', messageCount=" + messages.size() + "]";
    }
}

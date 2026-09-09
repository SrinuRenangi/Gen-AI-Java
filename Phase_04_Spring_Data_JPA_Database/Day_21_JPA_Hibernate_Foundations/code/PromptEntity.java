package code;

import java.util.Objects;

/**
 * Enterprise AI Prompt Template JPA Entity.
 *
 * In production AI platforms, prompt templates are versioned domain entities
 * stored in PostgreSQL, allowing dynamic prompt updates without redeploying code.
 */
public class PromptEntity {

    private Long id;
    private String name;
    private String templateContent;
    private String modelFamily;
    private int version;
    private boolean active;

    public PromptEntity() {}

    public PromptEntity(String name, String templateContent, String modelFamily) {
        this.name = name;
        this.templateContent = templateContent;
        this.modelFamily = modelFamily;
        this.version = 1;
        this.active = true;
    }

    /**
     * Creates a deep snapshot copy used by Hibernate for Dirty Checking.
     */
    public PromptEntity createSnapshot() {
        PromptEntity snap = new PromptEntity();
        snap.id = this.id;
        snap.name = this.name;
        snap.templateContent = this.templateContent;
        snap.modelFamily = this.modelFamily;
        snap.version = this.version;
        snap.active = this.active;
        return snap;
    }

    public boolean isDirty(PromptEntity snapshot) {
        if (snapshot == null) return true;
        return !Objects.equals(this.name, snapshot.name)
            || !Objects.equals(this.templateContent, snapshot.templateContent)
            || !Objects.equals(this.modelFamily, snapshot.modelFamily)
            || this.version != snapshot.version
            || this.active != snapshot.active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTemplateContent() { return templateContent; }
    public void setTemplateContent(String templateContent) { this.templateContent = templateContent; }

    public String getModelFamily() { return modelFamily; }
    public void setModelFamily(String modelFamily) { this.modelFamily = modelFamily; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "PromptEntity[id=" + id + ", name='" + name + "', model='" + modelFamily 
            + "', version=" + version + ", active=" + active + "]";
    }
}

package code;

import java.time.Instant;

/**
 * Enterprise AI Token Quota Entity.
 *
 * Demonstrates:
 * 1. Optimistic Locking via version field.
 * 2. JPA Auditing fields (createdAt, lastModifiedAt, lastModifiedBy).
 * 3. Atomic token deduction.
 */
public class TokenQuotaEntity {

    private Long id;
    private String tenantId;
    private int remainingTokens;
    private long version; // @Version for Optimistic Locking
    private Instant createdAt;
    private Instant lastModifiedAt;
    private String lastModifiedBy;

    public TokenQuotaEntity() {}

    public TokenQuotaEntity(Long id, String tenantId, int remainingTokens) {
        this.id = id;
        this.tenantId = tenantId;
        this.remainingTokens = remainingTokens;
        this.version = 1L;
        this.createdAt = Instant.now();
        this.lastModifiedAt = Instant.now();
        this.lastModifiedBy = "system";
    }

    public TokenQuotaEntity copy() {
        TokenQuotaEntity c = new TokenQuotaEntity();
        c.id = this.id;
        c.tenantId = this.tenantId;
        c.remainingTokens = this.remainingTokens;
        c.version = this.version;
        c.createdAt = this.createdAt;
        c.lastModifiedAt = this.lastModifiedAt;
        c.lastModifiedBy = this.lastModifiedBy;
        return c;
    }

    public void deductTokens(int tokens, String modifiedBy) {
        if (tokens > remainingTokens) {
            throw new IllegalStateException("Insufficient token quota: requested " + tokens + " but only " + remainingTokens + " remain");
        }
        this.remainingTokens -= tokens;
        this.lastModifiedAt = Instant.now();
        this.lastModifiedBy = modifiedBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public int getRemainingTokens() { return remainingTokens; }
    public void setRemainingTokens(int remainingTokens) { this.remainingTokens = remainingTokens; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModifiedAt() { return lastModifiedAt; }
    public String getLastModifiedBy() { return lastModifiedBy; }

    @Override
    public String toString() {
        return "TokenQuota[tenant='" + tenantId + "', balance=" + remainingTokens 
            + ", version=" + version + ", modifiedBy='" + lastModifiedBy + "']";
    }
}

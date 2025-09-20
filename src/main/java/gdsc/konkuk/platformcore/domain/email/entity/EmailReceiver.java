package gdsc.konkuk.platformcore.domain.email.entity;

import static gdsc.konkuk.platformcore.global.utils.FieldValidator.validateNotNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailReceiver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "receiver_id")
    private Long id;

    @Column(name = "task_id")
    private Long emailTaskId;

    @Column(name = "receiver_email")
    private String email;

    @Column(name = "receiver_name")
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status")
    private EmailSendStatus sendStatus;

    @Column(name = "status_updated_at")
    private LocalDateTime statusUpdatedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Builder
    public EmailReceiver(Long emailTaskId, String email, String name) {
        this.emailTaskId = validateNotNull(emailTaskId, "emailTaskId");
        this.email = validateNotNull(email, "email");
        this.name = validateNotNull(name, "name");
        this.sendStatus = EmailSendStatus.WAITING;
        this.statusUpdatedAt = LocalDateTime.now();
    }

    public void changeStatus(EmailSendStatus newStatus) {
        if (!this.sendStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Invalid status transition: %s -> %s", this.sendStatus, newStatus));
        }
        this.sendStatus = newStatus;
        this.statusUpdatedAt = LocalDateTime.now();

        if (newStatus == EmailSendStatus.COMPLETED) {
            this.sentAt = LocalDateTime.now();
        }
    }

    public void completeSend() {
        this.sendStatus = EmailSendStatus.COMPLETED;
        this.statusUpdatedAt = LocalDateTime.now();
        this.sentAt = LocalDateTime.now();
    }

    public boolean isPendingTimeout(int timeoutMinutes) {
        return this.sendStatus == EmailSendStatus.PENDING &&
            this.statusUpdatedAt != null &&
            this.statusUpdatedAt.isBefore(LocalDateTime.now().minusMinutes(timeoutMinutes));
    }

    @Override
    public int hashCode() {
        return (this.email + this.name).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmailReceiver that = (EmailReceiver) o;
        return email.equals(that.email) && name.equals(that.name);
    }
}

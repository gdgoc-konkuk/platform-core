package gdsc.konkuk.platformcore.external.email;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import gdsc.konkuk.platformcore.application.email.EmailService;
import gdsc.konkuk.platformcore.application.email.dtos.EmailTaskInfo;
import gdsc.konkuk.platformcore.domain.email.entity.EmailDetail;
import gdsc.konkuk.platformcore.domain.email.entity.EmailReceiver;
import gdsc.konkuk.platformcore.domain.email.entity.EmailSendStatus;
import gdsc.konkuk.platformcore.domain.email.entity.EmailTask;
import gdsc.konkuk.platformcore.external.email.exceptions.EmailSendingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.springframework.mail.MailParseException;
import org.springframework.mail.javamail.JavaMailSender;

class EmailClientTest {

    @Mock
    private JavaMailSender javaMailSender;
    @Mock
    private EmailService emailService;

    private EmailClient emailClient;

    @BeforeEach
    void setUp() {
        openMocks(this);
        emailClient = spy(new EmailClient(javaMailSender, emailService));
    }

    @Test
    @DisplayName("이메일의 내용 및 제목 형식 오류 발생")
    void should_fail_when_email_parsing() {
        // given
        EmailTask emailTask =
            EmailTask.builder()
                .id(1L)
                .emailDetail(EmailDetail.builder().subject("예시 이메일 제목").content("Html 문자열")
                    .build())
                .sendAt(LocalDateTime.of(2021, 10, 10, 10, 10))
                .build();
        List<EmailReceiver> emailReceivers = List.of(EmailReceiver.builder()
            .emailTaskId(emailTask.getId())
            .email("aaa@gmail.com").name("guest1")
            .build());
        EmailTaskInfo emailTaskInfo = new EmailTaskInfo(emailTask, emailReceivers);
        // when
        when(javaMailSender.createMimeMessage()).thenThrow(new MailParseException("error"));
        Executable result = () -> emailClient.sendEmailToReceivers(emailTaskInfo);

        // then
        assertThrowsExactly(EmailSendingException.class, result);
    }

    @Test
    @DisplayName("이메일 내용 중 {이름}은 수신자의 이름으로 치환되어야 함")
    void should_replace_to_receiver_name_when_name_token() {
        // given
        EmailTask emailTask =
            EmailTask.builder()
                .id(1L)
                .emailDetail(EmailDetail.builder()
                    .subject("예시 이메일 제목")
                    .content("안녕하세요, {이름}님 합격을 축하드립니다!. {이름}님과 함께할 수 있어 기쁩니다.")
                    .build())
                .sendAt(LocalDateTime.of(2024, 10, 10, 10, 10))
                .build();
        List<EmailReceiver> emailReceivers = List.of(
            EmailReceiver.builder()
                .emailTaskId(emailTask.getId())
                .email("ex@ex.com").name("guest1")
                .build());
        EmailTaskInfo emailTaskInfo = new EmailTaskInfo(emailTask, emailReceivers);

        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        given(javaMailSender.createMimeMessage()).willReturn(mockMimeMessage);
        given(emailClient.replaceNameToken(anyString(), anyString())).willReturn("");

        // when
        emailClient.sendEmailToReceivers(emailTaskInfo);

        // then
        verify(emailClient).replaceNameToken(
            eq("안녕하세요, {이름}님 합격을 축하드립니다!. {이름}님과 함께할 수 있어 기쁩니다."),
            eq("guest1"));
    }

    @Test
    @DisplayName("이메일 이름 토큰({이름}) 치환 테스트")
    void should_success_when_replace_name_token() {
        // given
        String content = "안녕하세요, {이름}님 합격을 축하드립니다!. {이름}님과 함께할 수 있어 기쁩니다.";
        String name = "guest1";

        // when
        String result = emailClient.replaceNameToken(content, name);

        // then
        assertEquals("안녕하세요, guest1님 합격을 축하드립니다!. guest1님과 함께할 수 있어 기쁩니다.", result);
    }

    @Test
    @DisplayName("completeSend() 호출 시 상태가 COMPLETED로 변경되고 시간이 설정된다")
    void completeSend_ShouldUpdateStatusAndTimestamps() {
        // given
        EmailReceiver emailReceiver = EmailReceiver.builder()
            .emailTaskId(1L)
            .email("test@example.com")
            .name("테스트 사용자")
            .build();

        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);

        // when
        emailReceiver.completeSend();

        // then
        LocalDateTime afterCall = LocalDateTime.now().plusSeconds(1);

        assertAll(
            () -> assertThat(emailReceiver.getSendStatus()).isEqualTo(EmailSendStatus.COMPLETED),
            () -> assertThat(emailReceiver.getStatusUpdatedAt()).isNotNull(),
            () -> assertThat(emailReceiver.getStatusUpdatedAt()).isAfter(beforeCall),
            () -> assertThat(emailReceiver.getStatusUpdatedAt()).isBefore(afterCall),
            () -> assertThat(emailReceiver.getSentAt()).isNotNull(),
            () -> assertThat(emailReceiver.getSentAt()).isAfter(beforeCall),
            () -> assertThat(emailReceiver.getSentAt()).isBefore(afterCall)
        );
    }

    @Test
    @DisplayName("WAITING 상태에서 completeSend() 호출 시 정상 동작")
    void completeSend_FromWaitingStatus_ShouldWork() {
        // given
        EmailReceiver emailReceiver = EmailReceiver.builder()
            .emailTaskId(1L)
            .email("test@example.com")
            .name("테스트 사용자")
            .build();

        // EmailReceiver는 생성시 WAITING 상태
        assertThat(emailReceiver.getSendStatus()).isEqualTo(EmailSendStatus.WAITING);

        // when
        emailReceiver.completeSend();

        // then
        assertThat(emailReceiver.getSendStatus()).isEqualTo(EmailSendStatus.COMPLETED);
    }

        @Test
    @DisplayName("completeSend() 호출 전후 sentAt 필드 변화 확인")
    void completeSend_SentAtField_ShouldBeSetCorrectly() {
        // given
        EmailReceiver emailReceiver = EmailReceiver.builder()
            .emailTaskId(1L)
            .email("test@example.com")
            .name("테스트 사용자")
            .build();

        // 초기 상태에서는 sentAt이 null이어야 함
        assertThat(emailReceiver.getSentAt()).isNull();

        // when
        emailReceiver.completeSend();

        // then
        assertThat(emailReceiver.getSentAt()).isNotNull();
    }
}

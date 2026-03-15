package gdsc.konkuk.platformcore.external.discord;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

import gdsc.konkuk.platformcore.external.email.exceptions.EmailClientErrorCode;
import gdsc.konkuk.platformcore.external.email.exceptions.EmailSendingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DiscordClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DiscordClient subject;

    @BeforeEach
    void setUp() {
        openMocks(this);
        subject = new DiscordClient(restTemplate);
        ReflectionTestUtils.setField(subject, "WEB_HOOK_URL", "/hi");
    }

    @Test
    @DisplayName("에러메세지 전송 성공")
    void should_send_error_message() {
        // given
        Exception e = EmailSendingException.of(EmailClientErrorCode.MAIL_SENDING_ERROR);

        // when
        subject.sendErrorMessage(e);

        // then
        verify(restTemplate).postForObject(any(String.class), any(), any());
    }
}

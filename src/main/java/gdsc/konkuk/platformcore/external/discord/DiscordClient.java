package gdsc.konkuk.platformcore.external.discord;

import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordClient {

    @Value("${discord.webhook.url}")
    private String WEB_HOOK_URL;

    private final RestTemplate restTemplate;

    /***
     * Discord Webhook을 통해 에러메세지를 전송한다.
     * @param e 발생한 Exception
     * @author ekgns33
     */
    public void sendErrorMessage(Exception e) {
        DiscordMessage message = DiscordMessage.of(e);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<DiscordMessage> entity = new HttpEntity<>(message, headers);
        try {
            restTemplate.postForObject(
                    UriComponentsBuilder.fromUriString(WEB_HOOK_URL).toUriString(), entity,
                    String.class);
        } catch (Exception ex) {
            //TODO: 웹훅 요청 자체가 잘못된 경우에 대한 처리 고안해야함.
            log.error(Arrays.toString(ex.getStackTrace()));
        }
    }
}

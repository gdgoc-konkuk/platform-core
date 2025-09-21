package gdsc.konkuk.platformcore.application.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.when;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.never;
import static org.mockito.MockitoAnnotations.openMocks;

import gdsc.konkuk.platformcore.application.email.dtos.EmailTaskInfo;
import gdsc.konkuk.platformcore.application.email.dtos.EmailTaskUpsertCommand;
import gdsc.konkuk.platformcore.application.email.exceptions.EmailAlreadyProcessedException;
import gdsc.konkuk.platformcore.application.email.exceptions.EmailNotFoundException;
import gdsc.konkuk.platformcore.controller.email.dtos.EmailSendRequest;
import gdsc.konkuk.platformcore.domain.email.entity.EmailReceiver;
import gdsc.konkuk.platformcore.domain.email.entity.EmailTask;
import gdsc.konkuk.platformcore.domain.email.repository.EmailReceiverRepository;
import gdsc.konkuk.platformcore.domain.email.repository.EmailTaskRepository;
import gdsc.konkuk.platformcore.util.fixture.email.EmailReceiverFixture;
import gdsc.konkuk.platformcore.util.fixture.email.EmailSendRequestFixture;
import gdsc.konkuk.platformcore.util.fixture.email.EmailTaskFixture;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class EmailServiceTest {

    @Mock
    private EmailTaskRepository emailTaskRepository;

    @Mock
    private EmailReceiverRepository emailReceiverRepository;

    private EmailService subject;

    @BeforeEach
    void setUp() {
        openMocks(this);
        subject = new EmailService(emailTaskRepository, emailReceiverRepository);
    }

    @Test
    @DisplayName("getAllTaskInfoAsList : 모든 이메일 전송 작업 목록 조회 성공")
    void should_success_when_getAllTaskInfoAsList() {
        // given
        EmailTask emailTask1 = EmailTaskFixture.builder()
            .id(1L)
            .build().getFixture();
        EmailTask emailTask2 = EmailTaskFixture.builder()
            .id(2L)
            .build().getFixture();

        List<EmailReceiver> emailReceiverList = List.of(
            EmailReceiverFixture.builder()
                .taskId(1L)
                .email("ex1")
                .build().getFixture(),
            EmailReceiverFixture.builder()
                .taskId(1L)
                .email("ex2")
                .build().getFixture(),
            EmailReceiverFixture.builder()
                .taskId(2L)
                .email("ex3")
                .build().getFixture()
        );

        given(emailTaskRepository.findAllByOrderBySendAtDesc())
            .willReturn(List.of(emailTask1, emailTask2));
        given(emailReceiverRepository.findEmailReceiversByEmailTaskId(any()))
            .willReturn(emailReceiverList);

        // when
        List<EmailTaskInfo> actual = subject.getAllTaskInfoAsList();

        //then
        assertEquals(2, actual.size());
        assertEquals(emailTask1.getId(), actual.get(0).emailTask().getId());
        assertEquals(emailTask2.getId(), actual.get(1).emailTask().getId());
    }

    @Test
    @DisplayName("이메일 수신자 완료 처리 - 수신자를 찾을 수 없는 경우 예외 발생")
    void completeEmailReceiver_EmailNotFound_ThrowsException() {
        // given
        Long receiverId = 999L;
        given(emailReceiverRepository.findById(receiverId))
            .willReturn(Optional.empty());

        // when & then
        EmailNotFoundException exception = assertThrows(
            EmailNotFoundException.class,
            () -> subject.completeEmailReceiver(receiverId)
        );

        verify(emailReceiverRepository).findById(receiverId);

    }

    @Test
    @DisplayName("이메일 수신자 완료 처리 - null ID로 호출시 예외 발생")
    void completeEmailReceiver_NullId_ThrowsException() {
        // given
        Long receiverId = null;
        given(emailReceiverRepository.findById(receiverId))
            .willReturn(Optional.empty());

        // when & then
        EmailNotFoundException exception = assertThrows(
            EmailNotFoundException.class,
            () -> subject.completeEmailReceiver(receiverId)
        );

        verify(emailReceiverRepository).findById(null);
    }

    @Test
    @DisplayName("getTaskDetails : 특정 이메일 전송 작업 조회 성공")
    void should_success_when_findById() {
        // given
        EmailTask emailTaskToFind = EmailTaskFixture.builder().build().getFixture();
        given(emailTaskRepository.findById(emailTaskToFind.getId()))
                .willReturn(java.util.Optional.of(emailTaskToFind));

        // when
        EmailTask actual = subject.findById(emailTaskToFind.getId());

        // then
        assertEquals(emailTaskToFind.getId(), actual.getId());
        assertEquals(emailTaskToFind.getEmailDetail().getSubject(),
                actual.getEmailDetail().getSubject());
    }

    @Test
    @DisplayName("registerTask : 이메일 전송 작업 등록 성공")
    void should_success_when_register_task() {
        // given
        EmailSendRequest emailRegisterRequest = EmailSendRequestFixture.builder().build()
                .getFixture();
        EmailTaskUpsertCommand command = EmailSendRequest.toCommand(emailRegisterRequest);


        given(emailTaskRepository.save(any(EmailTask.class)))
                .willReturn(EmailTaskUpsertCommand.toTaskEntity(command));
        given(emailReceiverRepository.saveAll(any()))
                .willReturn(List.of(EmailReceiverFixture.builder().build().getFixture()));

        // when
        EmailTask expected = EmailTaskUpsertCommand.toTaskEntity(command);
        EmailTask actual = subject.registerTask(EmailTaskUpsertCommand.toTaskEntity(command));

        // then
        assertEquals(expected.getEmailDetail(), actual.getEmailDetail());
    }

    @Test
    @DisplayName("update : 이메일 전송 작업 수정 성공")
    void should_success_when_update_task() {
        // given
        EmailSendRequest emailUpdateRequest = EmailSendRequestFixture.builder().build()
                .getFixture();
        EmailTask emailTaskToUpdate = EmailTaskFixture.builder().id(1L).build().getFixture();
        given(emailTaskRepository.findById(emailTaskToUpdate.getId()))
                .willReturn(Optional.of(emailTaskToUpdate));

        // when
        EmailTask actual = subject.update(emailTaskToUpdate.getId(), EmailSendRequest.toCommand(emailUpdateRequest));

        // then
        assertEquals(emailTaskToUpdate.getEmailDetail(), actual.getEmailDetail());
    }

    @Test
    @DisplayName("update : 이미 전송된 작업 수정 시도 실패")
    void should_fail_when_update_task_already_sent() {
        // given
        EmailTask emailTaskAlreadySent = EmailTaskFixture.builder()
                .sendAt(LocalDateTime.now().minusHours(1)).build().getFixture();
        emailTaskAlreadySent.markAsSent();
        EmailSendRequest emailRequest = EmailSendRequestFixture.builder().build().getFixture();
        EmailTaskUpsertCommand command = EmailSendRequest.toCommand(emailRequest);
        // when
        when(emailTaskRepository.findById(emailTaskAlreadySent.getId()))
                .thenReturn(Optional.of(emailTaskAlreadySent));

        // then
        assertThrows(
                EmailAlreadyProcessedException.class,
                () -> subject.update(emailTaskAlreadySent.getId(), command));
    }

    @Test
    @DisplayName("update : 존재하지 않는 작업 수정 시도 실패")
    void should_fail_when_update_task_not_found() {
        // given
        EmailSendRequest emailRequest = EmailSendRequestFixture.builder().build().getFixture();
        EmailTaskUpsertCommand command = EmailSendRequest.toCommand(emailRequest);
        // when
        when(emailTaskRepository.findById(0L))
                .thenReturn(Optional.empty());

        // then
        assertThrows(
                EmailNotFoundException.class,
                () -> subject.update(0L, command));
    }

    @Test
    @DisplayName("delete : 이메일 전송 작업 삭제 성공")
    void should_success_when_delete_task() {
        // given
        EmailTask emailTaskToCancel = EmailTaskFixture.builder().build().getFixture();
        given(emailTaskRepository.findById(emailTaskToCancel.getId()))
                .willReturn(Optional.of(emailTaskToCancel));
        willDoNothing().given(emailTaskRepository).delete(emailTaskToCancel);

        // when
        subject.delete(emailTaskToCancel.getId());

        // then
        verify(emailTaskRepository).delete(emailTaskToCancel);
    }
}

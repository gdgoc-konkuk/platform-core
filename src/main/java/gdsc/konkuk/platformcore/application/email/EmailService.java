package gdsc.konkuk.platformcore.application.email;

import static gdsc.konkuk.platformcore.application.email.mapper.EmailTaskMapper.mapToEmailReceiverList;

import gdsc.konkuk.platformcore.application.email.dtos.EmailReceiverInfo;
import gdsc.konkuk.platformcore.application.email.dtos.EmailTaskUpsertCommand;
import gdsc.konkuk.platformcore.application.email.dtos.EmailTaskInfo;
import gdsc.konkuk.platformcore.application.email.exceptions.EmailAlreadyProcessedException;
import gdsc.konkuk.platformcore.application.email.exceptions.EmailErrorCode;
import gdsc.konkuk.platformcore.application.email.exceptions.EmailNotFoundException;
import gdsc.konkuk.platformcore.domain.email.entity.EmailReceiver;
import gdsc.konkuk.platformcore.domain.email.entity.EmailTask;
import gdsc.konkuk.platformcore.domain.email.repository.EmailReceiverRepository;
import gdsc.konkuk.platformcore.domain.email.repository.EmailTaskRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailService {

    private final EmailTaskRepository emailTaskRepository;
    private final EmailReceiverRepository emailReceiverRepository;


    //////////// --- Query Methods ---
    public List<EmailTaskInfo> getAllTaskInfoAsList() {
        List<EmailTask> tasks = emailTaskRepository.findAllByOrderBySendAtDesc();
        List<Long> taskIds = tasks.stream().map(EmailTask::getId).toList();

        List<EmailReceiver> receivers = emailReceiverRepository.findByEmailTaskIdIn(taskIds);
        Map<Long, List<EmailReceiver>> receiversByTaskId = receivers.stream()
            .collect(Collectors.groupingBy(EmailReceiver::getEmailTaskId));

        return tasks.stream().map(task -> new EmailTaskInfo(task,
            receiversByTaskId.getOrDefault(task.getId(), List.of()))).toList();
    }

    public EmailTaskInfo findByIdWithReceivers(Long taskId) {
        EmailTask emailTask = findById(taskId);
        List<EmailReceiver> receivers = emailReceiverRepository.findEmailReceiversByEmailTaskId(taskId);
        return new EmailTaskInfo(emailTask, receivers);
    }

    public List<EmailTask> getTasksInIds(List<Long> emailIds) {
        return emailTaskRepository.findAllById(emailIds);
    }

    public EmailTask findById(Long taskId) {
        return emailTaskRepository.findById(taskId)
            .orElseThrow(() -> EmailNotFoundException.of(EmailErrorCode.EMAIL_NOT_FOUND));
    }

    public List<EmailTask> getAllTaskWhereNotSent() {
        return emailTaskRepository.findAllWhereNotSent();
    }

    //////////// --- Command Methods ---
    @Transactional
    public EmailTask registerTask(EmailTask emailTask) {
        return emailTaskRepository.save(emailTask);
    }

    @Transactional
    public List<EmailReceiver> registerReceivers(EmailTask task, Set<EmailReceiverInfo> receivers) {
        return emailReceiverRepository.saveAll(mapToEmailReceiverList(task, receivers));
    }

    @Transactional
    public void completeEmailReceiver(Long receiverId) {
        EmailReceiver receiver = emailReceiverRepository.findById(receiverId)
            .orElseThrow(() -> EmailNotFoundException.of(EmailErrorCode.EMAIL_NOT_FOUND));
        receiver.completeSend();
    }

    @Transactional
    public EmailTask update(Long emailId, EmailTaskUpsertCommand command) {
        EmailTask task = findById(emailId);

        validateEmailTaskAlreadySent(task);

        task.changeEmailDetails(command.emailTaskDetail());
        task.changeSendAt(command.sendAt());
        var newReceivers = new HashSet<>(mapToEmailReceiverList(task, command.emailReceiverInfos()));
        mergeEmailReceivers(task, newReceivers);
        return task;
    }

    @Transactional
    public void markAsCompleted(Long emailTaskId) {
        EmailTask emailTask = findById(emailTaskId);
        emailTask.markAsSent();
    }

    @Transactional
    public void delete(Long emailId) {
        EmailTask task = findById(emailId);
        emailTaskRepository.delete(task);
    }

    @Transactional
    public void deleteAll(List<EmailTask> emailTasks) {
        emailTaskRepository.deleteAllInBatch(emailTasks);
    }

    private void mergeEmailReceivers(EmailTask task, Set<EmailReceiver> newReceivers) {
        var oldReceivers = emailReceiverRepository.findEmailReceiversByEmailTaskId(task.getId());
        // 지울 애들만 필터링
        var receiversToDelete = oldReceivers.stream()
            .filter(receiver -> !newReceivers.contains(receiver)).toList();
        emailReceiverRepository.deleteAllInBatch(receiversToDelete);
        emailReceiverRepository.saveAll(newReceivers);
    }

    private void validateEmailTaskAlreadySent(EmailTask emailTask) {
        if (emailTask.isSent()) {
            throw EmailAlreadyProcessedException.of(EmailErrorCode.EMAIL_ALREADY_PROCESSED);
        }
    }
}

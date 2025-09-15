package gdsc.konkuk.platformcore.application.member;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import gdsc.konkuk.platformcore.application.attendance.dtos.MemberAttendanceQueryDto;
import gdsc.konkuk.platformcore.application.attendance.exceptions.AttendanceErrorCode;
import gdsc.konkuk.platformcore.application.attendance.exceptions.ParticipantNotFoundException;
import gdsc.konkuk.platformcore.application.member.dtos.AttendanceUpdateCommand;
import gdsc.konkuk.platformcore.application.member.dtos.MemberAttendanceAggregate;
import gdsc.konkuk.platformcore.application.member.dtos.MemberCreateCommand;
import gdsc.konkuk.platformcore.application.member.dtos.MemberUpdateCommand;
import gdsc.konkuk.platformcore.application.member.exceptions.MemberErrorCode;
import gdsc.konkuk.platformcore.application.member.exceptions.UserAlreadyExistException;
import gdsc.konkuk.platformcore.application.member.exceptions.UserNotFoundException;
import gdsc.konkuk.platformcore.domain.attendance.entity.Participant;
import gdsc.konkuk.platformcore.domain.attendance.repository.AttendanceRepository;
import gdsc.konkuk.platformcore.domain.attendance.repository.ParticipantRepository;
import gdsc.konkuk.platformcore.domain.member.entity.Member;
import gdsc.konkuk.platformcore.domain.member.repository.MemberRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberFinder memberFinder;
    private final MemberRepository memberRepository;
    private final AttendanceRepository attendanceRepository;
    private final ParticipantRepository participantRepository;

    public List<Member> getMembersInBatch(String batch) {
        return memberRepository.findAllActiveByBatch(batch);
    }

    @Transactional
    public Member register(MemberCreateCommand memberCreateCommand) {
        if (memberFinder.checkMemberExistWithStudentId(memberCreateCommand.getStudentId())) {
            throw UserAlreadyExistException.of(MemberErrorCode.USER_ALREADY_EXISTS);
        }
        return memberRepository.save(MemberCreateCommand
                .toEntity(memberCreateCommand));
    }
    @Transactional
    public List<Member> bulkRegister(List<MemberCreateCommand> memberCreateCommands) {
        List<String> studentIds = memberCreateCommands.stream()
            .map(MemberCreateCommand::getStudentId)
            .toList();

        List<String> existingStudentIds = memberFinder.filterExistingStudentIds(studentIds);
        if (!existingStudentIds.isEmpty()) {
            throw UserAlreadyExistException.of(
                MemberErrorCode.USER_ALREADY_EXISTS,
                "이미 존재하는 유저 학번 List : " + existingStudentIds
            );
        }

        List<Member> members = memberCreateCommands.stream()
                .map(MemberCreateCommand::toEntity)
                .toList();
        return memberRepository.saveAll(members);
    }

    @Transactional
    public void withdraw(Long currentId) {
        Member member = memberFinder.fetchMemberById(currentId);
        member.withdraw();
    }

    @Transactional
    public void updateMembers(String batch, @Valid List<MemberUpdateCommand> updateInfos) {
        List<Long> memberIds = updateInfos.stream().map(MemberUpdateCommand::getMemberId).toList();
        Map<Long, Member> memberMap = memberFinder.fetchMembersByIdsAndBatch(memberIds, batch);
        updateMembers(memberMap, updateInfos);
    }

    /**
     * {batch} 기수에 속한 멤버들의 {month}간 출석 정보를 조회하는 메소드
     *
     * @param batch 소속 기수
     * @param month 출석 정보를 조회할 월
     * @return List<MemberAttendances> 멤버별 출석 정보, 통계 dto 리스트
     */
    public List<MemberAttendanceAggregate> getMemberAttendanceWithBatchAndPeriod(String batch,
            LocalDate month) {
        List<MemberAttendanceQueryDto> attendanceInfoList =
                attendanceRepository.findAllAttendanceInfoByBatchAndPeriod(
                        batch,
                        month.withDayOfMonth(1).atStartOfDay(),
                        month.withDayOfMonth(month.lengthOfMonth()).atTime(LocalTime.MAX));
        return MemberAttendanceAggregator.process(attendanceInfoList);
    }

    /***
     * {batch} 기수에 속한 멤버들의 {month}간 출석 정보를 업데이트하는 메소드
     * @param batch 소속 기수
     * @param month 출석 정보를 업데이트할 월
     * @param attendanceUpdateCommandList 업데이트할 출석 정보 리스트
     */
    @Transactional
    public void updateAttendances(
            String batch, LocalDate month,
            List<AttendanceUpdateCommand> attendanceUpdateCommandList) {
        Map<Long, Participant> participantMap = fetchParticipants(batch, month);
        updateAttendanceStatuses(participantMap, attendanceUpdateCommandList);
    }

    private void updateMembers(Map<Long, Member> memberMap,
            List<MemberUpdateCommand> updateCommands) {
        for (MemberUpdateCommand memberUpdateCommand : updateCommands) {
            if (!memberMap.containsKey(memberUpdateCommand.getMemberId())) {
                throw UserNotFoundException.of(MemberErrorCode.USER_NOT_FOUND);
            }
            Member member = memberMap.get(memberUpdateCommand.getMemberId());
            member.update(memberUpdateCommand);
        }
    }

    private void updateAttendanceStatuses(Map<Long, Participant> participants,
            List<AttendanceUpdateCommand> updateCommands) {
        for (AttendanceUpdateCommand attendanceUpdateCommand : updateCommands) {
            if (!participants.containsKey(attendanceUpdateCommand.getParticipantId())) {
                throw ParticipantNotFoundException.of(AttendanceErrorCode.PARTICIPANT_NOT_FOUND);
            }
            Participant participant = participants.get(attendanceUpdateCommand.getParticipantId());
            participant.updateAttendanceStatus(attendanceUpdateCommand.getAttendanceType());
        }
    }

    private Map<Long, Participant> fetchParticipants(String batch, LocalDate month) {
        return participantRepository
                .findAllByBatchAndStartAtBetween(
                        batch,
                        month.withDayOfMonth(1).atStartOfDay(),
                        month.withDayOfMonth(month.lengthOfMonth()).atTime(LocalTime.MAX))
                .stream()
                .collect(toMap(Participant::getId, identity()));
    }
}

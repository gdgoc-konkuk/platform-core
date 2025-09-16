package gdsc.konkuk.platformcore.application.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.spy;

import gdsc.konkuk.platformcore.application.member.dtos.MemberCreateCommand;
import gdsc.konkuk.platformcore.application.member.exceptions.UserAlreadyDeletedException;
import gdsc.konkuk.platformcore.application.member.exceptions.UserAlreadyExistException;
import gdsc.konkuk.platformcore.application.member.exceptions.UserNotFoundException;
import gdsc.konkuk.platformcore.domain.attendance.repository.AttendanceRepository;
import gdsc.konkuk.platformcore.domain.attendance.repository.ParticipantRepository;
import gdsc.konkuk.platformcore.domain.member.entity.Member;
import gdsc.konkuk.platformcore.domain.member.repository.MemberRepository;
import gdsc.konkuk.platformcore.util.fixture.member.MemberFixture;
import gdsc.konkuk.platformcore.util.fixture.member.MemberRegisterRequestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;

class MemberServiceTest {

    private MemberService subject;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ParticipantRepository participantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberFinder memberFinder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        subject = new MemberService(memberFinder, memberRepository, attendanceRepository,
                participantRepository);
    }

    @Test
    @DisplayName("Register : 새로운 멤버 회원가입 성공")
    void should_success_when_newMember_register() {
        // given
        MemberCreateCommand memberCreateCommand = MemberRegisterRequestFixture.builder().build()
                .getFixture()
                .toCommand();
        Member memberToRegister = MemberFixture.builder().build().getFixture();
        given(memberFinder.checkMemberExistWithStudentId(
                memberCreateCommand.getStudentId())).willReturn(
                false);
        given(memberRepository.save(any(Member.class))).willReturn(memberToRegister);

        // when
        Member actual = subject.register(memberCreateCommand);

        // then
        assertNotNull(actual);
    }

    @Test
    @DisplayName("Register : 이미 존재하는 멤버 회원가입 실패")
    void should_fail_when_already_exist_member_register() {
        // given
        MemberCreateCommand memberCreateCommand = MemberRegisterRequestFixture.builder().build()
                .getFixture()
                .toCommand();
        given(memberFinder.checkMemberExistWithStudentId(
                memberCreateCommand.getStudentId())).willReturn(
                true);

        // when
        Executable action = () -> subject.register(memberCreateCommand);

        // then
        assertThrows(UserAlreadyExistException.class, action);
    }

    @Test
    @DisplayName("bulkRegister : bulk로 가입하려는 멤버들 회원가입 성공")
    void should_success_when_newMembers_bulkRegister() {
        // given
        MemberCreateCommand memberCreateCommand1 = MemberRegisterRequestFixture.builder().build()
                .getFixture()
                .toCommand();
        MemberCreateCommand memberCreateCommand2 = MemberRegisterRequestFixture.builder().build()
                .getFixture()
                .toCommand();

        Member savedMember1 = MemberFixture.builder().build().getFixture();
        Member savedMember2 = MemberFixture.builder().build().getFixture();

        List<String> studentIds = List.of(
            memberCreateCommand1.getStudentId(),
            memberCreateCommand2.getStudentId()
        );

        given(memberRepository.findExistingStudentIds(studentIds))
                .willReturn(Collections.emptyList()); // 기존 회원 없음
        given(memberRepository.saveAll(any(List.class)))
                .willReturn(List.of(savedMember1, savedMember2));

        // when
        var actual = subject.bulkRegister(
                List.of(memberCreateCommand1, memberCreateCommand2));

        // then
        assertNotNull(actual);
        assertThat(actual).hasSize(2);
    }

    @Test
    @DisplayName("bulkRegister : bulk로 가입하려는 멤버 중 기등록된 회원이 존재하는 경우 회원가입 실패")
    void should_fail_when_already_exist_members_bulkRegister() {
        // given
        MemberCreateCommand memberCreateCommand1 = MemberRegisterRequestFixture.builder()
                .studentId("12345678")
                .build().getFixture().toCommand();
        MemberCreateCommand memberCreateCommand2 = MemberRegisterRequestFixture.builder()
                .studentId("87654321")
                .build().getFixture().toCommand();
        MemberCreateCommand memberCreateCommand3 = MemberRegisterRequestFixture.builder()
                .studentId("12344321")
                .build().getFixture().toCommand();
        List<String> studentIds = List.of(
            memberCreateCommand1.getStudentId(),
            memberCreateCommand2.getStudentId(),
            memberCreateCommand3.getStudentId()
        );
        given(memberFinder.filterExistingStudentIds(studentIds))
            .willReturn(List.of("12345678"));

        // when & then
        assertThrows(UserAlreadyExistException.class,
            () -> subject.bulkRegister(List.of(
                    memberCreateCommand1, memberCreateCommand2,memberCreateCommand3
            )));
    }

    @Test
    @DisplayName("withdraw : 존재하는 멤버 탈퇴 성공")
    void should_success_when_user_exists() {
        // given
        Member memberToDelete = MemberFixture.builder().build().getFixture();
        given(memberFinder.fetchMemberById(memberToDelete.getId())).willReturn(memberToDelete);

        // when
        subject.withdraw(memberToDelete.getId());

        // then
        assertTrue(memberToDelete.isMemberDeleted());
        assertNotNull(memberToDelete.getSoftDeletedAt());
    }

    @Test
    @DisplayName("withdraw : 존재하지 않는 멤버 탈퇴 실패")
    void should_fail_when_user_not_exists() {
        // given
        given(memberFinder.fetchMemberById(any(Long.class))).willThrow(UserNotFoundException.class);

        // when
        Executable action = () -> subject.withdraw(0L);

        // then
        assertThrows(UserNotFoundException.class, action);
    }

    @Test
    @DisplayName("withdraw : 이미 삭제된 멤버 탈퇴 실패")
    void should_fail_when_user_already_deleted() {
        // given
        Member memberAlreadyDeleted = spy(MemberFixture.builder().build().getFixture());
        given(memberFinder.fetchMemberById(any(Long.class))).willReturn(
                memberAlreadyDeleted);
        given(memberAlreadyDeleted.isMemberDeleted()).willReturn(true);

        // when `Member` soft deleted
        subject.withdraw(memberAlreadyDeleted.getId());
        Executable action = () -> subject.withdraw(memberAlreadyDeleted.getId());

        // then
        assertThrows(UserAlreadyDeletedException.class, action);
    }
}

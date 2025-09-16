package gdsc.konkuk.platformcore.application.member;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import gdsc.konkuk.platformcore.application.member.exceptions.MemberErrorCode;
import gdsc.konkuk.platformcore.application.member.exceptions.UserNotFoundException;
import gdsc.konkuk.platformcore.domain.member.entity.Member;
import gdsc.konkuk.platformcore.domain.member.repository.MemberRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberFinder {

    private final MemberRepository memberRepository;

    public Member fetchMemberById(Long memberId) {
        return memberRepository
                .findById(memberId)
                .orElseThrow(() -> UserNotFoundException.of(MemberErrorCode.USER_NOT_FOUND));
    }
    public List<String> filterExistingStudentIds(List<String> studentIds) {
        return memberRepository.findExistingStudentIds(studentIds);
    }

    public Map<Long, Member> fetchMembersByIdsAndBatch(List<Long> memberIds, String batch) {
        List<Member> members = memberRepository.findAllByIdsAndBatch(memberIds, batch);
        if (members.size() != memberIds.size()) {
            throw UserNotFoundException.of(MemberErrorCode.USER_NOT_FOUND);
        }
        return members.stream().collect(toMap(Member::getId, identity()));
    }

    public boolean checkMemberExistWithStudentId(String studentId) {
        Optional<Member> member = memberRepository.findByStudentId(studentId);
        return member.isPresent();
    }
}

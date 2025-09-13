package gdsc.konkuk.platformcore.domain.member.repository;

import gdsc.konkuk.platformcore.domain.member.entity.Member;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m WHERE m.batch = :batch AND m.isDeleted = false")
    List<Member> findAllActiveByBatch(String batch);

    @Query("SELECT m FROM Member m WHERE m.id IN :memberIds AND m.batch = :batch")
    List<Member> findAllByIdsAndBatch(List<Long> memberIds, String batch);

    Optional<Member> findByStudentId(String studentId);

    Optional<Member> findByEmail(String memberEmail);

    List<Member> findAllByBatch(String batch);

    @Query("SELECT m.studentId FROM Member m WHERE m.studentId IN :studentIds")
    List<String> findExistingStudentIds(List<String> studentIds);
}

package gdsc.konkuk.platformcore.controller.member;

import gdsc.konkuk.platformcore.application.member.MemberService;
import gdsc.konkuk.platformcore.application.member.dtos.MemberAttendanceAggregate;
import gdsc.konkuk.platformcore.controller.member.dtos.AttendanceUpdateRequest;
import gdsc.konkuk.platformcore.controller.member.dtos.MemberInfo;
import gdsc.konkuk.platformcore.controller.member.dtos.MemberRegisterRequest;
import gdsc.konkuk.platformcore.controller.member.dtos.MemberUpdateRequest;
import gdsc.konkuk.platformcore.domain.member.entity.Member;
import gdsc.konkuk.platformcore.global.responses.SuccessResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/{batch}")
    public ResponseEntity<SuccessResponse> getMembers(@PathVariable String batch) {
        List<MemberInfo> memberInfos = memberService.getMembersInBatch(batch).stream()
                .map(MemberInfo::from)
                .toList();
        return ResponseEntity.ok(SuccessResponse.of(memberInfos));
    }

    @PostMapping()
    public ResponseEntity<SuccessResponse> signup(
            @RequestBody @Valid MemberRegisterRequest registerRequest) {
        Member registeredMember = memberService.register(registerRequest.toCommand());
        return ResponseEntity.created(getCreatedURI(registeredMember.getId()))
                .body(SuccessResponse.messageOnly());
    }
    @PostMapping("/bulk")
    public ResponseEntity<SuccessResponse> bulkSignup(
            @RequestBody @Valid List<MemberRegisterRequest> registerRequests) {
        List<Member> registeredMembers = memberService.bulkRegister(
                registerRequests.stream()
                        .map(MemberRegisterRequest::toCommand)
                        .toList()
        );
        return ResponseEntity.created(getCreatedURI((long)registeredMembers.size()))
                .body(SuccessResponse.messageOnly());
    }

    @DeleteMapping("/{batch}/{memberId}")
    public ResponseEntity<SuccessResponse> withdraw(
            @PathVariable String batch, @PathVariable Long memberId
    ) {
        memberService.withdraw(memberId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{batch}")
    public ResponseEntity<SuccessResponse> updateMembers(
            @PathVariable String batch, @RequestBody @Valid MemberUpdateRequest updateInfos) {
        memberService.updateMembers(batch, MemberUpdateRequest.toCommand(updateInfos));
        return ResponseEntity.ok(SuccessResponse.messageOnly());
    }

    @GetMapping("/{batch}/attendances")
    public ResponseEntity<SuccessResponse> getAttendances(
            @PathVariable String batch, @RequestParam Integer year, @RequestParam Integer month) {
        List<MemberAttendanceAggregate> memberAttendanceInfoList =
                memberService.getMemberAttendanceWithBatchAndPeriod(batch,
                        LocalDate.of(year, month, 1));
        return ResponseEntity.ok(SuccessResponse.of(memberAttendanceInfoList));
    }

    @PatchMapping("/{batch}/attendances")
    public ResponseEntity<SuccessResponse> updateAttendances(
            @PathVariable String batch,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestBody @Valid AttendanceUpdateRequest attendanceUpdateRequest) {
        memberService.updateAttendances(
                batch, LocalDate.of(year, month, 1),
                attendanceUpdateRequest.getAttendanceUpdateInfoList());
        return ResponseEntity.ok(SuccessResponse.messageOnly());
    }

    private URI getCreatedURI(Long memberId) {
        return ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(memberId)
                .toUri();
    }
}

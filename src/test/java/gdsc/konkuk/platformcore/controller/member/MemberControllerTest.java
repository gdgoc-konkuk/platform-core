package gdsc.konkuk.platformcore.controller.member;

import static com.epages.restdocs.apispec.ResourceDocumentation.headerWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.parameterWithName;
import static com.epages.restdocs.apispec.ResourceDocumentation.resource;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epages.restdocs.apispec.ResourceSnippetParameters;
import com.fasterxml.jackson.databind.ObjectMapper;
import gdsc.konkuk.platformcore.application.member.MemberService;
import gdsc.konkuk.platformcore.application.member.dtos.AttendanceUpdateCommand;
import gdsc.konkuk.platformcore.application.member.dtos.MemberCreateCommand;
import gdsc.konkuk.platformcore.application.member.exceptions.MemberErrorCode;
import gdsc.konkuk.platformcore.application.member.exceptions.UserAlreadyExistException;
import gdsc.konkuk.platformcore.controller.member.dtos.AttendanceUpdateRequest;
import gdsc.konkuk.platformcore.controller.member.dtos.MemberRegisterRequest;
import gdsc.konkuk.platformcore.controller.member.dtos.MemberUpdateInfo;
import gdsc.konkuk.platformcore.controller.member.dtos.MemberUpdateRequest;
import gdsc.konkuk.platformcore.domain.attendance.entity.AttendanceType;
import gdsc.konkuk.platformcore.domain.member.entity.Member;
import gdsc.konkuk.platformcore.domain.member.entity.MemberRole;
import gdsc.konkuk.platformcore.domain.member.repository.MemberRepository;
import gdsc.konkuk.platformcore.filter.auth.JwtTokenProvider;
import gdsc.konkuk.platformcore.util.annotation.RestDocsTest;
import gdsc.konkuk.platformcore.util.fixture.member.MemberAttendancesFixture;
import gdsc.konkuk.platformcore.util.fixture.member.MemberFixture;
import gdsc.konkuk.platformcore.util.fixture.member.MemberRegisterRequestFixture;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RestDocsTest
@SpringBootTest
class MemberControllerTest {

    MockMvc mockMvc;

    @MockBean
    private MemberService memberService;

    @MockBean
    private MemberRepository memberRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp(
            WebApplicationContext webApplicationContext,
            RestDocumentationContextProvider restDocumentation) {
        mockMvc =
                MockMvcBuilders.webAppContextSetup(webApplicationContext)
                        .apply(springSecurity())
                        .apply(documentationConfiguration(restDocumentation))
                        .build();
    }

    @Test
    @DisplayName("특정 기수의 멤버 목록 조회 성공")
    void should_success_when_get_members() throws Exception {
        // given
        Member member = MemberFixture.builder().role(MemberRole.CORE).build().getFixture();
        String jwt = jwtTokenProvider.createToken(member);
        given(memberService.getMembersInBatch("24-25"))
                .willReturn(
                        List.of(
                                MemberFixture.builder().id(0L).batch("24-25").build().getFixture(),
                                MemberFixture.builder().id(1L).batch("24-25").build().getFixture(),
                                MemberFixture.builder().id(2L).batch("24-25").build()
                                        .getFixture()));

        // when
        ResultActions result =
                mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/api/v1/members/{batch}", "24-25")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(APPLICATION_JSON)
                                .with(csrf()));

        // then
        result
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "member/get_members",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                resource(
                                        ResourceSnippetParameters.builder()
                                                .description("특정 기수의 멤버 목록 조회")
                                                .tag("member")
                                                .pathParameters(
                                                        parameterWithName("batch").description(
                                                                "조회할 멤버 기수"))
                                                .responseFields(
                                                        fieldWithPath("success").description(true),
                                                        fieldWithPath("message").description(
                                                                "멤버 목록 조회 성공"),
                                                        fieldWithPath(
                                                                "data[].memberId").description(
                                                                "멤버 아이디"),
                                                        fieldWithPath(
                                                                "data[].studentId").description(
                                                                "학번"),
                                                        fieldWithPath("data[].email").description(
                                                                "이메일"),
                                                        fieldWithPath("data[].name").description(
                                                                "이름"),
                                                        fieldWithPath(
                                                                "data[].department").description(
                                                                "학과"),
                                                        fieldWithPath("data[].batch").description(
                                                                "배치"),
                                                        fieldWithPath("data[].role").description(
                                                                "역할"))
                                                .build())));
    }

    @Test
    @DisplayName("새로운 멤버 회원 가입 성공")
    void should_success_when_newMember() throws Exception {
        // given
        Member member = MemberFixture.builder().role(MemberRole.CORE).build().getFixture();
        String jwt = jwtTokenProvider.createToken(member);
        MemberRegisterRequest memberRegisterRequest = MemberRegisterRequestFixture.builder()
                .studentId("202400000").build().getFixture();
        Member memberToRegister = MemberFixture.builder()
                .studentId("202400000").build().getFixture();
        given(memberService.register(any(MemberCreateCommand.class))).willReturn(
                memberToRegister);

        // when
        ResultActions result =
                mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/api/v1/members")
                                .contentType(APPLICATION_JSON)
                                .header("Authorization", "Bearer " + jwt)
                                .content(objectMapper.writeValueAsString(memberRegisterRequest))
                                .with(csrf()));

        // then
        result
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(
                        document(
                                "member/register",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                resource(
                                        ResourceSnippetParameters.builder()
                                                .description("새로운 멤버 회원 가입 성공")
                                                .tag("member")
                                                .responseHeaders(
                                                        headerWithName("Location").description(
                                                                "등록한 Member URI"))
                                                .requestFields(
                                                        fieldWithPath("studentId").description(
                                                                "회원 아이디"),
                                                        fieldWithPath("email").description("이메일"),
                                                        fieldWithPath("name").description("이름"),
                                                        fieldWithPath("department").description(
                                                                "학과"),
                                                        fieldWithPath("batch").description("배치"),
                                                        fieldWithPath("role").description("역할"))
                                                .responseFields(
                                                        fieldWithPath("success").description(true),
                                                        fieldWithPath("message").description(
                                                                "회원 가입 성공"),
                                                        fieldWithPath("data").description("null"))
                                                .build())));
    }

    @Test
    @DisplayName("이미 존재하는 유저 회원 가입 실패")
    void should_fail_when_existingMember() throws Exception {
        // given
        Member member = MemberFixture.builder().role(MemberRole.CORE).build().getFixture();
        String jwt = jwtTokenProvider.createToken(member);
        MemberRegisterRequest memberRegisterRequest = MemberRegisterRequestFixture.builder().build()
                .getFixture();
        given(memberService.register(any(MemberCreateCommand.class)))
                .willThrow(UserAlreadyExistException.class);

        // when
        ResultActions result =
                mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/api/v1/members")
                                .contentType(APPLICATION_JSON)
                                .header("Authorization", "Bearer " + jwt)
                                .content(objectMapper.writeValueAsString(memberRegisterRequest))
                                .with(csrf()));

        // then
        result.andDo(print()).andExpect(status().isConflict());
    }


    @Test
    @DisplayName("여러 멤버 회원 가입 성공")
    void should_success_when_bulk_new_member() throws Exception {
        // given
        Member member = MemberFixture.builder().role(MemberRole.CORE).build().getFixture();
        String jwt = jwtTokenProvider.createToken(member);
        List<MemberRegisterRequest> memberRegisterRequests = List.of(
                MemberRegisterRequestFixture.builder().studentId("202400000").build()
                        .getFixture(),
                MemberRegisterRequestFixture.builder().studentId("202400001").build()
                        .getFixture(),
                MemberRegisterRequestFixture.builder().studentId("202400002").build()
                        .getFixture());
        List<Member> memberToRegister = List.of(
                MemberFixture.builder().studentId("202400000").build().getFixture(),
                MemberFixture.builder().studentId("202400001").build().getFixture(),
                MemberFixture.builder().studentId("202400002").build().getFixture(),
                MemberFixture.builder().studentId("202400003").build().getFixture(),
                MemberFixture.builder().studentId("202400004").build().getFixture());
        given(memberService.bulkRegister(any()))
                .willReturn(memberToRegister);

        // when
        ResultActions result =
                mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/api/v1/members/bulk")
                                .contentType(APPLICATION_JSON)
                                .header("Authorization", "Bearer " + jwt)
                                .content(objectMapper.writeValueAsString(memberRegisterRequests))
                                .with(csrf()));

        // then
        result
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(
                        document(
                                "member/bulk_register",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                resource(
                                        ResourceSnippetParameters.builder()
                                                .description("여러 멤버 회원 가입 성공")
                                                .tag("member")
                                                .responseHeaders(
                                                        headerWithName("Location").description(
                                                                "등록한 Member URI"))
                                                .requestFields(
                                                        fieldWithPath("[].studentId").description(
                                                                "회원 아이디"),
                                                        fieldWithPath("[].email").description(
                                                                "이메일"),
                                                        fieldWithPath("[].name").description(
                                                                "이름"),
                                                        fieldWithPath("[].department").description(
                                                                "학과"),
                                                        fieldWithPath("[].batch").description(
                                                                "배치"),
                                                        fieldWithPath("[].role").description(
                                                                "역할"))
                                                .responseFields(
                                                        fieldWithPath("success").description(true),
                                                        fieldWithPath("message").description(
                                                                "회원 가입 성공"),
                                                        fieldWithPath("data").description("null"))
                                                .build())));
    }

    @Test
    @DisplayName("여러 멤버 회원 가입 실패 - 이미 존재하는 학번이 포함된 경우")
    void should_throw_UserAlreadyExistException_when_studentId_already_exists() throws Exception {
        // given
        Member member = MemberFixture.builder().role(MemberRole.CORE).build().getFixture();
        String jwt = jwtTokenProvider.createToken(member);

        List<MemberRegisterRequest> memberRegisterRequests = List.of(
                MemberRegisterRequestFixture.builder().studentId("202400000").build().getFixture(),
                MemberRegisterRequestFixture.builder().studentId("202400001").build().getFixture(), // 중복 학번
                MemberRegisterRequestFixture.builder().studentId("202400002").build().getFixture()
        );

        given(memberService.bulkRegister(argThat(requests ->
            requests.stream().anyMatch(request -> "202400001".equals(request.getStudentId()))
        )))
        .willThrow(UserAlreadyExistException.of(
            MemberErrorCode.USER_ALREADY_EXISTS,
            "이미 존재하는 유저 학번 List : [202400001]"  // 테스트용 메시지
        ));

        // when
        ResultActions result =
                mockMvc.perform(
                        RestDocumentationRequestBuilders.post("/api/v1/members/bulk")
                                .contentType(APPLICATION_JSON)
                                .header("Authorization", "Bearer " + jwt)
                                .content(objectMapper.writeValueAsString(memberRegisterRequests))
                                .with(csrf()));

        // then
        result
                .andDo(print())
                .andExpect(status().isConflict())
                .andDo(
                        document(
                                "member/bulk_register_fail",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                resource(
                                        ResourceSnippetParameters.builder()
                                                .description("여러 멤버 회원 가입 실패 - 중복 학번 포함")
                                                .tag("member")
                                                .requestFields(
                                                        fieldWithPath("[].studentId").description("회원 아이디"),
                                                        fieldWithPath("[].email").description("이메일"),
                                                        fieldWithPath("[].name").description("이름"),
                                                        fieldWithPath("[].department").description("학과"),
                                                        fieldWithPath("[].batch").description("배치"),
                                                        fieldWithPath("[].role").description("역할"))
                                                .responseFields(
                                                        fieldWithPath("success").description(false),
                                                        fieldWithPath("message").description("에러 메시지"),
                                                        fieldWithPath("errorCode").description("에러 코드")) // data 제거, errorCode 추가
                                                .build())));
    }
    @Test
    @DisplayName("회원 탈퇴 성공")
    void should_success_when_delete_member() throws Exception {
        // given
        Member member = MemberFixture.builder().role(MemberRole.CORE).build().getFixture();
        String jwt = jwtTokenProvider.createToken(member);
        Member memberToWithdraw = MemberFixture.builder().build().getFixture();
        willDoNothing().given(memberService).withdraw(memberToWithdraw.getId());

        // when
        ResultActions result =
                mockMvc.perform(
                        RestDocumentationRequestBuilders
                                .delete("/api/v1/members/{batch}/{memberId}", "24-25", 1L)
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(APPLICATION_JSON)
                                .with(csrf()));

        // then
        result
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(
                        document(
                                "member/delete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                resource(
                                        ResourceSnippetParameters.builder()
                                                .description("존재하는 회원 탈퇴")
                                                .tag("member")
                                                .pathParameters(
                                                        parameterWithName("batch").description(
                                                                "탈퇴할 멤버 기수"),
                                                        parameterWithName("memberId").description(
                                                                "탈퇴할 멤버 아이디"))
                                                .build())));
    }

    @Test
    @DisplayName("멤버 정보 수정 성공")
    void should_success_when_update_member() throws Exception {
        // given
        Member member = MemberFixture.builder().role(MemberRole.CORE).build().getFixture();
        String jwt = jwtTokenProvider.createToken(member);
        MemberUpdateRequest memberUpdateRequest = new MemberUpdateRequest(List.of(
                MemberUpdateInfo.builder().memberId(1L).studentId("202400000").build(),
                MemberUpdateInfo.builder().memberId(2L).name("member2").build(),
                MemberUpdateInfo.builder().memberId(3L).email("member3").build()));
        willDoNothing().given(memberService)
                .updateMembers("24-25", MemberUpdateRequest.toCommand(memberUpdateRequest));

        // when
        ResultActions result =
                mockMvc.perform(
                        RestDocumentationRequestBuilders.patch("/api/v1/members/{batch}", "24-25")
                                .header("Authorization", "Bearer " + jwt)
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(memberUpdateRequest))
                                .with(csrf()));

        // then
        result
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "member/update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                resource(
                                        ResourceSnippetParameters.builder()
                                                .description("멤버 정보 수정")
                                                .tag("member")
                                                .pathParameters(
                                                        parameterWithName("batch").description(
                                                                "수정할 멤버 기수"))
                                                .requestFields(
                                                        fieldWithPath(
                                                                "memberUpdateInfoList[].memberId").description(
                                                                "멤버 아이디").optional(),
                                                        fieldWithPath(
                                                                "memberUpdateInfoList[].studentId").description(
                                                                "학번").optional(),
                                                        fieldWithPath(
                                                                "memberUpdateInfoList[].name").description(
                                                                "이름").optional(),
                                                        fieldWithPath(
                                                                "memberUpdateInfoList[].email").description(
                                                                "이메일").optional(),
                                                        fieldWithPath(
                                                                "memberUpdateInfoList[].department").description(
                                                                "학과").optional(),
                                                        fieldWithPath(
                                                                "memberUpdateInfoList[].batch").description(
                                                                "배치").optional(),
                                                        fieldWithPath(
                                                                "memberUpdateInfoList[].role").description(
                                                                "역할").optional())
                                                .responseFields(
                                                        fieldWithPath("success").description(true),
                                                        fieldWithPath("message").description(
                                                                "멤버 정보 수정 성공"),
                                                        fieldWithPath("data").description("null"))
                                                .build())));
    }

    @Test
    @DisplayName("특정 배치의 특정 월의 멤버 출석 정보 조회 성공")
    void should_success_when_get_attendances_by_batch() throws Exception {
        // given
        Member member = MemberFixture.builder().role(MemberRole.CORE).build().getFixture();
        String jwt = jwtTokenProvider.createToken(member);
        // TODO: 좀 더 상세하고 정확한 Fixture 필요 (`batch`, `eventId`, `participantId` 등)
        given(memberService.getMemberAttendanceWithBatchAndPeriod(anyString(), any()))
                .willReturn(
                        List.of(
                                MemberAttendancesFixture.builder().memberId(1L)
                                        .memberName("member1").build().getFixture(),
                                MemberAttendancesFixture.builder().memberId(2L)
                                        .memberName("member2").build().getFixture(),
                                MemberAttendancesFixture.builder().memberId(3L)
                                        .memberName("member3").build().getFixture()));

        // when
        ResultActions result =
                mockMvc.perform(
                        RestDocumentationRequestBuilders.get("/api/v1/members/{batch}/attendances",
                                        "24-25")
                                .header("Authorization", "Bearer " + jwt)
                                .param("year", "2024")
                                .param("month", "07")
                                .contentType(APPLICATION_JSON)
                                .with(csrf()));

        // then
        result
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "member/attendances",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                resource(
                                        ResourceSnippetParameters.builder()
                                                .description("특정 달의 멤버 출석 정보 조회")
                                                .tag("member")
                                                .pathParameters(
                                                        parameterWithName("batch").description(
                                                                "조회할 멤버 기수"))
                                                .queryParameters(
                                                        parameterWithName("year").description(
                                                                "조회할 년도"),
                                                        parameterWithName("month").description(
                                                                "조회할 월"),
                                                        parameterWithName("_csrf").ignored())
                                                .responseFields(
                                                        fieldWithPath("success").description(true),
                                                        fieldWithPath("message").description(
                                                                "멤버 출석 정보 조회 성공"),
                                                        fieldWithPath("data").description(
                                                                "멤버 출석 정보 리스트"),
                                                        fieldWithPath(
                                                                "data[].memberId").description(
                                                                "멤버 아이디"),
                                                        fieldWithPath(
                                                                "data[].memberName").description(
                                                                "멤버 이름"),
                                                        fieldWithPath(
                                                                "data[].memberRole").description(
                                                                "멤버 역할"),
                                                        fieldWithPath(
                                                                "data[].department").description(
                                                                "멤버 학과"),
                                                        fieldWithPath(
                                                                "data[].totalAttendances").description(
                                                                "전체 등록 횟수"),
                                                        fieldWithPath(
                                                                "data[].actualAttendances").description(
                                                                "실제 출석 횟수"),
                                                        fieldWithPath(
                                                                "data[].attendanceInfoList[].attendanceId")
                                                                .description("출석 아이디"),
                                                        fieldWithPath(
                                                                "data[].attendanceInfoList[].memberId")
                                                                .description("멤버 아이디"),
                                                        fieldWithPath(
                                                                "data[].attendanceInfoList[].participantId")
                                                                .description("참가자 아이디"),
                                                        fieldWithPath(
                                                                "data[].attendanceInfoList[].attendanceDate")
                                                                .description("출석 날짜"),
                                                        fieldWithPath(
                                                                "data[].attendanceInfoList[].attendanceType")
                                                                .description("출석 여부"))
                                                .build())));
    }

    @Test
    @DisplayName("특정 배치의 특정 월의 멤버 출석 정보 수정 성공")
    void should_success_when_update_attendances_by_batch() throws Exception {
        // given
        Member member = MemberFixture.builder().role(MemberRole.CORE).build().getFixture();
        String jwt = jwtTokenProvider.createToken(member);
        List<AttendanceUpdateCommand> attendanceUpdateCommandList = List.of(
                AttendanceUpdateCommand.builder().participantId(1L)
                        .attendanceType(AttendanceType.ATTEND).build(),
                AttendanceUpdateCommand.builder().participantId(2L)
                        .attendanceType(AttendanceType.ABSENT).build(),
                AttendanceUpdateCommand.builder().participantId(3L)
                        .attendanceType(AttendanceType.LATE)
                        .build());
        AttendanceUpdateRequest attendanceUpdateRequest = new AttendanceUpdateRequest(
                attendanceUpdateCommandList);
        willDoNothing().given(memberService).updateAttendances(
                "24-25",
                LocalDate.of(2024, 7, 1),
                attendanceUpdateCommandList);

        // when
        ResultActions result =
                mockMvc.perform(
                        RestDocumentationRequestBuilders.patch(
                                        "/api/v1/members/{batch}/attendances", "24-25")
                                .header("Authorization", "Bearer " + jwt)
                                .param("year", "2024")
                                .param("month", "07")
                                .contentType(APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(attendanceUpdateRequest))
                                .with(csrf()));

        // then
        result
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(
                        document(
                                "member/update_attendances",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                resource(
                                        ResourceSnippetParameters.builder()
                                                .description("특정 달의 멤버 출석 정보 수정")
                                                .tag("member")
                                                .pathParameters(
                                                        parameterWithName("batch").description(
                                                                "수정할 멤버 기수"))
                                                .queryParameters(
                                                        parameterWithName("year").description(
                                                                "수정할 년도"),
                                                        parameterWithName("month").description(
                                                                "수정할 월"),
                                                        parameterWithName("_csrf").ignored())
                                                .requestFields(
                                                        fieldWithPath(
                                                                "attendanceUpdateInfoList[]").description(
                                                                "출석 정보 수정 리스트"),
                                                        fieldWithPath(
                                                                "attendanceUpdateInfoList[].participantId")
                                                                .description("참가자 아이디"),
                                                        fieldWithPath(
                                                                "attendanceUpdateInfoList[].attendanceType")
                                                                .description("출석 여부"))
                                                .responseFields(
                                                        fieldWithPath("success").description(true),
                                                        fieldWithPath("message").description(
                                                                "멤버 출석 정보 수정 성공"),
                                                        fieldWithPath("data").description("null"))
                                                .build())));
    }
}

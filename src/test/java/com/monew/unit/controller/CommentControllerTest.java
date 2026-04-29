package com.monew.unit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monew.config.LoginUserArgumentResolver;
import com.monew.controller.CommentController;
import com.monew.dto.request.CreateCommentRequest;
import com.monew.dto.request.UpdateCommentRequest;
import com.monew.dto.response.CommentDto;
import com.monew.dto.response.CommentLikeResponse;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.service.CommentService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class CommentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private CommentService commentService;

  @MockitoBean
  private LoginUserArgumentResolver loginUserArgumentResolver;

  private UUID userId;
  private UUID commentId;
  private UUID articleId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    commentId = UUID.randomUUID();
    articleId = UUID.randomUUID();
    
    // LoginUserArgumentResolver 모킹
    given(loginUserArgumentResolver.supportsParameter(any())).willReturn(true);
    given(loginUserArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userId);
  }

  @Test
  @DisplayName("댓글 생성 API")
  void createComment() throws Exception {
    // given
    CreateCommentRequest request = new CreateCommentRequest(articleId, userId, "content");
    CommentDto response = new CommentDto(commentId, articleId, userId, "nickname", "content", 0, false, Instant.now());
    
    given(commentService.createComment(eq(userId), eq(articleId), eq("content")))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/comments")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(commentId.toString()))
        .andExpect(jsonPath("$.content").value("content"));
  }

  @Test
  @DisplayName("댓글 수정 API")
  void updateComment() throws Exception {
    // given
    UpdateCommentRequest request = new UpdateCommentRequest("updated content");
    CommentDto response = new CommentDto(commentId, articleId, userId, "nickname", "updated content", 0, false, Instant.now());
    
    given(commentService.updateComment(eq(userId), eq(commentId), eq("updated content")))
        .willReturn(response);

    // when & then
    mockMvc.perform(patch("/api/comments/{commentId}", commentId)
            .header("Monew-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("updated content"));
  }

  @Test
  @DisplayName("댓글 논리 삭제 API")
  void deleteComment() throws Exception {
    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}", commentId)
            .header("Monew-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("댓글 좋아요 API")
  void likeComment() throws Exception {
    // given
    CommentLikeResponse response = new CommentLikeResponse(
        UUID.randomUUID(), userId, Instant.now(), commentId, articleId, UUID.randomUUID(), "nickname", "content", 1, Instant.now()
    );
    given(commentService.likeComment(eq(userId), eq(commentId))).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.commentLikeCount").value(1));
  }

  @Test
  @DisplayName("댓글 목록 조회 API")
  void getComments() throws Exception {
    // given
    CursorPageResponseDto<CommentDto> response = CursorPageResponseDto.<CommentDto>builder()
        .content(List.of())
        .hasNext(false)
        .build();
    
    given(commentService.getComments(any(), any())).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/comments")
            .param("articleId", articleId.toString())
            .param("orderBy", "CREATED_AT")
            .param("direction", "desc")
            .param("limit", "10")
            .header("Monew-Request-User-ID", userId.toString()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("댓글 물리 삭제 API")
  void hardDeleteComment() throws Exception {
    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/hard", commentId)
            .header("Monew-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("댓글 좋아요 취소 API")
  void unlikeComment() throws Exception {
    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/comment-likes", commentId)
            .header("Monew-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());
  }
}

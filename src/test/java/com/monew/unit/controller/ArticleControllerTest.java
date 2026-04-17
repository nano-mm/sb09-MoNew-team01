package com.monew.unit.controller;

import com.monew.config.SecurityConfig;
import com.monew.controller.ArticleController;
import com.monew.dto.response.ArticleDto;
import com.monew.dto.response.ArticleViewDto;
import com.monew.dto.response.CursorPageResponseDto;
import com.monew.entity.Article;
import com.monew.entity.ArticleView;
import com.monew.entity.User;
import com.monew.exception.article.ArticleNotFoundException;
import com.monew.mapper.ArticleViewMapper;
import com.monew.repository.UserRepository;
import com.monew.service.impl.ArticleServiceImpl;
import com.monew.service.impl.ArticleViewServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArticleController.class)
@Import(SecurityConfig.class)
class ArticleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ArticleServiceImpl articleService;

  @MockitoBean
  private ArticleViewServiceImpl articleViewService;

  @MockitoBean
  private ArticleViewMapper articleViewMapper;

  // LoginUserArgumentResolver때문에 추가...
  @MockitoBean
  private UserRepository userRepository;

  private final UUID ARTICLE_ID = UUID.randomUUID();
  private final UUID USER_ID = UUID.randomUUID();
  private final String HEADER_USER_ID = "Monew-Request-User-ID";

  @Test
  @DisplayName("출처 목록 조회 - 성공")
  void getSources_Success() throws Exception {
    List<String> mockSources = List.of("NAVER", "CHOSUN");
    given(articleService.getSources()).willReturn(mockSources);

    mockMvc.perform(get("/api/articles/sources"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0]").value("NAVER"))
        .andExpect(jsonPath("$[1]").value("CHOSUN"));
  }

  @Test
  @DisplayName("기사 단건 조회 - 성공")
  void search_Success() throws Exception {
    ArticleDto mockDto = ArticleDto.builder().id(ARTICLE_ID).title("테스트 기사").build();
    given(articleService.find(ARTICLE_ID)).willReturn(mockDto);

    mockMvc.perform(get("/api/articles/{articleId}", ARTICLE_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("테스트 기사"));
  }

  @Test
  @DisplayName("기사 단건 조회 - 실패 (존재하지 않는 기사)")
  void search_Fail_NotFound() throws Exception {
    given(articleService.find(ARTICLE_ID)).willThrow(new ArticleNotFoundException(ARTICLE_ID));

    mockMvc.perform(get("/api/articles/{articleId}", ARTICLE_ID))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("조회수 생성 - 성공")
  void createArticleView_Success() throws Exception {
    Article mockArticle = Article.builder().title("test").build();
    User mockUser = User.builder().nickname("test").build();

    ReflectionTestUtils.setField(mockArticle, "id", ARTICLE_ID);
    ReflectionTestUtils.setField(mockUser, "id", USER_ID);

    ArticleView articleView = ArticleView.builder()
        .article(mockArticle)
        .user(mockUser)
        .build();

    ArticleViewDto mockResponse = articleViewMapper.toDto(articleView);
    given(articleViewService.create(ARTICLE_ID, USER_ID)).willReturn(mockResponse);

    mockMvc.perform(post("/api/articles/{articleId}/article-views", ARTICLE_ID)
            .header(HEADER_USER_ID, USER_ID.toString()))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("조회수 생성 - 실패 (필수 헤더 누락)")
  void createArticleView_Fail_MissingHeader() throws Exception {

    mockMvc.perform(post("/api/articles/{articleId}/article-views", ARTICLE_ID))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("기사 목록 페이징 조회 - 성공")
  void searchArticles_Success() throws Exception {
    ArticleDto dummyArticle1 = ArticleDto.builder().id(UUID.randomUUID()).title("first").build();
    ArticleDto dummyArticle2 = ArticleDto.builder().id(UUID.randomUUID()).title("second").build();
    List<ArticleDto> dummyArticles = List.of(dummyArticle1, dummyArticle2);

    CursorPageResponseDto<ArticleDto> mockResponse = new CursorPageResponseDto<>(
        dummyArticles,
        UUID.randomUUID().toString(),
        null,
        null,
        2L,
        true
    );

    given(articleService.findArticles(any(), any(), any())).willReturn(mockResponse);

    mockMvc.perform(get("/api/articles")
            .header(HEADER_USER_ID, USER_ID.toString())
            .param("limit", "10")
            .param("orderBy", "publishDate")
            .param("direction", "DESC"))
        .andExpect(status().isOk())

        .andExpect(jsonPath("$.content[0].title").value("first"))
        .andExpect(jsonPath("$.content[1].title").value("second"))
        .andExpect(jsonPath("$.hasNext").value(true));
  }

  @Test
  @DisplayName("기사 목록 페이징 조회 - 실패 (잘못된 파라미터 타입)")
  void searchArticles_Fail_TypeMismatch() throws Exception {
    mockMvc.perform(get("/api/articles")
            .header(HEADER_USER_ID, USER_ID.toString())
            .param("limit", "ten"))
        .andExpect(status().isBadRequest());
  }


  @Test
  @DisplayName("기사 논리 삭제 - 성공")
  void softDelete_Success() throws Exception {
    mockMvc.perform(delete("/api/articles/{articleId}", ARTICLE_ID))
        .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("기사 물리 삭제 - 성공")
  void hardDelete_Success() throws Exception {
    mockMvc.perform(delete("/api/articles/{articleId}/hard", ARTICLE_ID))
        .andExpect(status().isNoContent());
  }
}

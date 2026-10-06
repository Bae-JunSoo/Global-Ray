package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.exception.NotFoundException;
import kopo.poly.globalray.dto.BoardCommentDto;
import kopo.poly.globalray.dto.BoardDto;
import kopo.poly.globalray.entity.BoardCommentEntity;
import kopo.poly.globalray.entity.BoardEntity;
import kopo.poly.globalray.repository.BoardCommentRepository;
import kopo.poly.globalray.repository.BoardRepository;
import kopo.poly.globalray.service.IBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements IBoardService {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_COMMENT_LENGTH = 1000;

    private final BoardRepository boardRepository;
    private final BoardCommentRepository boardCommentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BoardDto> getPostList(int page, String keyword) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), 15);
        String cleanKeyword = keyword == null ? "" : keyword.trim();

        Page<BoardEntity> posts = cleanKeyword.isEmpty()
                ? boardRepository.findAllByOrderByRegDtDesc(pageable)
                : boardRepository.findByTitleContainingOrderByRegDtDesc(cleanKeyword, pageable);

        return posts.map(e -> BoardDto.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .regDt(e.getRegDt())
                        .viewCount(e.getViewCount())
                        .commentCount(e.getComments().size())
                        .build());
    }

    @Override
    @Transactional
    public BoardDto getPost(Long id, String loginUserId) {
        BoardEntity entity = findPost(id);
        entity.increaseViewCount();

        List<BoardCommentDto> comments = boardCommentRepository
                .findByBoardIdOrderByRegDtAsc(id)
                .stream()
                .map(c -> BoardCommentDto.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .regDt(c.getRegDt())
                        .modDt(c.getModDt())
                        .mine(c.getUserId().equals(loginUserId))
                        .build())
                .toList();

        return BoardDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .regDt(entity.getRegDt())
                .modDt(entity.getModDt())
                .viewCount(entity.getViewCount())
                .commentCount(comments.size())
                .mine(entity.getUserId().equals(loginUserId))
                .comments(comments)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BoardDto getPostForEdit(Long id, String userId) {
        BoardEntity entity = findPost(id);
        checkOwner(entity.getUserId(), userId);
        return BoardDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .build();
    }

    @Override
    @Transactional
    public void writePost(String title, String content, String userId) {
        validatePost(title, content);
        boardRepository.save(BoardEntity.builder()
                .title(title.trim())
                .content(content.trim())
                .userId(userId)
                .build());
    }

    // save() 호출 없이 엔티티 값만 바꾸면 트랜잭션 커밋 시 변경 감지(Dirty Checking)로 UPDATE 실행
    @Override
    @Transactional
    public void updatePost(Long id, String title, String content, String userId) {
        validatePost(title, content);
        BoardEntity entity = findPost(id);
        checkOwner(entity.getUserId(), userId);
        entity.update(title.trim(), content.trim());
    }

    @Override
    @Transactional
    public void deletePost(Long id, String userId) {
        BoardEntity entity = findPost(id);
        checkOwner(entity.getUserId(), userId);
        boardRepository.delete(entity);
    }

    @Override
    @Transactional
    public void addComment(Long boardId, String content, String userId) {
        validateComment(content);
        BoardEntity board = findPost(boardId);
        boardCommentRepository.save(BoardCommentEntity.builder()
                .board(board)
                .content(content.trim())
                .userId(userId)
                .build());
    }

    @Override
    @Transactional
    public void updateComment(Long commentId, String content, String userId) {
        validateComment(content);
        BoardCommentEntity comment = findComment(commentId);
        checkOwner(comment.getUserId(), userId);
        comment.update(content.trim());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String userId) {
        BoardCommentEntity comment = findComment(commentId);
        checkOwner(comment.getUserId(), userId);
        boardCommentRepository.delete(comment);
    }

    private BoardCommentEntity findComment(Long commentId) {
        return boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));
    }

    private void validateComment(String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("댓글 내용을 입력해주세요.");
        }
        if (content.trim().length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("댓글은 " + MAX_COMMENT_LENGTH + "자 이내로 입력해주세요.");
        }
    }

    private BoardEntity findPost(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("게시글을 찾을 수 없습니다."));
    }

    private void checkOwner(String ownerId, String userId) {
        if (!ownerId.equals(userId)) {
            throw new AccessDeniedException("작성자만 수정하거나 삭제할 수 있습니다.");
        }
    }

    private void validatePost(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목을 입력해주세요.");
        }
        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("제목은 " + MAX_TITLE_LENGTH + "자 이내로 입력해주세요.");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용을 입력해주세요.");
        }
    }
}

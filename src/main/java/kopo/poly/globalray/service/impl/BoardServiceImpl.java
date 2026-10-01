package kopo.poly.globalray.service.impl;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements IBoardService {

    private final BoardRepository boardRepository;
    private final BoardCommentRepository boardCommentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BoardDto> getPostList(int page) {
        return boardRepository.findAllByOrderByRegDtDesc(PageRequest.of(page, 15))
                .map(e -> BoardDto.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .regDt(e.getRegDt())
                        .viewCount(e.getViewCount())
                        .commentCount(e.getComments().size())
                        .build());
    }

    @Override
    @Transactional
    public BoardDto getPost(Long id) {
        BoardEntity entity = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        entity.increaseViewCount();

        List<BoardCommentDto> comments = boardCommentRepository
                .findByBoardIdOrderByRegDtAsc(id)
                .stream()
                .map(c -> BoardCommentDto.builder()
                        .id(c.getId())
                        .content(c.getContent())
                        .regDt(c.getRegDt())
                        .build())
                .collect(Collectors.toList());

        return BoardDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .regDt(entity.getRegDt())
                .viewCount(entity.getViewCount())
                .commentCount(comments.size())
                .comments(comments)
                .build();
    }

    @Override
    @Transactional
    public void writePost(String title, String content, String userId) {
        boardRepository.save(BoardEntity.builder()
                .title(title)
                .content(content)
                .userId(userId)
                .build());
    }

    @Override
    @Transactional
    public void deletePost(Long id, String userId) {
        BoardEntity entity = boardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        if (!entity.getUserId().equals(userId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        boardRepository.delete(entity);
    }

    @Override
    @Transactional
    public void addComment(Long boardId, String content, String userId) {
        BoardEntity board = boardRepository.findById(boardId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        boardCommentRepository.save(BoardCommentEntity.builder()
                .board(board)
                .content(content)
                .userId(userId)
                .build());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, String userId) {
        BoardCommentEntity comment = boardCommentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        if (!comment.getUserId().equals(userId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        boardCommentRepository.delete(comment);
    }
}

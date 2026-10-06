package kopo.poly.globalray.service;

import kopo.poly.globalray.dto.BoardDto;
import org.springframework.data.domain.Page;

public interface IBoardService {
    Page<BoardDto> getPostList(int page, String keyword);
    BoardDto getPost(Long id, String loginUserId);
    BoardDto getPostForEdit(Long id, String userId);
    void writePost(String title, String content, String userId);
    void updatePost(Long id, String title, String content, String userId);
    void deletePost(Long id, String userId);
    void addComment(Long boardId, String content, String userId);
    void updateComment(Long commentId, String content, String userId);
    void deleteComment(Long commentId, String userId);
}

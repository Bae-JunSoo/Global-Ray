package kopo.poly.globalray.repository;

import kopo.poly.globalray.entity.BoardCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BoardCommentRepository extends JpaRepository<BoardCommentEntity, Long> {
    List<BoardCommentEntity> findByBoardIdOrderByRegDtAsc(Long boardId);
}

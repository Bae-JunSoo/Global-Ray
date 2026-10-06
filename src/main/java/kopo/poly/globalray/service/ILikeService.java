package kopo.poly.globalray.service;

public interface ILikeService {

    // 좋아요 토글 (true = 좋아요, false = 취소)
    boolean toggleLike(String userId, String articleUrl);

    // 회원 탈퇴 시 해당 회원의 좋아요 전체 삭제
    void deleteAllByUser(String userId);
}

package kopo.poly.globalray.service;

public interface ILikeService {

    // 좋아요 토글 — true: 추가됨, false: 취소됨
    boolean toggleLike(String userId, String articleUrl);

    // 특정 유저가 해당 기사에 좋아요 눌렀는지 여부
    boolean isLiked(String userId, String articleUrl);
}

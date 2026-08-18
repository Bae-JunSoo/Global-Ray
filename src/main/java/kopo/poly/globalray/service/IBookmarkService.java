package kopo.poly.globalray.service;

public interface IBookmarkService {

    // 북마크 토글 (있으면 삭제, 없으면 추가)
    // true = 북마크 추가, false = 북마크 해제
    boolean toggleBookmark(String userId, String articleUrl);
}
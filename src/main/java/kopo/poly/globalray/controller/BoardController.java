package kopo.poly.globalray.controller;

import kopo.poly.globalray.dto.BoardDto;
import kopo.poly.globalray.service.IBoardService;
import kopo.poly.globalray.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
public class BoardController {

    private final IBoardService boardService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        Page<BoardDto> postPage = boardService.getPostList(page, cleanKeyword);
        model.addAttribute("postPage", postPage);
        model.addAttribute("currentPage", postPage.getNumber());
        model.addAttribute("keyword", cleanKeyword);
        model.addAttribute("pageTitle", "익명 게시판");
        return "board/list";
    }

    @GetMapping("/write")
    @PreAuthorize("isAuthenticated()")
    public String writePage(Model model) {
        model.addAttribute("pageTitle", "글쓰기");
        return "board/write";
    }

    @PostMapping("/write")
    @PreAuthorize("isAuthenticated()")
    public String write(@RequestParam String title,
                        @RequestParam String content,
                        Principal principal) {
        String userId = SecurityUtil.extractUserId(principal);
        boardService.writePost(title, content, userId);
        return "redirect:/board";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        BoardDto post = boardService.getPost(id, SecurityUtil.extractUserId(principal));
        model.addAttribute("post", post);
        model.addAttribute("pageTitle", post.getTitle());
        return "board/detail";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editPage(@PathVariable Long id, Principal principal, Model model) {
        model.addAttribute("post", boardService.getPostForEdit(id, SecurityUtil.extractUserId(principal)));
        model.addAttribute("pageTitle", "글 수정");
        return "board/write";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("isAuthenticated()")
    public String edit(@PathVariable Long id,
                       @RequestParam String title,
                       @RequestParam String content,
                       Principal principal) {
        boardService.updatePost(id, title, content, SecurityUtil.extractUserId(principal));
        return "redirect:/board/" + id;
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deletePost(@PathVariable Long id, Principal principal) {
        String userId = SecurityUtil.extractUserId(principal);
        boardService.deletePost(id, userId);
        return "redirect:/board";
    }

    @PostMapping("/{id}/comment")
    @PreAuthorize("isAuthenticated()")
    public String addComment(@PathVariable Long id,
                             @RequestParam String content,
                             Principal principal) {
        String userId = SecurityUtil.extractUserId(principal);
        boardService.addComment(id, content, userId);
        return "redirect:/board/" + id;
    }

    @PostMapping("/comment/{commentId}/edit")
    @PreAuthorize("isAuthenticated()")
    public String editComment(@PathVariable Long commentId,
                              @RequestParam String content,
                              @RequestParam Long boardId,
                              Principal principal) {
        boardService.updateComment(commentId, content, SecurityUtil.extractUserId(principal));
        return "redirect:/board/" + boardId;
    }

    @PostMapping("/comment/{commentId}/delete")
    @PreAuthorize("isAuthenticated()")
    public String deleteComment(@PathVariable Long commentId,
                                @RequestParam Long boardId,
                                Principal principal) {
        String userId = SecurityUtil.extractUserId(principal);
        boardService.deleteComment(commentId, userId);
        return "redirect:/board/" + boardId;
    }
}

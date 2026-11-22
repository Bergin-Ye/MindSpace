package com.mindspace.controller;
import com.mindspace.common.result.PageResult;
import com.mindspace.common.result.Result;
import com.mindspace.dto.ChatRequestDTO;
import com.mindspace.dto.ChatSessionUpdateDTO;
import com.mindspace.service.ChatHistoryService;
import com.mindspace.service.ChatService;
import com.mindspace.vo.ChatAnswerVO;
import com.mindspace.vo.ChatMessageVO;
import com.mindspace.vo.ChatSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatHistoryService chatHistoryService;

    /*
    * RAG智能问答
    * 其实没那么高级或者可以叫做 和ai对话
    * */
    @PostMapping("/ask")
    public Result<ChatAnswerVO> ask(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody ChatRequestDTO dto){
        return Result.success(chatService.ask(userId, dto));
    }

    /*
     * 会话列表
     * */
    @GetMapping("/sessions")
    public Result<PageResult<ChatSessionVO>> listSessions(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "10") long size) {
        return Result.success(
                chatHistoryService.listSessions(userId, knowledgeBaseId, page, size));
    }

    /*
     * 某个会话的消息列表
     * */
    @GetMapping("/sessions/{id}/messages")
    public Result<PageResult<ChatMessageVO>> listMessages(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "size", defaultValue = "20") long size) {
        return Result.success(
                chatHistoryService.listMessages(userId, id, page, size));
    }

    /*
     * 重命名会话
     * */
    @PutMapping("/sessions/{id}")
    public Result<ChatSessionVO> renameSession(
            @AuthenticationPrincipal Long userId,
            @PathVariable("id") Long id,
            @RequestBody @Valid ChatSessionUpdateDTO dto) {
        return Result.success(
                chatHistoryService.renameSession(userId, id, dto));
    }

    /*
     * 删除会话
     * */
    @DeleteMapping("/sessions/{id}")
    public Result<Void> deleteSession(@AuthenticationPrincipal Long userId,
                                      @PathVariable("id") Long id) {
        chatHistoryService.deleteSession(userId, id);
        return Result.success();
    }
}

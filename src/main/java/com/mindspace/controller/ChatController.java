package com.mindspace.controller;
import com.mindspace.common.result.Result;
import com.mindspace.dto.ChatRequestDTO;
import com.mindspace.service.ChatService;
import com.mindspace.vo.ChatAnswerVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    @Autowired
    private ChatService chatService;

    /*
    * RAG智能问答
    * 其实没那么高级或者可以叫做 和ai对话
    * */
    @PostMapping("/ask")
    public Result<ChatAnswerVO> ask(@AuthenticationPrincipal Long userId,
                                    @Valid @RequestBody ChatRequestDTO dto){
        return Result.success(chatService.ask(userId, dto));
    }
}

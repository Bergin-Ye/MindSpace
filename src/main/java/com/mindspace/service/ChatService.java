package com.mindspace.service;

import com.mindspace.dto.ChatRequestDTO;
import com.mindspace.vo.ChatAnswerVO;
import jakarta.validation.Valid;


public interface ChatService {


    //RAG问答
    ChatAnswerVO ask(Long userId, @Valid ChatRequestDTO dto);

}

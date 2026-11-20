package com.mindspace.service;

import com.mindspace.vo.DocumentDetailVO;

public interface DocumentAiService {

    /*
    * ai解析的接口
    * */
    DocumentDetailVO analyze(Long userId, Long id);
}

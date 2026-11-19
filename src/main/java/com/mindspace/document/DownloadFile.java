package com.mindspace.document;

import org.springframework.core.io.Resource;

/**
 * 下载接口的返回对象，不参与 JSON 序列化。
 */
public record DownloadFile(Resource resource, String originalName, long size) {
}
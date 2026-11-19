package com.mindspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mindspace.common.exception.BusinessException;
import com.mindspace.common.result.ResultCode;
import com.mindspace.entity.Document;
import com.mindspace.entity.Folder;
import com.mindspace.entity.KnowledgeBase;
import com.mindspace.mapper.DocumentMapper;
import com.mindspace.mapper.FolderMapper;
import com.mindspace.mapper.KnowledgeBaseMapper;
import com.mindspace.service.DocumentService;
import com.mindspace.vo.BatchUploadItemVO;
import com.mindspace.vo.BatchUploadResultVO;
import com.mindspace.vo.DocumentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    //允许上传文件类型
    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "docx", "txt", "md");
    //最大文件大小
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    //注入Mapper接口
    private final DocumentMapper documentMapper;
    private final FolderMapper folderMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;


    //指定接受的文件存储在哪个路径 通过application.yaml配置
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    //单文件上传
    @Override
    public DocumentVO upload(Long userId, Long knowledgeBaseId, Long folderId, MultipartFile file) {
        //确认一下你要上传的路径 知识库是你的吗
        checkKnowledgeBase(userId, knowledgeBaseId);
        //检查一下文件放在哪个文件夹
        Long targetFolderId = resolveFolderId(userId, knowledgeBaseId, folderId);
        //都通过就上传
        return uploadOne(userId, knowledgeBaseId, targetFolderId, file);
    }

    //批量上传
    @Override
    public BatchUploadResultVO uploadBatch(Long userId, Long knowledgeBaseId, Long folderId, List<MultipartFile> files) {

        //确认一下你传过来的文件是不是空的 或者 没传文件
        if (files == null || files.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的文件");
        }

        //知识库和文件夹只校验一次 避免每个文件重复查库
        checkKnowledgeBase(userId, knowledgeBaseId);
        //检查一下文件放在哪个文件夹
        Long targetFolderId = resolveFolderId(userId, knowledgeBaseId, folderId);

        //初始化一个列表BatchUploadItemVO格式的 用于存储每个文件的上传结果
        List<BatchUploadItemVO> items = new ArrayList<>();
        //初始化成功个数
        int successCount = 0;

        //根据收到的一堆文件 然后一个一个遍历
        for (MultipartFile file : files) {
            //我们的数组有多个files对吧 我们每一个都需要有一个结果对象 叫做item
            BatchUploadItemVO item = new BatchUploadItemVO();
            //先存储初始文件名字 这样后端失败也知道哪个失败了
            item.setOriginalName(file.getOriginalFilename());
            //然后我们要去try catch一下
            //try catch就是try里面有执行失败 就去catch里面执行 抓错误并且不中断循环
            try {
                //根据需要返回给前端的vo格式去存储
                //然后用uploadOne去做检验
                DocumentVO vo = uploadOne(userId, knowledgeBaseId, targetFolderId, file);
                //成功就设置成功状态
                item.setSuccess(true);
                item.setDocumentId(vo.getId());
                item.setCode(ResultCode.SUCCESS.getCode());
                item.setMessage(ResultCode.SUCCESS.getMessage());
                successCount++;
            } catch (BusinessException e) {
                //失败就设置失败状态 并且存储错误码和提示
                item.setSuccess(false);
                item.setCode(e.getCode());
                item.setMessage(e.getMessage());
            } catch (Exception e) {
                //并且还要记录日志和抛出异常
                log.error("批量上传文件失败：{}", file.getOriginalFilename(), e);
                item.setSuccess(false);
                item.setCode(ResultCode.SYSTEM_ERROR.getCode());
                item.setMessage(ResultCode.SYSTEM_ERROR.getMessage());
            }
            items.add(item);
        }
        //如果成功就定义一个result来存储所有结果
        BatchUploadResultVO result = new BatchUploadResultVO();
        //获取结果统计信息 这里是直接setItem就是直接给你全部数据 而不是循环一个一个给
        result.setTotal(files.size());
        result.setSuccessCount(successCount);
        result.setFailCount(files.size() - successCount);
        result.setItems(items);
        //返回结果
        return result;
    }

    //这里是单个文件上传从检验到入库的全流程
    private DocumentVO uploadOne(Long userId, Long knowledgeBaseId,
                                 Long folderId,
                                 MultipartFile file) {
        //检验文件
        //检验文件类型
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_EMPTY);
        }
        //检验文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ResultCode.FILE_SIZE_EXCEEDED);
        }

        // 2. 去掉路径，只保留文件名，防止原始文件名里带 ../ 之类的内容
        //防止注入攻击 清洗老旧浏览器携带的路径
        String originalName = sanitizeName(file.getOriginalFilename());
        //对原本文件名进行长度校验
        if (originalName.length() > 255) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件名不能超过255个字符");
        }

        // 3. 取扩展名并校验类型
        //源文件名字 判断一下扩展 也就是末尾类型.xxx这个东西看下是否符合
        String extension = getExtension(originalName);

        // 4. 计算 SHA-256
        //hash是一个工具 他有这些功能 但是不一定用到了 用于 去重 完整性校验 芳村改 当存储文件名 内容寻址 快速比对 断点续传 黑名单匹配
        String hash = sha256(file);

        //5. 去重 去数据库查一下这个用户这个知识库是否存在这个文件
        Long count = documentMapper.selectCount(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getUserId, userId)
                        .eq(Document::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(Document::getFileHash, hash));
        //如果存在就不让你上传了 也就是重复文件
        if (count > 0) {
            throw new BusinessException(ResultCode.FILE_ALREADY_EXISTS);
        }

        // 6. 生成服务端文件名并保存到本地
        //定义存储名字 生成一个uuid 然后去掉横线变成三十二位十六进制 然后拼接上扩展名
        /*
        * uploadDir = "uploads"
          userId    = 123
          * -> uploads/user_123
          * -> /app/uploads/user_123
          * -> /app/uploads/user_123/../user_456  ->  /app/uploads/user_456
        * */
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        //定义用户目录 把根目录和userid拼接起来 然后转换成绝对路径 并且规范一下
        Path userDir = Paths.get(uploadDir, "user_" + userId).toAbsolutePath().normalize();
        //拼接最终文件路径 路径+文件名 + 规范化
        Path target = userDir.resolve(storedName).normalize();

        try {
            //运行上面的
            Files.createDirectories(userDir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            //出错 就异常处理 抛出异常
            log.error("保存文件失败：{}", originalName, e);
            throw new BusinessException(ResultCode.FILE_SAVE_FAILED);
        }

        // 7. 写数据库；失败时把已经存盘的文件删掉，避免磁盘和数据库不一致
        try {
            //上面都没问题了 定一个document对象来存储数据库信息
            //然后把没问题的数据都赋值给doc
            Document doc = new Document();
            doc.setUserId(userId);
            doc.setKnowledgeBaseId(knowledgeBaseId);
            doc.setFolderId(folderId);
            doc.setOriginalName(originalName);
            doc.setFileName(storedName);
            doc.setFilePath("user_" + userId + "/" + storedName);
            doc.setFileType(extension);
            doc.setFileSize(file.getSize());
            doc.setFileHash(hash);
            doc.setProcessStatus("UPLOADED");

            //插入数据库
            documentMapper.insert(doc);

            //返回最终结果
            return DocumentVO.from(documentMapper.selectById(doc.getId()));
        } catch (Exception e) {
            try {
                //如果插入数据库失败 就删除本地文件
                //避免磁盘和数据库不一致
                Files.deleteIfExists(target);
            } catch (IOException ex) {
                //如果删除文件失败 就异常处理 抛出异常
                log.error("回滚删除文件失败：{}", target, ex);
            }
            //打印日志
            log.error("保存文件记录失败：{}", originalName, e);
            //抛出异常
            throw new BusinessException(ResultCode.FILE_SAVE_FAILED);
        }

    }

    /**
     * 校验知识库是否属于当前用户
     */
    private void checkKnowledgeBase(Long userId, Long knowledgeBaseId) {
        //定义count 去数据库查数量
        Long count = knowledgeBaseMapper.selectCount(
                //根据id userid 去数据库 查知识库id的数量
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getId, knowledgeBaseId)
                        .eq(KnowledgeBase::getUserId, userId));
        //如果没有知识库 就抛出异常
        if (count == 0) {
            throw new BusinessException(ResultCode.KB_NOT_FOUND);
        }
    }


    /**
     * 决定文件放在哪个文件夹：
     * 传了 folderId 就校验它；没传就用该知识库的默认“未分类”。
     */
    private Long resolveFolderId(Long userId, Long knowledgeBaseId, Long folderId) {
        //传过来文件夹id存在 并且 不是根文件夹（0L）
        //就是正常情况 文件知道要放哪个文件夹 并且不是丢到根文件夹
        if (folderId != null && folderId != 0L) {
            //你想用这个文件夹 我就要验证 id userid 知识库id 是否是你的
            Folder folder = folderMapper.selectOne(
                    new LambdaQueryWrapper<Folder>()
                            .eq(Folder::getId, folderId)
                            .eq(Folder::getUserId, userId)
                            .eq(Folder::getKnowledgeBaseId, knowledgeBaseId));
            //如果没有文件夹 就抛出异常
            if (folder == null) {
                throw new BusinessException(ResultCode.FOLDER_NOT_FOUND);
            }
            //返回文件夹id
            return folder.getId();
        }
        //folderId = null && folderId = 0L
        //就是没穿文件夹id 和 传了根文件夹（0L） 那就是知识库的默认“未分类”文件夹（默认文件夹）
        Folder defaultFolder = folderMapper.selectOne(
                //这里就是根据id userid 知识库id 去找这个的默认文件夹
                new LambdaQueryWrapper<Folder>()
                        .eq(Folder::getUserId, userId)
                        .eq(Folder::getKnowledgeBaseId, knowledgeBaseId)
                        .eq(Folder::getIsDefault, 1));
        //不存在默认文件夹 就跑出异常
        if (defaultFolder == null) {
            throw new BusinessException(ResultCode.FOLDER_NOT_FOUND);
        }
        //存在就返回默认文件夹id
        return defaultFolder.getId();
    }

    /**
     * 只保留文件名本身，去掉浏览器可能带上的路径
     */
    //本质是一个安全方法
    //求带老旧浏览器或者恶意输入的文件名 只保留文件名本身
    private String sanitizeName(String rawName) {
        //如果文件名为空 或者 空格 就返回unnamed
        if (rawName == null || rawName.isBlank()) {
            return "unnamed";
        }
        //去掉传过来的文件名的\\替换成/
        String name = rawName.replace("\\", "/");
        //去掉文件名前面的路径信息
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        //如果文件名为空 或者 空格 就返回unnamed
        return name.isEmpty() ? "unnamed" : name;
    }

    /**
     * 取小写扩展名，并校验是否在允许列表里
     */
    //获取拓展名
    private String getExtension(String fileName) {
        //dot代表着文件名最后一个和.的位置
        int dot = fileName.lastIndexOf('.');
        //如果dot的位置在文件名的开头 或者 在文件名的结尾 就抛出异常
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }
        //获取文件名的拓展名 如果不在允许列表里 就抛出异常
        String extension = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(extension)) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }
        //返回拓展名
        return extension;
    }

    /**
     * 流式计算 SHA-256，避免把大文件整个读进内存
     */
    //计算文件的哈希值
    private String sha256(MultipartFile file) {
        //打开输入流
        try (InputStream in = file.getInputStream()) {
            //获取SHA-256的哈希值
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //缓冲区 8192字节
            byte[] buffer = new byte[8192];
            int len;
            //循环读 一次读8192字节
            while ((len = in.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
            //返回文件的哈希值
            return HexFormat.of().formatHex(digest.digest());
            //如果计算文件哈希值失败 就抛出异常
        } catch (IOException | NoSuchAlgorithmException e) {
            log.error("计算文件哈希失败", e);
            throw new BusinessException(ResultCode.FILE_SAVE_FAILED);
        }
    }
}



package com.mindspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mindspace.common.exception.BusinessException;
import com.mindspace.common.result.PageResult;
import com.mindspace.common.result.ResultCode;
import com.mindspace.document.DownloadFile;
import com.mindspace.dto.DocumentMoveDTO;
import com.mindspace.dto.DocumentUpdateDTO;
import com.mindspace.entity.Document;
import com.mindspace.entity.Folder;
import com.mindspace.entity.KnowledgeBase;
import com.mindspace.mapper.DocumentMapper;
import com.mindspace.mapper.FolderMapper;
import com.mindspace.mapper.KnowledgeBaseMapper;
import com.mindspace.service.DocumentService;
import com.mindspace.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.apache.tika.Tika;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    //允许上传文件类型
    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "docx", "txt", "md");
    //最大文件大小
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    //提取正文的最大字符数，超出部分截断
    private static final int MAX_EXTRACTED_TEXT_LENGTH = 5_000_000;

    //error_message 的最大长度，避免写库过长
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    //Tika 门面可以复用；有初始化的 final 字段不会被 @RequiredArgsConstructor 纳入构造参数
    private final Tika tika = new Tika();



    //注入Mapper接口
    private final DocumentMapper documentMapper;
    private final FolderMapper folderMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;


    //指定接受的文件存储在哪个路径 通过application.yaml配置
    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    // 异步解析线程池；@Autowired + @Qualifier 按名字注入，避免和 Spring Boot 默认线程池冲突
    @Autowired
    @Qualifier("documentParseExecutor")
    private ThreadPoolTaskExecutor parseExecutor;

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




    /*······················上面是上传文件操作 下面是上传完对文件的一些操作················*/

    //查询文件分页列表
    @Override
    public PageResult<DocumentVO> list(Long userId, Long knowledgeBaseId, Long folderId, String keyword, String processStatus, long page, long size) {
        //检验当前知识库是不是你
        checkKnowledgeBase(userId, knowledgeBaseId);
        //如果传递了folderid（指定了文件夹） 检验文件夹属于你这个知识库
        if (folderId != null && folderId != 0L) {
            checkFolder(userId, knowledgeBaseId, folderId);
        }

        //修正分页参数 如果page小于1 就默认第一页
        long currentPage = page < 1 ? 1 : page;
        long pageSize = size < 1 ? 10 : Math.min(size, 100);

        //组装查询条件
        //按照Document进行封装
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<Document>()
                //知识库id userId来加固权限
                .eq(Document::getUserId, userId)
                .eq(Document::getKnowledgeBaseId, knowledgeBaseId)

                //可选条件 folderId 个接口支持“全部 / 按文件夹 / 按状态 / 按关键词 / 任意组合”的筛选
                .eq(folderId != null && folderId != 0L, Document::getFolderId, folderId)
                .eq(StringUtils.hasText(processStatus), Document::getProcessStatus, processStatus)
                .like(StringUtils.hasText(keyword), Document::getOriginalName,
                        keyword == null ? null : keyword.trim())
                .orderByDesc(Document::getCreateTime)
                .orderByDesc(Document::getId);

        //分页查询
        //去数据库通过page size 去查数据 查出来的封装进mpPage
        Page<Document> mpPage = documentMapper.selectPage(new Page<>(currentPage, pageSize), wrapper);

        //上面得到的mpPage去拿数据 然后转换成VO
        List<DocumentVO> records = mpPage.getRecords().stream()
                //这一段意思就是把 10 个 Document 逐个变成 10 个 DocumentVO
                .map(DocumentVO::from)
                .collect(Collectors.toList());
        //返回
        return new PageResult<>(mpPage.getTotal(), mpPage.getCurrent(), mpPage.getSize(),
                mpPage.getPages(), records);


    }

    //查详情
    @Override
    public DocumentDetailVO detail(Long userId, Long id) {
        //查详情 用了getOwned 传userId id他去数据库查 有就返回 无就抛出异常 2013
        return DocumentDetailVO.from(getOwned(userId, id));
    }

    //重命名
    @Override
    public DocumentVO rename(Long userId, Long id, DocumentUpdateDTO dto) {
        //拿前端传的id 和 userid去检验文件是否存在
        getOwned(userId, id);

        //new了一个空对象 只设置了id 和 originalName（前段传的）
        Document update = new Document();
        update.setId(id);
        update.setOriginalName(dto.getOriginalName().trim());

        //然后去更新了数据库
        documentMapper.updateById(update);

        return DocumentVO.from(documentMapper.selectById(id));
    }

    @Override
    public DocumentVO move(Long userId, Long id, DocumentMoveDTO dto) {
        //拿前端传的id去检验当前文件夹是否属于你 然后根据id查出来当前文件夹的数据 并且保存到doc了
        Document doc = getOwned(userId, id);
        //拿到要移动到的文件夹id 然后校验文件夹是否存在
        checkFolder(userId, doc.getKnowledgeBaseId(), dto.getFolderId());

        //new一个update对象 是Document类型的 值存储id 和 folderId（前段传的 要移动到的文件夹id）
        Document update = new Document();
        update.setId(id);
        update.setFolderId(dto.getFolderId());
        //更新id 对应的文件夹id
        documentMapper.updateById(update);

        //返回更新后的VO
        return DocumentVO.from(documentMapper.selectById(id));
    }

    /*
    * 删除文件
    * */
    @Override
    public void delete(Long userId, Long id) {
        //删除之前检验文件是否存在 存在就拿出来文件数据 并且保存到doc了
        Document doc = getOwned(userId, id);

        // 先解析路径，避免删完数据库后因为路径异常导致接口报错
        Path path = null;
        try {
            path = resolvePath(doc.getFilePath());
        } catch (Exception e) {
            log.warn("解析文件路径失败：{}", doc.getFilePath(), e);
        }

        // 先删数据库记录（逻辑删除）
        documentMapper.deleteById(id);

        // 再删磁盘文件；失败只记日志，不影响接口成功
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("删除磁盘文件失败：{}", path, e);
            }
        }
    }

    @Override
    public void deleteByKnowledgeBase(Long userId, Long knowledgeBaseId) {
        // 查出该知识库下所有未删除的文件
        List<Document> docs = documentMapper.selectList(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getUserId, userId)
                        .eq(Document::getKnowledgeBaseId, knowledgeBaseId));

        for (Document doc : docs) {
            // 先逻辑删除记录
            documentMapper.deleteById(doc.getId());
            // 再删磁盘文件，失败只记日志，不阻断知识库删除
            try {
                Files.deleteIfExists(resolvePath(doc.getFilePath()));
            } catch (Exception e) {
                log.warn("删除知识库时删除文件失败：{}", doc.getFilePath(), e);
            }
        }
    }

    @Override
    public DownloadFile download(Long userId, Long id) {
        //下载之前检验文件是否存在 存在就拿出来文件数据 并且保存到doc了
            Document doc = getOwned(userId, id);
            //把相对路径安全的转换成绝对路径
            Path path = resolvePath(doc.getFilePath());

            //如果文件不存在 就抛出异常
            if (!Files.exists(path)) {
                throw new BusinessException(ResultCode.FILE_NOT_FOUND);
            }

            //new FileSystemResource(path); 根据路径在系统磁盘上查找文件 然后返回一个Resource对象
            Resource resource = new FileSystemResource(path);
            //获取文件大小
            long size = doc.getFileSize() == null ? 0L : doc.getFileSize();
            //返回下载材料
            return new DownloadFile(resource, doc.getOriginalName(), size);
    }

    /*
    * 这里是解析文档的service接口
    * 这里属于同步解析接口
    * */
    @Override
    public DocumentDetailVO parseDocument(Long userId, Long id) {
        //基础检验
        Document doc = prepareParse(userId, id);
        //原子抢状态
        markProcessing(userId, id);
        return doParse(doc);
    }

    /*
    * 异步解析
    * 步骤 校验 -> 原子抢状态 -> 提交线程池 -> 工作线程二次检查 -> 真正解析
    * */
    @Override
    public DocumentParseStatusVO startAsyncParse(Long userId, Long id) {
        //基础检验
        prepareParse(userId, id);
        //原子抢状态
        markProcessing(userId, id);

        try{
            //parseExecutor这个指向线程池 然后提交一个任务 把parseProcessingDocument这个方法检查通过的文档 交给线程池去执行
            parseExecutor.execute(() -> parseProcessingDocument(userId, id));
        }catch (TaskRejectedException e) {
            //如果线程池拒绝了任务 就记录日志 并且更新数据库状态为FAILED 然后抛出异常
            log.error("异步解析任务提交失败：id={}", id, e);
            updateParseStatus(id, "FAILED", "解析任务提交失败，请稍后重试", null);
            throw new BusinessException(ResultCode.PARSE_TASK_SUBMIT_FAILED);
        }catch (Exception e) {
            //其他提交异常也要兜底，避免文档卡在 PROCESSING
            log.error("异步解析任务提交异常：id={}", id, e);
            updateParseStatus(id, "FAILED", "解析任务提交失败，请稍后重试", null);
            throw new BusinessException(ResultCode.PARSE_TASK_SUBMIT_FAILED);
        }
        //返回当前状态
        return getParseStatus(userId, id);
    }

    /*
    * 去查当前状态的接口
    * */
    @Override
    public DocumentParseStatusVO getParseStatus(Long userId, Long id) {
        //DocumentParseStatusVO 这个vo专门存储解析状态
        //然后就通过userid id验证并且返回数据库最新状态 然后封装进这个vo去返回给前端
        return DocumentParseStatusVO.from(getOwned(userId, id));
    }

    /*······················工具类························*/

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

    /*
    * 根据id 和 userid 去查文件是否存在 注意是文件 文件 文件
    * */
    //定义了getOwned方法 传递userid id 放回Document对象
    private Document getOwned(Long userId, Long id) {
        //根据id 和 userid 双重保障去查
        //selectOne就是去查看下有几条数据 一条返回 多条 或者 0都属于异常
        Document doc = documentMapper.selectOne(
                new LambdaQueryWrapper<Document>()
                        .eq(Document::getId,id)
                        .eq(Document::getUserId,userId));
        if(doc == null) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        return doc;
    }

    /*
    * checkFolder 校验文件夹是否存在 注意是 文件夹 文件夹 文件夹
    * */
    //传递参数进来 不返回任何值
    private void checkFolder(Long userId, Long knowledgeBaseId, Long folderId) {
        //根据id userid 数据库id 去查Folder是否存在 注意selectOne只返回一个数据
        Folder folder = folderMapper.selectOne(
                new LambdaQueryWrapper<Folder>()
                        .eq(Folder::getId, folderId)
                        .eq(Folder::getUserId, userId)
                        .eq(Folder::getKnowledgeBaseId, knowledgeBaseId));
        //不存在抛出异常
        if (folder == null) {
            throw new BusinessException(ResultCode.FOLDER_NOT_FOUND);
        }
        //存在继续执行
    }

    /**
     * 把数据库里的相对路径解析为 uploads 下的绝对路径，并防止路径穿越。
     */
    private Path resolvePath(String filePath) {
        //接收数据库里的相对路径 如果为空 或者 空格 就抛出异常
        if (filePath == null || filePath.isBlank()) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        //提取带根目录的绝对路径
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        //拼接出来相对路径
        Path path = root.resolve(filePath).normalize();
        //如果相对路径不是以上传目录开头 就抛出异常
        if (!path.startsWith(root)) {
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        //返回绝对路径
        return path;
    }

    /**
     * 更新解析状态。
     * 用 UpdateWrapper 显式 set，因为 updateById 会忽略 null。
     * extractedText 为 null 时不 set，避免把上一次的正文清空。
     *
     * 说白了就是一设置状态 一设置错误信息 一设置提取正文
     */
    private void updateParseStatus(Long id, String status,
                                   String errorMessage, String extractedText) {
        documentMapper.update(null, new LambdaUpdateWrapper<Document>()
                .eq(Document::getId, id)
                .set(Document::getProcessStatus, status)
                .set(Document::getErrorMessage, errorMessage)
                .set(extractedText != null, Document::getExtractedText, extractedText));
    }

    /**
     * 异常信息可能为 null 或很长，截断后再写入 error_message。
     */
    private String buildErrorMessage(Exception e) {
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            message = e.getClass().getSimpleName();
        }
        return message.length() > MAX_ERROR_MESSAGE_LENGTH
                ? message.substring(0, MAX_ERROR_MESSAGE_LENGTH)
                : message;
    }

    /*
    校验 -> 原子抢状态 -> 提交线程池 -> 工作线程二次检查（我在这里） -> 真正解析
    * 在进入线程池真正被Tika解析之前需要做的一个校验
    * 好处是：避免对已成功/已失败/已取消/已删除/已重置的文档做无效解析，
    * 省 CPU/IO，防止覆盖正确结果，兜住重复任务，保证解析基于最新文档。
    * */
    private void parseProcessingDocument(Long userId, Long id){
        try{
            //第一做检验文件是否属于你 第二拿的是最新数据
            Document doc = getOwned(userId, id);
            //看看当前文件状态是否是PROCESSING 也就是是否处于解析中状态
            if(!"PROCESSING".equals(doc.getProcessStatus())){
                //如果不是解析ING状态 就警告一下
                log.warn("异步解析跳过：文档状态不是 PROCESSING，id={}, status={}",
                        id, doc.getProcessStatus());
                return;
            }
            //如果是解析状态 属于你 那就真正开始解析
            doParse(doc);
        }catch (BusinessException e) {
            // doParse 内部已经写入 FAILED 和 error_message，这里只记录日志
            log.warn("异步解析业务失败：id={}, message={}", id, e.getMessage());
        } catch (Exception e) {
            log.error("异步解析异常：id={}", id, e);
            updateParseStatus(id, "FAILED", buildErrorMessage(e), null);
        }
    }

    /*
    校验（我在这里） -> 原子抢状态 -> 提交线程池 -> 工作线程二次检查 -> 真正解析
    * 解析之前校验 归属 类型 磁盘文件
    * */
    private Document prepareParse(Long userId, Long id){
        //校验文件是否属于你 第二拿的是最新数据
        Document doc = getOwned(userId, id);

        //从数据库拿到当前文件存储的是啥类型 看下是否符合规范
        if(doc.getFileType() == null || !ALLOWED_TYPES.contains(doc.getFileType())) {
            throw new BusinessException(ResultCode.FILE_TYPE_NOT_SUPPORTED);
        }
        //用之前写的工具类拿到文件的绝对路径
        Path path = resolvePath(doc.getFilePath());
        //取磁盘看绝对路径下文件是否存在
        if(!Files.exists(path)){
            //不存在就要抛出异常
            throw new BusinessException(ResultCode.FILE_NOT_FOUND);
        }
        //属于你 类型对 磁盘有 那我让你通过了
        return doc;
    }

    /*
    * 抢占原子状态
    * */
    private void markProcessing(Long userId, Long id){
        //定义一个row根据id userid 还有状态 是否是UPLOADED 或 FAILED 这三个条件满足就去修改状态和错误信息
        //然后row适用于看你成功修改了几条数据
        int rows = documentMapper.update(null, new LambdaUpdateWrapper<Document>()
                .eq(Document::getId, id)
                .eq(Document::getUserId, userId)
                .in(Document::getProcessStatus, "UPLOADED", "FAILED")
                .set(Document::getProcessStatus, "PROCESSING")
                .set(Document::getErrorMessage, null));

        //如果你没有修改任何数据那就抛出异常 说明当前文件状态不是UPLOADED 或 FAILED
        if (rows == 0) {
            throw new BusinessException(ResultCode.FILE_PARSE_STATUS_INVALID);
        }
    }

    /**
     * 真正解析，同步和异步共用。
     * 成功返回最新详情；失败写 FAILED + error_message 并抛出异常。
     */
    //把要解析的文档数据传过来 然后返回的是封装好的DocumentDetailVO
    private DocumentDetailVO doParse(Document doc) {
        //拿到当前文档的id
        Long id = doc.getId();
        //定义一个path 用于存储路径
        Path path = null;

        try {
            //去拿到文件的在磁盘的绝对路径然后存储下来
            path = resolvePath(doc.getFilePath());

            //定一个空text用于存储提取的文本内容
            String text;

            //一句话用tika专用方法把文件丢给他去解析 然后不要超过最大长度

            // 5. try-with-resources：括号里的流会在 try 块结束时自动 close()
            //    不管正常结束还是抛异常，都会关闭，防止文件句柄泄漏
            //    TikaInputStream 是 Tika 专用的输入流，支持 mark/reset、类型探测等
            try (InputStream in = TikaInputStream.get(path)) {

                // 6. 让 Tika 从输入流里抽文本
                //    new Metadata() 是给 Tika 填元信息用的，这里不关心
                //    MAX_EXTRACTED_TEXT_LENGTH 限制最多抽多少字符，防止超大文件撑爆内存/数据库
                text = tika.parseToString(in, new Metadata(), MAX_EXTRACTED_TEXT_LENGTH);
            }
            //去掉首位空格和换行符 如果是空的 就赋值为空字符串
            text = text == null ? "" : text.trim();

            //文本是空的抛出异常
            if (text.isEmpty()) {
                throw new BusinessException(ResultCode.FILE_PARSE_FAILED,
                        "未能提取到文本内容，可能是扫描件或图片型文件");
            }

            //设置好状态 错误信息 提取的文本 丢进数据库
            updateParseStatus(id, "SUCCESS", null, text);
            //返回最新详情
            return DocumentDetailVO.from(documentMapper.selectById(id));
            //如果有抛出异常 那我就去抓这个异常 然后设置好状态 错误信息 提取的文本 丢进数据库
        } catch (BusinessException e) {
            updateParseStatus(id, "FAILED", buildErrorMessage(e), null);
            throw e;
            //如果有其他异常 那我就去抓这个异常 然后设置好状态 错误信息 提取的文本 丢进数据库
        } catch (Exception e) {
            log.error("文档解析失败：id={}, path={}", id, path, e);
            String reason = buildErrorMessage(e);
            updateParseStatus(id, "FAILED", reason, null);
            throw new BusinessException(ResultCode.FILE_PARSE_FAILED, reason);
        }
    }



}
package com.mindspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mindspace.common.exception.BusinessException;
import com.mindspace.common.result.ResultCode;
import com.mindspace.dto.FolderCreateDTO;
import com.mindspace.dto.FolderMoveDTO;
import com.mindspace.dto.FolderUpdateDTO;
import com.mindspace.entity.Folder;
import com.mindspace.entity.KnowledgeBase;
import com.mindspace.mapper.FolderMapper;
import com.mindspace.mapper.KnowledgeBaseMapper;
import com.mindspace.service.FolderService;
import com.mindspace.vo.FolderTreeVO;
import com.mindspace.vo.FolderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderMapper folderMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    //创建文件夹
    @Override
    public FolderVO create(Long userId, FolderCreateDTO dto) {
        //你要在知识库下创建文件夹 我需要确定这个知识库是否存在 是你的吗
        checkKnowledgeBase(userId, dto.getKnowledgeBaseId());

        //接收一下传过来的父文件夹id
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        //我看下你的父文件夹是否存在
        checkParent(userId, dto.getKnowledgeBaseId(), parentId);
        //我现在知道你在你的知识库 和 存在的父文件夹 那我允许你创建文件夹

        //定义一个对象 来接收前端传过来的文件夹信息
        Folder folder = new Folder();
        folder.setUserId(userId);
        folder.setKnowledgeBaseId(dto.getKnowledgeBaseId());
        folder.setParentId(parentId);
        folder.setName(dto.getName().trim());
        folder.setIsDefault(0);

        //插入数据库
        folderMapper.insert(folder);
        //返回文档需要的FolderVO 数据从数据库获取 用id去查
        return FolderVO.from(folderMapper.selectById(folder.getId()));
    }

    //查询文件夹列表
    @Override
    public List<FolderVO> list(Long userId, Long knowledgeBaseId) {
        //确认知识库是你的
        //需要userid 和 knowledgeBaseId 去查
        checkKnowledgeBase(userId, knowledgeBaseId);

        //根据id取数据查列表
        List<Folder> folders = folderMapper.selectList(
                new LambdaQueryWrapper<Folder>()
                        //同时满足userid 和 知识库的id 升序排序 去数据库查文件夹列表
                        .eq(Folder::getUserId, userId)
                        .eq(Folder::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByAsc(Folder::getId));

        //说白了数据库的查出来 存到folders列表中
        //然后这个列表一个一个取出来 然后对应foldervo的一个一个转过去
        //然后处理完转换成你要的List<Folder>
        return folders.stream().map(FolderVO::from).collect(Collectors.toList());
    }

    //查询文件夹树结构
    @Override
    public List<FolderTreeVO> tree(Long userId, Long knowledgeBaseId) {
        //看下知识库是你的吗
        checkKnowledgeBase(userId, knowledgeBaseId);

        List<Folder> folders = folderMapper.selectList(
                new LambdaQueryWrapper<Folder>()
                        .eq(Folder::getUserId, userId)
                        .eq(Folder::getKnowledgeBaseId, knowledgeBaseId)
                        .orderByAsc(Folder::getId));

        //建一个map 用来存储文件夹id和文件夹树节点的映射关系
        Map<Long, FolderTreeVO> map = new LinkedHashMap<>();
        //遍历上面查出来的文件夹列表 然后一个一个取出来
        for (Folder folder : folders) {
            //map里面分别put 文件夹的id 还有把之前取出来的数据转换成树节点(带 children),
            //就是列表 第一列举例存储的就是 id + FolderTreeVO里面的所有东西
            map.put(folder.getId(), FolderTreeVO.from(folder));
        }

        //找根节点 新建一个列表用来存储根节点
        List<FolderTreeVO> roots = new ArrayList<>();
        //遍历map中的所有节点
        for (FolderTreeVO node : map.values()) {
            //取出来当前节点的父节点
            FolderTreeVO parent = map.get(node.getParentId());
            //如果当前节点没有父节点 或者 父节点不存在 就说明是根节点
            if (node.getParentId() == null || node.getParentId() == 0L || parent == null) {
                roots.add(node);
            } else {
                //其他情况 就把自己添加到父节点的children列表中
                parent.getChildren().add(node);
            }
        }
        return roots;
    }

    //更新文件夹
    @Override
    public FolderVO update(Long userId, Long id, FolderUpdateDTO dto) {
        //我得先看看你要更新的这个文件夹 是不是你的
        getOwned(userId, id);

        //定义一个对象接收修改后的值
        Folder update = new Folder();
        update.setId(id);
        update.setName(dto.getName().trim());
        folderMapper.updateById(update);

        return FolderVO.from(folderMapper.selectById(id));
    }

    //删除文件夹
    @Override
    public void delete(Long userId, Long id) {
        //删除的文件夹必须是你自己的文件夹
        Folder folder = getOwned(userId, id);

        //不能删除默认文件夹
        if(folder.getIsDefault() != null && folder.getIsDefault() == 1) {
            throw new BusinessException(ResultCode.FOLDER_DEFAULT_CANNOT_DELETE);
        }

        //假如你删除文件夹之前 里面有子文件夹不允许你删除
        //定义了children用来存储子文件夹的数量 我们去查数量
        Long children = folderMapper.selectCount(
                //创建一个lambda查询条件对象
                new LambdaQueryWrapper<Folder>()
                        //你只能删自己的文件夹
                        .eq(Folder::getUserId, userId)
                        //只能删除自己知识库的文件夹
                        .eq(Folder::getKnowledgeBaseId, folder.getKnowledgeBaseId())
                        //只数 parentId 等于当前文件夹 id 的
                        .eq(Folder::getParentId, id));
        //如果你有子文件夹 不能删除当前文件夹
        if (children > 0) {
            throw new BusinessException(ResultCode.FOLDER_HAS_CHILDREN);
        }

        // TODO 文件功能完成后：把该文件夹下的文件移动到默认文件夹
        folderMapper.deleteById(id);
    }

    //移动文件夹
    @Override
    public FolderVO move(Long userId, Long id, FolderMoveDTO dto) {
        //确认文件夹是的
        Folder folder = getOwned(userId, id);
        //如果前端没有传递这个id 我就认为放到根目录
        Long parentId = dto == null || dto.getParentId() == null ? 0L : dto.getParentId();

        //不能放到自己的下面
        if (parentId.equals(id)) {
            throw new BusinessException(ResultCode.FOLDER_MOVE_INVALID);
        }

        // 只有移动到非根目录时，才需要检查目标父文件夹
        if (parentId != 0L) {
            // 1. 目标父文件夹必须存在，并且和当前文件夹在同一个知识库
            Folder target = getOwnedInKb(userId, folder.getKnowledgeBaseId(), parentId);

            // 2. 从目标父文件夹开始一路往上找祖先：
            //    如果中途遇到“当前要移动的文件夹”，说明目标在它的子树里，会形成环
            Long cursor = target.getParentId();
            while (cursor != null && cursor != 0L) {
                if (cursor.equals(id)) {
                    throw new BusinessException(ResultCode.FOLDER_MOVE_INVALID);
                }
                cursor = getOwnedInKb(userId, folder.getKnowledgeBaseId(), cursor).getParentId();
            }
        }

        // 3. 检查通过，只更新 parent_id 一个字段，其他字段不动
        Folder update = new Folder();
        update.setId(id);
        update.setParentId(parentId);
        folderMapper.updateById(update);

        // 4. 返回数据库中的最新数据
        return FolderVO.from(folderMapper.selectById(id));
    }




    /* 小工具都在下面 */

    //小工具1 检查知识库是否存在
    private void checkKnowledgeBase(Long userId, Long knowledgeBaseId) {
        Long count = knowledgeBaseMapper.selectCount(
                //创建一个lambda查询条件对象
                new LambdaQueryWrapper<KnowledgeBase>()
                        //去数据库看知识库id是否匹配
                        .eq(KnowledgeBase::getId, knowledgeBaseId)
                        //去数据库看知识库是否属于当前用户
                        .eq(KnowledgeBase::getUserId, userId));
        if (count == 0) {
            throw new BusinessException(ResultCode.KB_NOT_FOUND);
        }
    }

    @Override
    public void createDefault(Long userId, Long knowledgeBaseId) {
        Folder folder = new Folder();
        folder.setUserId(userId);
        folder.setKnowledgeBaseId(knowledgeBaseId);
        folder.setParentId(0L);
        folder.setName("未分类");
        folder.setIsDefault(1);
        folderMapper.insert(folder);
    }

    @Override
    public void deleteByKnowledgeBase(Long userId, Long knowledgeBaseId) {
        folderMapper.delete(new LambdaQueryWrapper<Folder>()
                .eq(Folder::getUserId, userId)
                .eq(Folder::getKnowledgeBaseId, knowledgeBaseId));
    }


    //小工具2 检查父文件夹是否存在
    private void checkParent(Long userId, Long knowledgeBaseId, Long parentId) {
        //看下传过来的父文件夹id是不是0L 如果是0L 就说明是根文件夹 那就没有父文件夹 也就是创建根目录
        if (parentId == 0L) {
            return;
        }
        getOwnedInKb(userId, knowledgeBaseId, parentId);
    }

    //小工具3 校验属于当前用户
    //拿当前用户id和传过来的访问的文件夹id
    private Folder getOwned(Long userId, Long id) {
        //根据userid和id去查文件夹 判断是否存在
        Folder folder = folderMapper.selectOne(
                new LambdaQueryWrapper<Folder>()
                        .eq(Folder::getId, id)
                        .eq(Folder::getUserId, userId));
        //如果查不到 就不是你的文件夹
        if (folder == null) {
            //抛出异常 2001
            throw new BusinessException(ResultCode.FOLDER_NOT_FOUND);
        }
        return folder;
    }

    //小工具4 校验属于当前知识库
    //拿当前用户id和传过来的访问的文件夹id和知识库id
    private Folder getOwnedInKb(Long userId, Long knowledgeBaseId, Long id) {
        //根据userid id knowledgeBaseId 去查数据库
        Folder folder = folderMapper.selectOne(
                new LambdaQueryWrapper<Folder>()
                        .eq(Folder::getId, id)
                        .eq(Folder::getUserId, userId)
                        .eq(Folder::getKnowledgeBaseId, knowledgeBaseId));
        if (folder == null) {
            throw new BusinessException(ResultCode.FOLDER_PARENT_INVALID);
        }
        return folder;
    }


}

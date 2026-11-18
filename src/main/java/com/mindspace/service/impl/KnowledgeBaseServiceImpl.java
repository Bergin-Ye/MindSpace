package com.mindspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mindspace.common.exception.BusinessException;
import com.mindspace.common.result.ResultCode;
import com.mindspace.dto.KnowledgeBaseDTO;
import com.mindspace.entity.KnowledgeBase;
import com.mindspace.mapper.KnowledgeBaseMapper;
import com.mindspace.service.KnowledgeBaseService;
import com.mindspace.vo.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    //创建知识库
    @Override
    public KnowledgeBaseVO create(Long userId, KnowledgeBaseDTO dto) {
        //新建一个对象用来接收值
        KnowledgeBase kb = new KnowledgeBase();
        //给这个新建的对象赋值
        kb.setUserId(userId);
        kb.setName(dto.getName());
        kb.setDescription(dto.getDescription());
        //把这个新建对象插入数据库
        knowledgeBaseMapper.insert(kb);
        //返回文档需要的KnowledgeBaseVO 数据从数据库获取 用id去查
        return KnowledgeBaseVO.from(knowledgeBaseMapper.selectById(kb.getId()));
    }

    //查询知识库列表
    @Override
    public List<KnowledgeBaseVO> list(Long userId) {
        //创建对象用于接受查询结果
        List<KnowledgeBase> list = knowledgeBaseMapper.selectList(
                //去数据库查对象
                /*
                * 等于sql语句SELECT * FROM knowledge_base WHERE user_id = ? ORDER BY id DESC
                * */
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getUserId, userId)
                        .orderByDesc(KnowledgeBase::getId));

        //返回list 然后stream转换为KnowledgeBaseVO类型
        //这里转vo就是怕数据库有的东西泄露了 我们只根据vo返回给前端
        //stream就是一个一个的处理数据
        return list.stream()
                //每个通过完就转换成vo
                .map(KnowledgeBaseVO::from)
                //把vo转换为list
                .collect(Collectors.toList());
    }

    //查询知识库详分区
    @Override
    public KnowledgeBaseVO detail(Long userId, long id) {
        //根据userid和id去查知识库
        return KnowledgeBaseVO.from(getOwned(userId, id));
    }

    //修改知识库
    @Override
    public KnowledgeBaseVO update(Long userId, long id, KnowledgeBaseDTO dto) {
        //根据userid和id去查知识库 修改的前提是你得有这个知识库
        getOwned(userId, id);
        //定义一个对象用来接收修改后的值
        KnowledgeBase update = new KnowledgeBase();
        update.setId(id);
        update.setName(dto.getName().trim());
        update.setDescription(dto.getDescription());
        //更新数据库
        knowledgeBaseMapper.updateById(update);
        //返回文档需要的KnowledgeBaseVO 数据从数据库获取 用id去查
        return KnowledgeBaseVO.from(knowledgeBaseMapper.selectById(id));
    }

    //删除知识库
    @Override
    public void delete(Long userId, long id) {
        //删除的知识库必须是你自己的知识库
        getOwned(userId, id);
        //删除的知识库必须是你自己的知识库 不能是最后一个知识库
        Long count = knowledgeBaseMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getUserId, userId));
        //如果只有一个知识库 不能删除
        if (count <= 1) {
            throw new BusinessException(ResultCode.KB_LAST_CANNOT_DELETE);
        }
        //删除数据库中的知识库
        knowledgeBaseMapper.deleteById(id);
    }


    //注册时创建默认知识库
    @Override
    public void createDefault(Long userId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setUserId(userId);
        kb.setName("我的知识库");
        kb.setDescription("注册时自动创建的默认知识库");
        knowledgeBaseMapper.insert(kb);
    }

    /**
     * 按 id + userId 查，查不到或不属于当前用户都返回 2001
     */
    //根据userid和id去查知识库
    //如果查不到或不属于当前用户都返回 2001
    private KnowledgeBase getOwned(Long userId, Long id) {
        KnowledgeBase kb = knowledgeBaseMapper.selectOne(
                //根据userid 和 id 去查
                new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getId, id)
                        .eq(KnowledgeBase::getUserId, userId));
        //如果查不到或不属于当前用户都返回 2001
        if (kb == null) {
            throw new BusinessException(ResultCode.KB_NOT_FOUND);
        }
        return kb;
    }
}

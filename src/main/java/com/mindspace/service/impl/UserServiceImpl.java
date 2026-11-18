package com.mindspace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mindspace.common.exception.BusinessException;
import com.mindspace.common.result.ResultCode;
import com.mindspace.dto.LoginDTO;
import com.mindspace.dto.RegisterDTO;
import com.mindspace.dto.UpdatePasswordDTO;
import com.mindspace.dto.UpdateProfileDTO;
import com.mindspace.entity.SysUser;
import com.mindspace.mapper.SysUserMapper;
import com.mindspace.service.KnowledgeBaseService;
import com.mindspace.service.UserService;
import com.mindspace.vo.LoginVO;
import com.mindspace.vo.UserVO;
import com.mindspace.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    //注入依赖 Mapper 和 秘钥编码器
    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final KnowledgeBaseService knowledgeBaseService;

    @Override
    //开启事务 有异常回滚事务
    @Transactional(rollbackFor = Exception.class)
    //接收来自控制层的数据
    public void register(RegisterDTO dto) {
        //从dto中获取用户名和邮箱规范化 然后存起来
        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim().toLowerCase();

        //定义usernameCount看看数据库有几个用户名和邮箱（检验是否重复）
        //给mapper发查询请求 查总数
        Long usernameCount = sysUserMapper.selectCount(
                //去SysUser数据库查有没有相同的
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        //如果有相同的
        if (usernameCount > 0) {
            //抛异常
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        //和之前一样只是检查邮箱
        Long emailCount = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (emailCount > 0) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS);
        }

        //把传过来的数据转换为SysUser实体类
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setEmail(email);
        //密码加密
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        //插入数据库
        sysUserMapper.insert(user);
        //注册成功后创建默认知识库（与用户插入在同一个事务）
        knowledgeBaseService.createDefault(user.getId());
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        //传过来的数据去掉首尾空格小写
        String email = dto.getEmail().trim().toLowerCase();

        //这里相当于sql查询语句用email找有没有用户 还有他的密码是啥
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));

        //如果没有找到 抛出异常
        if (user == null) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        //匹配两个密码 如果不匹配 抛出异常
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        //如果找到了用jwt去生成token 然后返回LoginVO
        String token = jwtUtil.generateToken(user);
        return new LoginVO(token, "Bearer", jwtUtil.getExpiration(), UserVO.from(user));
    }

    @Override
    public UserVO getCurrentUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        return UserVO.from(user);
    }

    /*
    * 更新用户信息
    * */
    @Override
    public UserVO updateProfile(Long userId, UpdateProfileDTO updateProfileDTO) {
        //判断当前用户是否存在 抛出异常
        SysUser user = sysUserMapper.selectById(userId);
        if(user == null){
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }

        //更新用户信息

        //先获取传过来的用户名
        String username = updateProfileDTO.getUsername().trim();
        //判断数据库除了自己有没有别人用这个用户名
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .ne(SysUser::getId, userId));
        //有重复抛出异常
        if(count > 0){
            throw  new BusinessException(ResultCode.USERNAME_EXISTS);
        }

        //更新数据库
        SysUser update = new SysUser();
        update.setId(userId);
        update.setUsername(username);
        update.setAvatar(updateProfileDTO.getAvatar());
        sysUserMapper.updateById(update);

        //重新查一次，返回最新数据
        SysUser updated = sysUserMapper.selectById(userId);
        return UserVO.from(updated);
    }

    /*
    * 更新密码
    * */
    @Override
    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        //判断用户是否存在
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXIST);
        }
        //判断新旧密码是否匹配 不匹配抛出异常
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }

        //判断是否相同
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_SAME);
        }

        //更新数据库
        SysUser update = new SysUser();
        update.setId(userId);
        //加密并且更新数据库
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        //返回
        sysUserMapper.updateById(update);
    }
}
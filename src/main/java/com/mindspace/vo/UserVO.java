package com.mindspace.vo;


import com.mindspace.entity.SysUser;
import lombok.Data;

import java.time.LocalDateTime;

/*
* VO 是给前端的出参白名单。需要返回业务数据的接口，
* 把 Entity（或多个 Entity、聚合结果、计算值）的字段手动 set 到 VO 里，
* Controller 返回 Result<VO>，
* 前端只看到 VO 定义的字段；不返回数据的接口用 Result<Void> 即可。
* */



@Data
//定义用户VO视图类，用于返回用户信息
public class UserVO {
    private Long id;
    private String username;
    private String email;
    private String avatar;
    private LocalDateTime createTime;

    //方法：将SysUser实体转换为UserVO视图类
    public static UserVO from(SysUser sysUser) {
        UserVO vo = new UserVO();
        vo.setId(sysUser.getId());
        vo.setUsername(sysUser.getUsername());
        vo.setEmail(sysUser.getEmail());
        vo.setAvatar(sysUser.getAvatar());
        vo.setCreateTime(sysUser.getCreateTime());
        return vo;
    }

}

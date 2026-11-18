package com.mindspace.controller;


import com.mindspace.common.result.Result;
import com.mindspace.dto.UpdatePasswordDTO;
import com.mindspace.dto.UpdateProfileDTO;
import com.mindspace.service.UserService;
import com.mindspace.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    /*
    * 这里的思路是这样 因为我们已经完成了安全过滤器fillter
    * 所以我们接受前端数据之前他会跑一次安全过滤器 然后把userId和一些必要的东西提取出来
    * 放到所谓的安全context中
    * 我们已经信任了这次发来的请求 我们就可以通过这个id也就是唯一标识去进行服务层操作
    * 我们只需要验证一次发过来数据是否合法就行
    * */

    /*
    * 改用户信息 需要返回新的用户信息
    * */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@AuthenticationPrincipal Long userId,
                                        @RequestBody @Valid UpdateProfileDTO updateProfileDTO
    ) {
        return Result.success(userService.updateProfile(userId, updateProfileDTO));
    }

    /*
    * 改密码 不需要返回任何内容
    * */
    @PutMapping("/password")
    public Result<Void> updatePassword(@AuthenticationPrincipal Long userId,
                                                    @RequestBody @Valid UpdatePasswordDTO updatePasswordDTO) {
        userService.updatePassword(userId, updatePasswordDTO);
        return Result.success();
    }

}

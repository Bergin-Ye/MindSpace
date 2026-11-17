git status          # ① 看哪些文件改了
git add .           # ② 把改动加入暂存区（. 表示当前目录全部）
git commit -m "feat: 完成用户注册接口"   # ③ 提交到本地仓库
git log --oneline   # ④ 看提交历史

2026.11.17日学习笔记
今日开发注册接口 result 和 错误拦截器
1.result

result大概是个啥？
当你在service层进行一些业务逻辑操作 很有可能会报错
一些业务异常 比如注册的时候 用户名存在了
我们做了什么 基本的result 和 对应的枚举 resultcode
我们创建了result的success方法和error方法
分别都有有参无参以及一份error的自定义方法（可以自己指定code msg）

然后回到service层 当你对应的业务出错
你就可以去使用businessexception抛出一份异常
并且携带相对应以及写好的枚举resultcode
然后他会通过冒泡的或者说是自动的形式传递给全局的错误拦截器

全局错误拦截器会识别你的异常类型
当前有三种 业务错误 valid检验错误 还有兜底异常
他们都会提取出code 和 message
然后result对应的error方法返回给前端

2.注册接口
我们定义了registerDTO 他是一个data transfer object
可以用于接收前端传递过来的注册参数 也可以用于@Valid 校验是否符合要求

我们定义了SysUser 实体类存储注册用户信息

然后来到了控制层 我们通过post接收指定路径的参数
然后封装到dto中 让他变成我们需要的格式 并且检验是否符合要求
然后我们注册一下服务层
并且把参数传递给服务层
服务层会判断当前用户是否被注册
我们会去查询数据库equal有几个 大于零我们就会报出异常
也就是丢给BusinessException 以及对应的 resultcode
如果都正确
那么我们就会去把用户名 邮箱存储到数据库
然后把密码经过加密 存储到数据库
然后来到了mapper层 由于我们使用的是mybatis-plus
以及BaseMapper
所以我们让服务层直接调用mapper层的方法 这样我们就可以直接操作数据库



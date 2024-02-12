# IpacWhitelist
带有许多功能的白名单插件. 标签: \[白名单, 参观账户, Ban, AuthMe 集成, SQLite, 离线, 正版, Geyser\]
- 同时支持正版账号/离线账号/Geyser账号. 
- 支持查看/导出数据

下载: [[发布版本]](https://modrinth.com/plugin/ipacwhitelist) -- [[开发版本]](https://github.com/ApliNi/IpacWhitelist/actions)

---

## v4 更新
此版本对插件进行完全重构, 添加新功能并优化性能, 属于不兼容更新.
在发布正式版后, 建议所有服主更新到此版本, 这需要一些时间来重新修改配置

### 从 v3 导入数据
1. 在 v3 版本控制台中输入指令: `/wl list * ALL`
2. 复制这部分的完整日志内容, 到 IpacWhitelist 插件目录下的 `Data.txt` (需要自己创建)
3. 安装 v4 版本, 根据服务器需求修改配置, 比如玩家名称检查正则
4. 在 v4 版本运行指令 `/wl importData`
5. 检查内容是否识别正确

### 从任何白名单插件导入数据
根据其他插件的账户类型分别复制出白名单中的玩家和黑名单中的玩家.
通过任意高级的文本编辑器在玩家 UUID 或名称前添加指令:
1. [白名单] 指令格式: `/wl add <UUID|NAME>`
2. [黑名单] 指令格式: `/wl ban <UUID|NAME>`

然后将带有换行的指令复制到控制台, 即可一次性导入完毕

[注意] 对于正版服务器和支持正版以及离线的服务器尽量使用 UUID 而非 NAME


### 注意
- 此版本添加了完整的指令补全能力, 当显示 `....` 时代表这里支持通过输入的内容进行补全.
  补全内容可以直接在指令中使用, 包括控制台中补全内容带反斜杠的情况
- 此版本的配置顺序与插件进行数据处理的流程顺序一致
- 此版本许多功能基于"事件程序", 它将被用于发送消息, 以及调度各种功能. 代替了旧版杂乱的功能配置
  ```yaml
  # [事件程序] 以 "on" 开头, "Event" 结尾的配置均可使用此模板
  # kick    = 同步踢出玩家, 并显示消息
  # cmd     = 同步运行控制台命令
  # msg     = 发送消息给这个玩家
  # msgBroadcast = 广播消息给所有玩家
  # msgExclude   = 广播消息给其他所有玩家
  # [变量] 对于操作数据的功能尽量使用 UUID 而非 NAME, 防止因为名称冲突而影响数据
  # %playerUUID%  = 玩家 UUID 36 位字符串
  # %playerName%  = 玩家名称, 区分大小写
  ```
- 此版本改动特别大, 建议不要从旧配置中复制内容到新配置, 因为有很多变量名被修改
- 此版本准备弃用 Time 为 `-1` 的功能, 这在旧版本代表永远不会过期或等待玩家加入时进行更新

---

### 指令和功能
- `/wl` - 显示指令列表
- `/wl reload`    - 重载插件
- `/wl add <Name|UUID>`   - 添加到白名单
- `/wl del <Name|UUID>`   - 从白名单移出
- `/wl ban <Name|UUID>`   - 封禁一个玩家
- `/wl unban <Name|UUID>` - 解除封禁玩家
- `/wl info <Name|UUID>`  - 显示玩家信息
- `/wl list <Type>`       - 查询玩家数据
- `/wl clear PLAYER|TYPE <Name|UUID|Type>`  - 清除数据
- `/wl importData`        - 导入数据

> 支持使用 32 或 36 位的 UUID. 我们有完整的指令补全支持!

#### 操作规范
1. 对于同时支持正版账户和离线账户的服务器, 应始终优先使用 UUID (如果存在), 而非玩家名称.
   并且保持开启插件的防止重复数据功能
2. 若需要取消一个玩家的白名单, 可以通过 `del` 和 `ban` 实现, 但这两者有区别:
   1. `del` 会将账户标签标记为 `NOT` 这同时代表数据已被删除. 如果不需要删除玩家数据, 请不要使用此方法
   2. `ban` 操作不会修改账户的数据, 同时会防止其他操作影响账户数据, 适合需要保持玩家数据不被改动的情况
3. 清理玩家数据功能最好在服务器重启后使用, 有少部分插件只会在服务器关闭时断开与玩家数据文件的连接
4. `....`

---

### 参观账户
推荐让参观账户使用观察模式加入服务器, 特别是红石服务器. 因为还无法解决生存模式下玩家碰撞实体的问题.

参观账户是外挂在当前的游戏环境中的, 并不是完美的隔离.

可以配合这些插件使用:
- [[LuckPerms]](https://luckperms.net/) - 权限组插件
- [[IpacPER]](https://github.com/ApliNi/IpacPER) - 用于防止参观账户获取成就, 使用观察模式传送功能, 修复安装 OpenInv 后无法使用部分箱子菜单
- [[UseTranslatedNames]](https://github.com/ApliNi/useTranslatedNames) - 消息过滤/编辑/翻译插件. 可用于防止参观账户收到不必要的消息, 比如可能出现的登录注册消息
- [[CommandWhitelist]](https://github.com/YouHaveTrouble/CommandWhitelist) - 命令白名单. 隐藏和防止执行命令


---


### 开发和调试 [待更新]

<details><summary>点击展开这部分内容</summary>

#### 数据表

如果你想了解这个插件的运行方式, 最简单的方法是打开数据库, 然后试试各种指令.
> 如果数据库软件卡死, 请尝试运行 `/wl reload`

| ID | Type | Ban | UUID | Name | Time       | Config |
|----|------|-----|------|------|------------|--------|
| 1  | 0    | 0   | UUID | Name | 1694231705 | JSON   |

- `ID` - 唯一的自增ID

- `Type` - 这个账户的类型:

  - `0`: `NOT` - 未定义 or 不在白名单中

    使用 `/wl del` 时, 插件不会将玩家永久从白名单中移除, 而是将 Type 改为 NOT. 这用于支持账户数据清理以及其他查询功能.

  - `1`: `VISIT` - 参观账户. 如果开启参观账户, 那么玩家第一次加入时 Type 会从 NOT 转换为 VISIT
  
  - `2`: `WHITE` - 在白名单中

    将一些非白名单(WHITE) 类型的玩家通过 `/wl add` 设置为白名单时, 根据当前类型运行不同程序...
  
  - `4`: `VISIT_CONVERT` - 需要转换的参观账户

    当参观账户(VISIT) 被添加到白名单(WHITE) 时, 会为参观账户运行配置中设定的转换程序, 会踢出玩家(此时设置为 VISIT_CONVERT), 并在玩家重新上线后运行剩下的转换程序, 然后再设置为 WHITE. 至此完成参观账户到白名单的转换.
  
    [!] 如果为 VISIT_CONVERT 类型的玩家继续运行 `/wl add`, 那么它会重新执行一遍完整的转换程序. 
  
  - 是的, 没有 `3`.

- `Ban` - 这个账户是否被封禁:

  - `0`: `NOT_BAN` - 没有被封禁
  
  - `1`: `BAN` - 已被封禁:

    Ban 数据是单独的, 当它为 `BAN` 时会"锁定"大部分功能.
  
    如果账户已被封禁, 则会完全绕过加入部分的代码. 也就是无论 Type 如何, 都不能加入.
  
    [!] 如果账户处于 BAN 状态, 则涉及其他数据的操作都不能使用! 比如 `/wl add|del|clear`, `/wl clear VISIT` 也会绕过处于 Ban 状态的参观账户.  

- `UUID` AND `Name` - 存储玩家的 UUID 和名称.
  - UUID 使用 36 位(带连字符) 的字符串格式保存在数据库中.
  - Name 只是字符串

    可以在建表时通过配置中的 `sqlite.nameCaseSensitive` 切换此列的大小写敏感. 

- `Time` - 数据操作的时间戳

  这是毫秒级时间戳, 在玩家加入/退出和使用需要修改信息的指令时都会更新为当前时间.

  它被用于判断~~账户是否过期(默认4个月)~~, 以及是否达到可以安全删除数据的时间(默认12小时)


#### SQLite

直到现在, 我也没有去支持 MySQL 之类的数据库. 我认为它在这个应用里并不实用, 如果我需要去适配群组端, 那么才可能会支持它...  

在这个插件里, SQLite 的连接只会在 `/wl reload` 或者关服时被释放, 其余时间都保持连接并且文件被锁定, 同时不会进行提交(当`db-wal`达到一定大小时, SQLite会自动提交它). 所以不要删除 `db-shm` 和 `db-wal` 文件.

</details>


### 错误处理

<details><summary>点击展开这部分内容</summary>

#### 解决名称重复问题
这个错误在 v4 版本 (默认配置) 中已被修复, 但可能因为数据导入 / 玩家修改名称, 或其他未知的问题触发了这个检查, 可根据此步骤修复.

玩家名称重复时可通过指令 `/wl list NAME_CONFLICT` 检查, 输出内容就像这样:
```
IpacEL > 查询玩家信息[NAME_CONFLICT]:
  - [2024-02-10 10:31:50] {ID: 50, Type: "WHITE", Ban: "NOT", UUID: "aaa", Name: "A", Time: 1707532310}
  - [2024-01-06 19:38:08] {ID: 147, Type: "VISIT", Ban: "NOT", UUID: "bbb", Name: "A", Time: 1704541088}
  - [2024-02-06 18:03:08] {ID: 13, Type: "WHITE", Ban: "NOT", UUID: "ccc", Name: "B", Time: 1707213788}
  - [2024-01-06 19:36:24] {ID: 148, Type: "WHITE", Ban: "NOT", UUID: "ddd", Name: "B", Time: 1704540984}
```

[其他错误造成的名称重复] 如果同一个玩家 (比如 A) 存在两个 UUID, 并且其中一个为参观账户 `VISIT` 或 `NOT`, 则数据 `147` 很可能是一条产生错误的数据, 并且这个 UUID 下没有实际有效的玩家存档.
请手动检查玩家存档是否为空, 然后使用 `/wl clear PLAYER bbb` 删除这条数据, 并清理产生的存档文件.

[玩家改名造成的名称重复] 如果出现例如玩家 B 的情况, 可能因为两个白名单内的玩家修改过名称, 并且其中一位玩家没有上线过 (玩家 "B" -> "C", 并且玩家 "E" -> "B", 且玩家 C 没有上线过).
这时候需要让玩家 C 上线一次 (这将自动更新玩家名称), 或者手动更新玩家 C 的名称来解决.

</details>


---

### 配置
```yaml

# 数据库配置
sqlite:
  # 是否区分玩家名称的大小写, 仅可在创建数据库时启用, 影响所有需要查询名称的功能
  # 启用后, 使用指令可能会变得麻烦
  nameCaseSensitive: false


# 连接到其他插件
hook:
  # 离线登录插件
  AuthMe: true


# 白名单配置
# 白名单验证流程与配置的顺序相同
whitelist:

  # 简易的 ip 黑名单. 在这里添加正则表达式, 匹配的ip不允许加入服务器 (也不允许使用参观账户
  # 使用 /wl reload 重载配置即可应用
  ipBlacklist: [ ]
  #    - '^192\.168\.100\..+$'
  #    - '^fe80::1234:.+$' # IPv6 没有方括号
  ipBlacklistMsg: '§6IpacEL §f> §b您已被列入黑名单: §a%playerName%' # var: %ip%

  # 玩家名称字符检查
  # 名称长度为 3 到 16 字符, 允许包含 Geyser 的 "." 前缀
  playerNameRule: '^(?:\.?[a-zA-Z0-9_]{3,16})$'
  playerNameRuleMsg: '§6IpacEL §f> §a名称中包含不支持的字符: §b%playerName%'

  # 服务器启动完毕后需要等待多长时间才允许玩家加入 (毫秒
  lateJoinTime: 4000 # 4秒
  lateJoinTimeMsg: '§6IpacEL §f> §b服务器正在启动'

  # 防止玩家短时间内重复加入退出 (毫秒
  repeatJoinInterval: 1200 # 1.2秒
  repeatJoinIntervalMsg: '§6IpacEL §f> §b连接受限, 请稍后重试'

  # 启用最大玩家数量限制
  # 拥有 IpacWhitelist.maxPlayers.bypass 权限的玩家可绕过最大人数限制
  maxPlayers: true
  maxPlayersIncludesVisit: true  # 参观账户是否包含在玩家总数中
  maxPlayersMsg: '§6IpacEL §f> §b服务器已满'

  # 防止名称相同, 但 UUID 不同的玩家加入
  preventNameDuplication: true
  preventNameDuplicationMsg: '§6IpacEL §f> §b存在名称重复的玩家, 请联系管理员检查'


  # 以下根据玩家当前的类型对配置分类

  # 玩家不在白名单中
  NOT:
    # 使用参观账户, 允许玩家参观服务器
    visitEnable: true
    # 否则不允许加入, 发出以下消息
    notMsg: '§6IpacEL §f> §b您不在白名单中或已失效, 请联系管理员恢复§7: §a%playerName%'

    # 注意! 这里可能缺少 UUID 或 NAME, 这取决于使用 `/wl add` 指令时的输入内容
    onWhitelistAddEvent: [ ] # 玩家被添加到白名单

  # 参观账户
  VISIT:
    # 参观账户的名称字符检查
    playerNameRule: '^(?:\.?[a-zA-Z0-9_]{3,16})$'
    playerNameRuleMsg: '§6IpacEL §f> §a名称中包含不支持的字符: §b%playerName%'

    # 同时允许多少参观账户加入服务器
    maxPlayers: 10
    maxPlayersMsg: '§6IpacEL §f> §b参观队列已满'

    # [AuthMe] 自动注册登录参观账户
    AuthMePlugin:
      # 为参观账户自动注册和登录, 相当于 `/authme register <playerName> <password>` 和 `/authme forcelogin <playerName>`, 但不会踢出玩家
      autoRegisterAndLogin: true
      # 需要在这里填写一个复杂的密码, 只要满足 AuthMe 的密码规则即可
      autoRegisterPassword: 'complexPassword'

    # [事件程序] 以 "on" 开头, "Event" 结尾的配置均可使用此模板
    # kick    = 同步踢出玩家, 并显示消息
    # cmd     = 同步运行控制台命令
    # msg     = 发送消息给这个玩家
    # msgBroadcast = 广播消息给所有玩家
    # msgExclude   = 广播消息给其他所有玩家
    # [变量] 对于操作数据的功能尽量使用 UUID 而非 NAME, 防止因为名称冲突而影响数据
    # %playerUUID%  = 玩家 UUID 36 位字符串
    # %playerName%  = 玩家名称, 区分大小写
    onNewPlayerLoginEvent: # 参观账户第一次登录服务器
      cmd:
        - 'lp user %playerUUID% parent add visit' # 将玩家添加到 visit 权限组

    onPlayerLoginEvent: [ ] # 参观账户每次登录服务器

    onPlayerJoinEvent: # 参观账户加入服务器
      cmd:
        - 'gamemode spectator %playerName%' # 将玩家设置为观察模式
      msg:
        - '§6IpacEL §f> §a您正在使用参观账户=w='
      #        - '§6IpacEL §f> §b如需加入, 请访问我们的网站: §ahttps://ipacel.cc/'
      msgExclude:
        - '§6IpacEL §f> §a%playerName% §b使用参观账户加入游戏'

    onPlayerQuitEvent: # 参观账户退出服务器
      msgBroadcast:
        - '§6IpacEL §f> §a%playerName% §b跑了'

    onWhitelistAddEvent: # 参观账户被添加到白名单
      kick:
        - '§6IpacEL §f> §a您已添加到白名单, 请重新登录服务器'
      cmd:
        - 'lp user %playerUUID% parent remove visit' # 将玩家移出 visit 用户组
        - 'authme unregister %playerName%' # 取消注册玩家

  # 被添加到白名单中的参观账户, 需要通过此流程进行数据转换
  # 之后在 WHITE 分类中继续运行
  VISIT_CONVERT:
    onPlayerLoginEvent: [ ] # 需要转换账户类型的玩家登录

    onPlayerJoinEvent: # 需要转换账户类型的玩家加入
      cmd:
        - 'spawn %playerName%' # 传送到重生点
        - 'gamemode survival %playerName%' # 将玩家设置为生存模式

    # <- 现在玩家正式添加到白名单, 将继续运行 WHITE 中的配置

  # 白名单中的玩家
  WHITE:
    onPlayerLoginEvent: [ ] # 玩家登录服务器

    onPlayerJoinEvent: # 玩家加入服务器
      msgBroadcast:
        - '§6IpacEL §f> §a%playerName% §b加入游戏'

    onAuthMeLoginEvent: # 玩家通过 AuthMe 登录成功
      msgBroadcast:
        - '§6IpacEL §f> §a%playerName% §b登录成功'

    onAuthMeFailedLoginEvent: # 玩家通过 AuthMe 登录失败
      msgBroadcast:
        - '§6IpacEL §f> §a%playerName% §b断开连接: §7密码错误'

    onPlayerQuitEvent: # 玩家退出服务器
      msgBroadcast:
        - '§6IpacEL §f> §a%playerName% §b跑了'

    onAuthMeLogoutEvent: [ ] # 玩家通过 AuthMe 注销

    onWhitelistDelEvent: # 白名单被移除
      kick:
        - '§6IpacEL §f> §b您已被移出白名单'


  # 被封禁的账户
  BAN:
    # 被封禁账户登录时的消息
    kickMsg: '§6IpacEL §f> §a您已被列入黑名单: §b%playerName%'

    onWhitelistBanEvent: # 任何玩家被添加到黑名单
      kick:
        - '§6IpacEL §f> §b您已被添加到黑名单'

    onWhitelistUnbanEvent: [ ] # 玩家从黑名单中移出

    onPlayerLoginEvent: [ ] # 玩家登录服务器


# 指令配置
# 如无必要, 请勿修改这部分消息, 防止信息显示错误
command:

  add:
    title:   '§6IpacEL §f> §a添加到白名单[§b%var%§a]:'
    isBan:   '  - §a%playerName%§f[§7%playerUUID%§f] §b已在黑名单中, 不可操作'
    isExist: '  - §a%playerName%§f[§7%playerUUID%§f] §b已在白名单中'
    isVisit: '  - §a%playerName%§f[§7%playerUUID%§f] §a已从参观账户中重置'
    finish:  '  - §a%playerName%§f[§7%playerUUID%§f] §a已完成'

  del:
    title:   '§6IpacEL §f> §b从白名单移出[§a%var%§b]:'
    isEmpty: '  - §a%playerName%§f[§7%playerUUID%§f] §b不存在'
    isMulti: '  - §a%playerName%§f[§7%playerUUID%§f] §b存在重复数据'
    isBan:   '  - §a%playerName%§f[§7%playerUUID%§f] §b已在黑名单中, 不可操作'
    finish:  '  - §a%playerName%§f[§7%playerUUID%§f] §a已完成'

  ban:
    title:   '§6IpacEL §f> §b添加到黑名单[§a%var%§b]:'
    isBan:   '  - §a%playerName%§f[§7%playerUUID%§f] §b已在黑名单中'
    isMulti: '  - §a%playerName%§f[§7%playerUUID%§f] §b存在重复数据'
    finish:  '  - §a%playerName%§f[§7%playerUUID%§f] §a已完成'

  unban:
    title:   '§6IpacEL §f> §a从黑名单移出[§b%var%§a]:'
    isEmpty: '  - §a%playerName%§f[§7%playerUUID%§f] §b不存在'
    isMulti: '  - §a%playerName%§f[§7%playerUUID%§f] §b存在重复数据'
    isUnban: '  - §a%playerName%§f[§7%playerUUID%§f] §b不在黑名单中'
    finish:  '  - §a%playerName%§f[§7%playerUUID%§f] §a已完成'

  info:
    title:   '§6IpacEL §f> §b查询玩家信息[§a%var%§b]:'
    isEmpty: '  - §a%playerName%§f[§7%playerUUID%§f] §b不存在'
    finish: >
      §f  - §a%playerName%§f[§7%playerUUID%§f]: [§bID: §6%id%§f]
            - §bTYPE: §6%type%
            - §bBAN: §6%ban%
            - §bTIME: §6%time%

  list:
    title:   '§6IpacEL §f> §b查询玩家信息[§a%type%§b]:'
    noData:  '  - §b这个标签下没有任何数据'
    isEmpty: '  - §a%playerName%§f[§7%playerUUID%§f] §b不存在'
    finish:  '  - [§6%time%§f] {§bID§f: §6%id%§f, §bType§f: "§6%type%§f", §bBan§f: "§6%ban%§f", §bUUID§f: "§a%playerUUID%§f", §bName§f: "§a%playerName%§f", §bTime§f: §6%timeLong%§f}'

  # 用于清理玩家数据的功能, 请提前在本地测试完毕后再启用
  # [注意]
  # 在使用清理功能前请检查是否存在名称重复的数据: /wl list NAME_CONFLICT
  clear:
    enable: false
    title:  '§6IpacEL §f> §b运行数据清理[§a%type%:%var%§b]:'
    isMiss: '  - §a%playerName%§f[§7%playerUUID%§f] §b缺少必要的玩家信息'
    online: '  - §a%playerName%§f[§7%playerUUID%§f] §b玩家在线'

    # 只处理离线超过指定时间的账户, 防止清除后被保存 (秒
    delTime: 43200 # 12小时. 它必须大于数据缓存时间
    delTimeMsg: '  - §a%playerName%§f[§7%playerUUID%§f] §b未达到可删除的时间'

    # 不应该处理存在重复名称的数据
    repeat: '  - §a%playerName%§f[§7%playerUUID%§f] §b存在重复的名称'

    # [注意]
    # 清理数据的配置尽量使用 playerUUID, 而非 playerName, 防止因为重名而误删数据

    # 运行指令
    runCommand:
      - 'lp user %playerUUID% clear'      # 清除玩家权限
      - 'authme unregister %playerName%'  # 取消注册玩家

    # 运行指令和清除文件之间的间隔 (毫秒
    delayStep: 270

    # 清除文件
    # %worldPath% = 所有地图的根目录
    # %worldName% = 所有地图的名称
    clearFile:
      # 清理所有地图下的存档
      - '%worldPath%/playerdata/%playerUUID%.dat'
      - '%worldPath%/playerdata/%playerUUID%.dat_old'
      - '%worldPath%/advancements/%playerUUID%.json'
      - '%worldPath%/stats/%playerUUID%.json'
      # 清理 Essentials 插件下的玩家数据
      - 'plugins/Essentials/userdata/%playerUUID%.yml'
      # 清理粘液科技文件
      - 'data-storage/Slimefun/waypoints/%playerUUID%.yml'

    # 循环之间的时间间隔 (毫秒
    delayLoop: 727

    ing:     '  - [%var%:%id%] §a%playerName%§f[§7%playerUUID%§f] §a已完成'
    finish:  '§6IpacEL §f> §a数据清理运行完毕'

  # 从旧版本 IpacWhitelist 导入数据的功能, 默认关闭防止误触
  importData:
    enable: false
    # 从文本文件中导入数据, 每行一条数据
    file: 'Data.txt'
    # 用于匹配数据的正则, 这里是 v3 版本的 List 指令导出格式. 使用指令: /wl list * ALL
    regExp: '\{ID: \d+, Type: "([^"]+)", Ban: "([^"]+)", UUID: "([^"]+)", Name: "([^"]+)", Time: (-?\d+)\}$'
    # 不同数据所在的匹配组 ID
    TYPE: 1
    BAN: 2
    UUID: 3
    NAME: 4
    TIME: 5


# 其他消息
message:
  noEnable: '§6IpacEL §f> §b功能未启用'
  noPermission: '§6IpacEL §f> §b没有权限'
  parameterErr: '§6IpacEL §f> §a参数不可识别或未通过检查: §b%var%'
  playerLoginErr: '§6IpacEL §f> §b出现未知的错误, 请联系管理员检查'

```

### 权限
```yaml
permissions:

  IpacWhitelist.maxPlayers.bypass:
    description: '绕过最大人数限制'
    default: op

  IpacWhitelist.cmd:
    description: '使用指令和指令补全'
    default: op

  IpacWhitelist.cmd.reload:
    description: '使用 /wl reload 指令'
    default: op

  IpacWhitelist.cmd.add:
    description: '使用 /wl add 指令'
    default: op

  IpacWhitelist.cmd.del:
    description: '使用 /wl del 指令'
    default: op

  IpacWhitelist.cmd.ban:
    description: '使用 /wl ban 指令'
    default: op

  IpacWhitelist.cmd.unban:
    description: '使用 /wl unban 指令'
    default: op

  IpacWhitelist.cmd.info:
    description: '使用 /wl info 指令'
    default: op

  IpacWhitelist.cmd.list:
    description: '使用 /wl list 指令'
    default: op

  IpacWhitelist.cmd.clear:
    description: '使用 /wl clear 指令'
    default: op

  IpacWhitelist.cmd.importData:
    description: '使用 /wl importData 指令'
    default: op

```
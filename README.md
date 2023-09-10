# IpacWhitelist
带有许多功能的白名单插件. 标签: \[白名单, 参观账户, Ban, AuthMe 集成, SQLite, 离线, 正版, Geyser\]
- 同时支持正版账号/离线账号/Geyser账号. 
- 支持查看/导出数据

下载: [[发布版本]](https://modrinth.com/plugin/ipacwhitelist) -- [[开发版本]](https://github.com/ApliNi/IpacWhitelist/actions)


### 指令列表
- `/wl` - 主命令
- `/wl reload` - 重载配置, 同时重新连接数据库
- `/wl add <playerName|playerUUID>` - 添加到白名单, uuid为空时自动获取
- `/wl del <playerName|playerUUID>` - 取消玩家的白名单
- `/wl ban <playerName|playerUUID>` - 封禁玩家
- `/wl unban <playerName|playerUUID>` - 解封玩家
- `/wl info <playerName|playerUUID>` - 查看玩家数据
- `/wl list <NOT|VISIT|WHITE|BLACK|VISIT_CONVERT|VISIT_BLACK|*> <num|ALL>` - 列出玩家数据
- `/wl clean <VISIT|NOT>` - 清理一个类型下所有玩家的数据, 详见配置
- `/wl clean PLAYER <playerName|playerUUID>` - 清理一个玩家的所有数据, 详见配置

> 支持使用 32 或 36 位的 UUID

---

### 参观账户
推荐让参观账户使用观察模式加入服务器, 特别是红石服务器. 因为还无法解决生存模式下玩家碰撞实体的问题.  

可以配合这些插件使用: 
- [[LuckPerms]](https://luckperms.net/) - 权限组插件
- [[IpacPER]](https://github.com/ApliNi/IpacPER) - 用于防止参观账户获取成就, 使用观察模式传送功能, 修复安装 OpenInv 后无法使用部分箱子菜单
- [[ProtocolStringReplacer]](https://github.com/Rothes/ProtocolStringReplacer) - 消息过滤/编辑插件. 用于防止参观账户收到不必要的消息, 比如可能出现的登录注册消息
- [[CommandWhitelist]](https://github.com/YouHaveTrouble/CommandWhitelist) - 命令白名单. 隐藏和防止执行命令


### 开发和调试

<details><summary>点击展开这部分内容</summary>

#### 数据表

如果你想了解这个插件的运行方式, 最简单的方法是打开数据库, 然后试试各种指令. (如果数据库软件卡死, 请尝试运行 `/wl reload`)

| ID | Type | Ban | UUID | Name | Time       |
|----|------|-----|------|------|------------|
| 1  | 0    | 0   | UUID | Name | 1694231705 |

- `ID` - 唯一的自增ID
- `Type` - 这个账户的类型:
  - `0`: `NOT` - 未定义 or 不在白名单中
    使用 `/wl del` 时, 插件不会将玩家永久从白名单中移除, 而是将 Type 改为 NOT. 这用于支持账户数据清理以及其他查询功能.
  - `1`: `VISIT` - 参观账户. 如果开启参观账户, 那么玩家第一次加入时 Type 会从 NOT 转换为 VISIT
  - `2`: `WHITE` - 在白名单中.  
    将一些非白名单(WHITE) 类型的玩家通过 `/wl add` 设置为白名单时, 根据当前类型运行不同程序...  
    这一部分的核心代码是这样的: 
    ```java
    switch(pd.Type){
        // 如果为这些 Type, 数据库中一定有名称和 UUID
        case VISIT, VISIT_CONVERT -> { // 参观账户/需要转换的参观账户
            // 运行 wl-add
            startVisitConvertFunc(plugin, pd.Name, pd.UUID, "visit.wl-add.command");
            // 玩家在线
            Player player = Bukkit.getPlayer(pd.Name);
            if(player != null){
                // 是否需要踢出玩家
                if(plugin.getConfig().getBoolean("whitelist.kick-on-add-visit")){
                    player.kickPlayer(plugin.getConfig().getString("message.join.add"));
                }else{
                    // 运行 wl-add-convert
                    startVisitConvertFunc(plugin, pd.Name, pd.UUID, "visit.wl-add-convert.command");
                }
            }else{
                // 标记为 VISIT_CONVERT, 等待下一次加入时处理 (在 onPlayerJoin 中)
                pd.Type = Type.VISIT_CONVERT;
                getLogger().info("[IpacWhitelist] "+ pd.Name +" 不在线, wl-add-convert 将推迟到玩家重新上线时运行");
            }
            sender.sendMessage(plugin.getConfig().getString("message.command.add-reset-visit", "").replace("%player%", pd.Name));
        }

        // 如果为这些 Type, 数据库中可能缺失名称或 UUID

        case WHITE -> // 白名单, 只需要更新即可
                sender.sendMessage(plugin.getConfig().getString("message.command.add-reset", "").replace("%player%", inpData));
        case NOT -> { // 没有账户, 添加到白名单
            pd.Type = Type.WHITE;
            sender.sendMessage(plugin.getConfig().getString("message.command.add", "").replace("%player%", inpData));
        }
        default -> {
            sender.sendMessage(plugin.getConfig().getString("message.command.err", ""));
            return true;
        }
    }
    ```
  - `4`: `VISIT_CONVERT` - 需要转换的参观账户.  
    当参观账户(VISIT) 被添加到白名单(WHITE) 时, 会为参观账户运行配置中设定的转换程序, 会踢出玩家(此时设置为 VISIT_CONVERT), 并在玩家重新上线后运行剩下的转换程序, 然后再设置为 WHITE. 至此完成参观账户到白名单的转换.  
    [!] 如果为 VISIT_CONVERT 类型的玩家继续运行 `/wl add`, 那么它会重新执行一遍完整的转换程序. 
  - 是的, 没有 `3`.

- `Ban` - 这个账户是否被封禁:
  - `0`: `NOT_BAN` - 没有被封禁
  - `1`: `BAN` - 已被封禁: 
    为了防止数据混乱, 所以 Ban 是单独的.  
    如果账户已被封禁, 则会完全绕过加入部分的代码. 也就是无论 Type 如何, 都不能加入.  
    [!] 如果账户处于 BAN 状态, 则涉及其他数据的操作都不能使用! 比如 `/wl add|del|clean`, `/wl clean_visit` 也会绕过处于 Ban 状态的参观账户.  

- `UUID` AND `Name` - 存储玩家的 UUID 和名称.
  - UUID 使用 36 位(带连字符) 的字符串格式保存在数据库中. 
  - Name 只是字符串.
    可以在建表时通过配置中的 `sql.Name_COLLATE_NOCASE` 切换此列的大小写敏感. 

- `Time` - 数据操作的时间戳
  这是毫秒级时间戳, 在玩家加入/退出和使用 `/wl add` 时都会更新为当前时间.  
  它被用于判断这个账户是否过期(默认4个月), 以及是否达到可以安全删除数据的时间(默认12小时)


#### SQLite

直到现在, 我也没有去支持 MySQL 之类的数据库. 我认为它在这个应用里并不实用, 如果我需要去适配群组端, 那么才可能会支持它...  

在这个插件里, SQLite 的连接只会在 `/wl reload` 或者关服时被释放, 其余时间都保持连接并且文件被锁定, 同时不会进行提交(当`db-wal`达到一定大小时, SQLite会自动提交它). 所以不要删除 `db-shm` 和 `db-wal` 文件.  

```roomsql
PRAGMA journal_mode = WAL; -- 设置为 WAL 运行模式
PRAGMA auto_vacuum = 2;    -- 自动复用碎片空间
```

</details>


### 配置
```yaml
# 数据库
sql:
  # 使 Name 大小写不敏感, 同时影响白名单和指令. 仅可在建表时修改
  Name_COLLATE_NOCASE: true

# 连接到其他插件, 修改此处需要重启服务器
hook:
  AuthMe: false


# 白名单功能
whitelist:
  # 简易的 ip 黑名单. 在这里添加正则表达式, 匹配的ip不允许加入服务器 (也不允许使用参观账户
  # 使用 /wl reload 重载配置即可应用
  ip-blacklist: []
  #    - '^192\.168\.100\..+$'
  #    - '^fe80::1234:.+$' # ipv6没有方括号

  # 限定玩家可以使用的用户名正则
  name-rule: '^\.?[a-zA-Z0-9_]+$' # 白名单中的玩家
  name-rule-visit: '^\.?[a-zA-Z0-9_]+$' # 参观账户

  # 玩家在线时, 对其使用 /wl del 或 /wl ban 等指令是否需要踢出玩家
  kick-on-del: true # del
  kick-on-ban: true # ban
  kick-on-add-visit: true # 参观账户被添加到白名单时

  # 自动检查并处理出错的记录
  # 比如添加玩家 UUID 后又添加了 NAME (此时产生两条记录), 当玩家登录时自动删除其中一条记录
  autoClean:
    enable: true
    # 如果两条记录中的关键数据不一致, 比如记录1是白名单, 而记录2中是黑名单, 则将记录2中的数据迁移到记录1
    dataByWeight: true

  # 如果玩家在指定秒数内没上线过, 则视为不在白名单中. -1 = 禁用
  # 参观账户不受此限制
  timeout: 10368000 # 10368000 = 4个月

  # 服务器启动完成后等待多少毫秒才能允许玩家加入
  late-join-time: 4000 # 4秒

  # 玩家因任何原因断开连接后需要等待多长时间才能再次加入 (毫秒
  # 用于防止玩家在短时间内重复加入退出
  playerDisconnectToReconnectMinTime: 1000


# 参观账户
# 参观账户允许不在白名单中的玩家加入服务器, 可以限制其功能
visit:
  # 如果开启, 不在白名单和移出白名单的玩家将会以参观账户的形式加入服务器. 白名单过期和封禁中的账户依然不可加入服务器
  # 关闭可重载配置, 但开启它需要重启服务器
  enable: false
  # 最多允许同时加入多少参观账户
  max-visit-player: 10

  # 限定参观账户只能使用以下域名加入服务器, 否则提示不在白名单
  limit-hostname:
    enable: false
    list:
      - 'visit.your-mc-server.com:25565'
      - 'visit2.your-mc-server.com:25565'

  # 需要启用 hook.AuthMe
  # 自动注册和登录都在运行完事件程序后运行
  AuthMe:
    autoRegisterPassword: 'complexPassword' # 需要在这里填写一个复杂的密码
    autoRegister: true # 为参观账户自动注册, 相当于 `authme register <playerName> <password>`, 但不会踢出玩家
    autoLogin: true # 为参观账户自动登录, 相当于 `/authme forcelogin <playerName>`

  # 这些指令只是示例, 请根据自己的需求修改
  # 关于 LuckPerms. 使用 lp 命令操作权限时, 需要保证用户至少有一个权限组. 也就是先添加权限组再删除权限组

  # 参观账户事件程序
  # command 和 message 中可使用的变量: %playerName%, %playerUUID%
  event:
    onNewVisitPlayerLoginEvent: # 参观账户第一次登录服务器 (第一次登录也会触发 onVisitPlayerLoginEvent 事件)
      command:
        - 'lp user %playerUUID% parent add visit' # 将玩家添加到 visit 用户组
        - 'gamemode spectator %playerName%' # 将玩家设置为观察模式

    onVisitPlayerLoginEvent: # 参观账户登录服务器
      command: []

    onVisitPlayerJoinEvent: # 参观账户加入服务器
      command: []
      message:
        - '§6IpacEL §f> §a您正在使用参观账户=w='

    onVisitPlayerQuitEvent: # 参观账户退出服务器
      command: []


  # 参观账户被添加到白名单时执行一些指令 `/wl add <playerName> [playerUUID]`
  # 可用变量: %playerName%, %playerUUID% (如果指令中不包含 UUID, 则会从数据库中查找)
  wl-add:
    command:
      - 'lp user %playerUUID% parent remove visit' # 将玩家移出 visit 用户组
      - 'authme unregister %playerName%' # 取消注册玩家

  # 参观账户被添加到白名单后执行一些指令, 如果参观账户现在不在线, 则推迟到下一次上线时运行
  # 可用变量: %playerName%, %playerUUID% (从玩家对象中获取)
  wl-add-convert:
    command:
      - 'spawn %playerName%' # 传送到重生点
      - 'gamemode survival %playerName%' # 将玩家设置为生存模式


# 玩家加入消息广播
playerJoinMessage:
  enable: true # 修改这个需要重启服务器

  # 退出的锁在多长时间后释放 (毫秒
  quitLockFreedTime: 2000

  # 参观账户是否可以收到自己的加入消息
  visitOwnJoinMessage: false
  # 玩家是否可以收到自己的加入消息
  ownJoinMessage: true

  # 事件触发时将发出广播消息
  # 如果 message 留空, 则忽略这个事件
  # 如果 terminate: true, 运行后将不再运行其他事件的广播. 默认为 true, 可以不需要填写

  # 玩家加入
  playerJoin:

    # IpacWL 参观账户登录服务器
    onVisitPlayerJoin:
      message: '§6IpacEL §f> §a%player% §b使用参观账户加入游戏'

    # AuthMe 玩家登录或注册成功
    onAuthMeLoginEvent: # 需要启用 hook.AuthMe
      message: '§6IpacEL §f> §a%player% §b加入游戏'

    # 玩家加入事件. 如果使用 AuthMe, 同时这里留空, 就能在玩家登录后显示加入游戏
    onPlayerJoinEvent:
      message: '§6IpacEL §f> §a%player% §b加入游戏'

  # 玩家退出
  playerQuit:

    # AuthMe 登录密码错误
    onAuthMeFailedLoginEvent: # 需要启用 hook.AuthMe
      message: '§6IpacEL §f> §a%player% §b断开连接: §7密码错误'

    # AuthMe 玩家注销事件 (/logout)
    onLogoutEvent:
      message: ''

    # 玩家退出事件
    onPlayerQuitEvent:
      message: '§6IpacEL §f> §a%player% §b跑了'



# 模块配置
dev:
  # 删除一个玩家的所有数据
  deletePlayerDataAll:
    # 只删除玩家离线超过指定时间的数据, 防止删除后服务器又进行保存 (毫秒
    deleteDataTimeout: 43200000 # 12小时. 它必须大于数据缓存时间
    # true=在删除期间锁定正在删除的账户, false=在数据删除期间完全禁用参观账户
    deletingLockPlayer: true
    # 删除两个参观账户之间间隔多长时间, 然后取消这个玩家的锁定, 有些插件会使用异步处理删除请求 (毫秒
    intervalTime: 100
    # 执行指令与删除文件之间间隔多长时间 (毫秒
    intervalTime2: 150

    # 执行哪些指令, 来删除其他插件数据. 可用变量: %playerUUID%, %playerName%
    playerDataCommand:
      - 'lp user %playerUUID% clear'
    #      - 'authme unregister %playerName%' # AuthMe: 取消注册这个玩家

    # 玩家数据文件的路径. 可用变量: %playerUUID%, %playerName%
    playerDataFile:
      # world 存档
      - 'world/playerdata/%playerUUID%.dat'
      - 'world/playerdata/%playerUUID%.dat_old'
      - 'world/advancements/%playerUUID%.json'
      - 'world/stats/%playerUUID%.json'
      # Essentials 插件数据
    #      - 'plugins/Essentials/userdata/%playerUUID%.yml'

    # 如果你使用一些会将玩家数据存储在每个world目录里的多世界插件, 则可以使用这个自动遍历所有world目录
    # 玩家存档路径. 可用变量: %worldRoot%, %playerUUID%, %playerName%
    playerDataFileWorld:
    # 主世界
    #      - '%worldRoot%/playerdata/%playerUUID%.dat'
    #      - '%worldRoot%/playerdata/%playerUUID%.dat_old'
    #      - '%worldRoot%/advancements/%playerUUID%.json'
    #      - '%worldRoot%/stats/%playerUUID%.json'
    # 下界和末地
#      - '%worldRoot%/DIM-1/playerdata/%playerUUID%.dat'
#      - '%worldRoot%/DIM1/playerdata/%playerUUID%.dat'


# 消息
message:
  # 指令消息
  command:
    add: '§6IpacEL §f> §b%player% §a已添加到白名单'
    add-reset: '§6IpacEL §f> §b%player% §a的白名单已重置'
    add-reset-visit: '§6IpacEL §f> §b%player% §a已从参观账户中重置'
    del: '§6IpacEL §f> §a%player% §b已移出白名单'
    ban: '§6IpacEL §f> §a%player% §b已列入黑名单'
    ban-exist: '§6IpacEL §f> §a%player% §b在黑名单中'
    unban: '§6IpacEL §f> §b%player% §a已移出黑名单'
    unban-exist: '§6IpacEL §f> §a%player% §b不在黑名单中'
    reload: '§6IpacEL §f> IpacWhitelist 配置和数据库重载完成'
    info: '§6IpacEL §f> §a%player%§7: §f{§bID§f: §6%ID%§f, §bType§f: "§6%Type%§f", §bBan§f: "§6%Ban%§f", §bUUID§f: "§a%UUID%§f", §bName§f: "§a%Name%§f", §bTime§f: §6%Time%§f}'
    list: '§6IpacEL §f> §a%num% §7-> §f{§bID§f: §6%ID%§f, §bType§f: "§6%Type%§f", §bBan§f: "§6%Ban%§f", §bUUID§f: "§a%UUID%§f", §bName§f: "§a%Name%§f", §bTime§f: §6%Time%§f}'
    clean: '§6IpacEL §f> §b数据删除程序将在后台执行'
    clean-ok: '§6IpacEL §f> §a共删除了 §b%num% §a个账户及其数据'
    err: '§6IpacEL §f> §b内部错误'
    err-ban: '§6IpacEL §f> §b此账户被封禁, 执行此操作需要解封'
    err-length: '§6IpacEL §f> §b名称或UUID长度异常'
    err-parameter: '§6IpacEL §f> §b无效参数: §a%i%'
    err-note-exist: '§6IpacEL §f> §a%player% §b不存在'
    err-clean-incomplete: '§6IpacEL §f> §a%player% §b数据不完整, 可能这位玩家从未进入过服务器'
    err-clean-deleteDataTimeout: '§6IpacEL §f> §a%player% §b未达到可删除的时间'
    err-clean-online: '§6IpacEL §f> §a%player% §b玩家在线'
    err-permission: '§6IpacEL §f> §b权限不足'

  # 玩家加入
  join:
    add: '§6IpacEL §f> §a您的白名单已重置, 请重新加入服务器'
    not: '§6IpacEL §f> §b您不在白名单中或已失效, 请联系管理员恢复§7: §a%player%'
    expired: '§6IpacEL §f> §b太久没有上线? 请联系管理员恢复白名单§7: §a%player%'
    limiter-reconnection-time: '§6IpacEL §f> §b连接受限'
    ban: '§6IpacEL §f> §b您被列入黑名单: §a%player%'
    ban-ip: '§6IpacEL §f> §b您被列入黑名单: §a%player%' # %ip%
    err: '§6IpacEL §f> §a发生内部错误, 请稍后重试或联系管理员解决'
    err-name: '§6IpacEL §f> §b无效的名称: §a%player%'
    err-name-visit: '§6IpacEL §f> §b无效的名称: §a%player%'
    starting: '§6IpacEL §f> §b服务器正在启动'
    clean: '§6IpacEL §f> §b正在清理数据... 请稍后再试'

  # 参观账户加入
  visit:
    illegal-hostname: '§6IpacEL §f> §b您不在白名单中或已失效, 请联系管理员恢复§7: §a%player%' # 未使用限定的主机名
    full: '§6IpacEL §f> §b参观队列已满'

```

### 权限
```yaml
permissions:
  IpacWhitelist.command.reload:
    description: '使用 /wl reload 指令'
    default: op

  IpacWhitelist.command.add:
    description: '使用 /wl add 指令'
    default: op

  IpacWhitelist.command.del:
    description: '使用 /wl del 指令'
    default: op

  IpacWhitelist.command.ban:
    description: '使用 /wl ban 指令'
    default: op

  IpacWhitelist.command.unban:
    description: '使用 /wl unban 指令'
    default: op

  IpacWhitelist.command.info:
    description: '使用 /wl info 指令'
    default: op

  IpacWhitelist.command.list:
    description: '使用 /wl list 指令'
    default: op

  IpacWhitelist.command.clean:
    description: '使用 /wl clean 指令'
    default: op
```
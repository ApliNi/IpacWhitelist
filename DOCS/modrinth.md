https://github.com/ApliNi/IpacWhitelist

---

# IpacWhitelist

[[下载]](https://github.com/ApliNi/IpacWhitelist/actions) - 选择第一个, 滑动到最下方, 下载压缩包文件, 使用压缩包中的第一个.jar即可


### 指令列表
- `/wl` - 主命令
- `/wl reload` - 重载配置
- `/wl add <playerName|playerUUID>` - 添加到白名单, uuid为空时自动获取
- `/wl del <playerName|playerUUID>` - 取消玩家的白名单
- `/wl ban <playerName|playerUUID>` - 封禁玩家
- `/wl unban <playerName|playerUUID>` - 解封玩家
- `/wl info <playerName|playerUUID>` - 查看玩家数据
- `/wl list <VISIT|WHITE|BLACK|VISIT_CONVERT|VISIT_BLACK|*> <num|ALL>` - 列出玩家数据
- `/wl reconnect_database` - 重新连接数据库

> 支持使用 32 或 36 位的 UUID

---

### 配置
```yaml
# 数据库
sql:
  # sqlite, 暂不支持 mysql
  db: sqlite
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
    # 玩家退出事件
    onPlayerQuitEvent:
      message: '§6IpacEL §f> §a%player% §b跑了'


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
    info: '§6IpacEL §f> §a%player%§7: §b{ID: %ID%, Type: "%Type%", Ban: "%Ban%", UUID: "%UUID%", Name: "%Name%", Time: %Time%}'
    list: '§6IpacEL §f> §a%num% §7-> §b{ID: %ID%, Type: "%Type%", Ban: "%Ban%", UUID: "%UUID%", Name: "%Name%", Time: %Time%}'
    err: '§6IpacEL §f> §b内部错误'
    err-ban: '§6IpacEL §f> §b此账户被封禁, 执行此操作需要解封'
    err-length: '§6IpacEL §f> §b名称或UUID长度异常'
    err-parameter: '§6IpacEL §f> §b无效参数: §a%i%'
    err-note-exist: '§6IpacEL §f> §a%player% §b不存在'
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

  IpacWhitelist.command.clean_visit:
    description: '使用 /wl clean_visit 指令'
    default: op
```
# IpacWhitelist

[[下载]](https://github.com/ApliNi/IpacWhitelist/actions) - 选择第一个, 滑动到最下方, 下载压缩包文件, 使用压缩包中的第一个.jar即可


Ipacamod 服务器的新白名单插件
- [x] 白名单功能
- [x] 使用数据库
- [x] 根据最后上线时间取消白名单


**指令列表**
- `/wl` - 主命令
- `/wl reload` - 重载配置
- `/wl add <playerName> [playerUUID]` - 添加到白名单, uuid为空时自动获取
- `/wl del <playerName|playerUUID>` - 取消玩家的白名单
- `/wl ban <playerName|playerUUID>` - 封禁玩家
- `/wl unban <playerName|playerUUID>` - 取消封禁玩家
- `/wl reconnect_database` - 重新连接数据库


**特性**
- 玩家加入时要求UUID与白名单中的相同
  - 如果UUID为空则要求名称相同, 并在该玩家加入时填充UUID
- 如果玩家改名, 则更新名称
  - 如果新名称与其他名称相同, 则报错

> 在出现一个能像 FloodGate 一样自动修改玩家名称前缀的软件出来之前, 您需要让新的离线玩家名称中包含任意正版账号不支持的字符, 比如 `-`. 
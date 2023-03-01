# IpacWhitelist

[[下载]](https://github.com/ApliNi/IpacWhitelist/actions) - 选择第一个, 滑动到最下方, 下载压缩包文件, 使用压缩包中的第一个.jar即可

Ipacamod 服务器的新白名单插件
- [x] 白名单功能
- [x] 使用数据库
- [ ] 自动为离线玩家设置名称前缀
- [ ] 根据最后上线时间取消白名单
- [ ] 完全删除功能
  - [ ] 同时删除各处的用户数据

**指令列表**
- `/wl` - 主命令
- `/wl reload` - 重载配置
- `/wl add <name> [uuid]` - 添加到白名单, uuid为空时自动获取
- `/wl del <name|uuid>` - 取消一个玩家的白名单, 不删除数据库中的记录
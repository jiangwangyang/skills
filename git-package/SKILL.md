---
name: git-package
description: 打包(git提交历史)和解包(json文件)。将 git 仓库从指定版本开始的全部提交打包为可文本传递的 JSON，回滚到该版本后按原提交信息逐个重新提交，恢复全部提交。
---

# Git Commits Pack and Unpack

## 目标

- 将 git 仓库从指定版本 (base)开始的全部提交打包成 json 文件

```bash
# 打包 base 之后的全部提交(不含 base 本身)
python scripts/pack_git_commits.py your_repo commits.json --base v1.0

# 同时打包未提交的变动(含未跟踪文件, 恢复时写入工作区不建提交)
python scripts/pack_git_commits.py your_repo commits.json --base v1.0 --include-uncommitted
```

- 回滚到 base 版本后, 用打包的 json 文件恢复全部提交

```bash
# 要求当前 HEAD 等于 base 且工作区干净
python scripts/unpack_git_commits.py commits.json your_repo

# 强制恢复: 先 git reset --hard 到 base 再恢复(会丢弃未提交内容)
python scripts/unpack_git_commits.py commits.json your_repo --force
```

## JSON 格式

```json
{
    "base": "<base 提交哈希>",
    "commits": [
        {
            "tree": "<该提交的内容树哈希, 用于恢复后逐提交校验>",
            "message": "<完整提交信息>",
            "changes": {
                "文件路径": {
                    "status": "A|M|D",
                    "encoding": "text|base64",
                    "content": "..."
                }
            }
        }
    ],
    "uncommitted": {
        "文件路径": {
            "status": "A|M|D",
            "encoding": "text|base64",
            "content": "..."
        }
    }
}
```

## 说明

- 快照语义: 新增/修改存该提交时的完整文件内容 (从 git 对象读取, 不受工作区影响), 恢复时直接写入覆盖; 删除的文件恢复时执行删除
- 恢复时逐提交执行 写入变动 -> git add -A -> git commit -m, 只保留提交信息, 作者/时间为恢复环境的当前配置
- 每个提交恢复后用 tree 哈希自动校验内容一致性, 不一致会输出警告
- 合并提交按第一父提交线性化, 恢复后为线性历史
- 文本文件打包时统一转为 LF 换行; 二进制文件自动 base64 编码; 文件可执行权限等模式信息不保留
- JSON 为纯文本, 可直接复制/发送传递
- 恢复环境需配置 git user.name/user.email

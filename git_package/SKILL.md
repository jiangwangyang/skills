---
name: git_package
description: 打包(git仓库变动部分)和解包(json文件)文件。将 git 仓库中未提交的增删改变动打包为可文本传递的 JSON，项目回滚后可用该 JSON 恢复全部变动。
---

# Git Package and Unpack

## 目标

- 将 git 仓库的变动部分（新增/修改/删除，含未跟踪文件）打包成 json 文件

```bash
# 打包相对 HEAD 的全部未提交变动（默认）
python scripts/pack_git_changes.py your_repo changes.json

# 打包相对指定 commit/分支 的全部差异
python scripts/pack_git_changes.py your_repo changes.json --base main
```

- 项目回滚后，用打包的 json 文件恢复全部变动

```bash
python scripts/unpack_git_changes.py changes.json your_repo
```

## 说明

- JSON 根层级直接是变动内容，无元数据包装，格式为：
  `{ "文件路径": {"status": "A|M|D", "encoding": "text|base64", "content": "..."} }`
- JSON 为纯文本，可直接复制/发送传递；二进制文件自动 base64 编码
- 文本文件打包时统一转为 LF 换行，不会把 CRLF 打包进去；恢复时按 LF 写入
- 快照语义：新增/修改存完整内容，恢复时直接写入覆盖；删除的文件恢复时执行删除

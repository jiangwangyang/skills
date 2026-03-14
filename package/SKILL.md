---
name: package
description: 打包(目录文件)和解包(json文件)文件
---

# Pack and Unpack

## 目标

- 将目录打包成json文件

```bash
python scripts/pack_json.py your_dir your_dir.json
```

- 将打包的json文件解包为目录

```bash
python scripts/unpack_json.py pack.json restore_dir
```

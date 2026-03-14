---
name: rules
description: 默认规则 必须加载
---

# 默认规则 必须遵守

- 文件编码: UTF-8 without BOM
- 语言: 简体中文 禁止出现乱码
- python:
    - 禁止使用 `python`
    - 使用 `uv run your_script.py`
- pip:
    - 禁止使用 `pip`
    - 使用 `uv pip install ...`

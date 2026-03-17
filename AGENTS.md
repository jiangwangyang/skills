# 默认规则 必须遵守

- 文件编码: UTF-8 without BOM
- 语言: 简体中文 禁止出现乱码
- python:
    - 禁止使用 `python`
    - 必须使用 `uv run your_script.py`
- pip:
    - 禁止使用 `pip`
    - 必须使用 `uv pip ...`
    - 使用镜像 `-i http://mirrors.aliyun.com/pypi/simple/`

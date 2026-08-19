import argparse
import base64
import json
import os
import subprocess
import sys


def run_git(repo_dir: str, args: list) -> str:
    """执行 git 命令并返回 stdout（文本）。"""
    result = subprocess.run(
        ['git', '-C', repo_dir] + args,
        capture_output=True, text=True, encoding='utf-8', errors='replace'
    )
    if result.returncode != 0:
        print(f"错误：git {' '.join(args)} 执行失败:\n{result.stderr.strip()}")
        sys.exit(1)
    return result.stdout


def read_file_content(file_path: str) -> dict:
    """读取文件内容，文本文件用 utf-8（统一转为 LF 换行），失败则 base64。"""
    with open(file_path, 'rb') as f:
        raw = f.read()
    try:
        text = raw.decode('utf-8')
    except UnicodeDecodeError:
        return {'encoding': 'base64', 'content': base64.b64encode(raw).decode('ascii')}
    # 统一换行符为 LF，不把 CRLF 打包进去
    text = text.replace('\r\n', '\n').replace('\r', '\n')
    return {'encoding': 'text', 'content': text}


def pack_git_changes(repo_dir: str, output_json: str, base: str):
    if not os.path.isdir(repo_dir):
        print(f"错误：路径 '{repo_dir}' 不存在或不是目录。")
        return

    # 校验是否为 git 仓库
    run_git(repo_dir, ['rev-parse', '--git-dir'])

    # 校验 base 是否有效
    run_git(repo_dir, ['rev-parse', '--verify', base])

    # 1. 已跟踪文件的变动（修改/删除/新增到暂存区）：git diff --name-status <base>
    #    包含 staged + unstaged，基准为 base（默认 HEAD）
    diff_output = run_git(repo_dir, ['diff', '--name-status', base])

    changes = {}
    for line in diff_output.splitlines():
        if not line.strip():
            continue
        parts = line.split('\t')
        status_code = parts[0]
        # 重命名 R100 形式：视为 删除旧文件 + 新增新文件
        if status_code.startswith('R'):
            old_path, new_path = parts[1], parts[2]
            changes[old_path.replace('\\', '/')] = {'status': 'D'}
            entry = {'status': 'A'}
            entry.update(read_file_content(os.path.join(repo_dir, new_path)))
            changes[new_path.replace('\\', '/')] = entry
            print(f"已读取(重命名): {old_path} -> {new_path}")
            continue

        rel_path = parts[1].replace('\\', '/')
        if status_code == 'D':
            changes[rel_path] = {'status': 'D'}
            print(f"已记录(删除): {rel_path}")
        else:  # M / A / T 等
            full_path = os.path.join(repo_dir, rel_path)
            if not os.path.isfile(full_path):
                # 工作区中已删除但 diff 未标 D 的边界情况
                changes[rel_path] = {'status': 'D'}
                print(f"已记录(删除): {rel_path}")
                continue
            entry = {'status': 'M' if status_code != 'A' else 'A'}
            entry.update(read_file_content(full_path))
            changes[rel_path] = entry
            print(f"已读取({'修改' if entry['status'] == 'M' else '新增'}): {rel_path}")

    # 2. 未跟踪的新文件（未被 .gitignore 忽略）
    untracked_output = run_git(repo_dir, ['ls-files', '--others', '--exclude-standard'])
    for line in untracked_output.splitlines():
        rel_path = line.strip().replace('\\', '/')
        if not rel_path or rel_path in changes:
            continue
        entry = {'status': 'A'}
        entry.update(read_file_content(os.path.join(repo_dir, rel_path)))
        changes[rel_path] = entry
        print(f"已读取(未跟踪新增): {rel_path}")

    if not changes:
        print("\n没有检测到任何变动，未生成打包文件。")
        return

    # 根层级直接放变动内容，无元数据包装
    with open(output_json, 'w', encoding='utf-8', newline='\n') as jf:
        json.dump(changes, jf, ensure_ascii=False, indent=4)

    added = sum(1 for v in changes.values() if v['status'] == 'A')
    modified = sum(1 for v in changes.values() if v['status'] == 'M')
    deleted = sum(1 for v in changes.values() if v['status'] == 'D')
    print(f"\n成功！已打包至: {output_json}")
    print(f"统计: 新增 {added} 个, 修改 {modified} 个, 删除 {deleted} 个")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='打包 git 仓库的变动部分（增删改）为 JSON 文件',
        epilog='使用示例: python pack_git_changes.py repo_dir output.json --base HEAD'
    )
    parser.add_argument('repo_dir', type=str, help='git 仓库目录路径（必填）')
    parser.add_argument('output_json', type=str, help='打包后生成的 JSON 文件路径（必填）')
    parser.add_argument('--base', type=str, default='HEAD',
                        help='对比基准（commit/分支/tag），默认为 HEAD，即打包全部未提交变动')

    args = parser.parse_args()
    pack_git_changes(args.repo_dir, args.output_json, args.base)

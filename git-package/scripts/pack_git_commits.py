import argparse
import base64
import json
import os
import subprocess
import sys


def pack_git_commits(repo_dir: str, output_json: str, base: str, include_uncommitted: bool) -> None:
    # 校验仓库目录存在
    if not os.path.isdir(repo_dir):
        print(f"错误: 路径 '{repo_dir}' 不存在或不是目录")
        sys.exit(1)

    # 校验是否为 git 仓库
    result = subprocess.run(['git', '-C', repo_dir, 'rev-parse', '--git-dir'], capture_output=True)
    if result.returncode != 0:
        print(f"错误: '{repo_dir}' 不是 git 仓库")
        sys.exit(1)

    # 解析 base 为完整提交哈希
    result = subprocess.run(['git', '-C', repo_dir, 'rev-parse', '--verify', f'{base}^{{commit}}'], capture_output=True)
    if result.returncode != 0:
        print(f"错误: 无效的基准版本 '{base}'")
        sys.exit(1)
    base_sha = result.stdout.decode('ascii', errors='replace').strip()

    # 获取 base 之后的全部提交(从旧到新)
    result = subprocess.run(['git', '-C', repo_dir, 'rev-list', '--reverse', f'{base_sha}..HEAD'], capture_output=True)
    if result.returncode != 0:
        print(f"错误: 读取提交列表失败:\n{result.stderr.decode('utf-8', errors='replace').strip()}")
        sys.exit(1)
    commit_shas = [line.strip() for line in result.stdout.decode('ascii', errors='replace').splitlines() if line.strip()]
    print(f"基准: {base_sha[:8]}, 共 {len(commit_shas)} 个提交待打包")

    # 逐提交记录 提交信息 + 相对第一父提交的变动快照
    commits = []
    for index, sha in enumerate(commit_shas, 1):
        # 读取完整提交信息
        result = subprocess.run(['git', '-C', repo_dir, 'show', '-s', '--format=%B', sha], capture_output=True)
        if result.returncode != 0:
            print(f"错误: 读取提交 {sha[:8]} 的信息失败")
            sys.exit(1)
        message = result.stdout.decode('utf-8', errors='replace').strip()

        # 记录该提交的内容树哈希, 用于恢复后逐提交校验
        result = subprocess.run(['git', '-C', repo_dir, 'rev-parse', '--verify', f'{sha}^{{tree}}'], capture_output=True)
        tree_sha = result.stdout.decode('ascii', errors='replace').strip()

        # 读取相对第一父提交的变动列表(合并提交按第一父线性化, 关闭重命名检测)
        # 使用 -z 避免路径转义问题, 输出格式为 状态\0路径\0 交替排列
        result = subprocess.run(
            ['git', '-C', repo_dir, 'diff', '--name-status', '-z', '--no-renames', f'{sha}^1', sha],
            capture_output=True
        )
        if result.returncode != 0:
            print(f"错误: 读取提交 {sha[:8]} 的变动失败:\n{result.stderr.decode('utf-8', errors='replace').strip()}")
            sys.exit(1)
        tokens = result.stdout.decode('utf-8', errors='replace').split('\0')

        changes = {}
        # 状态与路径成对出现, 步进读取
        for i in range(0, len(tokens) - 1, 2):
            status_code = tokens[i]
            rel_path = tokens[i + 1].replace('\\', '/')
            if not status_code or not rel_path:
                continue
            if status_code == 'D':
                changes[rel_path] = {'status': 'D'}
                print(f"  已记录(删除): {rel_path}")
                continue
            # A / M / T 等: 从该提交的 git 对象中读取完整内容
            result = subprocess.run(['git', '-C', repo_dir, 'show', f'{sha}:{rel_path}'], capture_output=True)
            if result.returncode != 0:
                print(f"错误: 读取文件 {rel_path} (提交 {sha[:8]}) 失败")
                sys.exit(1)
            raw = result.stdout
            try:
                # 统一换行符为 LF, 不把 CRLF 打包进去
                text = raw.decode('utf-8')
                entry = {'status': 'A' if status_code == 'A' else 'M', 'encoding': 'text',
                         'content': text.replace('\r\n', '\n').replace('\r', '\n')}
            except UnicodeDecodeError:
                # 二进制文件 base64 编码
                entry = {'status': 'A' if status_code == 'A' else 'M', 'encoding': 'base64',
                         'content': base64.b64encode(raw).decode('ascii')}
            changes[rel_path] = entry
            print(f"  已读取({'新增' if entry['status'] == 'A' else '修改'}): {rel_path}")

        commits.append({'tree': tree_sha, 'message': message, 'changes': changes})
        print(f"已打包提交 {index}/{len(commit_shas)}: {sha[:8]} {message.splitlines()[0] if message else '(无提交信息)'}")

    # 可选: 打包未提交的变动(已跟踪文件 + 未跟踪文件)
    uncommitted = {}
    if include_uncommitted:
        # 已跟踪文件的变动(修改/删除/新增到暂存区)
        result = subprocess.run(
            ['git', '-C', repo_dir, 'diff', '--name-status', '-z', '--no-renames', 'HEAD'],
            capture_output=True
        )
        tokens = result.stdout.decode('utf-8', errors='replace').split('\0')
        for i in range(0, len(tokens) - 1, 2):
            status_code = tokens[i]
            rel_path = tokens[i + 1].replace('\\', '/')
            if not status_code or not rel_path:
                continue
            full_path = os.path.join(repo_dir, rel_path)
            if status_code == 'D' or not os.path.isfile(full_path):
                uncommitted[rel_path] = {'status': 'D'}
                print(f"  已记录(删除): {rel_path}")
                continue
            # 从工作区读取完整内容
            with open(full_path, 'rb') as f:
                raw = f.read()
            try:
                text = raw.decode('utf-8')
                entry = {'status': 'A' if status_code == 'A' else 'M', 'encoding': 'text',
                         'content': text.replace('\r\n', '\n').replace('\r', '\n')}
            except UnicodeDecodeError:
                entry = {'status': 'A' if status_code == 'A' else 'M', 'encoding': 'base64',
                         'content': base64.b64encode(raw).decode('ascii')}
            uncommitted[rel_path] = entry
            print(f"  已读取({'新增' if entry['status'] == 'A' else '修改'}): {rel_path}")

        # 未跟踪的新文件(未被 .gitignore 忽略)
        result = subprocess.run(
            ['git', '-C', repo_dir, 'ls-files', '--others', '--exclude-standard', '-z'],
            capture_output=True
        )
        for rel_path in result.stdout.decode('utf-8', errors='replace').split('\0'):
            rel_path = rel_path.replace('\\', '/')
            if not rel_path or rel_path in uncommitted:
                continue
            with open(os.path.join(repo_dir, rel_path), 'rb') as f:
                raw = f.read()
            try:
                text = raw.decode('utf-8')
                entry = {'status': 'A', 'encoding': 'text', 'content': text.replace('\r\n', '\n').replace('\r', '\n')}
            except UnicodeDecodeError:
                entry = {'status': 'A', 'encoding': 'base64', 'content': base64.b64encode(raw).decode('ascii')}
            uncommitted[rel_path] = entry
            print(f"  已读取(未跟踪新增): {rel_path}")

    if not commits and not uncommitted:
        print("\n没有检测到任何提交或变动, 未生成打包文件")
        return

    # 组装 JSON 数据
    data = {'base': base_sha, 'commits': commits}
    if include_uncommitted:
        data['uncommitted'] = uncommitted

    with open(output_json, 'w', encoding='utf-8', newline='\n') as jf:
        json.dump(data, jf, ensure_ascii=False, indent=2)

    print(f"\n成功! 已打包至: {output_json}")
    print(f"统计: {len(commits)} 个提交 (基准 {base_sha[:8]})" + (f", 未提交变动 {len(uncommitted)} 个文件" if include_uncommitted else ""))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='打包 git 仓库从指定版本开始的全部提交为 JSON 文件',
        epilog='使用示例: python pack_git_commits.py repo_dir output.json --base v1.0'
    )
    parser.add_argument('repo_dir', type=str, help='git 仓库目录路径(必填)')
    parser.add_argument('output_json', type=str, help='打包后生成的 JSON 文件路径(必填)')
    parser.add_argument('--base', type=str, required=True, help='基准版本(commit/分支/tag), 打包该版本之后的全部提交')
    parser.add_argument('--include-uncommitted', action='store_true', help='同时打包未提交的变动(含未跟踪文件), 恢复时写入工作区不建提交')

    args = parser.parse_args()
    pack_git_commits(args.repo_dir, args.output_json, args.base, args.include_uncommitted)

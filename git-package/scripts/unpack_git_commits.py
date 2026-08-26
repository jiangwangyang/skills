import argparse
import base64
import json
import os
import subprocess
import sys


def unpack_git_commits(json_file: str, repo_dir: str, force: bool) -> None:
    # 校验输入文件和目标目录
    if not os.path.exists(json_file):
        print(f"错误: 找不到 JSON 文件 '{json_file}'")
        sys.exit(1)
    if not os.path.isdir(repo_dir):
        print(f"错误: 目标目录 '{repo_dir}' 不存在")
        sys.exit(1)

    # 读取 JSON 打包文件
    try:
        with open(json_file, 'r', encoding='utf-8') as jf:
            data = json.load(jf)
    except Exception as e:
        print(f"错误: 读取 JSON 失败: {e}")
        sys.exit(1)

    # 校验 JSON 结构
    if not isinstance(data, dict) or not isinstance(data.get('base'), str) or not isinstance(data.get('commits'), list):
        print("错误: JSON 格式不正确, 缺少 base 或 commits 字段")
        sys.exit(1)
    base_sha = data['base']
    commits = data['commits']
    uncommitted = data.get('uncommitted') or {}

    # 校验目标目录是 git 仓库
    result = subprocess.run(['git', '-C', repo_dir, 'rev-parse', '--git-dir'], capture_output=True)
    if result.returncode != 0:
        print(f"错误: '{repo_dir}' 不是 git 仓库")
        sys.exit(1)

    # 校验 base 提交在仓库中存在
    result = subprocess.run(['git', '-C', repo_dir, 'cat-file', '-e', f'{base_sha}^{{commit}}'], capture_output=True)
    if result.returncode != 0:
        print(f"错误: 基准提交 {base_sha[:8]} 不存在于当前仓库, 无法恢复")
        sys.exit(1)

    # 检查当前状态: HEAD 必须等于 base 且工作区干净, 否则需要 --force
    result = subprocess.run(['git', '-C', repo_dir, 'rev-parse', '--verify', 'HEAD'], capture_output=True)
    if result.returncode != 0:
        print("错误: 仓库当前没有任何提交, 请先回滚到基准版本")
        sys.exit(1)
    current_sha = result.stdout.decode('ascii', errors='replace').strip()

    result = subprocess.run(['git', '-C', repo_dir, 'status', '--porcelain'], capture_output=True)
    dirty = bool(result.stdout.decode('utf-8', errors='replace').strip())

    if current_sha != base_sha or dirty:
        if not force:
            if current_sha != base_sha:
                print(f"错误: 当前 HEAD ({current_sha[:8]}) 与打包基准 ({base_sha[:8]}) 不一致")
                print("请先回滚到基准版本, 或使用 --force 自动执行 git reset --hard")
            else:
                print("错误: 工作区存在未提交变动, 恢复可能失败")
                print("请先提交或清理工作区, 或使用 --force 自动执行 git reset --hard")
            sys.exit(1)
        # --force: 强制回滚到 base 并清理工作区
        result = subprocess.run(['git', '-C', repo_dir, 'reset', '--hard', base_sha], capture_output=True)
        if result.returncode != 0:
            print(f"错误: 回滚到基准版本失败:\n{result.stderr.decode('utf-8', errors='replace').strip()}")
            sys.exit(1)
        print(f"已回滚到基准版本: {base_sha[:8]}")

    if not commits and not uncommitted:
        print("打包文件中没有任何提交或变动, 无需恢复")
        return

    # 逐提交恢复: 写入变动 -> git add -A -> git commit
    for index, commit in enumerate(commits, 1):
        changes = commit.get('changes') or {}
        message = commit.get('message') or ''

        # 应用该提交的文件变动到工作区
        for rel_path, info in changes.items():
            full_path = os.path.join(repo_dir, rel_path)
            try:
                if info['status'] == 'D':
                    if os.path.exists(full_path):
                        os.remove(full_path)
                else:
                    parent_dir = os.path.dirname(full_path)
                    if parent_dir and not os.path.exists(parent_dir):
                        os.makedirs(parent_dir)
                    if info.get('encoding') == 'base64':
                        with open(full_path, 'wb') as f:
                            f.write(base64.b64decode(info['content']))
                    else:
                        with open(full_path, 'w', encoding='utf-8', newline='\n') as f:
                            f.write(info['content'])
            except Exception as e:
                print(f"错误: 恢复文件 {rel_path} 失败: {e}")
                sys.exit(1)

        # 暂存全部变动并创建提交(仅保留提交信息, 作者/时间为当前环境配置)
        result = subprocess.run(['git', '-C', repo_dir, 'add', '-A'], capture_output=True)
        if result.returncode != 0:
            print(f"错误: 第 {index} 个提交暂存失败:\n{result.stderr.decode('utf-8', errors='replace').strip()}")
            sys.exit(1)
        result = subprocess.run(
            ['git', '-C', repo_dir, 'commit', '--allow-empty', '--allow-empty-message', '-m', message],
            capture_output=True
        )
        if result.returncode != 0:
            print(f"错误: 第 {index} 个提交创建失败:\n{result.stderr.decode('utf-8', errors='replace').strip()}")
            sys.exit(1)
        print(f"已恢复提交 {index}/{len(commits)}: {message.splitlines()[0] if message else '(无提交信息)'}")

        # 用内容树哈希校验该提交的恢复结果
        if isinstance(commit.get('tree'), str):
            result = subprocess.run(['git', '-C', repo_dir, 'rev-parse', '--verify', 'HEAD^{tree}'], capture_output=True)
            if result.stdout.decode('ascii', errors='replace').strip() != commit['tree']:
                print(f"警告: 第 {index} 个提交的内容树与打包时不一致, 请检查")

    # 恢复未提交的变动到工作区(不建提交)
    if uncommitted:
        failed = 0
        for rel_path, info in uncommitted.items():
            full_path = os.path.join(repo_dir, rel_path)
            try:
                if info['status'] == 'D':
                    if os.path.exists(full_path):
                        os.remove(full_path)
                else:
                    parent_dir = os.path.dirname(full_path)
                    if parent_dir and not os.path.exists(parent_dir):
                        os.makedirs(parent_dir)
                    if info.get('encoding') == 'base64':
                        with open(full_path, 'wb') as f:
                            f.write(base64.b64decode(info['content']))
                    else:
                        with open(full_path, 'w', encoding='utf-8', newline='\n') as f:
                            f.write(info['content'])
            except Exception as e:
                print(f"警告: 恢复未提交文件 {rel_path} 失败: {e}")
                failed += 1
        print(f"已恢复未提交变动 {len(uncommitted) - failed} 个文件" + (f", 失败 {failed} 个" if failed else ""))

    print(f"\n完成! 共恢复 {len(commits)} 个提交")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='从 JSON 打包文件恢复 git 仓库的全部提交(需先回滚到打包基准版本)',
        epilog='使用示例: python unpack_git_commits.py commits.json repo_dir --force'
    )
    parser.add_argument('json_file', type=str, help='打包生成的 JSON 文件路径(必填)')
    parser.add_argument('repo_dir', type=str, help='要恢复到的 git 仓库目录(必填)')
    parser.add_argument('--force', action='store_true', help='强制恢复: 先 git reset --hard 到基准版本(会丢弃未提交内容)')

    args = parser.parse_args()
    unpack_git_commits(args.json_file, args.repo_dir, args.force)

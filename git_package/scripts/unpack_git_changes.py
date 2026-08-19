import argparse
import base64
import json
import os


def unpack_git_changes(json_file: str, repo_dir: str):
    if not os.path.exists(json_file):
        print(f"错误：找不到 JSON 文件 '{json_file}'。")
        return
    if not os.path.isdir(repo_dir):
        print(f"错误：目标目录 '{repo_dir}' 不存在。")
        return

    try:
        with open(json_file, 'r', encoding='utf-8') as jf:
            changes = json.load(jf)
    except Exception as e:
        print(f"读取 JSON 失败: {e}")
        return

    # JSON 根层级即为变动内容：{ "路径": {"status": ..., "encoding": ..., "content": ...} }
    if not isinstance(changes, dict) or not changes:
        print("打包文件中没有任何变动记录。")
        return

    restored, deleted, failed = 0, 0, 0
    for rel_path, info in changes.items():
        full_path = os.path.join(repo_dir, rel_path)
        try:
            if info['status'] == 'D':
                if os.path.exists(full_path):
                    os.remove(full_path)
                    print(f"已删除: {rel_path}")
                else:
                    print(f"跳过(不存在): {rel_path}")
                deleted += 1
            else:  # A / M
                parent_dir = os.path.dirname(full_path)
                if parent_dir and not os.path.exists(parent_dir):
                    os.makedirs(parent_dir)
                if info.get('encoding') == 'base64':
                    with open(full_path, 'wb') as f:
                        f.write(base64.b64decode(info['content']))
                else:
                    with open(full_path, 'w', encoding='utf-8', newline='\n') as f:
                        f.write(info['content'])
                print(f"已恢复({'新增' if info['status'] == 'A' else '修改'}): {rel_path}")
                restored += 1
        except Exception as e:
            print(f"恢复失败 {rel_path}: {e}")
            failed += 1

    print(f"\n完成！写入 {restored} 个文件, 删除 {deleted} 个文件" + (f", 失败 {failed} 个" if failed else ""))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description='从 JSON 打包文件恢复 git 仓库的变动（项目回滚后使用）',
        epilog='使用示例: python unpack_git_changes.py changes.json repo_dir'
    )
    parser.add_argument('json_file', type=str, help='打包生成的 JSON 文件路径（必填）')
    parser.add_argument('repo_dir', type=str, help='要恢复到的 git 仓库目录（必填）')

    args = parser.parse_args()
    unpack_git_changes(args.json_file, args.repo_dir)

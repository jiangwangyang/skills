import argparse
import json
import os


# 解析 JSON 并递归重建目录结构还原文件
def unpack_json(json_file: str, restore_folder: str):
    if not os.path.exists(json_file):
        print(f"错误：找不到 JSON 文件 '{json_file}'。")
        return

    try:
        with open(json_file, 'r', encoding='utf-8') as jf:
            combined_data = json.load(jf)

        for rel_path, content in combined_data.items():
            # 拼接完整的还原路径
            full_output_path = os.path.join(restore_folder, rel_path)

            # 关键：自动创建文件所在的父级目录
            parent_dir = os.path.dirname(full_output_path)
            if not os.path.exists(parent_dir):
                os.makedirs(parent_dir)

            with open(full_output_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"已还原: {rel_path}")

        print(f"\n成功！目录结构已在 '{restore_folder}' 完整还原。")
    except Exception as e:
        print(f"还原失败: {e}")


# 启动方法
if __name__ == "__main__":
    # 创建参数解析器
    parser = argparse.ArgumentParser(
        description='从JSON文件中还原目录结构和文件内容',
        epilog='使用示例: python unpack_json.py file.json restore_dir'
    )

    # 添加必选参数：JSON文件路径
    parser.add_argument(
        'json_file',
        type=str,
        help='待解析的JSON文件路径（必填）'
    )

    # 添加必选参数：还原目标目录
    parser.add_argument(
        'restore_folder',
        type=str,
        help='文件还原的目标目录（必填）'
    )

    # 解析命令行参数
    args = parser.parse_args()

    # 调用核心函数，传入解析后的参数
    unpack_json(args.json_file, args.restore_folder)

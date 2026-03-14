import argparse
import json
import os


# 递归读取文件夹内所有文本文件并合并
def pack_json(input_folder: str, output_json: str):
    if not os.path.exists(input_folder):
        print(f"错误：源路径 '{input_folder}' 不存在。")
        return

    combined_data = {}

    # os.walk 会递归遍历所有子目录
    for root, dirs, files in os.walk(input_folder):
        for filename in files:
            file_path = os.path.join(root, filename)

            # 计算相对路径，以便还原时保持目录结构
            # 例如：input_folder/sub/test.txt -> sub/test.txt
            rel_path = os.path.relpath(file_path, input_folder).replace("\\", "/")

            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    combined_data[rel_path] = f.read()
                print(f"已读取: {rel_path}")
            except Exception as e:
                print(f"跳过文件 {rel_path} (读取出错或非文本): {e}")

    with open(output_json, 'w', encoding='utf-8') as jf:
        json.dump(combined_data, jf, ensure_ascii=False, indent=4)
    print(f"\n成功！所有文件（含子目录）已打包至: {output_json}")


# 启动方法
if __name__ == "__main__":
    # 创建参数解析器
    parser = argparse.ArgumentParser(
        description='递归读取文件夹内所有文本文件并合并为JSON文件',
        epilog='使用示例: python pack_json.py input_dir output.json'
    )

    # 添加必选参数：输入文件夹路径
    parser.add_argument(
        'input_folder',
        type=str,
        help='要打包的源文件夹路径（必填）'
    )

    # 添加必选参数：输出JSON文件路径
    parser.add_argument(
        'output_json',
        type=str,
        help='打包后生成的JSON文件路径（必填）'
    )

    # 解析命令行参数
    args = parser.parse_args()

    # 调用核心函数，传入解析后的参数
    pack_json(args.input_folder, args.output_json)

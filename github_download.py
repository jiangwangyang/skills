import os
from typing import List, Dict

import requests


# 获取 GitHub 公共仓库指定目录下的所有内容（文件/目录）
def get_github_dir_contents(
        repo_owner: str,
        repo_name: str,
        path: str,
        branch: str = "main"
) -> List[Dict]:
    # 构建目录内容的 API 链接（仅针对公共仓库）
    api_url = f"https://api.github.com/repos/{repo_owner}/{repo_name}/contents/{path}?ref={branch}"

    # 仅保留必要的 User-Agent，避免被 GitHub 拦截
    headers = {"User-Agent": "Python-Public-Repo-Downloader/1.0"}

    while True:
        response = None
        try:
            response = requests.get(api_url, headers=headers, timeout=30, verify=False)
            response.raise_for_status()  # 触发 HTTP 错误（如 404、403）
            return response.json()
        except requests.exceptions.HTTPError as e:
            if response and response.status_code == 404:
                print(f"❌ 错误: 目录/文件不存在 - {path}")
            else:
                print(f"❌ HTTP 错误: {e}")
        except requests.exceptions.ConnectionError:
            print("❌ 错误: 网络连接失败，请检查网络")
        except requests.exceptions.Timeout:
            print("❌ 错误: 请求超时（GitHub 访问可能较慢）")
        except Exception as e:
            print(f"❌ 未知错误: {e}")


# 下载单个 GitHub 公共仓库文件
def download_github_file(
        file_url: str,
        local_save_path: str
):
    headers = {"User-Agent": "Python-Public-Repo-Downloader/1.0"}

    # 先创建文件所在的目录
    local_dir = os.path.dirname(local_save_path)
    if not os.path.exists(local_dir):
        os.makedirs(local_dir, exist_ok=True)

    while True:
        try:
            response = requests.get(file_url, headers=headers, timeout=30, verify=False)
            response.raise_for_status()

            # 写入文件（二进制模式兼容所有文件类型：文本/图片/二进制文件）
            with open(local_save_path, "wb") as f:
                f.write(response.content)
            print(f"✅ 下载成功: {local_save_path}")
            return
        except Exception as e:
            print(f"❌ 下载失败 {local_save_path}: {str(e)}")


# 递归下载 GitHub 公共仓库指定目录下的所有文件和子目录 保持本地目录结构与仓库一致
def download_github_directory(
        repo_owner: str,
        repo_name: str,
        remote_dir_path: str,
        local_root_dir: str,
        branch: str = "main"
):
    # 获取目录下的所有文件/子目录信息
    contents = get_github_dir_contents(repo_owner, repo_name, remote_dir_path, branch)
    if not contents:
        return

    # 遍历处理每个文件/目录
    for item in contents:
        # 拼接本地保存路径（保持仓库目录结构）
        local_item_path = os.path.join(local_root_dir, item["name"])

        # 如果是文件：直接下载
        if item["type"] == "file":
            download_github_file(item["download_url"], local_item_path)

        # 如果是目录：递归下载子目录
        elif item["type"] == "dir":
            # 递归处理子目录（传入子目录的远程路径）
            download_github_directory(
                repo_owner, repo_name,
                item["path"],  # 子目录的远程完整路径
                local_item_path,  # 子目录的本地路径
                branch
            )


if __name__ == "__main__":
    download_tasks = [
        ("anthropics", "skills", "main", "skills/docx", "docx"),
        ("anthropics", "skills", "main", "skills/pdf", "pdf"),
        ("anthropics", "skills", "main", "skills/pptx", "pptx"),
        ("anthropics", "skills", "main", "skills/xlsx", "xlsx"),
    ]

    for idx, task in enumerate(download_tasks, 1):
        repo_owner, repo_name, branch, remote_dir, local_dir = task
        print(f"\n📌 任务 {idx}: 开始下载 {repo_owner}/{repo_name}({branch})/{remote_dir} -> 本地 {local_dir}")
        download_github_directory(repo_owner, repo_name, remote_dir, local_dir, branch)

    print("\n🎉 所有下载任务执行完成！")

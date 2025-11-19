import os
import subprocess
from pathlib import Path

def is_git_repo(path):
    return (Path(path) / '.git').is_dir()

def get_git_remote_url(repo_path):
    try:
        result = subprocess.run(
            ['git', '-C', repo_path, 'remote', 'get-url', 'origin'],
            capture_output=True, text=True, check=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError:
        return None

def option_one_scan_and_save(folder_path):
    folder = Path(folder_path).expanduser().resolve()
    if not folder.is_dir():
        print("Invalid folder path.")
        return
    
    repo_urls = []
    for root, dirs, files in os.walk(folder):
        if '.git' in dirs:
            repo_path = Path(root)
            url = get_git_remote_url(repo_path)
            if url:
                repo_urls.append(url)

    output_file = folder / "github_repos.txt"
    with open(output_file, "w") as f:
        for url in repo_urls:
            f.write(url + "\n")
    
    print(f"Found and saved {len(repo_urls)} GitHub repos to {output_file}")

def option_two_clone_from_file(file_path, target_folder):
    file = Path(file_path).expanduser().resolve()
    target = Path(target_folder).expanduser().resolve()
    if not file.is_file():
        print("Invalid file path.")
        return
    if not target.is_dir():
        print("Invalid target folder.")
        return

    with open(file, "r") as f:
        urls = [line.strip() for line in f if line.strip()]

    for url in urls:
        try:
            print(f"Cloning {url}...")
            subprocess.run(['git', 'clone', url], cwd=target, check=True)
        except subprocess.CalledProcessError:
            print(f"Failed to clone: {url}")

    print(f"Cloned {len(urls)} repositories into {target}")

def main():
    print("Enter 1 to scan folder for GitHub repos")
    print("Enter 2 to clone repos from a file")
    choice = input("Your choice (1/2): ").strip()

    if choice == '1':
        folder_path = input("Enter folder path to scan: ").strip()
        option_one_scan_and_save(folder_path)
    elif choice == '2':
        file_path = input("Enter path to file with GitHub repo URLs: ").strip()
        target_folder = input("Enter folder path to clone repos into: ").strip()
        option_two_clone_from_file(file_path, target_folder)
    else:
        print("Invalid choice. Please enter 1 or 2.")

if __name__ == "__main__":
    main()


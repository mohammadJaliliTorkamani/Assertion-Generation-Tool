## This script will take the original CSV file (got from SourceGraph) and then
## will discard those whose build tool is not supported, or not compilable.
## Finally it will save the new csv file (clean CSV) as a new CSV file, as well as
## the reason for accepting or discarding each repo.
import csv
import os
import shutil
import subprocess
import sys
import tempfile


def detect_build_tool(repo_path):
    if os.path.isfile(os.path.join(repo_path, "pom.xml")):
        return "maven"
    elif os.path.isfile(os.path.join(repo_path, "gradlew")):
        return "gradle"
    else:
        return "unsupported"


def compile_repo(repo_path, build_tool):
    timeout_seconds = 360  # 6 minutes
    env = os.environ.copy()
    env["MAVEN_OPTS"] = "-Xmx16g"

    if build_tool == "maven":
        cmd = [
            "mvn", "clean", "install",
            "-Dmaven.javadoc.skip=true",
            "-Dcheckstyle.skip=true",
            "-Dspotbugs.skip=true",
            "-Dspotless.check.skip=true"
        ]
    elif build_tool == "gradle":
        gradlew = os.path.join(repo_path, "gradlew")
        subprocess.run(["chmod", "+x", gradlew], cwd=repo_path)
        cmd = ["./gradlew", "clean", "build", "-x", "javadoc"]
    else:
        return "unsupported", ""

    try:
        subprocess.run(cmd, cwd=repo_path, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                       timeout=timeout_seconds)
        return "success", ""
    except subprocess.TimeoutExpired:
        return "timeout", "compilation exceeded 5 minutes"

    except subprocess.CalledProcessError as e:
        # Check if process was killed (SIGKILL = 9 → exit code 137)
        if e.returncode == 137:
            return "killed", "Process killed (likely OOM or SIGKILL)"
        else:
            result = subprocess.run(
                cmd,
                cwd=repo_path,
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                timeout=timeout_seconds,
                env=env
            )
            error_tail = result.stderr[-300:] if result.stderr else "no error message"
            return "failure", error_tail
    except Exception as e:
        return "failure", f"unexpected error during error capture: {str(e)[-300:]}"


def process_repos(csv_file, output_csv):
    seen_repos = set()

    with open(csv_file, newline='', encoding='utf-8') as infile, \
            open(output_csv, "w", newline='', encoding='utf-8') as outfile:

        reader = csv.reader(infile)
        writer = csv.writer(outfile)

        all_rows = list(reader)
        total = len(all_rows) - 1  # exclude header

        writer.writerow(["Repository URL", "Build Tool", "Compile Status", "Error Message (last 100 chars)"])

        success_csv = output_csv.replace(".csv", "_successful.csv")
        success_file = open(success_csv, "w", newline='', encoding='utf-8')
        success_writer = csv.writer(success_file)
        success_writer.writerow(all_rows[0])  # Write original header

        print(f"Total repositories to process: {total}")
        sys.stdout.flush()

        for idx, row in enumerate(all_rows[1:], 1):  # skip header
            if len(row) < 2:
                continue

            repo_url = row[1].strip()
            if not repo_url.startswith("http"):
                repo_url = "https://" + repo_url

            if repo_url in seen_repos:
                print(f"[{idx}/{total}] Skipping redundant repository: {repo_url}")
                sys.stdout.flush()
                continue
            seen_repos.add(repo_url)

            repo_name = repo_url.split("/")[-1].replace(".git", "")
            print(f"[{idx}/{total}] Cloning {repo_name}...")
            sys.stdout.flush()

            tmpdir = tempfile.mkdtemp()
            repo_path = os.path.join(tmpdir, repo_name)

            try:
                print("repo:", repo_url)
                subprocess.run(["git", "clone", repo_url, repo_path], check=True, stdout=subprocess.DEVNULL,
                               stderr=subprocess.DEVNULL)
                build_tool = detect_build_tool(repo_path)
                print(f"[{idx}/{total}] Detected build tool: {build_tool}")
                sys.stdout.flush()

                if build_tool == "unsupported":
                    writer.writerow([repo_url, "unsupported build tool", "unsupported", ""])
                else:
                    status, error_msg = compile_repo(repo_path, build_tool)
                    print(f"[{idx}/{total}] Compilation status: {status}")
                    writer.writerow([repo_url, build_tool, status, error_msg])
                    if status == "success":
                        success_writer.writerow(row)

            except:
                print(f"[{idx}/{total}] Clone failed")
                writer.writerow([repo_url, "N/A", "clone failed", ""])

            finally:
                shutil.rmtree(tmpdir, ignore_errors=True)
                print(f"[{idx}/{total}] Removed directory {tmpdir}")
                progress_percent = (idx / total) * 100
                print(f"Progress: {progress_percent:.2f}% complete\n")
                sys.stdout.flush()

        success_file.close()


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python compile_temp_batch.py <csv_file> <output_csv>")
        sys.exit(1)

    csv_file = sys.argv[1]
    output_csv = sys.argv[2]
    process_repos(csv_file, output_csv)

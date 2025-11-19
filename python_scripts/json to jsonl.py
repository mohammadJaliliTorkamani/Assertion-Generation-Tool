import json
import sys
import tiktoken

def estimate_token_count_from_messages(messages, model="gpt-4o"):
    enc = tiktoken.encoding_for_model(model)
    full_text = " ".join(msg["content"] for msg in messages)
    return len(enc.encode(full_text.strip()))

def convert_json_to_jsonl(json_file_path, jsonl_file_path, model="gpt-4o", token_limit=8192):
    with open(json_file_path, 'r', encoding='utf-8') as json_file:
        data = json.load(json_file)

    over_limit_count = 0
    over_limit_indices = []
    written_count = 0

    with open(jsonl_file_path, 'w', encoding='utf-8') as jsonl_file:
        for idx, item in enumerate(data):
            messages = []

            if 'system' in item:
                messages.append({"role": "system", "content": item['system']})

            user_messages = item.get('user', [])
            assistant_messages = item.get('assistant', [])

            if isinstance(user_messages, str):
                user_messages = [user_messages]
            if isinstance(assistant_messages, str):
                assistant_messages = [assistant_messages]

            for u, a in zip(user_messages, assistant_messages):
                messages.append({"role": "user", "content": u})
                messages.append({"role": "assistant", "content": a})

            total_tokens = estimate_token_count_from_messages(messages, model)

            if total_tokens > token_limit:
                over_limit_count += 1
                over_limit_indices.append(idx)
                continue  # Skip writing this record

            jsonl_file.write(json.dumps({"messages": messages}, ensure_ascii=False) + '\n')
            written_count += 1

    print(f"Total input records: {len(data)}")
    print(f"Written to {jsonl_file_path}: {written_count} records")
    print(f"Skipped {over_limit_count} records exceeding {token_limit} tokens")
    if over_limit_indices:
        print("Indices of skipped records:", over_limit_indices)

# Usage:
# python json_to_jsonl.py input.json output.jsonl
if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python json_to_jsonl.py input.json output.jsonl")
    else:
        convert_json_to_jsonl(sys.argv[1], sys.argv[2])

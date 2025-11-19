import argparse
import os

import tiktoken

system_path = os.getenv("SYSTEM_PATH", None)
system_input = None


def count_tokens(encoder):
    global system_input
    enc = tiktoken.get_encoding(encoder)
    num_tokens = len(enc.encode(system_input))
    return num_tokens


def main():
    if not system_path:
        return

    global system_input

    parser = argparse.ArgumentParser(description="Count the number of tokens in a given input string.")
    parser.add_argument("--encoder", default="cl100k_base", help="The tokenizer encoder.")

    args = parser.parse_args()

    if os.path.isfile(system_path):
        system_input = str(open(system_path, "r", encoding="utf-8").read())

    encoder = args.encoder
    token_count = count_tokens(encoder)

    print(token_count)


if __name__ == "__main__":
    main()

import json
import os
import sys
import time

from openai import OpenAI


def get_embedding(client, list_, model="text-embedding-3-small"):
    return client.embeddings.create(input=list_, model=model)


def extract_message(response):
    if response is None or len(response.choices) == 0:
        return None
    choice = response.choices[0]
    if choice.message is None or choice.message.content is None:
        return None
    return choice.message.content


def extract_embedding(response, is_batch):
    if response is None or len(response.data) == 0:
        return None
    if is_batch:
        total_embeddings = [item.embedding for item in response.data if item.embedding]
        return total_embeddings
    data = response.data[0]
    return data.embedding if data.embedding else None


def main():
    system_path = os.getenv("SYSTEM_PATH")
    user_path = os.getenv("USER_PATH")
    assistant_path = os.getenv("ASSISTANT_PATH")

    temperature = float(os.getenv("TEMPERATURE", 1.0))
    top_p = float(os.getenv("TOP_P", 1.0))
    api_key = os.getenv("API_KEY")
    model = os.getenv("MODEL")
    is_embedding = os.getenv("ASSERTRON_TYPE", "generate") == "embedding"
    is_batch = os.getenv("IS_BATCH", "false").lower() == "true"
    trial_number = int(os.getenv("TRIAL_NUMBER", 3))
    frequency_penalty = float(os.getenv("FREQUENCY_PENALTY", -1))
    presence_penalty = float(os.getenv("PRESENCE_PENALTY", -1))
    max_length = int(os.getenv("MAX_LENGTH", 4096))

    client = OpenAI(api_key=api_key)

    user_prompt = None
    system_prompt = None
    assistant_prompt = None
    if user_path and os.path.isfile(user_path):
        user_prompt = str(open(user_path, "r", encoding="utf-8").read())
    if system_path and os.path.isfile(system_path):
        system_prompt = str(open(system_path, "r", encoding="utf-8").read())
    if assistant_path and os.path.isfile(assistant_path):
        assistant_prompt = str(open(assistant_path, "r", encoding="utf-8").read())

    if not is_embedding:
        while trial_number > 0:
            messages = []
            if system_prompt:
                if (json.loads(system_prompt) is not None):
                    messages.append({'role': 'system', 'content': json.loads(system_prompt)['message']})

            if assistant_prompt is None:
                if user_prompt:
                    for user in json.loads(json.loads(user_prompt)['message']):
                        messages.append({'role': 'user', 'content': user})
            else:
                for (u, a) in zip(json.loads(json.loads(user_prompt)['message']),
                                  json.loads(json.loads(assistant_prompt)['message'])):
                    messages.append({'role': 'user', 'content': u})
                    messages.append({'role': 'assistant', 'content': a})

                users = json.loads(json.loads(user_prompt)['message'])
                messages.append({'role': 'user', 'content': users[len(users) - 1]})

            arguments = {'model': model,
                         'temperature': temperature,
                         'top_p': top_p,
                         'stream': False,
                         'presence_penalty': presence_penalty,
                         'frequency_penalty': frequency_penalty,
                         'messages': messages
                         }

            try:
                response = client.chat.completions.create(**arguments)

                message = extract_message(response)
                if (message is not None) and ("Traceback (most recent call last)" not in message) and (
                        "Bad gateway" not in message) and ("Traceback" not in message):
                    print(message)  # was print(message.encode('utf-8'))
                    sys.stdout.flush()
                    break
                else:
                    trial_number -= 1
            except Exception as e:
                print(e)
                return
                trial_number -= 1
            time.sleep(5)

        if trial_number == 0:
            print("Exceeded maximum trials. Could not generate response!")
            sys.stdout.flush()

    else:
        while trial_number > 0:
            system_input = json.loads(json.loads(system_prompt)['message']) if is_batch else json.loads(system_prompt)[
                'message']

            try:
                response = get_embedding(client, system_input, model)
                embeddings = extract_embedding(response, is_batch)
                if embeddings and "Traceback" not in embeddings and "Bad gateway" not in embeddings:
                    print(json.dumps(embeddings))
                    sys.stdout.flush()
                    break
                else:
                    trial_number -= 1
            except Exception as e:
                print(e)
                trial_number -= 1
            time.sleep(5)

        if trial_number == 0:
            print("Exceeded maximum trials. Could not generate response!")
            sys.stdout.flush()


if __name__ == "__main__":
    main()

import json
import torch
import transformers
import uvicorn
from fastapi import FastAPI, Request

app = FastAPI()

import os

# Remove the file
tmp_file_name = 'ASSERTION_TMP_FILE.txt'
if os.path.exists(tmp_file_name):
    os.remove(tmp_file_name)
    print(f"File '{tmp_file_name}' has been removed.")

# Load the model once
model_id = "meta-llama/Meta-Llama-3.1-8B-Instruct"
pipeline = transformers.pipeline(
    "text-generation",
    model=model_id,
    model_kwargs={"torch_dtype": torch.bfloat16},
    device_map="cuda",  # Assumes CUDA is available; adjust if using CPU
)

embedding_model = transformers.AutoModel.from_pretrained(model_id, torch_dtype=torch.bfloat16)
embedding_tokenizer = transformers.AutoTokenizer.from_pretrained(model_id)
embedding_tokenizer.pad_token = embedding_tokenizer.eos_token

with open(tmp_file_name, 'w'):
    pass


@app.post("/generate")
async def generate_text(request: Request):
    # Read paths and parameters from environment variables
    user_path = os.getenv("USER_PATH", None)
    system_path = os.getenv("SYSTEM_PATH", None)
    assistant_path = os.getenv("ASSISTANT_PATH", None)
    temperature = float(os.getenv("TEMPERATURE", 1.0))
    top_p = float(os.getenv("TOP_P", 1.0))

    # Load prompts from files if they exist
    user_prompt, system_prompt, assistant_prompt = None, None, None
    if os.path.isfile(user_path):
        with open(user_path, "r", encoding="utf-8") as f:
            user_prompt = json.load(f)
    if os.path.isfile(system_path):
        with open(system_path, "r", encoding="utf-8") as f:
            system_prompt = json.load(f)
    if os.path.isfile(assistant_path):
        with open(assistant_path, "r", encoding="utf-8") as f:
            assistant_prompt = json.load(f)
        # Initialize the messages list
    messages = []

    # Add system message if exists
    if system_prompt and system_prompt.get("message"):
        messages.append({"role": "system", "content": system_prompt["message"]})

    # Add user and assistant messages
    if user_prompt and user_prompt.get("message"):
        user_messages = user_prompt["message"]
        assistant_messages = assistant_prompt["message"] if assistant_prompt and assistant_prompt.get("message") else []

        # Match user and assistant messages
        for i, user_msg in enumerate(user_messages):
            messages.append({"role": "user", "content": user_msg})
            if i < len(assistant_messages):
                messages.append({"role": "assistant", "content": assistant_messages[i]})

        # If there are extra user messages, append them without assistant responses
    #        for i in range(len(assistant_messages), len(user_messages)):
    #            messages.append({"role": "user", "content": user_messages[i]})

    # Generate response with custom temperature and top_p
    outputs = pipeline(
        messages,
        max_new_tokens=256,
        temperature=temperature,
        top_p=top_p
    )

    if len(outputs[0]["generated_text"]) - len(messages) > 0:
        return outputs[0]["generated_text"][-1]["content"]
    else:
        return None


@app.post("/embedding")
async def generate_text(request: Request):
    # Read paths and parameters from environment variables
    system_path = os.getenv("SYSTEM_PATH", None)
    is_batch = os.getenv("IS_BATCH", "true")

    # Load prompts from files if they exist
    if os.path.isfile(system_path):
        with open(system_path, "r", encoding="utf-8") as f:
            system_prompt = json.load(f)

    print(system_prompt)
    if is_batch == "true":
        system_input = json.loads(system_prompt['message'])
    else:
        system_input = system_prompt['message']

    embedding_inputs = embedding_tokenizer(system_input, return_tensors="pt", padding=True, truncation=True)
    # Forward pass through the model to get hidden states (embeddings)
    with torch.no_grad():
        embedding_outputs = embedding_model(**embedding_inputs)
        embedding_last_hidden_state = embedding_outputs.last_hidden_state

    # The embeddings are in the `last_hidden_state` tensor
    embedding_vector = embedding_last_hidden_state.mean(dim=1)  # Mean pooling across token dimension

    return str(embedding_vector.to(torch.float32).numpy())


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=5000)

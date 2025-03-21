# LLM Service

docker build -t llm-service -f Dockerfile .

docker run -p 8080:8080 \
  -v "$(pwd)/models:/models" \
  llm-service \
  -m /models/Mistral-7B-Instruct-v0.3.Q8_0.gguf \
  -c 512 \
  --host 0.0.0.0 \
  --port 8080

curl --request POST \
    --url http://localhost:8080/answer \
    --header "Content-Type: application/json" \
    --data '{"prompt": "Building a website can be done in 10 simple steps:","n_predict": 128,"callback": "http://127.0.0.1:3000/llm/receive"}'

## Threading code in server

```
if (params.n_threads_http < 1) {
    params.n_threads_http = std::max(params.n_parallel + 2, (int32_t) std::thread::hardware_concurrency() - 1);
}

svr->new_task_queue = [&params] {
    return new httplib::ThreadPool(params.n_threads_http);
};
```

## References

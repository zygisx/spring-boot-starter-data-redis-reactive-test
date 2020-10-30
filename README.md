# Performance results

```
wrk -t 50 -c 200 -d60s 'http://localhost:8080/redis-test'
Running 1m test @ http://localhost:8080/redis-test
  50 threads and 200 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    82.16ms   78.06ms   1.06s    97.60%
    Req/Sec    55.05     16.47   101.00     68.11%
  161562 requests in 1.00m, 12.63MB read
  Socket errors: connect 0, read 133, write 0, timeout 0
Requests/sec:   2688.18
Transfer/sec:    215.26KB
```

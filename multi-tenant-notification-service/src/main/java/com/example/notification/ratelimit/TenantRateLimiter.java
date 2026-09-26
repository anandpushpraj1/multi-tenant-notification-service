package com.example.notification.ratelimit;
import org.springframework.stereotype.Component; import java.time.*; import java.util.*; import java.util.concurrent.*;
@Component
public class TenantRateLimiter{
     private static class Bucket {
         double tokens;
         long lastNanos;
         Bucket(int cap) {
             tokens=cap;
             lastNanos=System.nanoTime();
         }
     }

     private final ConcurrentHashMap<UUID,Bucket> buckets=new ConcurrentHashMap<>();
     public boolean tryAcquire(UUID tenant,int rate) {
         if(rate<=0)
             return false;
         Bucket b=buckets.computeIfAbsent(tenant,k->new Bucket(rate));
         synchronized(b) {
             long now=System.nanoTime();
             double elapsed=(now-b.lastNanos)/1_000_000_000d;
             b.tokens=Math.min(rate,b.tokens+elapsed*rate);
             b.lastNanos=now;if(b.tokens>=1) {
                 b.tokens-=1;
                 return true;
             }

             return false;
         }
     }

     public void clear(UUID tenant) {
         buckets.remove(tenant);
     }
}

package com.example.notification.tenant;
import com.example.notification.common.ApiException; import org.springframework.http.HttpStatus; import org.springframework.stereotype.Service; import java.util.*;
@Service
public class TenantService {
    private final TenantRepository repo;
    public TenantService(TenantRepository repo){this.repo=repo;} public Tenant create(String name,int rate){if(repo.findByName(name).isPresent())throw new ApiException(HttpStatus.CONFLICT,"Tenant already exists");return repo.save(new Tenant(name,rate));} public List<Tenant> list(){return repo.findAll();} public Tenant get(UUID id){return repo.findById(id).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Tenant not found"));} public Tenant update(UUID id,int rate,TenantStatus status){Tenant t=get(id);t.setRateLimitPerSecond(rate);t.setStatus(status);return repo.save(t);}}

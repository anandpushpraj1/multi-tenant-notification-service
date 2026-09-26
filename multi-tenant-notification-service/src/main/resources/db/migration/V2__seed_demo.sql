insert into tenants(id,name,status,rate_limit_per_second,created_at,updated_at) values ('00000000-0000-0000-0000-000000000001','demo-tenant','ACTIVE',10,current_timestamp,current_timestamp) on conflict do nothing;
insert into app_users(id,external_id,tenant_id,role) values ('00000000-0000-0000-0000-000000000011','platform-admin',null,'PLATFORM_ADMIN') on conflict do nothing;
insert into app_users(id,external_id,tenant_id,role) values ('00000000-0000-0000-0000-000000000012','tenant-admin','00000000-0000-0000-0000-000000000001','TENANT_ADMIN') on conflict do nothing;

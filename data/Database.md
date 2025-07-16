# Working with AlloyDB in the App

# Install the AlloyDB proxy
[Connect using the AlloyDB Auth Proxy](https://cloud.google.com/alloydb/docs/auth-proxy/connect#psql)
[Main Postgres PSQL commands](https://www.geeksforgeeks.org/postgresql-psql-commands/)

```shell
 curl -o alloydb-auth-proxy https://storage.googleapis.com/alloydb-auth-proxy/v1.12.2/alloydb-auth-proxy.darwin.arm64
 
 chmod +x alloydb-auth-proxy
 
 gcloud alloydb instances list
 
 # Project = genai-playground24
 # Location = us-central1
 
 # Start with
 ./alloydb-auth-proxy projects/<project_name>/locations/us-central1/clusters/alloydb-aip-01/instances/alloydb-aip-01-pr --public-ip

#--- connectivity checks ---
# Start psql for connectivity check
psql -h 127.0.0.1 -p 5432 -U postgres -d library -W

select * from public.continents;
select * from public.capitals;

select capital, country

# describe tables
\d public.capitals
\d public.continents
```
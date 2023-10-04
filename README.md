# interop-backfill

### Rest Endpoints

Broadly, there are three sets of endpoints; ones for backfills as a whole, ones for discovery queue and ones for the backfill queue.
The specifications are described at [the openapi specification file](/interop-backfill-server/backfill-api.yaml),
<!-- 
If we get a SwaggerUI up like we do for mock-ehr and validation this should probably be removed, INT-2267
-->

|  Category | Operation          | Endpoints|
|-----------|--------------------| ---|
| Backfill  | create (POST)      | /backfill|
| Backfill  | read (GET)         |  /backfill, /backfill/{id}|
| Backfill  | update             | None|
| Backfill  | delete   (DELETE)  | /backfill/{id}|
| Discovery | create             | None, handled automatically|
| Discovery | read      (GET)    |  /discovery-queue, /discovery-queue/{id}|
| Discovery | update     (PATCH) |  /discovery-queue/{id}|
| Discovery | delete    (DELETE) |  /discovery-queue/{id}|
| Queue     | create    (POST)   |  /queue/backfill/{backfillId}|
| Queue     | read      (GET)    |  /queue, /queue/{id}, /queue/backfill/{backfillId}|
| Queue     | update    (PATCH)  |  /queue/{id}|
| Queue     | delete    (DELETE) |  /queue/{id}|
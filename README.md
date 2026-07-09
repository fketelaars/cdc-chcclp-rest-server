# CDC CHCCLP REST service

Interface into the CHCCLP command line interface using REST APIs. Later this will also be made available as an MCP server.

# Building the CDC CHCCLP REST service image

## Mandatory build arguments
None,

## Optional build arguments
None.

## Build steps for docker or podman
The below steps will build the container image and automatically install CDC Access Server.

### Download CDC Access Server JAR file

For the REST service to work, you need a working copy of the IDR CDC Access Server. The REST service uses embedded CHCCLP which requires the Access Server jar files. You must copy all jar files from the Access Server into the `lib` directory of this repo.

### Create the container image
```
docker build -t cdc-chcclp-rest:latest .
```

or using `podman`:
```
podman build -t cdc-chcclp-rest:latest .
```

# Running CDC CHCCLP REST Server

## Mandatory run arguments
None.

## Optional run arguments
`CDC_ACCESS_SERVER_HOST` - CDC Access Server host name
`CDC_ACCESS_SERVER_PORT` - CDC Access Server port

### Running CDC CHCCLP REST server without configuring the Access Server
```
docker run -d \
    -p 8080:8080 \
    cdc-chcclp-rest:latest
```

or using `podman`:
```
podman run -d \
    -p 8080:8080 \
    cdc-chcclp-rest:latest
```

### Running CDC CHCCLP REST server and configuring the Access Server

Set environment variables:
```
export CDC_ACCESS_SERVER_HOST=your-access-server-host
export CDC_ACCESS_SERVER_PORT=your-access-server-port
```

```
docker run -d \
    -p 8080:8080 \
    -e CDC_ACCESS_SERVER_HOST=${CDC_ACCESS_SERVER_HOST} \
    -e CDC_ACCESS_SERVER_PORT=${CDC_ACCESS_SERVER_PORT} \
    cdc-chcclp-rest:latest
```

or using `podman`:
```
podman run -d \
    -p 8080:8080 \
    -e CDC_ACCESS_SERVER_HOST=${CDC_ACCESS_SERVER_HOST} \
    -e CDC_ACCESS_SERVER_PORT=${CDC_ACCESS_SERVER_PORT} \
    cdc-chcclp-rest:latest
```

## REST API

All endpoints are prefixed with `/api/v1`. Every endpoint except `POST /api/v1/connect` requires an `Authorization: Bearer <token>` header.

### Endpoints

| Method   | Path                     | Auth required | Description                                      |
|----------|--------------------------|:-------------:|--------------------------------------------------|
| `POST`   | `/api/v1/connect`        | No            | Open a session and connect to the Access Server. Returns a Bearer token. |
| `GET`    | `/api/v1/sessions`       | Yes           | List all active sessions.                        |
| `GET`    | `/api/v1/sessions/{id}`  | Yes           | Get metadata for a specific session.             |
| `POST`   | `/api/v1/execute`        | Yes           | Execute a CHCCLP command (session resolved from the token). |
| `DELETE` | `/api/v1/sessions/{id}`  | Yes           | Disconnect and close the session.                |

---

### 1. Connect — `POST /api/v1/connect`

`accessServerHost` and `accessServerPort` are optional when they are set in the `.env` file.

**Request**
```json
{
  "accessServerHost": "my-access-server",
  "accessServerPort": 11001,
  "accessServerUser": "admin",
  "accessServerPassword": "secret"
}
```

**Response** `201 Created`
```json
{
  "token" : "eyJzZXNzaW9uSWQi...",
  "createdAt" : "2026-06-29T13:42:24.481634Z"
}
```

```bash
export TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/connect \
  -H "Content-Type: application/json" \
  -d '{
    "accessServerHost": "my-access-server",
    "accessServerPort": 10101,
    "accessServerUser": "admin",
    "accessServerPassword": "your-secret"
  }' | jq -r .token")
```

or,

```
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/connect \
  -H "Content-Type: application/json" \
  -d '{
    "accessServerUser": "admin",
    "accessServerPassword": "your-secret"
  }' | jq -r .token)
```

### 2. Execute a CHCCLP command — `POST /api/v1/execute`

The session is resolved from the Bearer token. A trailing `;` is added automatically if omitted.

**Request**
```json
{ "command": "list datastores" }
```

**Response** `200 OK`
```json
{
  "result" : "...",
  "executedAt" : "2026-06-29T13:43:01.123456Z"
}
```

```bash
# Connect a datastore and list subscriptions
curl -s -X POST http://localhost:8080/api/v1/execute \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"command": "connect datastore name Db2LUW context source"}'
```

---

### 3. Disconnect — `DELETE /api/v1/sessions/{id}`

```bash
curl -s -X DELETE http://localhost:8080/api/v1/sessions/<sessionId> \
  -H "Authorization: Bearer $TOKEN"
```

**Response** `204 No Content`

---

### 4. List sessions — `GET /api/v1/sessions`

```bash
curl -s http://localhost:8080/api/v1/sessions \
  -H "Authorization: Bearer $TOKEN"
```

---

### Error responses

All errors return a JSON body:
```json
{
  "error" : "bad_request",
  "detail" : "accessServerUser is required"
}
```

| HTTP status | `error` value      | Meaning                                         |
|-------------|--------------------|-------------------------------------------------|
| `400`       | `bad_request`      | A required field is missing or invalid.         |
| `401`       | `unauthorized`     | `Authorization` header is missing or invalid.   |
| `403`       | `forbidden`        | Token does not match the requested session.     |
| `404`       | `session_not_found`| No session exists with the given ID.            |
| `422`       | `command_failed`   | The CHCCLP command was rejected by the engine.  |
| `502`       | `connect_failed`   | The Access Server connection attempt failed.    |

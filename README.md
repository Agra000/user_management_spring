# User Management Service (this project name is test_spring because i forgot to create a new one)

A Spring Boot service for login and user management.

What it does:

- **Login** with email and password. You get a **JWT token** back.
- Login data is **cached in Redis** for a while, so the next login does not need to hit the database.
- Only an **ADMIN** can register new users.
- A logged-in user can **change their own password**.
- Every time a user is registered, an event is sent to **RabbitMQ**. The [notification service](https://github.com/Agra000/notif_service_spring) picks it up.

---

## What you need

- **IntelliJ IDEA**
- **JDK 21**
- **PostgreSQL** (installed on your computer, port `5432`)
- **Docker Desktop**
- **Postman**

---

## How to run (step by step)

### Step 1 — Open the project

1. Open IntelliJ → **File → Open...** → choose this project folder (the one with `pom.xml`).
2. Wait until the loading bar at the bottom is finished. IntelliJ downloads all libraries.
3. Go to **File → Project Structure → Project** and make sure **SDK = 21**.
4. If IntelliJ shows *"Lombok requires enabled annotation processing"*, click **Enable**.

### Step 2 — Create the `resources` folder and `application.yml`

The config file is **not** in the repo, so i'll give it to you.

1. In the project panel, right-click `src/main` → **New → Directory** → type `resources`.
2. Paste the file that i give to the resource folder

#### What you need to change in `application.yml`

| Line | What to put |
|---|---|
| `url` | Change `test_spring` at the end if your database has a different name. |
| `username` | Your PostgreSQL username (usually `postgres`). |
| `password` | **Your PostgreSQL password.** |

You can leave the Redis and RabbitMQ settings as they are. They match `docker-compose.yml`.

> Use **spaces**, not Tab, in the YAML file.

### Step 3 — Run Docker Compose

This starts **Redis** and **RabbitMQ**.

1. Open **Docker Desktop** and wait until it says *Engine running*.
2. In IntelliJ, open the **Terminal** (`Alt + F12`) and run:

```bash
docker compose up -d
```

3. Check that everything is running:

```bash
docker ps
```

You should see `redis`, `rabbitmq` and `redisinsight` with status `Up`.

You only need to do this once. The containers start again automatically every time Docker Desktop starts.

### Step 4 — Prepare the database

Open pgAdmin or DBeaver, create a database called `users_db`, then run this SQL inside it:

```sql
CREATE TABLE public.roles (
    id               INTEGER PRIMARY KEY,
    role_name        VARCHAR(25) NOT NULL,
    role_description VARCHAR(255),
    created_at       TIMESTAMP DEFAULT NOW()
);

INSERT INTO public.roles (id, role_name, role_description, created_at) VALUES
(1, 'ADMIN', 'User Manager', NOW()),
(2, 'USER', 'Public User', NOW());

CREATE TABLE public.users (
    id         UUID PRIMARY KEY,
    full_name  VARCHAR(150),
    user_name  VARCHAR(100) NOT NULL,
    password   TEXT NOT NULL,
    email      VARCHAR(150) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    role_id    INTEGER NOT NULL REFERENCES public.roles(id)
);

INSERT INTO public.users (id, full_name, user_name, password, email, role_id, created_at)
VALUES (
    '9b4a9df6-d03f-40b1-82e5-1193d6d4a1e9',
    'PCA Admin User',
    'pca_admin_user',
    'e99a18c428cb38d5f260853678922e03', -- MD5 of 'abc123'
    'pca_admin_user@yopmail.com',
    1,
    NOW()
);
```

The first admin can log in with **`pca_admin_user@yopmail.com`** / **`abc123`**.

### Step 5 — Clean and build (optional, like `dotnet clean` / `dotnet build`)

Open the **Maven** panel on the right side of IntelliJ → **test_spring → Lifecycle**:

1. Double-click **clean**
2. Double-click **install**

You don't have to do this every time. It is useful for the first run, or when something acts strange after you change code or config.

### Step 6 — Run

1. Open `src/main/java/com/example/test_spring/TestSpringApplication.java`.
2. Click the green **▶** button next to `public class TestSpringApplication`.

The service is ready when the console at the bottom shows:

```
Tomcat started on port 8080 (http)
```

Next time, just press **▶** at the top-right of IntelliJ.
To stop it, press the red **■** button.

---

## Endpoints

Base URL: `http://localhost:8080`

| Method | URL | Who can use it |
|---|---|---|
| `POST` | `/api/auth/login` | Anyone |
| `POST` | `/api/auth/regist-user` | ADMIN only (needs token) |
| `PUT` | `/api/auth/change-password` | Any logged-in user (needs token) |

### How to use the token in Postman

1. Call the login endpoint and copy the `token` value from the response.
2. In your next request, open the **Authorization** tab → choose **Bearer Token** → paste the token.

---

## Test cases

For every request with a body: **Body → raw → JSON**.

### Login

**TC-01 — Login success**

`POST /api/auth/login`

```json
{ "email": "pca_admin_user@yopmail.com", "password": "abc123" }
```

Expected: `"status": true` and a `token` in the response.

```json
{
  "status": true,
  "message": null,
  "token": "eyJhbGciOi...",
  "type": "Bearer",
  "ttl": ...
}
```

**TC-02 — Wrong password**

```json
{ "email": "pca_admin_user@yopmail.com", "password": "wrongpassword" }
```

Expected: `"status": false`, `"message": "Invalid Credentials"`.

**TC-03 — Email not registered**

```json
{ "email": "nobody@example.com", "password": "admin12345" }
```

Expected: `"status": false`, `"message": "Email not found"`.

### Register user

**TC-04 — Register without a token**

`POST /api/auth/regist-user` with **no** Authorization.

Expected: **401 Unauthorized**.

**TC-05 — Register success (as ADMIN)**

`POST /api/auth/regist-user` with the **admin** Bearer token.

```json
{
  "fullName": "Budi Santoso",
  "userName": "budi",
  "password": "budi12345",
  "email": "budi@example.com",
  "roleId": 2
}
```

Expected: `Added user successfully budi@example.com`.
The new user can now log in. An event is also sent to RabbitMQ (see below).

**TC-06 — Email already registered**

Send TC-05 again with the same email.

Expected: `Email already registered`.

**TC-07 — Role does not exist**

Same as TC-05, but with a new email and `"roleId": 99`.

Expected: `Role not found`.

**TC-08 — Register as a normal USER**

Log in as `budi@example.com` / `budi12345`, then call `regist-user` with Budi's token.

Expected: **403 Forbidden** (only ADMIN can register users).

### Change password

**TC-09 — Change password success**

Log in as Budi first, then:

`PUT /api/auth/change-password` with Budi's Bearer token.

```json
{ "oldPassword": "budi12345", "newPassword": "budiBaru123" }
```

Expected: `Password changed`.

**TC-10 — Login with the old password after changing it**

`POST /api/auth/login` with `budi@example.com` / `budi12345`.

Expected: `"status": false`, `"message": "Invalid Credentials"`.
Logging in with `budiBaru123` works.

**TC-11 — Wrong old password**

```json
{ "oldPassword": "wrong", "newPassword": "something123" }
```

Expected: `Invalid old password`.

**TC-12 — New password same as old password**

```json
{ "oldPassword": "budiBaru123", "newPassword": "budiBaru123" }
```

Expected: `New password can't be same like the old one`.

### Token

**TC-13 — Expired token**

Expected: **401 Unauthorized**. Log in again to get a new token.

---

## Check Redis and RabbitMQ

### Redis (login cache)

After a successful login, run this in the IntelliJ Terminal:

```bash
docker exec -it redis redis-cli
```

```
KEYS *
```

You should see the cached login email data.

You can also open **http://localhost:5540** (RedisInsight) → *Add Redis database* → host `redis`, port `6379`.

To clear the cache: `docker exec redis redis-cli FLUSHDB`

### RabbitMQ (events)

Open **http://localhost:15672** and log in with `guest` / `guest`.

- **Exchanges → `auth.exchange`** should exist.
- When you register a user (TC-05), a message is sent with routing key `auth.user.registered`.
- If the notification service is running, the message is picked up right away, so the queue looks empty. That is normal.

---

## Common problems

| Problem | What to do |
|---|---|
| Red imports / `Cannot resolve symbol` | Open `pom.xml` and click the small **"m"** reload icon at the top-right. |
| `Could not resolve placeholder 'app...'` | `application.yml` is missing, in the wrong folder, or a key is misspelled. It must be at `src/main/resources/application.yml`. |
| `Unable to connect to Redis` / `Connection refused` | Docker Desktop is not running. Open it, then run `docker compose up -d`. |
| `password authentication failed` | Wrong PostgreSQL username or password in `application.yml`. |
| `database "test_spring" does not exist` | Create the database first (Step 4). |
| Login fails right after start | `app.jwt.secret` is shorter than 32 characters. |
| `Port 8080 was already in use` | The app is already running. Press **■** first. |

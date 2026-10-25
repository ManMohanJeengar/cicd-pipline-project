# Experiment 9 - CI/CD Pipeline with Testing & Deployment

One Spring Boot (Java 17) project that covers

* **9.1 Continuous Integration** - unit, API (MockMvc), contract (Pact) and Testcontainers tests, run by GitHub Actions
* **9.2 Continuous Deployment** - Docker image, GitHub Actions CD workflow, canary release (90 % / 10 %)

```
Developer push -> CI Pipeline -> tests pass -> build Docker image -> push to Docker Hub
               -> canary (10 %) -> monitor -> approve -> promote to 100 %
```

## Project layout

| Path | Purpose |
|------|---------|
| `src/main/.../post` | Posts REST API (`GET/POST /posts`, `GET /posts/{id}`), JPA + PostgreSQL |
| `src/main/.../calculator` | Small class for the plain unit-test example |
| `src/main/.../info` | `GET /info` -> `{"track":"stable|canary","version":...}` used to measure the canary split |
| `src/main/.../client` | `PostClient` - the *consumer* of the API (what the frontend would do) |
| `src/test/.../calculator`, `post/PostServiceTest` | **Assignment 1** - JUnit 5 + Mockito unit tests |
| `src/test/.../post/PostControllerTest` | **Assignment 2** - MockMvc API tests |
| `src/test/.../post/PostRepositoryTest`, `PostApiIntegrationTest` | **Assignment 3** - Testcontainers (real PostgreSQL) |
| `src/test/.../contract` | Pact contract tests: consumer writes `target/pacts`, provider verifies it |
| `Dockerfile` | **Assignment 4** - multi-stage build, runs as non-root |
| `docker-compose.yml` | Local app + database |
| `deploy/` | **Assignment 5** - canary stack (nginx weights 9:1), monitor script |
| `.github/workflows/ci.yml` | CI pipeline (Experiment 9.1) |
| `.github/workflows/cd.yml` | CD pipeline (Experiment 9.2) |

## Run locally

Prerequisites: JDK 17+, Maven 3.9+, Docker, and Windows PowerShell 5.1 or later.

```powershell
# Run all tests (Docker must be running for Testcontainers).
mvn clean verify

# Run one test class.
mvn test "-Dtest=CalculatorTest"

# Start the app and PostgreSQL in the background at http://localhost:8080.
docker compose up --build -d

# Check the app information.
Invoke-RestMethod -Uri "http://localhost:8080/info"

# Create a post.
$post = @{
    title = "Hello"
    body  = "World"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/posts" `
    -Method Post `
    -ContentType "application/json" `
    -Body $post

# List posts.
Invoke-RestMethod -Uri "http://localhost:8080/posts"

# Stop the app and PostgreSQL.
docker compose down
```

### Try the canary locally

```powershell
# Build the stable and canary images.
docker build -t cicd-demo:v1 .
# Make an application change before building the canary image, if desired.
docker build -t cicd-demo:v2 .

# Start the canary stack with a 90/10 stable/canary traffic split.
# Stop the regular local stack first if it is using port 8080.
docker compose down
$env:STABLE_IMAGE = "cicd-demo:v1"
$env:CANARY_IMAGE = "cicd-demo:v2"
docker compose -f deploy/docker-compose.canary.yml up -d --wait --force-recreate

# Confirm port 8080 is served by the canary stack, not another local app.
$probe = Invoke-RestMethod -Uri "http://localhost:8080/info" -Method Get -ErrorAction Stop
if ($probe.track -notin @("stable", "canary")) {
    throw "Port 8080 is serving track '$($probe.track)', not the canary gateway. Stop any other app using port 8080, then restart the canary stack."
}

# Send 100 requests and check the canary share and API health.
$url = "http://localhost:8080"
$requests = 100
$expectedPercent = 10
$tolerance = 5
$canary = 0
$stable = 0
$errors = 0
$firstError = $null

Write-Host "Sending $requests requests to $url ..."
for ($i = 0; $i -lt $requests; $i++) {
    try {
        $response = Invoke-RestMethod -Uri "$url/info" -Method Get -ErrorAction Stop
        if ($response.track -eq "canary") {
            $canary++
        }
        elseif ($response.track -eq "stable") {
            $stable++
        }
        else {
            $errors++
            if ($null -eq $firstError) {
                $firstError = "Unexpected track value '$($response.track)' from /info."
            }
        }
    }
    catch {
        $errors++
        if ($null -eq $firstError) {
            $firstError = $_.Exception.Message
        }
    }
}

try {
    Invoke-RestMethod -Uri "$url/posts" -Method Get -ErrorAction Stop | Out-Null
}
catch {
    $errors++
    if ($null -eq $firstError) {
        $firstError = $_.Exception.Message
    }
}

$percent = [math]::Floor($canary * 100 / $requests)
$difference = [math]::Abs($percent - $expectedPercent)
Write-Host "stable=$stable canary=$canary errors=$errors canary_share=$percent% (expected about $expectedPercent%)"

if ($errors -gt 0) {
    Write-Host "First request error: $firstError" -ForegroundColor Red
    Write-Host "Check the canary containers with:"
    Write-Host "  docker compose -f deploy/docker-compose.canary.yml ps"
    Write-Host "  docker compose -f deploy/docker-compose.canary.yml logs"
    throw "Canary check failed: $errors request(s) failed. Roll back the canary."
}
if ($difference -gt $tolerance) {
    Write-Host "Check the individual backends and the gateway configuration:"
    Write-Host "  docker compose -f deploy/docker-compose.canary.yml exec stable curl -fsS http://localhost:8080/info"
    Write-Host "  docker compose -f deploy/docker-compose.canary.yml exec canary curl -fsS http://localhost:8080/info"
    Write-Host "  docker compose -f deploy/docker-compose.canary.yml exec gateway nginx -T"
    throw "Canary check failed: traffic share is outside $expectedPercent% +/- $tolerance%."
}
Write-Host "Canary is healthy and safe to promote."

# Stop the canary stack and remove its volumes.
docker compose -f deploy/docker-compose.canary.yml down -v
```

Widen the rollout by editing the weights in `deploy/nginx.conf` (e.g. `5` / `5` = 50 %, `0` / `1` = 100 %).

## GitHub setup

1. Create a repo and push this project to `main`.
2. Docker Hub -> Account settings -> Security -> create an access token.
3. Repo -> Settings -> Secrets and variables -> Actions -> add
   `DOCKERHUB_USERNAME` (your Docker Hub username) and `DOCKERHUB_TOKEN`.
   The CD workflow lowercases the username when building the Docker image name.
4. (For manual approval) Settings -> Environments -> New environment `production`
   -> *Required reviewers* -> add yourself.

```powershell
git init
git add .
git commit -m "CI/CD pipeline"
git branch -M main
git remote add origin https://github.com/<you>/<repo>.git
git push -u origin main
```

What happens on push: **CI Pipeline** runs `mvn clean verify`. If it is green, **CD Pipeline**
starts automatically: build image -> push `cicd-demo:<sha>` -> run stable + canary behind the
gateway -> `verify-canary.sh` (error rate 0 and ~10 % of traffic on canary) -> wait for approval
-> retag the image as `stable` / `latest`. If the canary check fails nothing is promoted (rollback).

## How the contract test works

1. `PostConsumerPactTest` describes what the frontend needs from `GET /posts/{id}` (`id`, `title`, `body`)
   and writes `target/pacts/post-frontend-post-service.json`.
2. `PostProviderPactTest` replays that contract against the real `PostController`.
3. If a backend developer renames `title` to `heading`, step 2 fails and so does the pipeline.

(Surefire is set to `runOrder=alphabetical` so the consumer test always runs before the provider test.)

## Notes

* Actions are updated to `checkout@v4` / `setup-java@v4`; the base image is `eclipse-temurin:17` because
  `openjdk:17` is deprecated on Docker Hub.
* The final "Deploy (Simulated)" step in `cd.yml` is where `ssh`, `kubectl` or a cloud deploy command goes.
* Next steps (Future Scope): Kubernetes (Argo Rollouts / Flagger for real canaries), Terraform, cloud CI/CD.



test
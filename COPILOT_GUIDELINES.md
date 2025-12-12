# GitHub Copilot Development Guidelines

## Critical Rules - Follow These Always

### 1. API Endpoint Standards
- **ALL endpoints MUST use `/api/` prefix**
- Standard format: `/api/{resource}` (e.g., `/api/companies`, `/api/deliveries`)
- Never create endpoints without the `/api/` prefix

### 2. Git Workflow
- **NEVER commit without testing the project first**
- Always run `./gradlew build -x test` before committing
- Test endpoints after making changes
- Use clear, feature-by-feature commit messages
- Format: `feat:`, `fix:`, `chore:`, `test:`, `docs:`

### 3. Docker Usage
- **We use Docker for everything**
- PostgreSQL runs in Docker (port 5433 → 5432)
- API runs in Docker (port 8081)
- Use `docker exec -i delivery-postgres psql -U postgres -d deliverydb` for database operations
- Never suggest installing PostgreSQL locally

### 4. Docker Deployment
- **Always ensure Docker uses the latest code**
- After code changes: `docker compose build --no-cache api && docker compose up -d`
- For clean rebuild: `docker compose down && docker compose up -d --build`
- Check if containers are running: `docker ps`

### 5. Testing After Changes
- Test endpoints with curl after deployment
- Use token from `new_token.json` file
- Format: `TOKEN=$(cat new_token.json | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')`
- Always verify response matches expected format

### 6. Terminal Commands
- **CRITICAL**: Always type full commands correctly - no missing first character!
- Use bash/Git Bash terminal (Windows environment)
- Use `&&` to chain commands
- Prefer `docker compose` (new) over `docker-compose` (old)

## Project-Specific Details

### Ports
- API: `http://localhost:8081`
- PostgreSQL: `localhost:5433` (external) → `5432` (internal)
- Adminer: `http://localhost:8080`

### Database
- Container name: `delivery-postgres`
- Database: `deliverydb`
- User: `postgres`
- Password: `postgres`

### Key Files
- Authentication tokens: `new_token.json`
- Docker config: `docker-compose.yml`
- Main controller: `src/main/java/com/delivery/deliveryapi/controller/`
- Migrations: `migration-*.sql` (kebab-case naming)

### Response Format Standards
- Company endpoints return IDs only (categoryId, provinceId, districtId)
- No redundant display name fields
- Frontend fetches display names separately from master data

### Common Mistakes to Avoid
1. ❌ Creating endpoints without `/api/` prefix
2. ❌ Committing before testing/building
3. ❌ Suggesting local PostgreSQL installation
4. ❌ Using cached Docker builds after code changes
5. ❌ Missing the first character in terminal commands
6. ❌ Not testing endpoints after deployment

## Build & Deploy Workflow

```bash
# 1. Make code changes
# 2. Build and test
./gradlew build -x test

# 3. Deploy with Docker (force rebuild)
docker compose build --no-cache api && docker compose up -d

# 4. Wait for startup (5-10 seconds)
sleep 8

# 5. Test the endpoints
TOKEN=$(cat new_token.json | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
curl -s "http://localhost:8081/api/your-endpoint" -H "Authorization: Bearer $TOKEN"

# 6. If tests pass, commit
git add <files>
git commit -m "feat: clear description of what changed"
```

## Documentation Standards
- Create API documentation for frontend team when adding/changing endpoints
- Include request/response examples
- Document all fields with types and nullable flags
- Include error responses with status codes
- Provide frontend integration examples

## Questions to Ask Before Committing
1. ✅ Did I test with `./gradlew build -x test`?
2. ✅ Did I rebuild Docker with latest code?
3. ✅ Did I test the endpoints with curl?
4. ✅ Is the commit message clear and descriptive?
5. ✅ Does the endpoint use `/api/` prefix?

---

**Last Updated:** December 12, 2025

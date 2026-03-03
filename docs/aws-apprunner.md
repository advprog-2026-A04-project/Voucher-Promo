# Deploy to AWS (App Runner + ECR + RDS MySQL)

This repo already ships a production-ready `Dockerfile` that bundles:
- React (built via Vite) as static assets
- Spring Boot API (serves the static UI + REST endpoints)

Recommended AWS setup for a demo/staging deployment:
- Push the Docker image to **Amazon ECR** via GitHub Actions
- Run it on **AWS App Runner** (auto-deploy from ECR)
- Use **Amazon RDS MySQL** for the database

## 0) Branching Model (Staging -> Production)

- `staging` branch: staging environment
- `main` branch: production environment

Recommended flow:
1. Feature branches open PRs into `staging`.
2. Only `staging` opens PR into `main` for production releases.

## 1) One-time AWS Setup

### A. Create an ECR repository
Create a repo (example name: `voucher-promo`).

You will use this as `${ECR_REPOSITORY}` in GitHub.

### B. Create the database (RDS MySQL)
Create an RDS MySQL instance and note:
- DB host (endpoint)
- DB name
- DB user + password

The backend reads these environment variables:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- Optional: `DB_SSL_MODE` (default `PREFERRED`)
- Optional: `ADMIN_TOKEN`, `APP_TIME_ZONE`, `PORT`

### C. Create an App Runner service (from ECR)
In App Runner:
1. Source: Container registry -> Amazon ECR.
2. Create **two** services (recommended):
   - Staging service: image tag `staging`
   - Production service: image tag `prod`
3. Enable auto-deployments (so every new image pushed to the configured tag redeploys).
4. Set runtime environment variables (see section 3).

If your RDS is private (recommended), configure an **App Runner VPC connector** so the service can reach RDS securely.

## 2) One-time GitHub Setup (for CI/CD to ECR)

Workflow: `.github/workflows/deploy-aws.yml`

### A. Create an IAM role for GitHub Actions (OIDC)
Create an IAM Role that trusts GitHub Actions OIDC and has permissions to push to ECR.

Minimum permissions typically include:
- `ecr:GetAuthorizationToken`
- ECR push actions on your repository (e.g. `ecr:PutImage`, `ecr:UploadLayerPart`, etc.)

### B. Add GitHub Environments + Variables
Create 2 GitHub Environments:
- `aws-staging`
- `aws-prod`

For each environment, set **Variables**:
- `AWS_REGION` (e.g. `ap-southeast-1`)
- `AWS_ROLE_TO_ASSUME` (IAM role ARN created above)
- `ECR_REPOSITORY` (e.g. `voucher-promo`)
- `APPRUNNER_SERVICE_ARN` (optional, if you want the workflow to call `start-deployment`)

## 3) App Runner Environment Variables (runtime)

Set these in the App Runner service:
- `DB_HOST`: RDS endpoint hostname (no `jdbc:` prefix)
- `DB_PORT`: usually `3306`
- `DB_NAME`: e.g. `voucherpromo`
- `DB_USER`: e.g. `app`
- `DB_PASSWORD`: your DB password
- `DB_SSL_MODE`: optional (use `REQUIRED` if your DB enforces TLS)
- `ADMIN_TOKEN`: optional (default `dev-admin-token`)
- `APP_TIME_ZONE`: optional (e.g. `Asia/Jakarta`)
- `PORT`: optional (default `8080`)

## 4) Deploy

1. Merge PR into `staging` to deploy staging automatically.
2. When ready, open a PR from `staging` -> `main` and merge to deploy production automatically.

Notes:
- If `AWS_REGION` / `AWS_ROLE_TO_ASSUME` / `ECR_REPOSITORY` are not set yet, the deploy workflow will exit successfully but will skip pushing/deploying until you configure them.

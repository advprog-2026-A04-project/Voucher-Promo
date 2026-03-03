# Deploy to AWS (App Runner + ECR + RDS MySQL)

This repo already ships a production-ready `Dockerfile` that bundles:
- React (built via Vite) as static assets
- Spring Boot API (serves the static UI + REST endpoints)

Recommended AWS setup for a demo/staging deployment:
- Push the Docker image to **Amazon ECR** via GitHub Actions
- Run it on **AWS App Runner** (auto-deploy from ECR)
- Use **Amazon RDS MySQL** for the database

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
2. Image: point to your ECR repo, tag `latest`.
3. Enable auto-deployments (so every `:latest` push redeploys).
4. Set runtime environment variables (see section 3).

If your RDS is private (recommended), configure an **App Runner VPC connector** so the service can reach RDS securely.

## 2) One-time GitHub Setup (for CI/CD to ECR)

Workflow: [publish-ecr.yml](/.github/workflows/publish-ecr.yml)

### A. Create an IAM role for GitHub Actions (OIDC)
Create an IAM Role that trusts GitHub Actions OIDC and has permissions to push to ECR.

Minimum permissions typically include:
- `ecr:GetAuthorizationToken`
- ECR push actions on your repository (e.g. `ecr:PutImage`, `ecr:UploadLayerPart`, etc.)

### B. Add GitHub Environment variables
In GitHub, create an environment named `aws-staging` and set **Variables**:
- `AWS_REGION` (e.g. `ap-southeast-1`)
- `AWS_ROLE_TO_ASSUME` (IAM role ARN created above)
- `ECR_REPOSITORY` (e.g. `voucher-promo`)

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

1. Merge PR that contains your changes to `main`.
2. Run the GitHub Action: `Publish Docker Image (AWS ECR)` (manual `workflow_dispatch`).
3. App Runner will auto-deploy the new `:latest` image.


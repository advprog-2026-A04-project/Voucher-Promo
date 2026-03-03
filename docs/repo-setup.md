# Repo Setup (staging + main + PR-only merges)

This guide describes the intended branching + merge rules:
- No squash merges
- No rebase merges
- No fast-forward merges (use merge commits)
- No force pushes
- All changes land via Pull Request
- Only `staging` is allowed to merge into `main`

## 1) Branch Rename / Default Branch

Recommended order:
1. Merge the workflow changes PR first (so CI/CD works for both branches).
2. Rename the current `main` branch to `staging`.
3. Create a new `main` branch (production) from `staging`.
4. Set the default branch to `main`.

GitHub UI steps (repo admin required):
1. Settings -> Branches -> rename `main` to `staging`.
2. Create branch `main` from `staging`.
3. Settings -> Branches -> set default branch to `main`.

## 2) Enforce Merge Method Rules

GitHub UI steps:
1. Settings -> General -> Pull Requests
2. Enable: "Allow merge commits"
3. Disable: "Allow squash merging"
4. Disable: "Allow rebase merging"

## 3) Branch Protection / Rulesets (Checks Required)

Create protections (or a ruleset) for both `staging` and `main`:
- Require a pull request before merging
- Require status checks to pass before merging
- Do not allow force pushes

Suggested required checks:
- `Backend (Gradle tests + JaCoCo)` (from `.github/workflows/ci.yml`)
- `Frontend (lint + build)` (from `.github/workflows/ci.yml`)
- `pmd` (from `.github/workflows/pmd.yml`)

For the `main` branch, also require:
- `policy` (from `.github/workflows/pr-policy.yml`)

Note: The exact check names shown in GitHub are derived from workflow job names. If you do not see a check name in the UI, trigger a PR once so GitHub learns the check name.

## 4) Enforce "Only staging -> main"

This repo includes a PR policy workflow:
- `.github/workflows/pr-policy.yml`

It fails any PR targeting `main` unless the head branch is exactly `staging`.

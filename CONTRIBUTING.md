# Contributing to ThrillhouseBot-test

## Feature Branch Workflow

Every new feature must follow this process:

1. **Create a branch** from `main`:
   ```bash
   git checkout main
   git pull origin main
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes** on the branch.

3. **Push the branch** and open a pull request:
   ```bash
   git push -u origin feature/your-feature-name
   gh pr create --title "Your feature description" --body "Description of changes"
   ```

4. **Wait for review** and CI checks to pass before merging.

> ⚠️ Never commit directly to `main`. All changes go through a branch and pull request.

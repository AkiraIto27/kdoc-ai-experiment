# Publication candidate and release procedure

Current state: the independently accepted initial snapshot was published as commit `a5d562ac59ddac2b6d4e3207cf877d472cbfec40`. HTTP acceptance 8/8, static acceptance of all 2640 pages, and the initial publication boundary passed. Pages main /docs deployment completed successfully; public JSON returned HTTP 200 with matching hashes, and the adapter fetched the default 50 products. HTTP-variant LLM measurements remain unperformed. This status-only followup is awaiting independent review before its own commit/push.

## Candidate boundary

Only this prepared repository is a publication source. `.publication-allowlist` lists every approved relative file path; `final-publication-manifest.json` records their bytes and SHA-256. The manifest excludes its own hash to avoid a circular hash. Recompute both after any approved change, including an independent-test fix or a publication-state update.

Measured conditions stay under `conditions/A` and `conditions/E`. Their 67 public files remain byte-identical to the measured initial trees; six common `.tools` files are explicitly excluded in `source-manifest.json`. `variants/http-pages` is separate and unmeasured. Synthetic source JSON and its dependency-free generator are included. Private graders, raw conversations, execution logs, credentials, local configuration, caches, and compiled classes are not publication inputs.

`.gitignore` prevents common local artifacts from normal staging. It cannot inspect file contents. A final content scan and allowlist/hash check are therefore required. Do not use `git add .`, force-add ignored files, import the original `.git`, reuse the original Git history, or copy files from the private audit directory.

## Existing authentication route

The connected GitHub account and a read-only GitHub CLI user lookup both identify `AkiraIto27`. The CLI lookup required leaving the network-restricted sandbox. No credential value was requested or displayed, and no login, token creation, credential-file read, authentication configuration change, or scope change was performed.

The available connector does not expose repository creation or Pages management tools. The proposed route is the existing `gh` authentication, standard HTTPS Git, and GitHub REST Pages endpoints. The required classic `repo` permission was confirmed in read-only API response metadata. No permission was added. Actual writes have not been probed; no probe repository or site will be created.

For [personal repository creation](https://docs.github.com/en/rest/repos/repos#create-a-repository-for-the-authenticated-user), classic OAuth/PAT permissions are `public_repo` or `repo` for a public repository; fine-grained permissions are repository Administration write or Repository creation write. HTTPS push requires write access to the new repository's contents. For [Pages creation](https://docs.github.com/en/rest/pages/pages#create-a-github-pages-site), classic tokens require `repo`; fine-grained tokens require Pages write and Administration write. If the existing route is insufficient, stop and report the rejected action, target and response. Do not change authentication, broaden scopes, add an app installation, or change any existing private repository's visibility.

## Procedure after independent acceptance

1. Record the separate acceptance result and refresh public status, allowlist and hashes. Check that all expected A/E and JSON hashes still match.
2. Recheck that `AkiraIto27/kdoc-ai-experiment` does not exist under the authenticated user. The earlier exact repository GET returned 404; do not rely on an old name check or reuse an existing repository.
3. Initialize a fresh `.git` **inside this prepared repository**. Verify Git's top-level directory equals this repository before staging. Use an empty init template and avoid importing hooks or any original history. Never run a broad Git add while the prepared directory could resolve to an ancestor repository.
4. Stage only allowlisted paths, using literal NUL-delimited pathspecs from standard input. Compare staged paths with the allowlist and reject additions, ignored files or hash mismatches. Create one new root commit using an appropriate existing/public Git identity. The original repository is never a source of Git objects or history.
5. Create a new public personal repository named `AkiraIto27/kdoc-ai-experiment`, without remote auto-init/license templates. Use `gh repo create AkiraIto27/kdoc-ai-experiment --public --source . --remote origin` from this newly initialized candidate only; do not use `--push` before reviewing the staged/root commit.
6. Push only the new `main` history to the newly created remote. Standard Git may use `gh auth git-credential` as a command-local credential helper if needed; do not run `gh auth token`, manually retrieve credentials, or persist an authentication configuration change. No force push.
7. Create Pages through `POST /repos/AkiraIto27/kdoc-ai-experiment/pages`, with `build_type=legacy` and source `branch=main, path=/docs`. Use `gh api` with a structured request body file and the already configured authentication. If HTTPS enforcement needs an ordinary Pages update after certificate issuance, use the same repository's Pages endpoint. No custom domain, runner, billing or account-wide setting is required.
8. Read the Pages deployment result and verify the published HTTPS root, manifest and representative pages by live GET. This is publication smoke verification, not an LLM experiment. Record the observed URL and hashes, then refresh publication status honestly. An HTTP adapter's complete contract and cancellation tests belong to the independent acceptance suite.

The workflow above intentionally has no automatic publish script or scheduled deployment. The initial release followed independent acceptance. Any changed candidate is reviewed and sealed again before push. Standard public-repository/Pages hosting fits [GitHub's free offering](https://docs.github.com/en/pages/getting-started-with-github-pages/what-is-github-pages), within its published limits. No paid resource or broader permission setting is part of this plan.

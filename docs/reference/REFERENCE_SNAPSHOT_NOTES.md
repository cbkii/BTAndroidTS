# Reference snapshot notes

This pack is a documentation/reference overlay. The sandbox that prepared it could inspect web references, but container-level DNS failed for `git clone`. Because of that, full source snapshots are fetched by `docs/reference/fetch-reference-repos.sh` in your real checkout.

Recommended workflow:

```bash
cd ~/repos/BTAndroidTS
unzip BTAndroidTS-reference-pack.zip -d .
bash docs/reference/fetch-reference-repos.sh
find docs/reference/repos -maxdepth 2 -name SNAPSHOT_COMMIT.txt -print -exec cat {} \;
```

If you previously ran the flawed script and it created nested Git checkouts under `docs/reference/repos/*/src`, refresh them as plain source snapshots:

```bash
bash docs/reference/fetch-reference-repos.sh --force
```

Commit policy options:

1. Commit only docs and manifests. This is lighter and lets agents fetch upstream refs when needed.
2. Commit source snapshots under `docs/reference/repos/*/src`. This supports offline agents, but increases repo size and requires licence preservation.

The fetcher now exports from temporary clones using `git archive` where available, so `src` contains tracked source files but no nested `.git` directory.

Before adapting any code from upstream, inspect the upstream licence and keep attribution.

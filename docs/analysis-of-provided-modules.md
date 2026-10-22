# Analysis of the two supplied audio modules

Both archives contain `system/product/media/audio/ui/Lock.ogg` and `Unlock.ogg`. The v2.1 archive has more extensive `post-fs-data.sh` checks, including `selfmount` and existing-mount detection. The older archive attempts file bind mounts directly and logs failed mounts, but does not reject a pre-existing mount based on inode checks.

The module comparison alone does not prove that LSPosed is needed or that HyperOS intentionally blocks replacement. First verify the mount table, hashes visible at `/product/media/audio/ui`, `settings get global lock_sound` and `unlock_sound`, and SystemUI's actual load calls. The prototype's hooks are meant to gather that evidence.

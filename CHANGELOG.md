# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.2.0] - 2025-12-26
### :sparkles: New Features
- [`84c7684`](https://github.com/WinDanesz/Menhir/commit/84c76848e6e5533ea3100076e30371af497446d9) - Added a new arrow_salvage passive ability. Upon hitting an entity with an arrow, the ability has a configurable chance (1-100%) to instantly return the arrow. `passive_daily_uses` limits the maximum amount this can happen a day (-1 = infinite). The Archer now has this new passive ability. *(commit by [@WinDanesz](https://github.com/WinDanesz))*
- [`1a7da26`](https://github.com/WinDanesz/Menhir/commit/1a7da26c9566d978747731f0b56e5ea369df5c93) - Added an inventory button for the Birthsign menu *(commit by [@WinDanesz](https://github.com/WinDanesz))*
- [`0a6f6e2`](https://github.com/WinDanesz/Menhir/commit/0a6f6e26634b525d9ce43eee3338b6f9d5cd12f1) - The Charmer: updated passive ability, now has 0.5 Luck *(commit by [@WinDanesz](https://github.com/WinDanesz))*
- [`c564948`](https://github.com/WinDanesz/Menhir/commit/c5649488f0edf7f8a3140857a1c84c76476b146d) - Added mark_and_recall ability. The Lodestone: Added passive ability: 50% knockback resistance. New active ability: Waystone (mark_and_recall): can teleport to a marked location once a day within 500 blocks. *(commit by [@WinDanesz](https://github.com/WinDanesz))*
- [`c5094be`](https://github.com/WinDanesz/Menhir/commit/c5094bef254784a38c70148a329ec4cc78a57c1e) - Added wiki page at https://windanesz.github.io/Menhir *(commit by [@WinDanesz](https://github.com/WinDanesz))*

### :bug: Bug Fixes
- [`edba147`](https://github.com/WinDanesz/Menhir/commit/edba147651572b9be7cf46bd78a90ebf4c7ee71a) - Fixed attribute modifier abilities not working in many cases or with 3rd party mods attributes *(commit by [@WinDanesz](https://github.com/WinDanesz))*
- [`37f8bc0`](https://github.com/WinDanesz/Menhir/commit/37f8bc0006e841e24061aa26813197e7e62a55a5) - Fixed crash on dedicated servers *(commit by [@WinDanesz](https://github.com/WinDanesz))*

### :wrench: Chores
- [`b163c8f`](https://github.com/WinDanesz/Menhir/commit/b163c8fdddf22936cc7463c225229bad6c36c4d5) - Bump dependency versions *(commit by [@WinDanesz](https://github.com/WinDanesz))*
- [`9783a41`](https://github.com/WinDanesz/Menhir/commit/9783a41cf3a58a3c45adcec83040a468aa0f3b6c) - update example *(commit by [@WinDanesz](https://github.com/WinDanesz))*


## [v1.0.4] - 2025-09-29
### :bug: Bug Fixes
- [`5f8c2b2`](https://github.com/WinDanesz/Menhir/commit/5f8c2b27381f6d8be41f6edf8f56869c43829c36) - Fixed custom birthsign loading. use path config/menhir/some_json.json *(commit by [@WinDanesz](https://github.com/WinDanesz))*
- [`abbf9fa`](https://github.com/WinDanesz/Menhir/commit/abbf9fa7948d9f28b034102636eb68f406a4da91) - Fixed crash with blaze passive *(commit by [@WinDanesz](https://github.com/WinDanesz))*

[v1.0.4]: https://github.com/WinDanesz/Menhir/compare/v1.0.3...v1.0.4
[v1.2.0]: https://github.com/WinDanesz/Menhir/compare/v1.1.0...v1.2.0

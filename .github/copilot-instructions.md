# GitHub Copilot Context

## Guidelines

- Never make changes to files in the `build` directory. These are temporary files.
- Don't introduce new Interfaces when it can be solved in a simpler way such as an abstract class or lambda.

### Developing Abilities
- When adding new abilities, ensure to document them in `.docs/abilities.md` with their type and properties.
- Follow the existing structure and formatting in the documentation for consistency.
- Abilities must be registered in `com.windanesz.menhir.api.Birthsign.EffectType` and `com.windanesz.menhir.ability.AbilityFactory#FACTORIES`
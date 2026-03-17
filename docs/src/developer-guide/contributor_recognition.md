# Contributor Recognition

GeoServer Cloud uses the [All Contributors](https://allcontributors.org) specification to recognize contributions of all kinds. While we primarily use the All Contributors bot for automated recognition, maintainers can manually add or update contributors using the CLI.

## How to add contributors

Install the CLI (one-time setup):

```bash
npm install --save-dev all-contributors-cli
```

To add a new contributor or add new contribution types to an existing contributor:

```bash
npx all-contributors-cli add <github-username> <contribution-types>
```

For example, to add a user who contributed code, documentation, tests, and bug reports:

```bash
npx all-contributors-cli add username code,doc,test,bug
```

This command will:
1.  Fetch the user's profile from GitHub.
2.  Update the `.all-contributorsrc` configuration file at the project root.
3.  Update the contributors list in `docs/src/community/contributors.md`.

### Generating the Contributors List

If you manually edit `.all-contributorsrc`, you can regenerate the contributors list in the documentation:

```bash
npx all-contributors-cli generate
```

## Contribution Types

We recognize a wide range of contribution types. For a full list of types and their corresponding emojis, see the [All Contributors Emoji Key](https://allcontributors.org/docs/en/emoji-key).

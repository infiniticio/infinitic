# webui

## Project setup

### Requirements

In order to build and run the project you will need the following:

- NodeJS 12.12+
- Yarn 1.22+

### Install dependencies

This will install the dependencies that are required in order to build and run the project.

```sh
yarn install
```

### Compiles and hot-reloads for development

This will build and run the project with a configuration suitable for development purposes.

```sh
yarn serve
```

### Compiles and minifies for production

This will build and run the project with a configuration suitable for production purposes.

```sh
yarn build
```

### Lints and fixes files

This will lint the project files.

```sh
yarn lint
```

## Development environment setup

### Recommended setup

The recommended code editor to work on this project is [VSCode](https://code.visualstudio.com/).

The following extensions are also recommended:

- [EditorConfig](https://marketplace.visualstudio.com/items?itemName=EditorConfig.EditorConfig): extension to automatically set some VSCode settings
  based on the `.editorconfig` file.
- [markdownlint](https://marketplace.visualstudio.com/items?itemName=DavidAnson.vscode-markdownlint): extension providing linting capabilities for
  Markdown files.
- [Vetur](https://marketplace.visualstudio.com/items?itemName=octref.vetur): extension providing some tooling for VueJS.
- [Prettier](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode): extension providing code formatting capabilities.
- [Tailwind CSS IntelliSense](https://marketplace.visualstudio.com/items?itemName=bradlc.vscode-tailwindcss): extension providing code completion for TailwindCSS.

The following VSCode settings are also recommended:

- "editor.formatOnSave": true,

## Customize configuration

See [Configuration Reference](https://cli.vuejs.org/config/).

## Application environment variables

There are some application environment variables available to tweak the behavior of the application for some precise use-case.

### Use API mocks

To develop the WebUI without a fully working setup, it is possible to use the `VUE_APP_USE_API_MOCKS` environment variable.
Set this variable to `true` to make sure the WebUI will mocks requests to the API and will be able to get mocked data to display components instead of real data.

```sh
VUE_APP_USE_API_MOCKS=true yarn serve
```

# Infinitic package template

This is a package template that you can copy to bootstrap new node packages.
It's a default TSDX bootstrapped package with some changes for Lerna integration.

In order to use it, go to the `packages` folder and do the following commands:

```sh
cp -R .package-template my-new-package
```

Then, edit the `package.json` file inside the `packages/my-new-package` directory and change occurrences of `package-template` to `my-new-package`.

Finally, add the new package as a dependency in the root `package.json` file.

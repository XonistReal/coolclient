# Security

## Is Cool Client safe to run?

You shouldn't have to take anyone's word for it, so here is what you can check without reading code:

- **Downloads are built by GitHub, not on someone's PC.** Every release is built by the public
  [release workflow](.github/workflows/release.yml) from the tagged source, and GitHub attaches a
  signed provenance record to each file. `gh attestation verify <file> --repo <owner>/<repo>`
  proves a file came from this repository and that workflow, unmodified.
- **Checksums.** Each release lists SHA-256 hashes in `SHA256SUMS.txt`. Compare with
  `certutil -hashfile <file> SHA256` on Windows.
- **No obfuscation, no hidden downloads.** The mod jars are plain, readable Java. The client
  doesn't download or run any code at runtime; the launcher only downloads Minecraft, Java and
  the official mod loaders (see [PRIVACY.md](PRIVACY.md)).
- **Your password never touches Cool Client.** Microsoft sign-in happens on microsoft.com.
  The Cool Client server verifies accounts the same way Minecraft servers do, so it never
  receives your Microsoft or Minecraft token.
- **No cheats.** Every module is visual, informational or quality of life, so using it
  doesn't put your account at risk on servers that allow clients.

## Reporting a vulnerability

Please don't open a public issue for security problems. Use GitHub's private vulnerability
reporting instead: the **Security** tab of this repository, then **Report a vulnerability**. We
aim to reply within 72 hours and will credit you in the release notes unless you'd rather we didn't.

## Release signing checklist (maintainers)

1. Tag the commit (`git tag v0.x.y && git push --tags`); the release workflow builds a draft.
2. Code-sign `Cool Client.exe` before publishing (SignPath's free open-source program or Azure
   Trusted Signing) so Windows SmartScreen doesn't warn.
3. Scan the files on VirusTotal and link the results in the release notes.
4. Publish the draft. Never upload files built on a personal machine.

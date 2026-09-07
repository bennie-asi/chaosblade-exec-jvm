![logo](https://chaosblade.oss-cn-hangzhou.aliyuncs.com/doc/image/chaosblade-logo.png)

# Chaosblade-exec-jvm: Chaosblade executor for chaos experiments on Java applications


## Introduction
The project is a chaosblade executor based on [jvm-sandbox](https://github.com/alibaba/jvm-sandbox) for chaos 
experiments on Java applications by enhancing classes. The drill can be implemented through the blade cli, see 
[chaosblade](https://github.com/chaosblade-io/chaosblade) project for details.


## Compiling
You can view the help using the following command in the project root directory
```bash
make help
```

## Branches, CI and releases (bennie-asi fork)

`master` is the integration branch. Open pull requests against `master`; PR checks
validate Java 8 and Java 11 compilation, tests, formatting and license headers.
Branch pushes and version tags do not start independent JVM packaging or releases.

Create version tags such as `v1.8.1` from the approved `master` commit. The
[ChaosBlade release workflow](https://github.com/bennie-asi/chaosblade/actions/workflows/release.yml)
checks out the selected JVM tag through `BLADE_EXEC_JVM_BRANCH`, builds this
component from source and publishes the complete distribution. A JVM tag alone
does not publish a new ChaosBlade version; update the selected component tag in
the main repository and release from there.

Historical JVM Releases and Actions artifacts are retained as records. The main
repository does not consume those archives.

## Contributing
We welcome every contribution, even if it is just a punctuation. See details of [CONTRIBUTING](CONTRIBUTING.md)


## Bugs and Feedback
For bug report, questions and discussions please submit [GitHub Issues](https://github.com/chaosblade-io/chaosblade/issues).

DingDing: 23177705

Slack: https://chaosblade-io.slack.com/archives/CRTNFPWE8

Gitter room: [chaosblade community](https://gitter.im/chaosblade-io/community)


## License
Chaosblade-exec-jvm is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for the full license text.

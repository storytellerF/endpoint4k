# 构建与发布验证

使用 JDK 21 和仓库中的 Gradle Wrapper。Android target 还需要包含 platform 36 的 Android SDK。Windows 使用 `gradlew.bat` 代替 `./gradlew`。

## 静态分析（Detekt）

运行 `./gradlew detekt` 检查全部模块，或用 `./gradlew :ktor:ktor-client:detekt` 检查单个模块。普通 Detekt 检查只读源码（`autoCorrect = false`），发现违规时失败，包括可以自动修复的格式违规。可与编译和其他只读分析同时运行。CI 使用 `./gradlew check`，不修正源码。

需要显式应用自动修复时，运行：

```sh
./gradlew detekt --auto-correct
```

现有 `excludeCorrectable: false` 策略可能让修复当次因刚修好的违规仍然失败；修复后重新运行 Detekt 验证结果。

显式自动修复不能与读取同一源码的编译或分析重叠。修复后，手动处理剩余违规，再运行 `./gradlew check` 验证分析、编译和测试。AST 并行保持启用；自动修复不要求改变并行或 Worker API 设置。

规则位于 `config/detekt/detekt.yml`。各模块的 XML、HTML、TXT、SARIF 和 Markdown 报告输出到 `build/reports/detekt/`；合并 SARIF 输出到 `build/reports/detekt/merge.sarif`。

## CI Gradle 缓存

Test 工作流针对 main 与 pull request 中的相关变更运行 `./gradlew check`，也可手动触发以验证缓存。Test 和 Publish 使用 `gradle/actions/setup-gradle@v6`、`cache-provider: enhanced` 和仓库中的 Wrapper；Java setup 只安装 JDK 21。只有 main 上的 Test 运行可写缓存，PR 与标签发布只读恢复。缓存预热不发布产物，也不需要发布凭据。

评估 [issue #13](https://github.com/storytellerF/endpoint4k/issues/13) 时：

1. 记录 main 冷 Test 运行的 URL、提交、缓存恢复／保存结果、check 步骤耗时和作业总耗时，确认 post-job 缓存保存成功。
2. 再次在 main 手动运行 Test，然后基于该 main 提交创建新 PR。确认两者均在 setup-gradle 作业摘要和日志中恢复兼容缓存。即使未精确命中提交键，回退恢复也有价值。
3. 对热 main 和新 PR 记录相同时长。在有代表性的观察期内，记录恢复成功数／尝试数，汇总新增 main 作业时长（runner 分钟），包含缓存开销。比较节省的 PR 时间与增加的计算成本，再决定是否保留 main 缓存生产。
4. 确认 `check` 通过，检查下一次正常发布的 Publish 运行是否恢复缓存并成功签名发布。不要仅为预热或测量缓存而创建 release。

运行链接和测量结果保留在 issue 中。本地工作流验证不能证明 GitHub 缓存复用、发布凭据可用或净运行时间收益。

## 查询解码与响应读取

运行 `./gradlew :http4k:test :okhttp:test --warning-mode all`，验证 http4k 服务器查询解码器和 OkHttp 响应读取器。测试包含重复、缺失和无值的列表查询参数，JSON 与字符串响应，以及按 `String` 或 `Unit` 返回的空响应。

这些部分的静态分析命令为：

```sh
./gradlew :http4k:detekt :http4k:http4k-server:detekt :okhttp:detekt :okhttp:okhttp-client:detekt
```

## 本地 Maven 验证

以下命令将受影响的 JVM 产物、源码、文档、POM 和 Gradle 元数据构建并发布到隔离的本地仓库：

```sh
./gradlew :common:publishJvmPublicationToMavenLocal \
  :http4k:http4k-server:publishToMavenLocal \
  :okhttp:okhttp-client:publishToMavenLocal \
  -x :common:signJvmPublication \
  -x :http4k:http4k-server:signMavenPublication \
  -x :okhttp:okhttp-client:signMavenPublication \
  -Pversion=0.0.0-issue10-SNAPSHOT -Pgroup=io.github.storytellerf \
  -Dmaven.repo.local=/absolute/path/to/temporary-maven-repository \
  --warning-mode all
```

将仓库路径替换为临时目录。本地检查排除签名任务，不验证签名、Central 上传或非 JVM target。标签触发的发布工作流仍保留签名和 Central 发布，并打印全部 Gradle 弃用警告。

## 追踪 Gradle 弃用项

用 `./gradlew help --warning-mode all -Dorg.gradle.deprecation.trace=true` 查看配置期警告；再针对相关测试或发布任务运行同样选项，纳入执行期警告。检查每条堆栈中最先出现的插件或项目帧。

使用 Gradle 9.8.2 验证 issue #10 时，剩余配置警告来自第三方插件：

| 弃用 API | 观察到的来源 | 移除版本 |
| --- | --- | --- |
| `ReportingExtension.file(String)` | Detekt 1.23.8，`DetektPlugin.apply` | Gradle 10 |
| 枚举属性 `KotlinNativeBundleArtifactsTypes` | Kotlin 插件，`KotlinNativeBundleArtifactFormat` | Gradle 10 |
| `Configuration.setVisible(boolean)` | Detekt 和 Android Gradle Plugin | Gradle 11 |

项目任务注册使用 `tasks.register` 和 `TaskProvider.configure`。不要屏蔽剩余插件警告，也不要把 Gradle 9 构建成功当作兼容 Gradle 10 的证据。升级所属插件时重新检查堆栈。Windows 上禁用的 iOS 测试需要在 macOS 运行，与弃用警告无关。

Action 运行时警告必须通过新的 GitHub Actions 运行检查。本地 Gradle 构建无法验证 checkout、Java setup 或 Android setup 使用的运行时。

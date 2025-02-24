#native-image --version
#=>
#native-image 23.0.1 2024-10-15
#GraalVM Runtime Environment Oracle GraalVM 23.0.1+11.1 (build 23.0.1+11-jvmci-b01)
#Substrate VM Oracle GraalVM 23.0.1+11.1 (build 23.0.1+11, serial gc, compressed references)

#uname -a
#=>
#Darwin Mac 24.3.0 Darwin Kernel Version 24.3.0: Thu Jan  2 20:24:23 PST 2025; root:xnu-11215.81.4~3/RELEASE_ARM64_T6031 arm64

rm -r classes
mkdir -p classes
clojure -M -e "(compile 'graalmem.core)"
clojure -M:uberjar --main-class "graalmem.core"
native-image -jar target/graal-memory-test.jar \
        -o target/graal-memory-test \
        -Ob \
        --diagnostics-mode \
        --bundle-create=jfr_issue.nib \
        --enable-monitoring=heapdump,jvmstat,jfr \
        --install-exit-handlers \
        --features=clj_easy.graal_build_time.InitClojureClasses \
        --no-fallback \
        --enable-preview \
        -H:+UnlockExperimentalVMOptions \
        -H:ReflectionConfigurationFiles="$(find ./graal -type f -name 'reflect-config\.json' | paste -s -d ',' -)" \
        -H:ResourceConfigurationFiles="$(find ./graal -type f -name 'resource-config\.json' | paste -s -d ',' -)" \
        -H:+ReportExceptionStackTraces \
        --enable-url-protocols="$(jq -r '.enableUrlProtocols | join(",")' ./graal/config.json)" \
        --initialize-at-build-time="$(jq -r '.initializeAtBuildTime | join(",")' ./graal/config.json)"

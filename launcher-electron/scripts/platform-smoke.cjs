const assert = require("node:assert/strict");
const path = require("node:path");
const {
  javaCandidates,
  parseJavaMajorVersion,
  platformKey,
  scriptName
} = require("../electron/platform.cjs");

assert.equal(platformKey("win32", "x64"), "windows-x64");
assert.equal(platformKey("darwin", "arm64"), "macos-arm64");
assert.equal(platformKey("linux", "x64"), "linux-x64");
assert.equal(scriptName("client", "win32"), "client.bat");
assert.equal(scriptName("client", "linux"), "client");
assert.equal(parseJavaMajorVersion('openjdk version "21.0.11" 2026-04-15'), 21);
assert.equal(parseJavaMajorVersion('java version "1.8.0_402"'), 8);
assert.equal(parseJavaMajorVersion("openjdk 22.0.2"), 22);

const candidates = javaCandidates({
  platform: "darwin",
  arch: "arm64",
  resourcesPath: "/opt/Adventura/resources",
  projectRoot: "/workspace/Adventura",
  env: {
    ADVENTURA_JAVA_HOME: "/custom/jdk",
    JAVA_HOME: "/system/jdk"
  }
});
assert.ok(candidates.some((candidate) => candidate.command.endsWith(path.join("runtime", "macos-arm64", "bin", "java"))));
assert.ok(candidates.some((candidate) => candidate.source === "ADVENTURA_JAVA_HOME"));
assert.ok(candidates.some((candidate) => candidate.source === "JAVA_HOME"));
assert.equal(candidates.at(-1).command, "java");

console.log("platform smoke ok");

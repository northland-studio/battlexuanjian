#!/usr/bin/env bash
# ============================================================================
#  玄剑·战争 BattledXuanjian —— Paper 服务端冒烟测试（GitHub Actions 专用）
#
#  1. 下载最新 Paper 26.2 服务端
#  2. 首次启动服务端并加载插件（验证 plugin.yml / api-version / 指令注册 / 事件监听）
#  3. 通过控制台执行 /bx selftest（插件内置运行期自检）与 /bx status
#  4. 断言关键标记、检查异常堆栈，最后优雅关服
# ============================================================================
set -euo pipefail

WORKDIR="${WORKDIR:-ci/run}"
MC_VERSION="${MC_VERSION:-26.2}"
JAR_DIR="${JAR_DIR:-ci/jar}"
BOOT_TIMEOUT="${BOOT_TIMEOUT:-480}"
SELFTEST_TIMEOUT="${SELFTEST_TIMEOUT:-180}"

log() { printf '\n\033[1;36m[smoke]\033[0m %s\n' "$*"; }
fail() {
  printf '\n\033[1;31m[smoke][FAIL]\033[0m %s\n' "$*"
  if [ -f "$WORKDIR/server.log" ]; then
    printf '\n----- server.log (tail 200) -----\n'
    tail -n 200 "$WORKDIR/server.log" || true
    printf -- '----- end of log -----\n'
  fi
  exit 1
}

# ---------------------------------------------------------------- 定位插件 jar
PLUGIN_JAR=$(ls "$JAR_DIR"/*.jar 2>/dev/null | head -n 1 || true)
[ -n "$PLUGIN_JAR" ] || fail "在 $JAR_DIR 中找不到插件 jar"
log "插件 jar: $PLUGIN_JAR ($(stat -c%s "$PLUGIN_JAR") bytes)"

# ---------------------------------------------------------------- 下载 Paper
mkdir -p "$WORKDIR/plugins"
log "获取 Paper $MC_VERSION 最新构建信息"
METADATA=$(curl -fsSL "https://fill.papermc.io/v3/projects/paper/versions/${MC_VERSION}/builds/latest")
BUILD_ID=$(printf '%s' "$METADATA" | python3 -c 'import json,sys; print(json.load(sys.stdin)["id"])')
DOWNLOAD_URL=$(printf '%s' "$METADATA" | python3 -c 'import json,sys; print(json.load(sys.stdin)["downloads"]["server:default"]["url"])')
SHA256=$(printf '%s' "$METADATA" | python3 -c 'import json,sys; print(json.load(sys.stdin)["downloads"]["server:default"]["checksums"]["sha256"])')
log "Paper $MC_VERSION build $BUILD_ID"
curl -fsSL "$DOWNLOAD_URL" -o "$WORKDIR/paper.jar"
echo "$SHA256  $WORKDIR/paper.jar" | sha256sum -c - || fail "Paper 服务端校验失败"

cp "$PLUGIN_JAR" "$WORKDIR/plugins/"
echo "eula=true" > "$WORKDIR/eula.txt"
cat > "$WORKDIR/server.properties" <<'EOF'
online-mode=false
level-name=world
level-type=minecraft:flat
level-seed=1337
spawn-protection=0
max-players=20
view-distance=4
simulation-distance=4
difficulty=peaceful
gamemode=survival
motd=BattledXuanjian CI
enable-command-block=false
allow-nether=false
sync-chunk-writes=false
EOF

# ---------------------------------------------------------------- 启动服务端
cd "$WORKDIR"
: > server.log
rm -f console.in
mkfifo console.in

log "启动 Paper 服务端（首次启动会生成世界，请稍候）"
java -Xms1G -Xmx2G -Dcom.mojang.eula.agree=true -jar paper.jar --nogui < console.in > server.log 2>&1 &
SERVER_PID=$!
exec 3> console.in

wait_for() { # $1=pattern $2=timeout秒 $3=描述
  local waited=0
  while [ "$waited" -lt "$2" ]; do
    if grep -aq -- "$1" server.log; then
      return 0
    fi
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
      fail "服务端进程已退出，未能等到：$3"
    fi
    sleep 2
    waited=$((waited + 2))
  done
  return 1
}

send() {
  printf '%s\n' "$1" >&3
  log "控制台执行: $1"
}

if ! wait_for 'Done (' "$BOOT_TIMEOUT" '服务端启动完成标记 Done ('; then
  fail "服务端 ${BOOT_TIMEOUT}s 内未完成启动"
fi
log "服务端启动完成，等待插件初始化"
sleep 5

# ---------------------------------------------------------------- 插件自检
grep -aq 'BattledXuanjian v' server.log || fail "插件启用日志缺失（插件可能未加载）"
grep -aq "BattledXuanjian" server.log || fail "日志中未出现 BattledXuanjian"

send "plugins"
send "bx status"
send "bx selftest"
if ! wait_for '\[BX-SELFTEST\] RESULT=' "$SELFTEST_TIMEOUT" '插件自检结果'; then
  fail "插件自检未返回结果（${SELFTEST_TIMEOUT}s 超时）"
fi

# 再跑一次指令链路，验证 tab 补全注册与据点列表分支
send "bx point list"
send "bx help"
sleep 5

# ---------------------------------------------------------------- 关服
send "stop"
exec 3>&-
for _ in $(seq 1 60); do
  if ! kill -0 "$SERVER_PID" 2>/dev/null; then
    break
  fi
  sleep 2
done
if kill -0 "$SERVER_PID" 2>/dev/null; then
  log "服务端未在 120s 内退出，强制结束"
  kill -9 "$SERVER_PID" 2>/dev/null || true
fi
wait "$SERVER_PID" 2>/dev/null || true
cd - >/dev/null

# ---------------------------------------------------------------- 断言
log "检查自检结果"
grep -a '\[BX-SELFTEST\]' "$WORKDIR/server.log" || true

grep -aq '\[BX-SELFTEST\] RESULT=PASS' "$WORKDIR/server.log" \
  || fail "插件运行期自检未通过（期望 [BX-SELFTEST] RESULT=PASS）"

if grep -aq '\[BX-SELFTEST\] RESULT=FAIL' "$WORKDIR/server.log"; then
  fail "插件运行期自检报告 FAIL"
fi

if grep -aq "Could not load 'plugins/BattledXuanjian" "$WORKDIR/server.log"; then
  fail "插件加载失败（Could not load plugins/BattledXuanjian）"
fi

if grep -aE '^\s+at com\.northland\.battledxuanjian' "$WORKDIR/server.log"; then
  fail "日志中出现插件自身的异常堆栈"
fi

grep -aq 'BattledXuanjian v' "$WORKDIR/server.log" || fail "缺少插件启用日志"
grep -aq '玄剑·战争' "$WORKDIR/server.log" || fail "缺少插件启用中文日志（编码可能异常）"

log "冒烟测试通过 ✅"
printf '\n----- server.log (tail 40) -----\n'
tail -n 40 "$WORKDIR/server.log"

#!/bin/sh
set -eu

fail() {
    echo "headless contract failure: $*" >&2
    exit 1
}

LOOM_ROOT="${GRADLE_USER_HOME:-/home/gradle/.gradle}/caches/fabric-loom"
CLIENT_JAR=$(find "$LOOM_ROOT" -type f -path '*/minecraft-clientonly-deobf/26.2/*.jar' | head -n 1 || true)
COMMON_JAR=$(find "$LOOM_ROOT" -type f -path '*/minecraft-common-deobf/26.2/*.jar' | head -n 1 || true)
[ -n "$CLIENT_JAR" ] || fail "Minecraft 26.2 client deobf jar not found"
[ -n "$COMMON_JAR" ] || fail "Minecraft 26.2 common deobf jar not found"
CP="$CLIENT_JAR:$COMMON_JAR"

assert_method() {
    class_name="$1"
    method_name="$2"
    if ! javap -classpath "$CP" -p "$class_name" 2>/dev/null | grep -F "$method_name" >/dev/null; then
        fail "$class_name no longer exposes expected method $method_name"
    fi
}

assert_method net.minecraft.client.multiplayer.ClientPacketListener 'handleBlockUpdate('
assert_method net.minecraft.client.multiplayer.ClientPacketListener 'handleChunkBlocksUpdate('
assert_method net.minecraft.client.multiplayer.ClientPacketListener 'handleForgetLevelChunk('
assert_method net.minecraft.client.multiplayer.ClientPacketListener 'handleBlockChangedAck('
assert_method net.minecraft.client.multiplayer.ClientPacketListener 'handleContainerSetSlot('
assert_method net.minecraft.client.multiplayer.ClientPacketListener 'handleContainerContent('
assert_method net.minecraft.client.multiplayer.ClientPacketListener 'handleSetCursorItem('
assert_method net.minecraft.client.multiplayer.ClientPacketListener 'handleSetPlayerInventory('
assert_method net.minecraft.client.multiplayer.ClientLevel 'syncBlockState('
assert_method net.minecraft.client.gui.screens.inventory.AbstractContainerScreen 'extractSlot('
assert_method net.minecraft.client.gui.screens.inventory.AbstractContainerScreen 'extractCarriedItem('
assert_method net.minecraft.client.gui.render.GuiRenderer 'submitBlitFromItemAtlas('
assert_method net.minecraft.client.renderer.block.ModelBlockRenderer 'tesselateBlock('
assert_method net.minecraft.client.renderer.chunk.SectionCompiler 'compile('
assert_method net.minecraft.client.renderer.chunk.SectionCompiler 'lambda$compile$1('
assert_method net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler 'endPredictionsUpTo('

MOD_JAR=$(find fabric/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-sources.jar' | head -n 1 || true)
[ -n "$MOD_JAR" ] || fail "built Fabric jar not found"

assert_jar_entry() {
    entry="$1"
    jar tf "$MOD_JAR" | grep -F -x "$entry" >/dev/null || fail "jar missing $entry"
}

assert_jar_entry 'fabric.mod.json'
assert_jar_entry 'ghostsync.client.mixins.json'
assert_jar_entry 'assets/ghostsync/shaders/core/item_white_silhouette.fsh'
assert_jar_entry 'com/ghostsync/fabric/client/GhostSyncClient.class'
assert_jar_entry 'com/ghostsync/fabric/client/mixin/ClientPacketListenerMixin.class'
assert_jar_entry 'com/ghostsync/fabric/client/mixin/SectionCompilerMixin.class'
assert_jar_entry 'com/ghostsync/fabric/client/mixin/GuiRendererMixin.class'
assert_jar_entry 'com/ghostsync/fabric/client/mixin/TrackingItemStackRenderStateMixin.class'
assert_jar_entry 'com/ghostsync/fabric/client/render/GhostItemSilhouettePipeline.class'

for mixin in \
    AbstractContainerScreenMixin \
    BlockStatePredictionHandlerMixin \
    ClientLevelMixin \
    ClientPacketListenerMixin \
    GameRendererMixin \
    GuiRendererMixin \
    SectionCompilerMixin \
    TrackingItemStackRenderStateMixin
do
    grep -F "\"$mixin\"" fabric/src/client/resources/ghostsync.client.mixins.json >/dev/null \
        || fail "mixin config no longer registers $mixin"
    assert_jar_entry "com/ghostsync/fabric/client/mixin/$mixin.class"
done

SHADER='fabric/src/client/resources/assets/ghostsync/shaders/core/item_white_silhouette.fsh'
[ -f "$SHADER" ] || fail "silhouette shader source missing"
grep -F 'uniform DynamicTransforms' "$SHADER" >/dev/null || fail 'shader missing DynamicTransforms contract'
grep -F 'uniform sampler2D Sampler0' "$SHADER" >/dev/null || fail 'shader missing Sampler0 contract'
grep -F 'in vec2 texCoord0' "$SHADER" >/dev/null || fail 'shader missing texCoord0 input'
grep -F 'in vec4 vertexColor' "$SHADER" >/dev/null || fail 'shader missing vertexColor input'
grep -F 'fragColor = vec4(ColorModulator.rgb * alpha, alpha);' "$SHADER" >/dev/null \
    || fail 'shader no longer emits premultiplied white'

# Compile the actual GLSL with an independent compiler. This catches syntax/type
# errors that Java/Fabric builds cannot see. -S frag handles Minecraft's .fsh suffix.
command -v glslangValidator >/dev/null 2>&1 || fail 'glslangValidator is not installed in the verification image'
glslangValidator -S frag "$SHADER" >/dev/null \
    || fail 'item white silhouette fragment shader failed standalone GLSL compilation'

# Rendering must remain backend-agnostic: no direct legacy OpenGL calls/imports.
if grep -R -E 'org\.lwjgl\.opengl|\bGL(11|20|30|40|45)\b|glEnable\(|glDisable\(|glBlend' \
        fabric/src/client/java fabric/src/client/resources >/dev/null 2>&1; then
    fail 'direct OpenGL usage detected; keep rendering on Blaze3D/Fabric abstractions'
fi

echo 'Headless Minecraft 26.2 contracts and GLSL verified.'
